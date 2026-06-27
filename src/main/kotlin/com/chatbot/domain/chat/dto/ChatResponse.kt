package com.chatbot.domain.chat.dto

import java.time.Instant

data class ChatResponse(
    val id: Long,
    val question: String,
    val answer: String,
    val createdAt: Instant
)

data class ThreadWithChatsResponse(
    val threadId: Long,
    val chats: List<ChatResponse>
)
