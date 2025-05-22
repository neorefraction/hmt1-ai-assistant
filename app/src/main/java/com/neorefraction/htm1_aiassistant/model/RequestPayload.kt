package com.neorefraction.htm1_aiassistant.model

import kotlinx.serialization.Serializable

@Serializable
data class RequestPayload(
    val messages: List<Message>,
    val max_tokens: Int = 100,
    val stream: Boolean = false
)