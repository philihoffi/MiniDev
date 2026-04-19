package org.philipp.fun.minidev.spring.repository;

import org.philipp.fun.minidev.spring.model.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAgentRunRepository extends JpaRepository<AgentRun, Long> {
}
