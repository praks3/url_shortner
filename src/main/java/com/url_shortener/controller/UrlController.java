package com.url_shortener.controller;

import com.url_shortener.dto.ShortenRequest;
import com.url_shortener.dto.UrlResponse;
import com.url_shortener.entity.Url;
import com.url_shortener.service.analyticservice.AnalyticsService;
import com.url_shortener.service.urlservice.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Create, retrieve, and delete short URLs")
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;

    @Operation(summary = "Create a short URL")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Short URL created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Custom alias already exists"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping(value = "/api/shorten", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UrlResponse> shorten(@Valid @RequestBody ShortenRequest request,
                                               HttpServletRequest httpRequest) {
        Long ownerUserId = (Long) httpRequest.getAttribute("authUserId");
        UrlResponse response = urlService.createShortUrl(request, ownerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Redirect to the original URL for a given short code")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short code not found"),
            @ApiResponse(responseCode = "410", description = "Short URL expired")
    })
    @GetMapping("/{shortCode:[A-Za-z0-9_-]{3,16}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         HttpServletRequest httpRequest) {
        Url url = urlService.resolveShortCode(shortCode);
        analyticsService.recordClick(
                url.getId(),
                clientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getHeader("Referer"));
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url.getOriginalUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @Operation(summary = "Get a short URL record by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping(value = "/api/url/{id}", produces = "application/json")
    public ResponseEntity<UrlResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(urlService.getById(id));
    }

    @Operation(summary = "Delete a short URL by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/api/url/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        urlService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List the current authenticated user's URLs")
    @GetMapping(value = "/api/me/urls", produces = "application/json")
    public ResponseEntity<List<UrlResponse>> myUrls(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("authUserId");
        return ResponseEntity.ok(urlService.findByUserId(userId));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
