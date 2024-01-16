package com.capitalone.identity.platform.loading;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.function.Supplier;

public class ConfigStoreClientLoader {

    final ConfigStoreClient client;
    final ConfigStoreClient_ApplicationEventPublisher eventPublisher;

    final EntityType firstType;
    final EntityType[] remainingTypes;

    final Scheduler scheduler;

    public ConfigStoreClientLoader(ConfigStoreClient client,
                                   ConfigStoreClient_ApplicationEventPublisher eventPublisher,
                                   Scheduler scheduler,
                                   EntityType type, EntityType... types) {
        this.client = client;
        this.eventPublisher = eventPublisher;
        this.firstType = type;
        this.remainingTypes = types;
        this.scheduler = scheduler;
    }

    public Flux<Entity> load() {
        EntityType[] as = EntityType.values();
        return client.getEntityInfo(as[0], as)
                .map(client::getEntity);
    }

    public Flux<ChangeNotification> monitor(Supplier<List<EntityInfo>> loadedEntities) {
        return Flux.defer(() -> client.getEntityUpdatesBatch(loadedEntities.get(), firstType, remainingTypes))
                .concatMap(updates -> {
                    return Flux.fromIterable(updates)
                            .concatMapDelayError(update -> {
                                switch (update.getType()) {
                                    case UPDATE:
                                        return Flux.just(client.getEntity(update.getEntityInfo()))
                                                .map(ChangeNotification.Update::new);
                                    case ADD:
                                        return Flux.just(client.getEntity(update.getEntityInfo()))
                                                .map(ChangeNotification.Add::new);
                                    case DELETE:
                                        return Flux.just(new ChangeNotification.Delete(update.getEntityInfo()));
                                    default:
                                        throw new IllegalStateException("Unrecognized update type " + update.getType());
                                }
                            });

                });
    }

}
