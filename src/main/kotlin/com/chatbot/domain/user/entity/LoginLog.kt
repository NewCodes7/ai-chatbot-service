package com.chatbot.domain.user.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "login_logs")
class LoginLog(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
