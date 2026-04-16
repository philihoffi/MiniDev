package org.philipp.fun.minidev.repository;

import org.philipp.fun.minidev.model.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataAgentRunRepository extends JpaRepository<AgentRun, UUID> {
}
