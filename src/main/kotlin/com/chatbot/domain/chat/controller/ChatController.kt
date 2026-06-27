package com.chatbot.domain.chat.controller

import com.chatbot.domain.chat.dto.ChatCreateRequest
import com.chatbot.domain.chat.dto.ThreadWithChatsResponse
import com.chatbot.domain.chat.service.ChatService
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class ChatController(
    private val chatService: ChatService,
    private val userRepository: UserRepository
) {
    @PostMapping("/chats")
    fun createChat(
        @Valid @RequestBody req: ChatCreateRequest,
        authentication: Authentication
    ): SseEmitter {
        val user = resolveUser(authentication)
        val emitter = SseEmitter(0L)
        chatService.createChat(user.id, req, emitter)
        return emitter
    }

    @GetMapping("/chats")
    fun listChats(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "desc") sort: String
    ): Page<ThreadWithChatsResponse> {
        val user = resolveUser(authentication)
        val direction = if (sort == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"))
        return chatService.listChats(user.id, user.role == Role.ADMIN, pageable)
    }

    @DeleteMapping("/threads/{threadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteThread(
        @PathVariable threadId: Long,
        authentication: Authentication
    ) {
        val user = resolveUser(authentication)
        chatService.deleteThread(user.id, threadId, user.role == Role.ADMIN)
    }

    private fun resolveUser(auth: Authentication) =
        userRepository.findByEmail(auth.name)
            ?: throw UsernameNotFoundException("User not found: ${auth.name}")
}
