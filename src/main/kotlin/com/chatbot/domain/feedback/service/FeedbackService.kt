package com.chatbot.domain.feedback.service

import com.chatbot.domain.chat.repository.ChatRepository
import com.chatbot.domain.feedback.dto.FeedbackCreateRequest
import com.chatbot.domain.feedback.dto.FeedbackResponse
import com.chatbot.domain.feedback.dto.FeedbackUpdateRequest
import com.chatbot.domain.feedback.entity.Feedback
import com.chatbot.domain.feedback.repository.FeedbackRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val chatRepository: ChatRepository
) {
    @Transactional
    fun createFeedback(userId: Long, req: FeedbackCreateRequest): FeedbackResponse {
        if (!chatRepository.existsById(req.chatId)) {
            throw NoSuchElementException("채팅을 찾을 수 없습니다: ${req.chatId}")
        }
        if (feedbackRepository.existsByUserIdAndChatId(userId, req.chatId)) {
            throw IllegalArgumentException("이미 해당 채팅에 피드백을 남겼습니다")
        }
        val saved = feedbackRepository.save(
            Feedback(userId = userId, chatId = req.chatId, isPositive = req.isPositive)
        )
        return FeedbackResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun listFeedbacks(userId: Long, isAdmin: Boolean, pageable: Pageable): Page<FeedbackResponse> {
        val page = if (isAdmin) feedbackRepository.findAll(pageable)
                   else feedbackRepository.findAllByUserId(userId, pageable)
        return page.map { FeedbackResponse.from(it) }
    }

    @Transactional
    fun updateFeedback(
        userId: Long,
        isAdmin: Boolean,
        feedbackId: Long,
        req: FeedbackUpdateRequest
    ): FeedbackResponse {
        val feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow { NoSuchElementException("피드백을 찾을 수 없습니다: $feedbackId") }

        if (!isAdmin && feedback.userId != userId) {
            throw AccessDeniedException("접근 권한이 없습니다")
        }

        if (isAdmin) {
            req.isPositive?.let { feedback.isPositive = it }
            req.status?.let { feedback.status = it }
        } else {
            if (req.status != null) throw AccessDeniedException("접근 권한이 없습니다")
            req.isPositive?.let { feedback.isPositive = it }
        }

        return FeedbackResponse.from(feedbackRepository.save(feedback))
    }

    @Transactional
    fun deleteFeedback(userId: Long, isAdmin: Boolean, feedbackId: Long) {
        val feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow { NoSuchElementException("피드백을 찾을 수 없습니다: $feedbackId") }

        if (!isAdmin && feedback.userId != userId) {
            throw AccessDeniedException("접근 권한이 없습니다")
        }

        feedbackRepository.delete(feedback)
    }
}
