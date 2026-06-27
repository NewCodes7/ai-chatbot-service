package com.chatbot.domain.feedback.controller

import com.chatbot.domain.feedback.dto.FeedbackCreateRequest
import com.chatbot.domain.feedback.dto.FeedbackResponse
import com.chatbot.domain.feedback.dto.FeedbackUpdateRequest
import com.chatbot.domain.feedback.service.FeedbackService
import com.chatbot.domain.user.entity.Role
import com.chatbot.domain.user.repository.UserRepository
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*

@RestController
class FeedbackController(
    private val feedbackService: FeedbackService,
    private val userRepository: UserRepository
) {

    @PostMapping("/feedbacks")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFeedback(
        @Valid @RequestBody req: FeedbackCreateRequest,
        authentication: Authentication
    ): FeedbackResponse {
        val user = resolveUser(authentication)
        return feedbackService.createFeedback(user.id, req)
    }

    @GetMapping("/feedbacks")
    fun listFeedbacks(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "desc") sort: String
    ): Page<FeedbackResponse> {
        val user = resolveUser(authentication)
        val direction = if (sort == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"))
        return feedbackService.listFeedbacks(user.id, user.role == Role.ADMIN, pageable)
    }

    @PatchMapping("/feedbacks/{id}")
    fun updateFeedback(
        @PathVariable id: Long,
        @RequestBody req: FeedbackUpdateRequest,
        authentication: Authentication
    ): FeedbackResponse {
        val user = resolveUser(authentication)
        return feedbackService.updateFeedback(user.id, user.role == Role.ADMIN, id, req)
    }

    @DeleteMapping("/feedbacks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFeedback(
        @PathVariable id: Long,
        authentication: Authentication
    ) {
        val user = resolveUser(authentication)
        feedbackService.deleteFeedback(user.id, user.role == Role.ADMIN, id)
    }

    private fun resolveUser(auth: Authentication) =
        userRepository.findByEmail(auth.name)
            ?: throw UsernameNotFoundException("User not found: ${auth.name}")
}
