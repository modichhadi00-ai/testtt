package com.wormgpt.app.data.model

data class Chat(
    val id: String,
    val userId: String,
    val title: String,
    val updatedAt: Long,
    val createdAt: Long
)
