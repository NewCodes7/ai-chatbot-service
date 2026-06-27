package com.chatbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemini")
class GeminiProperties {
    var apiKey: String = ""
    var chatModel: String = "gemini-3.5-flash"
    var embeddingModel: String = "gemini-embedding-2"
    var allowedModels: List<String> = listOf("gemini-3.5-flash")
}
