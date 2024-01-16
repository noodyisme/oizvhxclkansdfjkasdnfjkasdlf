package com.capitalone.identity.platform

import com.capitalone.identity.platform.runtime.PolicyErrorInfo
import com.capitalone.identity.platform.runtime.PolicyResultStatus
import java.io.Serializable
import java.util.*

sealed class ExampleApiResponse {

    data class Success(val results: Map<String, Serializable> = Collections.emptyMap()) : ExampleApiResponse() {
        val policyStatus: PolicyResultStatus = PolicyResultStatus.SUCCESS
    }

    data class Failure(val errorInfo: ErrorInfo = ErrorInfo()) : ExampleApiResponse() {
        val policyStatus: PolicyResultStatus = PolicyResultStatus.FAILURE
    }


    data class ErrorInfo(
        val id: String = "", val text: String = "", val developerText: String? = "",
    ) : ExampleApiResponse() {

        constructor(errorInfo: PolicyErrorInfo) : this(
            errorInfo.id,
            errorInfo.text,
            errorInfo.developerText,
        )

    }

}
