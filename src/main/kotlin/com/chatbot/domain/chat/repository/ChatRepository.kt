package com.chatbot.domain.chat.repository

import com.chatbot.domain.chat.entity.Chat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ChatRepository : JpaRepository<Chat, Long> {
    fun findByThreadIdOrderByCreatedAtAsc(threadId: Long): List<Chat>

    @Query("""
        SELECT MAX(c.createdAt) FROM Chat c
        JOIN ChatThread t ON c.threadId = t.id
        WHERE t.userId = :userId AND t.deletedAt IS NULL
    """)
    fun findLatestChatTimeByUserId(@Param("userId") userId: Long): Instant?

    fun countByCreatedAtAfter(since: Instant): Long
}
