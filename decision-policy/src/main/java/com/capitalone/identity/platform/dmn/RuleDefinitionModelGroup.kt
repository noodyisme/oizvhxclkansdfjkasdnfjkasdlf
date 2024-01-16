package com.capitalone.identity.platform.dmn

data class RuleDefinitionModelGroup(
    /**
     * The full path to these rules' policy in configuration store
     */
    val identifier: String,

    /**
     * The identifier used to lookup these rules at execution time
     */
    val runtimeId: String,

    /**
     * The list of rules
     */
    val ruleDefinitionModelList: List<RuleDefinitionModel>
)