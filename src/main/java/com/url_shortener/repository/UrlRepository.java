package com.url_shortener.repository;

import com.url_shortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Url> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("delete from Url u where u.expiresAt is not null and u.expiresAt < :cutoff")
    int deleteAllByExpiresAtBeforeAndExpiresAtIsNotNull(@Param("cutoff") OffsetDateTime cutoff);
}
