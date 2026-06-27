package com.chatbot.domain.analytics.service

import com.chatbot.domain.analytics.dto.ActivityResponse
import com.chatbot.domain.chat.repository.ChatRepository
import com.chatbot.domain.user.repository.LoginLogRepository
import com.chatbot.domain.user.repository.UserRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AnalyticsService(
    private val userRepository: UserRepository,
    private val loginLogRepository: LoginLogRepository,
    private val chatRepository: ChatRepository,
    private val jdbcTemplate: JdbcTemplate
) {
    fun getActivity(): ActivityResponse {
        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        return ActivityResponse(
            signupCount = userRepository.countByCreatedAtAfter(since),
            loginCount  = loginLogRepository.countByCreatedAtAfter(since),
            chatCount   = chatRepository.countByCreatedAtAfter(since)
        )
    }

    fun getChatCsv(): String {
        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        val sql = """
            SELECT u.id        AS user_id,
                   u.name      AS user_name,
                   u.email     AS user_email,
                   t.id        AS thread_id,
                   c.id        AS chat_id,
                   c.question,
                   c.answer,
                   c.created_at
            FROM chats c
            JOIN threads t ON c.thread_id = t.id
            JOIN users   u ON t.user_id   = u.id
            WHERE c.created_at >= ?
            ORDER BY c.created_at ASC
        """.trimIndent()

        val rows = jdbcTemplate.queryForList(sql, since)

        val sb = StringBuilder()
        sb.append("user_id,user_name,user_email,thread_id,chat_id,question,answer,created_at\r\n")
        for (row in rows) {
            val fields = listOf(
                row["user_id"].toString(),
                row["user_name"].toString(),
                row["user_email"].toString(),
                row["thread_id"].toString(),
                row["chat_id"].toString(),
                row["question"].toString(),
                row["answer"].toString(),
                row["created_at"].toString()
            )
            sb.append(fields.joinToString(",") { csvEscape(it) })
            sb.append("\r\n")
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String =
        if (value.contains(Regex("""[,"\r\n]""")))
            "\"${value.replace("\"", "\"\"")}\""
        else
            value
}
