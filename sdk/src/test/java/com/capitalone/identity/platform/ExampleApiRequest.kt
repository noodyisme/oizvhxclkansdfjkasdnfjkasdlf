package com.capitalone.identity.platform

import java.io.Serializable
import java.util.HashMap

data class ExampleApiRequest(
    val businessEvent: String = "",
    val policyAddress: String = "",
    val policyVersion: String = "",
    val decisionToExecute: String = "",
    val requestBody: Map<String, Serializable> = HashMap(),
)
