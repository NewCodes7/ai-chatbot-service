package com.chatbot.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class ContentDto(val role: String, val text: String)

@Component
class GeminiClient(
    restClientBuilder: RestClient.Builder,
    private val objectMapper: ObjectMapper
) {
    private val restClient = restClientBuilder
        .baseUrl("https://generativelanguage.googleapis.com/v1beta")
        .build()

    fun generateContent(
        contents: List<ContentDto>,
        systemInstruction: String?,
        modelName: String,
        apiKey: String
    ): String {
        val body = buildRequestBody(contents, systemInstruction)
        val response = restClient.post()
            .uri("/models/$modelName:generateContent?key=$apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map::class.java)
        return extractText(response)
    }

    fun generateContentStream(
        contents: List<ContentDto>,
        systemInstruction: String?,
        modelName: String,
        apiKey: String,
        onChunk: (String) -> Unit
    ) {
        val body = buildRequestBody(contents, systemInstruction)
        restClient.post()
            .uri("/models/$modelName:streamGenerateContent?key=$apiKey&alt=sse")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange { _, response ->
                response.body.bufferedReader().forEachLine { line ->
                    if (line.startsWith("data: ")) {
                        val json = line.removePrefix("data: ").trim()
                        if (json.isNotEmpty()) {
                            try {
                                val tree = objectMapper.readTree(json)
                                val text = tree.path("candidates")[0]
                                    .path("content").path("parts")[0]
                                    .path("text").asText("")
                                if (text.isNotEmpty()) onChunk(text)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
    }

    fun embedContent(text: String, modelName: String, apiKey: String): FloatArray {
        val body = mapOf("content" to mapOf("parts" to listOf(mapOf("text" to text))))
        val response = restClient.post()
            .uri("/models/$modelName:embedContent?key=$apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val values = ((response?.get("embedding") as? Map<*, *>)?.get("values") as? List<*>)
            ?: return FloatArray(0)
        return FloatArray(values.size) { values[it].toString().toFloat() }
    }

    private fun buildRequestBody(contents: List<ContentDto>, systemInstruction: String?): Map<String, Any> {
        val body = mutableMapOf<String, Any>()
        if (!systemInstruction.isNullOrBlank()) {
            body["system_instruction"] = mapOf("parts" to listOf(mapOf("text" to systemInstruction)))
        }
        body["contents"] = contents.map { c ->
            mapOf("role" to c.role, "parts" to listOf(mapOf("text" to c.text)))
        }
        return body
    }

    private fun extractText(response: Map<*, *>?): String {
        @Suppress("UNCHECKED_CAST")
        return ((response?.get("candidates") as? List<*>)
            ?.firstOrNull().let { it as? Map<*, *> }
            ?.get("content").let { it as? Map<*, *> }
            ?.get("parts").let { it as? List<*> }
            ?.firstOrNull().let { it as? Map<*, *> }
            ?.get("text") as? String) ?: ""
    }
}
