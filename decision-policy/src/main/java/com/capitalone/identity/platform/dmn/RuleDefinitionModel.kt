package com.capitalone.identity.platform.dmn

data class RuleDefinitionModel(
    /**
     * The name of the rule file stripped from the end of the full path (e.g, rule.dmn)
     */
    val ruleShortName: String,

    /**
     * Rule content as a string. Not verified to be in any particular format.
     */
    val content: String
)