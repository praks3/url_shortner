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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code.length:7}")
    private int defaultCodeLength;

    @Override
    @Transactional
    public UrlResponse createShortUrl(ShortenRequest request) {
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

    @Override
    @Transactional(readOnly = true)
    public String resolveOriginalUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UrlExpiredException(shortCode);
        }
        return url.getOriginalUrl();
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
        if (!urlRepository.existsById(id)) {
            throw new ShortCodeNotFoundException("id=" + id);
        }
        urlRepository.deleteById(id);
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
