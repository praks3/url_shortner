package com.url_shortener.scheduler;


import com.url_shortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredUrlCleanupJob {

    private final UrlRepository urlRepository;

    /**
     * Hourly purge of URLs whose expiry is in the past.
     * URLs without an expiry (expiresAt is null) are never touched.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpired() {
        OffsetDateTime now = OffsetDateTime.now();
        int deleted = urlRepository.deleteAllByExpiresAtBeforeAndExpiresAtIsNotNull(now);
        if (deleted > 0) {
            log.info("Purged {} expired URLs (cutoff={})", deleted, now);
        }
    }
}
