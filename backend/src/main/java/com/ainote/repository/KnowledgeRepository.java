package com.ainote.repository;

import com.ainote.entity.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {
    List<Knowledge> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
    List<Knowledge> findByUserIdAndTagsContainingAndIsDeletedFalse(Long userId, String tag);
}