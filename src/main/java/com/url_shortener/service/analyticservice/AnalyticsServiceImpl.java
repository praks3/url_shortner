package com.url_shortener.service.analyticservice;

import com.url_shortener.dto.UrlStatsResponse;
import com.url_shortener.entity.UrlClick;
import com.url_shortener.repository.UrlClickRepository;
import com.url_shortener.repository.UrlRepository;
import com.url_shortener.exception.ShortCodeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int TOP_N = 5;

    private final UrlClickRepository clickRepository;
    private final UrlRepository urlRepository;

    @Async
    @Override
    @Transactional
    public void recordClick(Long urlId, String ipAddress, String userAgent, String referer) {
        try {
            UrlClick click = UrlClick.builder()
                    .urlId(urlId)
                    .ipAddress(truncate(ipAddress, 64))
                    .userAgent(truncate(userAgent, 512))
                    .referer(truncate(referer, 512))
                    .build();
            clickRepository.save(click);
        } catch (Exception ex) {
            // Click recording must never break the redirect path.
            log.warn("Failed to record click for urlId={}: {}", urlId, ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(Long urlId) {
        if (!urlRepository.existsById(urlId)) {
            throw new ShortCodeNotFoundException("id=" + urlId);
        }

        long total = clickRepository.countByUrlId(urlId);
        var lastClicked = clickRepository.findLastClickedAt(urlId).orElse(null);
        var topUaProjections = clickRepository.topUserAgents(urlId, PageRequest.of(0, TOP_N));

        List<UrlStatsResponse.LabelCount> topUas = topUaProjections.stream()
                .map(p -> UrlStatsResponse.LabelCount.builder()
                        .label(p.getLabel())
                        .count(p.getCount())
                        .build())
                .toList();

        return UrlStatsResponse.builder()
                .urlId(urlId)
                .totalClicks(total)
                .lastClickedAt(lastClicked)
                .topUserAgents(topUas)
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
