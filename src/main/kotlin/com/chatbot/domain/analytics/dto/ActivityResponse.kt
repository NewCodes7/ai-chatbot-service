package com.chatbot.domain.analytics.dto

data class ActivityResponse(
    val signupCount: Long,
    val loginCount: Long,
    val chatCount: Long
)
