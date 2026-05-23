package com.url_shortener.service.analyticservice;

import com.url_shortener.dto.UrlStatsResponse;

public interface AnalyticsService {

    void recordClick(Long urlId, String ipAddress, String userAgent, String referer);

    UrlStatsResponse getStats(Long urlId);
}
