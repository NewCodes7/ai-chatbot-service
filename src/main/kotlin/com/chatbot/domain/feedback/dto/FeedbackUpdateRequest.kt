package com.chatbot.domain.feedback.dto

import com.chatbot.domain.feedback.entity.FeedbackStatus

data class FeedbackUpdateRequest(
    val isPositive: Boolean?,
    val status: FeedbackStatus?
)
