package com.ainote.service;

import com.ainote.common.BusinessException;
import com.ainote.dto.NoteCreateRequest;
import com.ainote.entity.QuickNote;
import com.ainote.repository.QuickNoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private QuickNoteRepository noteRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private NoteService noteService;

    @Test
    void shouldApplyDefaultsAndTrimFieldsWhenCreatingNote() {
        NoteCreateRequest request = new NoteCreateRequest();
        request.setContent("  第一行标题\n补充内容  ");

        when(userContextService.getCurrentUserId()).thenReturn(9L);
        when(noteRepository.save(any(QuickNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuickNote saved = noteService.create(request);

        ArgumentCaptor<QuickNote> captor = ArgumentCaptor.forClass(QuickNote.class);
        verify(noteRepository).save(captor.capture());

        assertEquals("第一行标题", saved.getTitle());
        assertEquals("第一行标题\n补充内容", saved.getContent());
        assertEquals("inbox", saved.getCategory());
        assertEquals("manual", saved.getSourceType());
        assertEquals(9L, captor.getValue().getUserId());
    }

    @Test
    void shouldUpdateOnlyOwnedNote() {
        QuickNote existing = new QuickNote();
        existing.setId(3L);
        existing.setUserId(9L);
        existing.setCategory("daily-plan");
        existing.setSourceType("manual");

        NoteCreateRequest request = new NoteCreateRequest();
        request.setTitle("  2026-05-09 日计划  ");
        request.setContent("  今天完成联调  ");
        request.setCategory("daily-review");
        request.setSourceType("imported");

        when(userContextService.getCurrentUserId()).thenReturn(9L);
        when(noteRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(noteRepository.save(any(QuickNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuickNote updated = noteService.update(3L, request);

        assertEquals("2026-05-09 日计划", updated.getTitle());
        assertEquals("今天完成联调", updated.getContent());
        assertEquals("daily-review", updated.getCategory());
        assertEquals("imported", updated.getSourceType());
    }

    @Test
    void shouldRejectBlankNoteContent() {
        NoteCreateRequest request = new NoteCreateRequest();
        request.setContent("   ");

        assertThrows(BusinessException.class, () -> noteService.create(request));
    }

    @Test
    void shouldRejectUpdatingNoteOwnedByAnotherUser() {
        QuickNote existing = new QuickNote();
        existing.setId(8L);
        existing.setUserId(2L);

        NoteCreateRequest request = new NoteCreateRequest();
        request.setContent("需要更新的内容");

        when(userContextService.getCurrentUserId()).thenReturn(9L);
        when(noteRepository.findById(8L)).thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () -> noteService.update(8L, request));
    }
}
