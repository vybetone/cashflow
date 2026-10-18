package com.example.data

import java.util.UUID

data class AssistantChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val associatedSymbol: String? = null,
    val signalAction: String? = null,
    val confidence: Int? = null
)
