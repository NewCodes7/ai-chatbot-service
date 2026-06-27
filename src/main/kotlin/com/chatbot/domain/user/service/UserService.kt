package com.chatbot.domain.user.service

import com.chatbot.domain.user.dto.LoginRequest
import com.chatbot.domain.user.dto.LoginResponse
import com.chatbot.domain.user.dto.SignupRequest
import com.chatbot.domain.user.entity.LoginLog
import com.chatbot.domain.user.entity.Role
import com.chatbot.domain.user.entity.User
import com.chatbot.domain.user.repository.LoginLogRepository
import com.chatbot.domain.user.repository.UserRepository
import com.chatbot.security.JwtTokenProvider
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val loginLogRepository: LoginLogRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {
    fun signup(request: SignupRequest) {
        if (userRepository.findByEmail(request.email) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다")
        }
        userRepository.save(
            User(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                name = request.name,
                role = Role.MEMBER
            )
        )
    }

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")
        }
        loginLogRepository.save(LoginLog(userId = user.id))
        return LoginResponse(token = jwtTokenProvider.createToken(user.id, user.role))
    }
}
