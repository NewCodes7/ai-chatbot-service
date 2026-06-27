package com.chatbot.domain.feedback.dto

import com.chatbot.domain.feedback.entity.Feedback
import com.chatbot.domain.feedback.entity.FeedbackStatus
import java.time.Instant

data class FeedbackResponse(
    val id: Long,
    val userId: Long,
    val chatId: Long,
    val isPositive: Boolean,
    val status: FeedbackStatus,
    val createdAt: Instant
) {
    companion object {
        fun from(feedback: Feedback) = FeedbackResponse(
            id = feedback.id,
            userId = feedback.userId,
            chatId = feedback.chatId,
            isPositive = feedback.isPositive,
            status = feedback.status,
            createdAt = feedback.createdAt
        )
    }
}
