package com.chatbot.domain.user.controller

import com.chatbot.domain.user.dto.LoginRequest
import com.chatbot.domain.user.dto.LoginResponse
import com.chatbot.domain.user.dto.SignupRequest
import com.chatbot.domain.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
class UserController(private val userService: UserService) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody request: SignupRequest) {
        userService.signup(request)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        return userService.login(request)
    }
}
