package org.philipp.fun.minidev.repository;

import org.philipp.fun.minidev.model.ApiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, Long> {
}
