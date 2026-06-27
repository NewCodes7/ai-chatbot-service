package com.chatbot.domain.chat.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "chats")
class Chat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "thread_id", nullable = false)
    val threadId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    val question: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val answer: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
