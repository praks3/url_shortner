package com.url_shortener.service.urlservice;


import com.url_shortener.dto.ShortenRequest;
import com.url_shortener.dto.UrlResponse;
import com.url_shortener.entity.Url;
import com.url_shortener.exception.AliasAlreadyExistsException;
import com.url_shortener.exception.ShortCodeGenerationException;
import com.url_shortener.exception.ShortCodeNotFoundException;
import com.url_shortener.exception.UrlExpiredException;
import com.url_shortener.repository.UrlRepository;
import com.url_shortener.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final String URL_CACHE = "urls";

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CacheManager cacheManager;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code.length:7}")
    private int defaultCodeLength;

    @Override
    @Transactional
    public UrlResponse createShortUrl(ShortenRequest request, Long ownerUserId) {
        String shortCode;
        boolean isCustom = request.getCustomAlias() != null && !request.getCustomAlias().isBlank();

        if (isCustom) {
            shortCode = request.getCustomAlias();
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new AliasAlreadyExistsException(shortCode);
            }
        } else {
            shortCode = generateUniqueCode();
        }

        Url toSave = Url.builder()
                .shortCode(shortCode)
                .originalUrl(request.getUrl())
                .expiresAt(request.getExpiresAt())
                .customAlias(isCustom)
                .userId(ownerUserId)
                .build();

        Url saved;
        try {
            saved = urlRepository.save(toSave);
        } catch (DataIntegrityViolationException ex) {
            if (isCustom) {
                throw new AliasAlreadyExistsException(shortCode);
            }
            log.warn("Collision on generated code {}, retrying once", shortCode);
            toSave.setShortCode(generateUniqueCode());
            saved = urlRepository.save(toSave);
        }

        return toResponse(saved);
    }

    /**
     * Cache-aside: read from Redis first, fall back to DB. Only permanent URLs
     * (no expiresAt) are cached so the expiry check never serves a stale "ok".
     */
    @Override
    @Cacheable(value = URL_CACHE, key = "#shortCode",
            unless = "#result == null || #result.expiresAt != null")
    @Transactional(readOnly = true)
    public Url resolveShortCode(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UrlExpiredException(shortCode);
        }
        return url;
    }

    @Override
    @Transactional(readOnly = true)
    public UrlResponse getById(Long id) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new ShortCodeNotFoundException("id=" + id));
        return toResponse(url);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new ShortCodeNotFoundException("id=" + id));
        urlRepository.delete(url);
        evictCache(url.getShortCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UrlResponse> findByUserId(Long userId) {
        return urlRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void evictCache(String shortCode) {
        Cache cache = cacheManager.getCache(URL_CACHE);
        if (cache != null) {
            cache.evict(shortCode);
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = shortCodeGenerator.generate(defaultCodeLength);
            if (!urlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new ShortCodeGenerationException(
                "Could not generate a unique short code after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    private UrlResponse toResponse(Url url) {
        return UrlResponse.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .build();
    }
}
