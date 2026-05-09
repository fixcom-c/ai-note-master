package com.ainote.service;

import com.ainote.common.BusinessException;
import com.ainote.entity.Knowledge;
import com.ainote.repository.KnowledgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final UserContextService userContextService;

    public KnowledgeService(KnowledgeRepository knowledgeRepository, UserContextService userContextService) {
        this.knowledgeRepository = knowledgeRepository;
        this.userContextService = userContextService;
    }

    public List<Knowledge> list() {
        Long userId = userContextService.getCurrentUserId();
        return knowledgeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Knowledge create(Knowledge knowledge) {
        validateKnowledge(knowledge);
        Long userId = userContextService.getCurrentUserId();
        knowledge.setUserId(userId);
        return knowledgeRepository.save(knowledge);
    }

    @Transactional
    public Knowledge update(Long id, Knowledge knowledgeUpdate) {
        validateKnowledge(knowledgeUpdate);
        Long userId = userContextService.getCurrentUserId();
        Knowledge knowledge = knowledgeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("知识库条目不存在"));
        if (!knowledge.getUserId().equals(userId)) {
            throw new BusinessException("无权限修改此条目");
        }
        knowledge.setTitle(knowledgeUpdate.getTitle().trim());
        knowledge.setContent(knowledgeUpdate.getContent());
        knowledge.setTags(knowledgeUpdate.getTags());
        return knowledgeRepository.save(knowledge);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = userContextService.getCurrentUserId();
        Knowledge knowledge = knowledgeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("知识库条目不存在"));
        if (!knowledge.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除此条目");
        }
        knowledge.setIsDeleted(true);
        knowledgeRepository.save(knowledge);
    }

    private void validateKnowledge(Knowledge knowledge) {
        if (knowledge == null || knowledge.getTitle() == null || knowledge.getTitle().trim().isEmpty()) {
            throw new BusinessException("知识标题不能为空");
        }
    }
}
