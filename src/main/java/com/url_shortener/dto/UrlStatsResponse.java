package com.url_shortener.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlStatsResponse {

    private Long urlId;
    private long totalClicks;
    private OffsetDateTime lastClickedAt;
    private List<LabelCount> topUserAgents;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LabelCount {
        private String label;
        private long count;
    }
}
