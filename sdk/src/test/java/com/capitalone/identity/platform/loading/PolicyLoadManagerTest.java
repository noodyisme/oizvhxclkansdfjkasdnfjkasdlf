package com.capitalone.identity.platform.loading;

import com.amazonaws.SdkClientException;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClientImpl;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.client.s3.ConfigStoreClientS3Configuration;
import com.capitalone.identity.identitybuilder.client.s3.S3ItemStore;
import com.capitalone.identity.identitybuilder.events.ApplicationEvent;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.identitybuilder.repository.CommonItemStore;
import com.capitalone.identity.identitybuilder.repository.EntityProvider;
import com.capitalone.identity.identitybuilder.repository.ItemStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test Requirement
 * <p>
 * 1. Distinguish between initialize/Load cases
 * case 1: single policy load failure
 * case 2: aws connection error
 */
@ExtendWith(MockitoExtension.class)
class PolicyLoadManagerTest {

    @Spy
    PolicyLoadTarget loadTarget = new TestLoadTarget();

    @Mock
    EntityLoadEvents_ApplicationEventPublisher eventPublisher;

    private final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));

    private final TestScanRequester testScanRequester = new TestScanRequester();

    PolicyLoadManager newTestLoadManager(ItemStore store) {
        return newTestLoadManager(newTestClient(store), false);
    }

    ConfigStoreClient newTestClient(ItemStore store) {
        EntityProvider provider = new EntityProvider(
                store,
                testScanRequester,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY
        );
        return new ConfigStoreClientImpl(provider);
    }

    PolicyLoadManager newTestLoadManager(ConfigStoreClient client, boolean isStrictStartupMode) {
        return new PolicyLoadManager(
                loadTarget, eventPublisher, client, fixedClock,
                isStrictStartupMode);
    }

    Entity.Policy newDecisionPolicy() {
        Set<ConfigStoreItem> items = Stream.of(
                new ConfigStoreItem("a/b/c/1.0/rules/blank.dmn", TestContent.DMN_BLANK),
                new ConfigStoreItem("a/b/c/1.0/policy-metadata.json", TestContent.POLICY_METADATA_DECISION_AVAILABLE)
        ).collect(Collectors.toSet());
        return new Entity.Policy(
                new EntityInfo.Policy(
                        new PolicyDefinition("a/b/c", "1.0"),
                        items.stream().map(item -> item.info).collect(Collectors.toSet())),
                items
        );
    }

    @Test
    void constructNoOpVersion() {
        PolicyLoadManager loadManager = assertDoesNotThrow(() -> new PolicyLoadManager());
        assertDoesNotThrow(loadManager::initialize);
    }

    @Test
    void initialize_success() {
        Entity.Policy policy = newDecisionPolicy();
        InMemoryItemStore store = new InMemoryItemStore(policy);
        PolicyLoadManager manager = newTestLoadManager(store);

        assertFalse(manager.isInitialized());
        assertDoesNotThrow(manager::initialize);
        assertTrue(manager.isInitialized());
        // second call expected to throw
        assertThrows(IllegalStateException.class, manager::initialize);

        // events
        Loaded expectEvent = new Loaded(policy, EntityState.Delta.ChangeType.ADD, true, new ApplicationEvent.Metadata(fixedClock));
        verify(eventPublisher, Mockito.only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);

        // complete lifecycle of object
        assertDoesNotThrow(manager::stop);

    }

    @Test
    void initialize_errorStrictStartupMode() {
        Entity.Policy policy = newDecisionPolicy();
        InMemoryItemStore store = new InMemoryItemStore(policy);
        PolicyLoadManager manager = newTestLoadManager(newTestClient(store), true);

        RuntimeException exception = new RuntimeException("test");
        doThrow(exception).when(loadTarget).load(any());

        Failure expectEvent = new Failure(EntityState.Delta.ChangeType.ADD,
                exception,
                true,
                policy.getInfo(),
                new ApplicationEvent.Metadata(fixedClock));

        assertThrows(PolicyLoadManager.AggregatedLoadError.class, manager::initialize);
        assertFalse(manager.isInitialized());

        verify(eventPublisher, Mockito.only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);

        assertDoesNotThrow(manager::stop);
    }
    @Test
    void initialize_errorLenientStartupMode() {
        Entity.Policy policy = newDecisionPolicy();
        InMemoryItemStore store = new InMemoryItemStore(policy);
        PolicyLoadManager manager = newTestLoadManager(newTestClient(store), false);

        RuntimeException exception = new RuntimeException("test");
        doThrow(exception).when(loadTarget).load(any());

        Failure expectEvent = new Failure(EntityState.Delta.ChangeType.ADD,
                exception,
                true,
                policy.getInfo(),
                new ApplicationEvent.Metadata(fixedClock));

        assertDoesNotThrow(manager::initialize);
        assertTrue(manager.isInitialized());

        verify(eventPublisher, Mockito.only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);

        assertDoesNotThrow(manager::stop);
    }

    @Test
    void initialize_awsError() {
        ConfigStoreClientS3Configuration s3Config = new ConfigStoreClientS3Configuration(
                "identitybuilder-core-testbed-NON-EXISTENT-BUCKET");
        CommonItemStore store = new S3ItemStore(s3Config);
        PolicyLoadManager manager = newTestLoadManager(store);

        assertThrows(SdkClientException.class, manager::initialize);
        assertFalse(manager.isInitialized());

        verifyNoMoreInteractions(eventPublisher);
        assertDoesNotThrow(manager::stop);
    }

    @Test
    void update_nonItemLoadError() {
        InMemoryItemStore store = new InMemoryItemStore();
        ConfigStoreClient configStoreClient = Mockito.spy(newTestClient(store));
        PolicyLoadManager manager = newTestLoadManager(configStoreClient, false);

        RuntimeException awsException = new RuntimeException("AWS Exception");
        doThrow(awsException).when(configStoreClient).getEntityUpdatesBatch(any(), any(), any());

        manager.initialize();

        NonLoadingError expectEvent = new NonLoadingError(awsException, new ApplicationEvent.Metadata(fixedClock));
        verify(eventPublisher, only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    void update_streamTerminateEventLogged() {
        ConfigStoreClient client = mock(ConfigStoreClient.class);
        PolicyLoadManager manager = newTestLoadManager(client, false);

        when(client.getEntityInfo(any(), any())).thenReturn(Flux.empty());
        when(client.getEntityUpdatesBatch(any(), any(), any())).thenReturn(Flux.empty());
        manager.initialize();

        assertTrue(manager.isInitialized());
        NonLoadingError expectEvent = new NonLoadingError(null, new ApplicationEvent.Metadata(fixedClock));
        verify(eventPublisher, only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);

    }

    @Test
    void update_success() {
        InMemoryItemStore store = new InMemoryItemStore();
        PolicyLoadManager manager = newTestLoadManager(store);
        manager.initialize();

        Entity.Policy policy = newDecisionPolicy();
        store.addEntity(policy);

        testScanRequester.triggerTestScanRequest(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));

        Loaded expectEvent = new Loaded(policy, EntityState.Delta.ChangeType.ADD, false, new ApplicationEvent.Metadata(fixedClock));
        verify(eventPublisher, Mockito.only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);

    }

    @Test
    void update_itemLoadError() {

        InMemoryItemStore store = new InMemoryItemStore();
        PolicyLoadManager manager = newTestLoadManager(store);
        manager.initialize();

        Entity.Policy policy = newDecisionPolicy();
        store.addEntity(policy);

        RuntimeException test = new RuntimeException("test");
        doThrow(test).when(loadTarget).load(any());
        testScanRequester.triggerTestScanRequest(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));

        Failure expectEvent = new Failure(EntityState.Delta.ChangeType.ADD, test, false,
                policy.getInfo(), new ApplicationEvent.Metadata(fixedClock));

        verify(eventPublisher, Mockito.only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);

    }

    @Test
    void delete_success() {

        Entity.Policy policy = newDecisionPolicy();
        InMemoryItemStore store = new InMemoryItemStore(policy);
        PolicyLoadManager manager = newTestLoadManager(store);

        manager.initialize();

        Mockito.clearInvocations(eventPublisher);

        store.removeEntity(policy);

        testScanRequester.triggerTestScanRequest(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));
        Unloaded expectEvent = new Unloaded(policy.getInfo(), EntityState.Delta.ChangeType.DELETE, false, new ApplicationEvent.Metadata(fixedClock));
        verify(eventPublisher, Mockito.only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);

    }

    @Test
    void delete_failure() {

        Entity.Policy policy = newDecisionPolicy();
        InMemoryItemStore store = new InMemoryItemStore(policy);
        PolicyLoadManager manager = newTestLoadManager(store);

        manager.initialize();

        Mockito.clearInvocations(eventPublisher);

        RuntimeException exception = new RuntimeException("test");
        doThrow(exception).when(loadTarget).unload(any());
        store.removeEntity(policy);

        testScanRequester.triggerTestScanRequest(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));
        Failure expectEvent = new Failure(EntityState.Delta.ChangeType.DELETE, exception, false, policy.getInfo(), new ApplicationEvent.Metadata(fixedClock));
        verify(eventPublisher, Mockito.only()).publishEvent(expectEvent);
        verifyNoMoreInteractions(eventPublisher);

    }

}
