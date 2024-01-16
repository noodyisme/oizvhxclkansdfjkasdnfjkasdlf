package com.capitalone.identity.platform.loading;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClientImpl;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.client.s3.ConfigStoreClientS3Configuration;
import com.capitalone.identity.identitybuilder.client.s3.S3ItemStore;
import com.capitalone.identity.identitybuilder.repository.CommonItemStore;
import com.capitalone.identity.identitybuilder.repository.EntityProvider;
import com.capitalone.identity.identitybuilder.repository.ItemStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PolicyLoadManagerTestIT {

    @Mock
    PolicyLoadTarget loadTarget;

    @Mock
    EntityLoadEvents_ApplicationEventPublisher eventPublisher;

    @Mock
    ConfigStoreClient_ApplicationEventPublisher configStoreClientEventPublisher;
    Clock fixed = Clock.fixed(Instant.now(), ZoneId.of("UTC"));

    PolicyLoadManager manager;

    ItemStore store;

    static String configStoreUUID;

    @BeforeAll
    static void createTestId() {
        configStoreUUID = "deleteme-lite-IT-" + UUID.randomUUID().toString();
    }

    @BeforeEach
    void setUp() {
        boolean isPipeline = System.getenv("BUILD_ID") != null;
        if (isPipeline) System.out.printf("SystemEnv:=%s%n", System.getenv());
        String profileName = isPipeline ? null : "GR_GG_COF_AWS_668484372489_Developer";
        boolean isAwsProxyEnabled = !isPipeline;
        ConfigStoreClientS3Configuration configuration = new ConfigStoreClientS3Configuration(
                "identitybuilder-core-testbed-configstore-dev-e1-gen3",
                configStoreUUID,
                Regions.US_EAST_1,
                profileName,
                isAwsProxyEnabled
        );

        store = new S3ItemStore(configuration);
        EntityProvider provider = new EntityProvider(
                store,
                Flux::never,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY
        );
        ConfigStoreClientImpl configStoreClientImpl = new ConfigStoreClientImpl(provider);


        manager = new PolicyLoadManager(
                loadTarget, eventPublisher, configStoreClientImpl, fixed,
                false);
    }

    @Test
    void initialize() {

        manager.initialize();

        verify(eventPublisher, Mockito.never()).publishEvent((Failure) any());
        verify(eventPublisher, Mockito.never()).publishEvent((NonLoadingError) any());
        verify(eventPublisher, Mockito.never()).publishEvent((Unloaded) any());
        verify(eventPublisher, Mockito.never()).publishEvent((Loaded) any());

    }

    @Test
    void initialize_error() {
        ConfigStoreClientS3Configuration configuration = new ConfigStoreClientS3Configuration(
                "identitybuilder-core-testbed-NON-EXISTENT-BUCKET",
                configStoreUUID,
                Regions.US_EAST_1,
                null,
                false
        );

        CommonItemStore store = new S3ItemStore(configuration);
        EntityProvider provider = new EntityProvider(
                store,
                Flux::never,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY
        );
        ConfigStoreClientImpl configStoreClientImpl = new ConfigStoreClientImpl(provider);


        PolicyLoadManager manager = new PolicyLoadManager(
                loadTarget, eventPublisher, configStoreClientImpl, fixed,
                false);

        assertThrows(SdkClientException.class, () -> manager.initialize());
        assertFalse(manager.isInitialized());
    }
}
