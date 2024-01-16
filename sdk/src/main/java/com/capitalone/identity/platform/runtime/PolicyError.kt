package com.capitalone.identity.platform.runtime

enum class PolicyError(val id: String, val text: String = id) {

    BAD_REQUEST_PARAMETER_RESERVED_PREFIX("787400", "Reserved parameter prefix used in request body key."),
    BAD_REQUEST_MISSING_DMN_FILE("787401", "Requested dmn file not found."),
    POLICY_EXECUTION_ERROR("787200", "Error during policy execution."),
    BAD_REQUEST_MISSING_CONFIG("788401", "Requested config could not be found");

    companion object {
        @JvmStatic
        fun fromId(id: String): PolicyError? {
            for (error: PolicyError in PolicyError.values()) {
                if (id == error.id) {
                    return error
                }
            }
            return null
        }
    }
}
