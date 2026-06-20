package org.philipp.fun.minidev.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link ApiRequestLog} entities.
 */
@Repository
public interface ApiRequestLogRepository
        extends JpaRepository<ApiRequestLog, Long> {
}