package com.chatbot.domain.feedback.repository

import com.chatbot.domain.feedback.entity.Feedback
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackRepository : JpaRepository<Feedback, Long> {
    fun existsByUserIdAndChatId(userId: Long, chatId: Long): Boolean
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<Feedback>
}
