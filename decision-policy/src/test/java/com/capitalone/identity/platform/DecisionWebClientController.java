package com.capitalone.identity.platform;

import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.platform.loading.AggregateEntityLoadListener;
import com.capitalone.identity.platform.loading.PolicyLoadManager;
import com.capitalone.identity.platform.runtime.PolicyErrorInfo;
import com.capitalone.identity.platform.runtime.PolicyInvoker;
import com.capitalone.identity.platform.runtime.PolicyRequestInfo;
import com.capitalone.identity.platform.runtime.PolicyResultHandler;
import com.capitalone.identity.platform.versioning.PolicyVersionEventListener;
import com.capitalone.identity.platform.versioning.PolicyVersionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.Map;

@RestController
public class DecisionWebClientController
        implements PolicyResultHandler<DecisionPolicyResponse, ResponseEntity<ExampleApiResponse>> {

    private final PolicyLoadManager loadManager;
    private final PolicyInvoker<DecisionPolicyRequest, DecisionPolicyResponse> policyInvoker;

    public DecisionWebClientController(ConfigStoreClient client) {
        PolicyVersionService versionService = new PolicyVersionService();
        DecisionPolicyRuntimeContext runtimeContext = new DecisionPolicyRuntimeContext();
        policyInvoker = new PolicyInvoker<>(runtimeContext, versionService);
        AggregateEntityLoadListener publisher = new AggregateEntityLoadListener(
                new PolicyVersionEventListener(versionService));
        loadManager = new PolicyLoadManager(runtimeContext, publisher, client, false);
    }

    @PostConstruct
    void doStart() {
        if (!loadManager.isInitialized()) loadManager.initialize();
    }

    @PreDestroy
    void doStop() {
        loadManager.stop();
    }

    @GetMapping("/health")
    public ResponseEntity<?> getHealthCheck() {
        loadManager.isInitialized();
        return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
    }

    @PostMapping(value = "/resource-foo/{policyAddress}/{policyVersion}")
    public ResponseEntity<ExampleApiResponse> postFooResourceEntity(@RequestParam String dmnName,
                                                                    @PathVariable String policyAddress,
                                                                    @PathVariable String policyVersion,
                                                                    @RequestHeader("Business-Event") String businessEvent,
                                                                    @RequestBody Map<String, Serializable> requestBody) {
        return policyInvoker.invoke(
                policyAddress,
                policyVersion,
                new DecisionPolicyRequest(requestBody, businessEvent, dmnName), this);
    }


    @Override
    public ResponseEntity<ExampleApiResponse> createResponseForMissingPolicy(String requestAddress, String requestVersion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ExampleApiResponse.ErrorInfo(ChassisErrorCode.NOT_FOUND, "Not Found", "Not Found"));

    }

    @Override
    public ResponseEntity<ExampleApiResponse> createSuccessResponse(PolicyRequestInfo requestInfo,
                                                                    DecisionPolicyResponse policyInvocationResult) {
        Map<String, Serializable> okContent = policyInvocationResult.getContent();
        return ResponseEntity.ok(new ExampleApiResponse.Success(okContent));
    }

    @Override
    public ResponseEntity<ExampleApiResponse> createErrorResponse(PolicyRequestInfo requestInfo, PolicyErrorInfo errorInfo) {
        ExampleApiResponse.ErrorInfo error = new ExampleApiResponse.ErrorInfo(errorInfo);
        switch (errorInfo.getError()) {
            case BAD_REQUEST_PARAMETER_RESERVED_PREFIX:
            case BAD_REQUEST_MISSING_DMN_FILE:
                return ResponseEntity.badRequest().body(error);
            case POLICY_EXECUTION_ERROR:
            default:
                return ResponseEntity.ok(new ExampleApiResponse.Failure(error));

        }
    }

}
