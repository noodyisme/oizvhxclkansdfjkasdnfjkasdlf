package com.capitalone.identity.platform.runtime;

import com.capitalone.identity.identitybuilder.model.LogicalVersion;

public interface PolicyRuntimeContext<T, R> {

    PolicyResult<R> invoke(LogicalVersion policyVersion, T request);
}
