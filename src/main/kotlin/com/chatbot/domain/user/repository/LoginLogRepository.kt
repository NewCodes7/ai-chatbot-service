package com.chatbot.domain.user.repository

import com.chatbot.domain.user.entity.LoginLog
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface LoginLogRepository : JpaRepository<LoginLog, Long> {
    fun countByCreatedAtAfter(since: Instant): Long
}
