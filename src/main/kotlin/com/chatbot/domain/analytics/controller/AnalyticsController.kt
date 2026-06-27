package com.chatbot.domain.analytics.controller

import com.chatbot.domain.analytics.dto.ActivityResponse
import com.chatbot.domain.analytics.service.AnalyticsService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AnalyticsController(private val analyticsService: AnalyticsService) {

    @GetMapping("/analytics/activity")
    @PreAuthorize("hasRole('ADMIN')")
    fun getActivity(): ActivityResponse = analyticsService.getActivity()

    @GetMapping("/analytics/report")
    @PreAuthorize("hasRole('ADMIN')")
    fun getReport(): ResponseEntity<String> {
        val csv = analyticsService.getChatCsv()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.csv\"")
            .body(csv)
    }
}
