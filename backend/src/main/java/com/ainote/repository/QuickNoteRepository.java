package com.ainote.repository;

import com.ainote.entity.QuickNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuickNoteRepository extends JpaRepository<QuickNote, Long> {
    List<QuickNote> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
}