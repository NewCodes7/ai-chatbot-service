package com.chatbot.domain.chat.service

import com.chatbot.config.ContentDto
import com.chatbot.config.GeminiClient
import com.chatbot.config.GeminiProperties
import com.chatbot.domain.chat.dto.ChatCreateRequest
import com.chatbot.domain.chat.dto.ChatResponse
import com.chatbot.domain.chat.dto.ThreadWithChatsResponse
import com.chatbot.domain.chat.entity.Chat
import com.chatbot.domain.chat.repository.ChatRepository
import com.chatbot.domain.chat.repository.ThreadRepository
import com.chatbot.domain.rag.service.RagService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant

@Service
class ChatService(
    private val geminiClient: GeminiClient,
    private val props: GeminiProperties,
    private val threadCacheService: ThreadCacheService,
    private val chatRepository: ChatRepository,
    private val threadRepository: ThreadRepository,
    private val ragService: RagService
) {
    fun createChat(userId: Long, req: ChatCreateRequest, emitter: SseEmitter) {
        val modelName = req.model?.also {
            require(it in props.allowedModels) { "허용되지 않은 모델: $it" }
        } ?: props.chatModel

        val thread = threadCacheService.getOrCreateThread(userId)
        val history = chatRepository.findByThreadIdOrderByCreatedAtAsc(thread.id)

        Thread {
            try {
                val embedding = ragService.embedText(req.question)
                val chunks = ragService.searchSimilarChunks(embedding)
                val systemInstruction = buildSystemInstruction(chunks)
                val contents = buildContents(history, req.question)
                val sb = StringBuilder()

                if (req.isStreaming) {
                    geminiClient.generateContentStream(contents, systemInstruction, modelName, props.apiKey) { chunk ->
                        sb.append(chunk)
                        emitter.send(SseEmitter.event().data(chunk))
                    }
                } else {
                    val text = geminiClient.generateContent(contents, systemInstruction, modelName, props.apiKey)
                    sb.append(text)
                    emitter.send(SseEmitter.event().data(text))
                }

                val chat = chatRepository.save(
                    Chat(threadId = thread.id, question = req.question, answer = sb.toString())
                )
                threadCacheService.updateCache(userId, chat.createdAt)
                emitter.complete()
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }.start()
    }

    fun listChats(userId: Long, isAdmin: Boolean, pageable: Pageable): Page<ThreadWithChatsResponse> {
        val threads = if (isAdmin)
            threadRepository.findAllByDeletedAtIsNull(pageable)
        else
            threadRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)

        return threads.map { thread ->
            val chats = chatRepository.findByThreadIdOrderByCreatedAtAsc(thread.id)
            ThreadWithChatsResponse(
                threadId = thread.id,
                chats = chats.map { ChatResponse(it.id, it.question, it.answer, it.createdAt) }
            )
        }
    }

    fun deleteThread(userId: Long, threadId: Long, isAdmin: Boolean) {
        val thread = threadRepository.findById(threadId)
            .orElseThrow { NoSuchElementException("Thread not found: $threadId") }
        if (!isAdmin && thread.userId != userId) throw AccessDeniedException("권한 없음")
        thread.deletedAt = Instant.now()
        threadRepository.save(thread)
        threadCacheService.invalidate(thread.userId)
    }

    private fun buildSystemInstruction(chunks: List<String>): String {
        if (chunks.isEmpty()) return "당신은 도움이 되는 AI 어시스턴트입니다."
        val context = chunks.joinToString("\n\n") { "- $it" }
        return "당신은 증권 전문가 AI 어시스턴트입니다. 아래 참고 문서를 바탕으로 답변하세요:\n\n$context"
    }

    private fun buildContents(history: List<Chat>, question: String): List<ContentDto> =
        buildList {
            history.forEach { chat ->
                add(ContentDto(role = "user", text = chat.question))
                add(ContentDto(role = "model", text = chat.answer))
            }
            add(ContentDto(role = "user", text = question))
        }
}
