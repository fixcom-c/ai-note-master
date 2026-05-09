package com.ainote.service;

import com.ainote.common.BusinessException;
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
    public QuickNote create(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("记录内容不能为空");
        }
        Long userId = userContextService.getCurrentUserId();
        QuickNote note = new QuickNote();
        note.setContent(content.trim());
        note.setUserId(userId);
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
}
