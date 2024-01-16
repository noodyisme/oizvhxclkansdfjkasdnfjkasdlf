package com.capitalone.identity.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class PolicyCoreSDKTest {

    @Mock
    EntityManager manager;

    private PolicyCoreSDK policyCoreSDK;

    @BeforeEach
    void setup() {
        policyCoreSDK = new PolicyCoreSDK(manager);
    }

    @Test
    void initialize() {
        policyCoreSDK.initialize();
        verify(manager).initialize();
    }

    @Test
    void shutdown() {
        policyCoreSDK.shutdown();
        verify(manager).shutdown();
    }
}
