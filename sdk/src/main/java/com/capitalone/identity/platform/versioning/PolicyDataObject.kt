package com.capitalone.identity.platform.versioning

import com.capitalone.identity.identitybuilder.model.LogicalVersion
import com.capitalone.identity.identitybuilder.model.PolicyInfo

/**
 * Immutable, comparable, and null-safe implementation of [PolicyInfo.Patch]
 */
data class PolicyDataObject(
    private val policyMinorVersion: Int,
    private val policyMajorVersion: Int,
    private val policyPatchVersion: Int,
    private val policyFullName: String,
) : LogicalVersion {

    companion object {
        @JvmStatic
        fun create(info: LogicalVersion) = PolicyDataObject(
            info.minorVersion,
            info.majorVersion,
            info.patchVersion,
            info.name,
        );
    }

    override fun getName(): String = policyFullName

    override fun getMajorVersion(): Int = policyMajorVersion

    override fun getMinorVersion(): Int = policyMinorVersion

    override fun getPatchVersion(): Int = policyPatchVersion

}