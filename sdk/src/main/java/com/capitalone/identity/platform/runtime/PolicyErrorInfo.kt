package com.capitalone.identity.platform.runtime

data class PolicyErrorInfo(
    val error: PolicyError, val developerText: String,
) {
    val id = error.id
    val text = error.text
}
