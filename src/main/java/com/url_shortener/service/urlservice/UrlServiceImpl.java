package com.url_shortener.service.urlservice;


import com.url_shortener.dto.ShortenRequest;
import com.url_shortener.dto.UrlResponse;
import com.url_shortener.entity.Url;
import com.url_shortener.exception.AliasAlreadyExistsException;
import com.url_shortener.exception.ShortCodeNotFoundException;
import com.url_shortener.exception.UrlExpiredException;
import com.url_shortener.repository.UrlRepository;
import com.url_shortener.util.Base62;
import com.url_shortener.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.url_shortener.util.CacheUtil.URL_CACHE;


@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {


    private static final long BASE62_ID_OFFSET = 100_000L;
    private static final int PLACEHOLDER_SUFFIX_LEN = 14;
    private static final char[] BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final UrlRepository urlRepository;
    private CacheUtil cacheUtil;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public UrlResponse createShortUrl(ShortenRequest request, Long ownerUserId) {
        boolean isCustom = request.getCustomAlias() != null && !request.getCustomAlias().isBlank();

        if (isCustom) {
            return createWithCustomAlias(request, ownerUserId);
        }
        return createWithBase62OfId(request, ownerUserId);
    }


    private UrlResponse createWithCustomAlias(ShortenRequest request, Long ownerUserId) {
        String alias = request.getCustomAlias();
        if (urlRepository.existsByShortCode(alias)) {
            throw new AliasAlreadyExistsException(alias);
        }

        Url toSave = Url.builder()
                .shortCode(alias)
                .originalUrl(request.getUrl())
                .expiresAt(request.getExpiresAt())
                .customAlias(true)
                .userId(ownerUserId)
                .build();

        try {
            Url saved = urlRepository.save(toSave);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert raced past the existence check.
            throw new AliasAlreadyExistsException(alias);
        }
    }

    private UrlResponse createWithBase62OfId(ShortenRequest request, Long ownerUserId) {
        String placeholder = randomPlaceholder();

        Url toSave = Url.builder()
                .shortCode(placeholder)
                .originalUrl(request.getUrl())
                .expiresAt(request.getExpiresAt())
                .customAlias(false)
                .userId(ownerUserId)
                .build();

        Url saved;
        try {
            saved = urlRepository.saveAndFlush(toSave);
        } catch (DataIntegrityViolationException ex) {
            // Astronomically unlikely placeholder collision — retry once with a fresh value.
            log.warn("Placeholder collision on {}; retrying once", placeholder);
            // Re-build to avoid Hibernate session inconsistencies after the failed flush.
            Url retry = Url.builder()
                    .shortCode(randomPlaceholder())
                    .originalUrl(request.getUrl())
                    .expiresAt(request.getExpiresAt())
                    .customAlias(false)
                    .userId(ownerUserId)
                    .build();
            saved = urlRepository.saveAndFlush(retry);
        }

        String shortCode = Base62.encode(saved.getId() + BASE62_ID_OFFSET);
        saved.setShortCode(shortCode);
        // Flush the update so the new code is durable before we return it to the client.
        saved = urlRepository.saveAndFlush(saved);
        return toResponse(saved);
    }


    private static String randomPlaceholder() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(1 + PLACEHOLDER_SUFFIX_LEN);
        sb.append('_');
        for (int i = 0; i < PLACEHOLDER_SUFFIX_LEN; i++) {
            sb.append(BASE62_CHARS[rnd.nextInt(BASE62_CHARS.length)]);
        }
        return sb.toString();
    }


    @Override
    @Cacheable(value = URL_CACHE, key = "#shortCode",
            unless = "#result == null || #result.expiresAt != null")
    @Transactional(readOnly = true)
    public Url resolveShortCode(String shortCode) {
        if (cacheUtil.isNegativelyCached(shortCode)) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseGet(() -> {
                    cacheUtil.rememberNotFound(shortCode);
                    throw new ShortCodeNotFoundException(shortCode);
                });
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
        cacheUtil.evictCache(url.getShortCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UrlResponse> findByUserId(Long userId) {
        return urlRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
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
