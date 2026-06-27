package com.chatbot.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PageController {

    @GetMapping("/")
    fun index(): String = "index"

    @GetMapping("/login")
    fun login(): String = "login"

    @GetMapping("/signup")
    fun signup(): String = "signup"

    @GetMapping("/app")
    fun app(): String = "app"

    @GetMapping("/feedback")
    fun feedback(): String = "feedback"

    @GetMapping("/analytics")
    fun analytics(): String = "analytics"
}
