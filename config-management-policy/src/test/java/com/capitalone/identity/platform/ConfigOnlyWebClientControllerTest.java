package com.capitalone.identity.platform;

import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.client.dynamic.PollingConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ConfigOnlyWebClientController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {
        ConfigOnlyWebClientControllerTest.StartupTestConfig.class,
})
class ConfigOnlyWebClientControllerTest {

    @Configuration
    static class StartupTestConfig {

        @Bean
        ConfigStoreClient configStoreClient() {
            return ConfigStoreClient.newLocalClient(
                    "web-client-config-store/us_consumers",
                    new PollingConfiguration(Duration.ofDays(1)),
                    ConfigStoreClient_ApplicationEventPublisher.EMPTY
            );
        }
    }

    private static final String VALID_ADDRESS = "us_consumers/sub_lob/config_only_policy";
    private static final String VALID_VERSION = "1.0";
    private static final String VALID_BUSINESS_EVENT_A = "useCase_A_valid";
    private static final String VALID_APP_LEVEL_BUSINESS_EVENT_A = "lob.channel.div.applevel_A_valid";
    private static final String VALID_EXTENDED_APP_LEVEL_BUSINESS_EVENT_A = "lob.channel.div.applevel_A_valid.extended_app_level_A_valid";
    private static final String INVALID_APP_LEVEL_BUSINESS_EVENT_B = "lob.channel.div.app_level_B_invalid";
    private static final String INVALID_EXTENDED_APP_LEVEL_BUSINESS_EVENT_B = "lob.channel.div.applevel_A_valid.extended_app_level_B_invalid";
    private static final String INVALID_BUSINESS_EVENT = "non_existing_usecase";
    @Autowired
    private ConfigOnlyWebClientController controller;

    @Test
    void testHealth() {
        WebTestClient.bindToController(controller).build()
                .get().uri("/health")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FAILED_DEPENDENCY);
    }

    @ParameterizedTest
    @CsvSource({
            VALID_BUSINESS_EVENT_A + ",AAA" + ",",
            VALID_APP_LEVEL_BUSINESS_EVENT_A + ",APP" + ",",
            "''," + "ZZZ" + ",",
            INVALID_BUSINESS_EVENT + ",ZZZ" + ",",
            INVALID_APP_LEVEL_BUSINESS_EVENT_B + ",ZZZ" + ",",
            INVALID_EXTENDED_APP_LEVEL_BUSINESS_EVENT_B + ",APP" + ",",
            VALID_EXTENDED_APP_LEVEL_BUSINESS_EVENT_A + ",EXTENDED" + ",ZZZ",
    })
    void testConfigOnlyPolicyForValidAndInValidUseCases(String useCase, String resultA, String resultB) {
        newWebTestClient(VALID_ADDRESS, VALID_VERSION, useCase)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExampleApiResponse.Success.class)
                .isEqualTo(new ExampleApiResponse.Success(new HashMap<String, Serializable>() {
                    {
                        ArrayList<String> paramArray = new ArrayList<>();
                        paramArray.add("AA");
                        paramArray.add("BB");
                        paramArray.add("CC");
                        put("param-bool", false);
                        put("param-array", paramArray);
                        put("param-num", 0.5);
                        put("param-A", resultA + "_A");
                        put("param-B", resultB == null ? resultA + "_B" : resultB + "_B");
                        put("param-int", 1);
                    }
                }));
    }

    @Test
    void testPolicyInvocationWithRequestData_Success_UsecaseDifferences() {
        Map<String, Serializable> requestBody = Collections.emptyMap();

        newWebTestClient(VALID_ADDRESS, VALID_VERSION, null)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @ParameterizedTest
    @CsvSource({
            "XXXXXXX/YYYY/ZZZZ," + VALID_VERSION,
            VALID_ADDRESS + ",9.9",
            VALID_ADDRESS + ",9",
    })
    void testApiInvocationErrorNotFoundPolicy(String policyAddress, String policyVersion) {
        Map<String, Serializable> requestBody = Collections.emptyMap();
        newWebTestClient(policyAddress, policyVersion, VALID_BUSINESS_EVENT_A)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ExampleApiResponse.ErrorInfo.class)
                .isEqualTo(new ExampleApiResponse.ErrorInfo(
                        ChassisErrorCode.NOT_FOUND,
                        "Not Found",
                        "Not Found"
                ));
    }

    @NotNull
    private WebTestClient.RequestHeadersSpec<?> newWebTestClient(String policyAddress, String policyVersion,
                                                                 String businessEvent) {
        return WebTestClient.bindToController(controller).build()
                .post()
                .uri(uriBuilder -> uriBuilder.path("/resource-foo/{id}/{version}")
                        .build(policyAddress, policyVersion))
                .header("Business-Event", businessEvent);
    }
}
