package com.capitalone.identity.platform;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;

public class EntityManager implements Lifecycle {

    final ConfigStoreClient configStoreClient;

    EntityManager(Configuration configuration) {
        this.configStoreClient = configuration.getConfigStoreClient();
    }

    @Override
    public void initialize() {


    }

    @Override
    public void shutdown() {

    }
}
