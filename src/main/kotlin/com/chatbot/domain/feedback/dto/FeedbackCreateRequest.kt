package com.chatbot.domain.feedback.dto

import jakarta.validation.constraints.Positive

data class FeedbackCreateRequest(
    @field:Positive(message = "chatId는 양수여야 합니다")
    val chatId: Long,
    val isPositive: Boolean
)
