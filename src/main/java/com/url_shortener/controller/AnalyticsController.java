package com.url_shortener.controller;


import com.url_shortener.dto.UrlStatsResponse;
import com.url_shortener.service.analyticservice.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Click stats for short URLs")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get click statistics for a short URL by id")
    @GetMapping(value = "/api/url/{id}/stats", produces = "application/json")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getStats(id));
    }
}
