package com.capitalone.identity.platform;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;

public class PolicyCoreSDK implements Lifecycle {
    final EntityManager manager;

    public PolicyCoreSDK(EntityManager manager) {
        this.manager = manager;
        ConfigStoreClient_ApplicationEventPublisher publisher = ConfigStoreClient_ApplicationEventPublisher.EMPTY;
    }

    public final void initialize() {
        manager.initialize();
    }

    public final void shutdown() {
        manager.shutdown();
    }


}
