package com.capitalone.identity.platform.loading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class AggregateEntityLoadListener implements EntityLoadEvents_ApplicationEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(AggregateEntityLoadListener.class);
    EntityLoadEvents_ApplicationEventPublisher[] listeners;

    public AggregateEntityLoadListener(EntityLoadEvents_ApplicationEventPublisher... listeners) {
        this.listeners = listeners;
    }

    private void executeOnAll(Consumer<EntityLoadEvents_ApplicationEventPublisher> consumer) {
        for (EntityLoadEvents_ApplicationEventPublisher publisher : listeners) {
            try {
                consumer.accept(publisher);
            } catch (Exception e) {
                logger.error("Entity Load Event consumer error.", e);
            }
        }
    }

    @Override
    public void publishEvent(Failure event) {
        executeOnAll(publisher -> publisher.publishEvent(event));
    }

    @Override
    public void publishEvent(Loaded event) {
        executeOnAll(publisher -> publisher.publishEvent(event));
    }

    @Override
    public void publishEvent(Unloaded event) {
        executeOnAll(publisher -> publisher.publishEvent(event));
    }

    @Override
    public void publishEvent(NonLoadingError event) {
        executeOnAll(publisher -> publisher.publishEvent(event));
    }

}
