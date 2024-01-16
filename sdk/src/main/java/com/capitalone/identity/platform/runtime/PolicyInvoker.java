package com.capitalone.identity.platform.runtime;

import com.capitalone.identity.identitybuilder.model.LogicalVersion;
import com.capitalone.identity.platform.versioning.PolicyVersionService;

/**
 * Common entry point for all policy invocations.
 *
 * @param <S> policy execution request object type submitted to {@link PolicyRuntimeContext}
 * @param <T> policy execution result body type
 */
public class PolicyInvoker<S, T> {

    final PolicyRuntimeContext<S, T> context;

    final PolicyVersionService versionService;

    public PolicyInvoker(PolicyRuntimeContext<S, T> context,
                         PolicyVersionService versionService) {
        this.context = context;
        this.versionService = versionService;
    }

    /**
     * @param <R> return type defined by the API host application
     */
    public <R> R invoke(String address, String version, S request, PolicyResultHandler<T, R> handler) {
        LogicalVersion policy = versionService.getPolicyVersion(address, version);
        if (policy == null) {
            return handler.createResponseForMissingPolicy(address, version);
        } else {
            PolicyRequestInfo requestInfo = new PolicyRequestInfo(address, version, policy);
            PolicyResult<T> result = context.invoke(policy, request);
            if (result.getErrorInfo() == null) {
                return handler.createSuccessResponse(requestInfo, result.getResult());
            } else {
                return handler.createErrorResponse(requestInfo, result.getErrorInfo());
            }
        }
    }
}
