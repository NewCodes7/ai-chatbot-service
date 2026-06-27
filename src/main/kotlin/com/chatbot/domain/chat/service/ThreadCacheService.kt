package com.chatbot.domain.chat.service

import com.chatbot.domain.chat.entity.ChatThread
import com.chatbot.domain.chat.repository.ChatRepository
import com.chatbot.domain.chat.repository.ThreadRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class ThreadCacheService(
    private val threadRepository: ThreadRepository,
    private val chatRepository: ChatRepository
) {
    private val cache = ConcurrentHashMap<Long, Instant>()

    fun getOrCreateThread(userId: Long): ChatThread {
        val lastChatTime = cache[userId]
            ?: chatRepository.findLatestChatTimeByUserId(userId)?.also { cache[userId] = it }

        val createNew = lastChatTime == null ||
            lastChatTime.plusSeconds(1800).isBefore(Instant.now())

        return if (createNew) {
            threadRepository.save(ChatThread(userId = userId))
        } else {
            threadRepository.findTopByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                ?: threadRepository.save(ChatThread(userId = userId))
        }
    }

    fun updateCache(userId: Long, time: Instant) {
        cache[userId] = time
    }

    fun invalidate(userId: Long) {
        cache.remove(userId)
    }
}
