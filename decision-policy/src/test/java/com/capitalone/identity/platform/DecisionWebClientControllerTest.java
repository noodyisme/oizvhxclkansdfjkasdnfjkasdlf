package com.capitalone.identity.platform;

import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.client.dynamic.PollingConfiguration;
import com.capitalone.identity.platform.runtime.PolicyError;
import com.capitalone.identity.platform.runtime.PolicyErrorInfo;
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
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = DecisionWebClientController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {
        DecisionWebClientControllerTest.StartupTestConfig.class,
})
class DecisionWebClientControllerTest {
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
    
    private static final String VALID_ADDRESS = "us_consumers/sub_lob/local_policy";
    private static final String VALID_VERSION = "1.0";
    private static final String VALID_DMN_PARAM_A_REQD = "test_rule_duplicate_1.dmn";
    private static final String VALID_DMN_PARAM_A_NOT_REQD = "test_rule_duplicate_3.dmn";
    private static final String VALID_BUSINESS_EVENT_A = "useCase_A_valid";
    private static final String VALID_USECASE_B = "useCase_B_valid";

    @Autowired
    private DecisionWebClientController controller;

    @Test
    void testHealth() {
        WebTestClient.bindToController(controller).build()
                .get().uri("/health")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FAILED_DEPENDENCY);

    }

    @Test
    void testPolicyInvocationWithRequestData_Success() {
        HashMap<String, Serializable> requestBody = new HashMap<String, Serializable>() {{
            put("param-A", "the quick brown fox");
            put("param-B", "extra unprocessed param");
        }};

        newWebTestClient(VALID_ADDRESS, VALID_VERSION, VALID_DMN_PARAM_A_REQD, VALID_BUSINESS_EVENT_A, requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExampleApiResponse.Success.class)
                .isEqualTo(new ExampleApiResponse.Success(new HashMap<String, Serializable>() {{
                    put("Decision-1", "decision=1.0.0 test_rule_duplicate_1.dmn");
                    put("Decision-2", "fox");
                }}));
    }

    @ParameterizedTest
    @CsvSource({
            VALID_BUSINESS_EVENT_A + ",AAA_A",
            VALID_USECASE_B + ",BBB_A",
            "unrecognizedusecase,ZZZ_A",
    })
    void testPolicyInvocationWithRequestData_Success_UsecaseDifferences(String usecase, String expectedResult) {
        Map<String, Serializable> requestBody = Collections.emptyMap();

        newWebTestClient(VALID_ADDRESS, VALID_VERSION, VALID_DMN_PARAM_A_NOT_REQD, usecase, requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExampleApiResponse.Success.class)
                .isEqualTo(new ExampleApiResponse.Success(new HashMap<String, Serializable>() {{
                    put("Decision-1", "decision=1.0.0 test_rule_duplicate_3.dmn");
                    put("Decision-2", expectedResult);
                }}));
    }

    @Test
    void testPolicyInvocationWithRequestData_Error_ParamTypeErrorDuringDecisionExecution() {
        HashMap<String, Serializable> requestBody = new HashMap<String, Serializable>() {{
            put("param-A", 999);
        }};

        newWebTestClient(VALID_ADDRESS, VALID_VERSION, VALID_DMN_PARAM_A_REQD, VALID_BUSINESS_EVENT_A, requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExampleApiResponse.Failure.class)
                .isEqualTo(new ExampleApiResponse.Failure(
                        new ExampleApiResponse.ErrorInfo(
                                new PolicyErrorInfo(
                                        PolicyError.POLICY_EXECUTION_ERROR,
                                        "Decision execution error."))
                ));
    }

    @ParameterizedTest
    @CsvSource({
            "XXXXXXX/YYYY/ZZZZ," + VALID_VERSION,
            VALID_ADDRESS + ",9.9",
            VALID_ADDRESS + ",9",
    })
    void testApiInvocationErrorNotFoundPolicy(String policyAddress, String policyVersion) {
        Map<String, Serializable> requestBody = Collections.emptyMap();
        newWebTestClient(policyAddress, policyVersion, VALID_DMN_PARAM_A_NOT_REQD, VALID_BUSINESS_EVENT_A, requestBody)
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

    @Test
    void testPolicyInvocationWithRequestData_InvalidDMN() {
        Map<String, Serializable> requestBody = Collections.emptyMap();

        newWebTestClient(VALID_ADDRESS, VALID_VERSION, "test_rule_duplicate_ABCD.dmn", VALID_BUSINESS_EVENT_A, requestBody)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ExampleApiResponse.ErrorInfo.class)
                .isEqualTo(new ExampleApiResponse.ErrorInfo(
                        new PolicyErrorInfo(
                                PolicyError.BAD_REQUEST_MISSING_DMN_FILE,
                                "Could not find dmn with name 'test_rule_duplicate_ABCD.dmn' in requested policy.")
                ));
    }

    @Test
    void testApiInvocationWithIllegalReservedParamInBody() {

        HashMap<String, Serializable> requestBody = new HashMap<String, Serializable>() {{
            put("config.illegalParamA", "VALUE-A");
        }};

        newWebTestClient(VALID_ADDRESS, VALID_VERSION, VALID_DMN_PARAM_A_NOT_REQD, VALID_BUSINESS_EVENT_A, requestBody)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ExampleApiResponse.ErrorInfo.class)
                .isEqualTo(new ExampleApiResponse.ErrorInfo(
                        "787400",
                        "Reserved parameter prefix used in request body key.",
                        "config.illegalParamA"
                ));

    }

    @NotNull
    private WebTestClient.RequestHeadersSpec<?> newWebTestClient(String policyAddress, String policyVersion,
                                                                 String dmnName, String businessEvent,
                                                                 Map<String, Serializable> requestBody) {
        WebTestClient.RequestHeadersSpec<?> dmnName1 = WebTestClient.bindToController(controller).build()
                .post()
                .uri(uriBuilder -> uriBuilder.path("/resource-foo/{id}/{version}")
                        .queryParam("dmnName", dmnName)
                        .build(policyAddress, policyVersion))
                .header("Business-Event", businessEvent)
                .body(Mono.just(requestBody), Map.class);
        return dmnName1;
    }
}
