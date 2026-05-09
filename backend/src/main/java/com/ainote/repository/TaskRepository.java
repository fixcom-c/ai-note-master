package com.ainote.repository;

import com.ainote.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
    List<Task> findByUserIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, String status);
    List<Task> findByUserIdAndReminderTimeBetweenAndIsDeletedFalse(Long userId, LocalDateTime start, LocalDateTime end);
    List<Task> findByUserIdAndCreatedAtBetweenAndIsDeletedFalse(Long userId, LocalDateTime start, LocalDateTime end);
}