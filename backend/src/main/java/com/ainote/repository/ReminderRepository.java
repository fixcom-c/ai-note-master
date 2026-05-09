package com.ainote.repository;

import com.ainote.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdAndIsDeletedFalseOrderByReminderTimeAsc(Long userId);
    List<Reminder> findByUserIdAndIsCompletedFalseAndIsDeletedFalseOrderByReminderTimeAsc(Long userId);
}