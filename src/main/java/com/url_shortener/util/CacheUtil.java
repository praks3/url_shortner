package com.url_shortener.util;

import com.url_shortener.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CacheUtil {

    private final CacheManager cacheManager;

    public static final String URL_CACHE = "urls";
    public static final String NOT_FOUND_SENTINEL = "__NOT_FOUND__";


    public void evictCache(String shortCode) {
        Cache cache = cacheManager.getCache(URL_CACHE);
        if (cache != null) {
            cache.evict(shortCode);
        }
        Cache notFound = cacheManager.getCache(CacheConfig.NOT_FOUND_CACHE);
        if (notFound != null) {
            notFound.evict(shortCode);
        }
    }

    public boolean isNegativelyCached(String shortCode) {
        Cache cache = cacheManager.getCache(CacheConfig.NOT_FOUND_CACHE);
        if (cache == null) return false;
        Cache.ValueWrapper wrapper = cache.get(shortCode);
        return wrapper != null && NOT_FOUND_SENTINEL.equals(wrapper.get());
    }

    public void rememberNotFound(String shortCode) {
        Cache cache = cacheManager.getCache(CacheConfig.NOT_FOUND_CACHE);
        if (cache != null) {
            cache.put(shortCode, NOT_FOUND_SENTINEL);
        }
    }
}
