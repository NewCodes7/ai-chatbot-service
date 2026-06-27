package com.chatbot.domain.rag.service

import com.chatbot.config.GeminiClient
import com.chatbot.config.GeminiProperties
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class RagService(
    private val geminiClient: GeminiClient,
    private val props: GeminiProperties,
    private val jdbcTemplate: JdbcTemplate
) {
    fun embedText(text: String): FloatArray =
        geminiClient.embedContent(text, props.embeddingModel, props.apiKey)

    fun searchSimilarChunks(embedding: FloatArray): List<String> {
        val vectorStr = "[${embedding.joinToString(",")}]"
        return jdbcTemplate.queryForList(
            "SELECT content FROM document_chunks ORDER BY embedding <=> CAST(? AS vector) LIMIT 3",
            String::class.java,
            vectorStr
        )
    }

    fun insertChunk(content: String, embedding: FloatArray, source: String, chunkIndex: Int) {
        val vectorStr = "[${embedding.joinToString(",")}]"
        jdbcTemplate.update(
            "INSERT INTO document_chunks (content, embedding, source, chunk_index) VALUES (?, CAST(? AS vector), ?, ?)",
            content, vectorStr, source, chunkIndex
        )
    }

    fun countChunks(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM document_chunks", Int::class.java) ?: 0
}
