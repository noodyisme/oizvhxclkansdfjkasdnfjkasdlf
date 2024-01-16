package com.capitalone.identity.platform.runtime

sealed class PolicyResult<R> {

    abstract val status: PolicyResultStatus

    abstract val result: R?

    abstract val errorInfo: PolicyErrorInfo?

    class Success<R>(override val result: R) : PolicyResult<R>() {
        override val status = PolicyResultStatus.SUCCESS
        override val errorInfo = null
    }

    class Failure<R : Any>(override val errorInfo: PolicyErrorInfo) : PolicyResult<R>() {
        override val status = PolicyResultStatus.FAILURE
        override val result = null
    }

}
