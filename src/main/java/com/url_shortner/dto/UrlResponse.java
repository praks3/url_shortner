package com.url_shortner.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlResponse {
    private Long id;
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
}
