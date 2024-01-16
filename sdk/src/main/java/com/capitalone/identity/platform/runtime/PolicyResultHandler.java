package com.capitalone.identity.platform.runtime;

/**
 * Host applications define this handler to supply responses for common policy invocation result categories:
 * <ol>
 *     <li>Successful invocation of the policy with no errors</li>
 *     <li>Policy invoked but there was an error</li>
 *     <li>Policy was missing and could not be invoked</li>
 * </ol>
 *
 * @param <T> result of a policy invocation defined by implementation of {@link PolicyRuntimeContext}
 * @param <R> a common return type defined by the API host application
 */
public interface PolicyResultHandler<T, R> {

    /**
     * @param requestAddress address used for policy lookup
     * @param requestVersion version used for policy lookup
     * @return A see {@link PolicyResultHandler}
     */
    R createResponseForMissingPolicy(String requestAddress, String requestVersion);

    R createSuccessResponse(PolicyRequestInfo requestInfo, T policyInvocationResult);

    R createErrorResponse(PolicyRequestInfo requestInfo, PolicyErrorInfo errorInfo);
}
