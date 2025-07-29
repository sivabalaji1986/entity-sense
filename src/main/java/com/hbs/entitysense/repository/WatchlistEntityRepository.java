package com.hbs.entitysense.repository;

import com.hbs.entitysense.entity.WatchlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistEntityRepository extends JpaRepository<WatchlistEntity, Long> {
    // Additional query methods can be added later if needed
}
