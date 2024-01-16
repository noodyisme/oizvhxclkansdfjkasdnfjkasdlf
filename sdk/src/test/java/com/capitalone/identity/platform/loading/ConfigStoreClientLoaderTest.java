package com.capitalone.identity.platform.loading;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.client.dynamic.PollingConfiguration;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityType;
import org.junit.jupiter.api.Test;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreClientLoaderTest {

    @Test
    void load() {
        /*
        Cases
        - ok
        - empty
        - AWS Error
        - client load error
         */

    }

    @Test
    void monitor() {
        /*
         Cases
         - ok
         internal errors
         - update error 1 of 3 update error (start, middle, end batch update error)
         - add error 1 of 3 update error (start, middle, end batch update error)
         - delete error 1 of 3 update error (start, middle, end batch update error)
         scan errors
         - scan source error
         - scan source never
         - scan source update?
         consumer errors
         - update
         - add
         - delete
         */

        PollingConfiguration properties = new PollingConfiguration(Duration.ofDays(365));
        ConfigStoreClient client = ConfigStoreClient.newLocalClient(
                "web-client-config-store/us_consumers",
                properties,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY
        );

        VirtualTimeScheduler testScheduler = VirtualTimeScheduler.getOrSet();
        ConfigStoreClientLoader loader = new ConfigStoreClientLoader(
                client,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY,
                testScheduler,
                EntityType.POLICY
        );

        List<Entity> block = loader.load().collectList().block();
        assertEquals(2,block.size());
    }
}