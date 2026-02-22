package com.wormgpt.app.data.model

data class Message(
    val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val attachmentUrls: List<String> = emptyList(),
    val createdAt: Long
)
