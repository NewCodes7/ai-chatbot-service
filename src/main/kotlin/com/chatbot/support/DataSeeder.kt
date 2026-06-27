package com.chatbot.support

import com.chatbot.domain.rag.service.RagService
import com.chatbot.domain.user.entity.Role
import com.chatbot.domain.user.entity.User
import com.chatbot.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataSeeder(
    private val userRepository: UserRepository,
    private val ragService: RagService,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    // Korean/mixed text approximation: 1 token ≈ 2 characters
    private val CHUNK_CHARS = 512 * 2          // ~1024 chars per chunk
    private val OVERLAP_CHARS = (CHUNK_CHARS * 0.20).toInt()  // ~205 chars overlap
    private val STRIDE_CHARS = CHUNK_CHARS - OVERLAP_CHARS     // ~819 chars stride

    override fun run(args: ApplicationArguments) {
        seedUsers()
        seedDocumentChunks()
    }

    private fun seedUsers() {
        if (userRepository.count() > 0) return
        log.info("Seeding test users...")
        userRepository.saveAll(
            listOf(
                User(email = "member@test.com", password = passwordEncoder.encode("member1234"), name = "테스트 멤버", role = Role.MEMBER),
                User(email = "admin@test.com", password = passwordEncoder.encode("admin1234"), name = "테스트 관리자", role = Role.ADMIN)
            )
        )
        log.info("Test users created — member@test.com (MEMBER), admin@test.com (ADMIN)")
    }

    private fun seedDocumentChunks() {
        if (ragService.countChunks() > 0) return
        log.info("Seeding document chunks from classpath:docs/ (chunk=${CHUNK_CHARS}chars, overlap=${OVERLAP_CHARS}chars)...")

        val chunks = loadChunksFromDocs()
        if (chunks.isEmpty()) {
            log.warn("No document chunks found — check src/main/resources/docs/")
            return
        }

        log.info("Found ${chunks.size} chunks from ${chunks.map { it.second }.distinct().size} documents")
        chunks.forEachIndexed { index, (text, source) ->
            try {
                val embedding = ragService.embedText(text)
                ragService.insertChunk(text, embedding, source, index)
                log.info("  Embedded chunk ${index + 1}/${chunks.size}: $source (${text.length} chars)")
            } catch (e: Exception) {
                log.warn("  Failed to embed chunk [$source]: ${e.message}")
            }
        }
        log.info("Document seeding complete — ${chunks.size} chunks stored")
    }

    private fun loadChunksFromDocs(): List<Pair<String, String>> {
        val resolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources("classpath:docs/*.md")

        return resources.flatMap { resource ->
            val source = resource.filename?.removeSuffix(".md") ?: return@flatMap emptyList()
            val content = resource.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val cleaned = stripMarkdown(content)
            slidingWindowChunks(cleaned, source)
        }
    }

    private fun stripMarkdown(content: String): String =
        content
            .replace(Regex("^---.*?---\\s*", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("^#{1,6}\\s+.*$", RegexOption.MULTILINE), "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    /**
     * Sliding window chunking at sentence boundaries.
     * Sentences are split on Korean/English period endings and newlines.
     * Each chunk targets CHUNK_CHARS with OVERLAP_CHARS overlap from the previous chunk.
     */
    private fun slidingWindowChunks(text: String, source: String): List<Pair<String, String>> {
        val sentences = text
            .split(Regex("(?<=[.。!?\\n])\\s*"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val chunks = mutableListOf<Pair<String, String>>()
        var startSentenceIdx = 0

        while (startSentenceIdx < sentences.size) {
            val sb = StringBuilder()
            var endSentenceIdx = startSentenceIdx

            // accumulate sentences until CHUNK_CHARS reached
            while (endSentenceIdx < sentences.size && sb.length < CHUNK_CHARS) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(sentences[endSentenceIdx])
                endSentenceIdx++
            }

            val chunkText = sb.toString().trim()
            if (chunkText.length >= 80) {
                chunks.add(chunkText to source)
            }

            if (endSentenceIdx >= sentences.size) break

            // advance by STRIDE_CHARS worth of sentences for next chunk
            var advancedChars = 0
            while (startSentenceIdx < endSentenceIdx && advancedChars < STRIDE_CHARS) {
                advancedChars += sentences[startSentenceIdx].length + 1
                startSentenceIdx++
            }
            // guard against infinite loop if a single sentence exceeds CHUNK_CHARS
            if (startSentenceIdx == 0) startSentenceIdx = endSentenceIdx
        }

        return chunks
    }
}
