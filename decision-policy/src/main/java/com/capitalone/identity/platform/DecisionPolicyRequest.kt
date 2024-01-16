package com.capitalone.identity.platform

import java.io.Serializable

data class DecisionPolicyRequest(
    val body: Map<String, Serializable>,
    val businessEventName: String,
    val dmnName: String,
)
