package com.ainote.service;

import com.ainote.common.BusinessException;
import com.ainote.dto.NoteCreateRequest;
import com.ainote.entity.QuickNote;
import com.ainote.repository.QuickNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteService {

    private final QuickNoteRepository noteRepository;
    private final UserContextService userContextService;

    public NoteService(QuickNoteRepository noteRepository, UserContextService userContextService) {
        this.noteRepository = noteRepository;
        this.userContextService = userContextService;
    }

    public List<QuickNote> list() {
        Long userId = userContextService.getCurrentUserId();
        return noteRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public QuickNote create(NoteCreateRequest request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException("记录内容不能为空");
        }
        Long userId = userContextService.getCurrentUserId();
        String content = request.getContent().trim();
        QuickNote note = new QuickNote();
        note.setTitle(buildTitle(request.getTitle(), content));
        note.setContent(content);
        note.setCategory(normalizeValue(request.getCategory(), "inbox"));
        note.setSourceType(normalizeValue(request.getSourceType(), "manual"));
        note.setUserId(userId);
        return noteRepository.save(note);
    }

    @Transactional
    public QuickNote update(Long id, NoteCreateRequest request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException("记录内容不能为空");
        }

        Long userId = userContextService.getCurrentUserId();
        QuickNote note = noteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权限修改此记录");
        }

        String content = request.getContent().trim();
        note.setTitle(buildTitle(request.getTitle(), content));
        note.setContent(content);
        note.setCategory(normalizeValue(request.getCategory(), note.getCategory() == null ? "inbox" : note.getCategory()));
        note.setSourceType(normalizeValue(request.getSourceType(), note.getSourceType() == null ? "manual" : note.getSourceType()));
        return noteRepository.save(note);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = userContextService.getCurrentUserId();
        QuickNote note = noteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除此记录");
        }
        note.setIsDeleted(true);
        noteRepository.save(note);
    }

    private String buildTitle(String title, String content) {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        String firstLine = content.split("\\R", 2)[0].trim();
        if (firstLine.length() <= 36) {
            return firstLine;
        }
        return firstLine.substring(0, 36) + "...";
    }

    private String normalizeValue(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
