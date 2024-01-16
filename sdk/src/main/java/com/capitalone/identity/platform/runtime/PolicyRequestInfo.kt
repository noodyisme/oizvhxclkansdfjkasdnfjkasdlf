package com.capitalone.identity.platform.runtime

import com.capitalone.identity.identitybuilder.model.LogicalVersion

data class PolicyRequestInfo(
    val requestedPolicyAddress: String,
    val requestedPolicyVersion: String,
    val policy: LogicalVersion
)
