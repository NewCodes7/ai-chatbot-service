package com.chatbot.domain.feedback.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "feedbacks")
class Feedback(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "chat_id", nullable = false)
    val chatId: Long,

    @Column(name = "is_positive", nullable = false)
    var isPositive: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FeedbackStatus = FeedbackStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class FeedbackStatus { PENDING, APPROVED, REJECTED }
