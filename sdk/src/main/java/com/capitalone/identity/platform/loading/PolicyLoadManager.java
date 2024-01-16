package com.capitalone.identity.platform.loading;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.events.ApplicationEvent;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.EntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PolicyLoadManager {

    private static final Logger logger = LogManager.getLogger(PolicyLoadManager.class.getName());
    private final EntityLoadEvents_ApplicationEventPublisher eventPublisher;
    private final PolicyLoadTarget policyLoadTarget;
    private final ConfigStoreClient client;

    private final Clock clock;
    private final boolean isStrictStartupMode;

    private boolean isInitialized = false;
    private Disposable dynamicUpdateSubscription;

    /**
     * Creates a working default implementation that doesn't load anything.
     */
    public PolicyLoadManager() {
        this(new NoOpPolicyLoadTarget(), new NoOpConfigStoreClient());
    }

    public PolicyLoadManager(PolicyLoadTarget loadTarget,
                             ConfigStoreClient client) {
        this(loadTarget, EntityLoadEvents_ApplicationEventPublisher.EMPTY, client, false);
    }

    public PolicyLoadManager(@NotNull PolicyLoadTarget policyLoadTarget,
                             @NotNull EntityLoadEvents_ApplicationEventPublisher publisher,
                             @NotNull ConfigStoreClient configStoreClient,
                             boolean isStrictStartupMode) {
        this(policyLoadTarget, publisher, configStoreClient, Clock.systemUTC(), isStrictStartupMode);
    }

    /**
     * @param policyLoadTarget    the class that handles actual object load and unload
     * @param publisher           application events are sent
     * @param configStoreClient   client object that will be used to access config store
     * @param clock               clock to use when generating application events (useful for testing)
     * @param isStrictStartupMode if true then {@link #initialize()} will fail if there are any entity load errors.
     *                            It is recommended to set 'true' only in dev environments to prevent a single entity
     *                            load operation from preventing initialization of the service.
     */
    public PolicyLoadManager(@NotNull PolicyLoadTarget policyLoadTarget,
                             @NotNull EntityLoadEvents_ApplicationEventPublisher publisher,
                             @NotNull ConfigStoreClient configStoreClient,
                             @NotNull Clock clock,
                             boolean isStrictStartupMode) {
        this.eventPublisher = Objects.requireNonNull(publisher);
        this.policyLoadTarget = Objects.requireNonNull(policyLoadTarget);
        this.client = Objects.requireNonNull(configStoreClient);
        this.clock = Objects.requireNonNull(clock);
        this.isStrictStartupMode = isStrictStartupMode;
    }

    public synchronized void initialize() {

        if (isInitialized) throw new IllegalStateException("Load manager already initialized.");

        List<EntityLoadOperationResult> results = client.getEntityInfo(EntityType.POLICY).toStream()
                .map(info -> tryLoadEntity(EntityState.Delta.ChangeType.ADD, info, true))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        AggregatedLoadError loadError = consolidateLoadErrors(results);
        if (loadError != null && isStrictStartupMode) {
            throw loadError;
        }

        dynamicUpdateSubscription = Flux.defer(() -> client.getEntityUpdatesBatch(
                        policyLoadTarget.getLoadedEntities()
                                .stream().sorted(Comparator.comparing(EntityInfo::getLocationPrefix))
                                .collect(Collectors.toList()),
                        EntityType.POLICY
                ))
                .map(updates -> {
                    List<AggregatedLoadError> errors = new ArrayList<>();
                    for (EntityState.Delta<EntityInfo> update : updates) {
                        List<EntityLoadOperationResult> updateResults = applyUpdate(update);
                        AggregatedLoadError updateError = consolidateLoadErrors(updateResults);
                        if (updateError != null) {
                            errors.add(updateError);
                        }
                    }
                    if (errors.isEmpty()) {
                        return updates;
                    } else {
                        throw new AggregatedLoadError(
                                String.format("Error in Batch Load with following messages: [%s]",
                                        errors.stream()
                                                .map(Throwable::getMessage)
                                                .collect(Collectors.joining(", "))));
                    }
                })
                .doOnError(
                        err -> !(err instanceof AggregatedLoadError),
                        error -> eventPublisher.publishEvent(new NonLoadingError(error, new ApplicationEvent.Metadata(clock)))
                )
                .retryWhen(Retry.backoff(10000, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(60))
                        .jitter(0.4)
                        .transientErrors(true))
                .doOnTerminate(() -> eventPublisher.publishEvent(new NonLoadingError(null, new ApplicationEvent.Metadata(clock))))
                .subscribe();

        isInitialized = true;

    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void stop() {
        if (dynamicUpdateSubscription != null) {
            dynamicUpdateSubscription.dispose();
        }
    }

    private AggregatedLoadError consolidateLoadErrors(List<EntityLoadOperationResult> results) {
        Map<String, Throwable> loadErrorMap = results.stream()
                .filter(Failure.class::isInstance)
                .collect(Collectors.toMap(entityLoadOperationResult -> entityLoadOperationResult.getInfo().getLocationPrefix(),
                        entityLoadOperationResult -> ((Failure) entityLoadOperationResult).getError()));

        if (loadErrorMap.isEmpty()) {
            return null;
        } else {
            String ids = String.join(",", loadErrorMap.keySet());
            AggregatedLoadError err = new AggregatedLoadError("Entity Locations: " + ids);
            loadErrorMap.values().forEach(err::addSuppressed);
            return err;
        }
    }

    private List<EntityLoadOperationResult> applyUpdate(EntityState.Delta<EntityInfo> delta) {
        switch (delta.getType()) {
            case ADD:
            case UPDATE:
                return tryLoadEntity(delta.getType(), delta.getEntityInfo(), false);
            case DELETE:
                return Collections.singletonList(tryDeleteEntity(delta.getEntityInfo()));
            default:
                throw new UnsupportedOperationException(delta.getType().toString());
        }
    }

    private List<EntityLoadOperationResult> tryLoadEntity(EntityState.Delta.ChangeType changeType,
                                                          EntityInfo entityInfo,
                                                          boolean isStartupLoadOperation) {
        EntityInfo curEntityInfo = entityInfo;
        List<EntityLoadOperationResult> loadResultList = new ArrayList<>();
        int entityLoadRetries = 5;
        while (entityLoadRetries-- > 0 && curEntityInfo != null) {
            EntityLoadOperationResult result = tryLoadEntity(changeType, curEntityInfo, isStartupLoadOperation, client::getEntity);
            loadResultList.add(result);
            if (result instanceof Failure) {
                curEntityInfo = curEntityInfo.getPriorVersion();
            } else {
                return loadResultList;
            }
        }
        return loadResultList;
    }

    private EntityLoadOperationResult tryLoadEntity(EntityState.Delta.ChangeType changeType,
                                                    EntityInfo entityInfo,
                                                    boolean isStartupLoadOperation,
                                                    Function<EntityInfo, Entity> factory) {
        final long startTime = clock.millis();
        return tryLoadOperation(entityInfo, changeType, isStartupLoadOperation, () -> {
            final Entity entity = factory.apply(entityInfo);
            policyLoadTarget.load(entity);
            Loaded loaded = new Loaded(entity, changeType, isStartupLoadOperation,
                    new ApplicationEvent.Metadata(startTime, clock.millis()));
            eventPublisher.publishEvent(loaded);
            logger.info(String.format("Entity Loaded [%s]", loaded));
            return loaded;
        });
    }

    private EntityLoadOperationResult tryDeleteEntity(EntityInfo entityInfo) {
        final long startTime = clock.millis();
        return tryLoadOperation(entityInfo, EntityState.Delta.ChangeType.DELETE, false, () -> {
            policyLoadTarget.unload(entityInfo);
            Unloaded unloaded = new Unloaded(entityInfo, new ApplicationEvent.Metadata(startTime, clock.millis()));
            eventPublisher.publishEvent(unloaded);
            logger.info(String.format("Entity Unloaded [%s]", unloaded));
            return unloaded;
        });
    }

    private EntityLoadOperationResult tryLoadOperation(
            EntityInfo entityInfo,
            EntityState.Delta.ChangeType type,
            boolean isStartupLoad, Supplier<EntityLoadOperationResult> loader) {
        final long start = clock.millis();
        try {
            return loader.get();
        } catch (Exception error) {
            Failure event = new Failure(type, error, isStartupLoad, entityInfo,
                    new ApplicationEvent.Metadata(start, clock.millis()));
            logger.info(String.format("Entity Load Failed [%s]", event), error);
            eventPublisher.publishEvent(event);
            return event;
        }
    }


    private static final class NoOpPolicyLoadTarget implements PolicyLoadTarget {

        @Override
        public void load(Entity entity) {
            // no-op do nothing
        }

        @Override
        public void unload(EntityInfo info) {
            // no-op do nothing
        }

        @Override
        public Collection<EntityInfo> getLoadedEntities() {
            return Collections.emptyList();
        }
    }

    private static class NoOpConfigStoreClient implements ConfigStoreClient {
        @Override
        public Entity getEntity(EntityInfo entityInfo) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Flux<EntityInfo> getEntityInfo(EntityType entityType, EntityType... entityTypes) {
            return Flux.empty();
        }

        @Override
        public Flux<List<EntityState.Delta<EntityInfo>>> getEntityUpdatesBatch(List<EntityInfo> list, EntityType entityType, EntityType... entityTypes) {
            return Flux.empty();
        }
    }

    static class AggregatedLoadError extends IllegalStateException {
        AggregatedLoadError(String message) {
            super(message);
        }
    }
}
