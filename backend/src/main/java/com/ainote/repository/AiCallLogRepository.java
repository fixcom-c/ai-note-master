package com.ainote.repository;

import com.ainote.entity.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {
}