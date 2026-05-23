package com.url_shortener.repository;

import com.url_shortener.entity.UrlClick;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {

    long countByUrlId(Long urlId);

    @Query("select max(c.clickedAt) from UrlClick c where c.urlId = :urlId")
    Optional<OffsetDateTime> findLastClickedAt(@Param("urlId") Long urlId);

    @Query("""
            select c.userAgent as label, count(c) as count
            from UrlClick c
            where c.urlId = :urlId and c.userAgent is not null
            group by c.userAgent
            order by count(c) desc
            """)
    List<LabelCount> topUserAgents(@Param("urlId") Long urlId,
                                   Pageable pageable);

    interface LabelCount {
        String getLabel();
        long getCount();
    }
}
