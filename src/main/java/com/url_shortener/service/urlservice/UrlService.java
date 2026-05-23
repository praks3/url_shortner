package com.url_shortener.service.urlservice;

import com.url_shortener.dto.ShortenRequest;
import com.url_shortener.dto.UrlResponse;
import com.url_shortener.entity.Url;

import java.util.List;


public interface UrlService {

    UrlResponse createShortUrl(ShortenRequest request, Long ownerUserId);

    Url resolveShortCode(String shortCode);

    UrlResponse getById(Long id);

    void deleteById(Long id);

    List<UrlResponse> findByUserId(Long userId);
}
