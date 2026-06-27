package com.chatbot.domain.feedback.controller

import com.chatbot.domain.feedback.dto.FeedbackCreateRequest
import com.chatbot.domain.feedback.dto.FeedbackResponse
import com.chatbot.domain.feedback.dto.FeedbackUpdateRequest
import com.chatbot.domain.feedback.service.FeedbackService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class FeedbackController(private val feedbackService: FeedbackService) {

    @PostMapping("/feedbacks")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFeedback(
        @Valid @RequestBody req: FeedbackCreateRequest,
        authentication: Authentication
    ): FeedbackResponse {
        val userId = authentication.principal as Long
        return feedbackService.createFeedback(userId, req)
    }

    @GetMapping("/feedbacks")
    fun listFeedbacks(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "desc") sort: String
    ): Page<FeedbackResponse> {
        val userId = authentication.principal as Long
        val isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" }
        val direction = if (sort == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"))
        return feedbackService.listFeedbacks(userId, isAdmin, pageable)
    }

    @PatchMapping("/feedbacks/{id}")
    fun updateFeedback(
        @PathVariable id: Long,
        @RequestBody req: FeedbackUpdateRequest,
        authentication: Authentication
    ): FeedbackResponse {
        val userId = authentication.principal as Long
        val isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" }
        return feedbackService.updateFeedback(userId, isAdmin, id, req)
    }

    @DeleteMapping("/feedbacks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFeedback(
        @PathVariable id: Long,
        authentication: Authentication
    ) {
        val userId = authentication.principal as Long
        val isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" }
        feedbackService.deleteFeedback(userId, isAdmin, id)
    }
}
