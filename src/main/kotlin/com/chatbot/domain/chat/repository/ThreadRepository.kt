package com.chatbot.domain.chat.repository

import com.chatbot.domain.chat.entity.ChatThread
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ThreadRepository : JpaRepository<ChatThread, Long> {
    fun findTopByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId: Long): ChatThread?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<ChatThread>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<ChatThread>
}
