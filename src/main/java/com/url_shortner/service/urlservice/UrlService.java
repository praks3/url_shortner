package com.url_shortner.service.urlservice;

import com.url_shortner.dto.ShortenRequest;
import com.url_shortner.dto.UrlResponse;

public interface UrlService {

    UrlResponse createShortUrl(ShortenRequest request);

    String resolveOriginalUrl(String shortCode);

    UrlResponse getById(Long id);

    void deleteById(Long id);
}
