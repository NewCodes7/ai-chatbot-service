package com.chatbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import com.chatbot.config.GeminiProperties

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties::class)
class ChatbotApplication

fun main(args: Array<String>) {
    runApplication<ChatbotApplication>(*args)
}
