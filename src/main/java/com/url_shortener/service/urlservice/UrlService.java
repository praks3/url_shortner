package com.url_shortener.service.urlservice;

import com.url_shortener.dto.ShortenRequest;
import com.url_shortener.dto.UrlResponse;

public interface UrlService {

    UrlResponse createShortUrl(ShortenRequest request);

    String resolveOriginalUrl(String shortCode);

    UrlResponse getById(Long id);

    void deleteById(Long id);
}
