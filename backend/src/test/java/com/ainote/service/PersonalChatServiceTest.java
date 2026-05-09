package com.ainote.service;

import com.ainote.entity.Knowledge;
import com.ainote.entity.PersonalProfile;
import com.ainote.entity.QuickNote;
import com.ainote.entity.Task;
import com.ainote.repository.KnowledgeRepository;
import com.ainote.repository.QuickNoteRepository;
import com.ainote.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalChatServiceTest {

    @Mock
    private QuickNoteRepository quickNoteRepository;

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PersonalProfileService personalProfileService;

    @Mock
    private UserContextService userContextService;

    @Mock
    private AiService aiService;

    @InjectMocks
    private PersonalChatService personalChatService;

    @Test
    void shouldBuildChatReplyFromPersonalContext() {
        PersonalProfile profile = new PersonalProfile();
        profile.setDisplayName("阿峰");
        profile.setCurrentFocus("把 AI Note 打磨成每天会打开的个人系统");
        profile.setLongTermGoals("沉淀长期知识与节奏");
        profile.setWorkStyle("先快速记录，再集中整理");
        profile.setAiPreference("直接、少空话");

        QuickNote note = new QuickNote();
        note.setTitle("今天的产品方向");
        note.setContent("最近更应该聚焦长期使用频率，而不是只堆功能。");
        note.setCategory("daily-review");
        note.setCreatedAt(LocalDateTime.now());

        Knowledge theme = new Knowledge();
        theme.setTitle("个人第二大脑");
        theme.setContent("反复出现的主线是记录、复盘和 AI 理解。");
        theme.setTags("产品,复盘");
        theme.setCreatedAt(LocalDateTime.now());

        Task task = new Task();
        task.setTitle("整理下周优先级");
        task.setDescription("收敛产品主线");
        task.setPriority("high");
        task.setStatus("pending");
        task.setCreatedAt(LocalDateTime.now());

        when(userContextService.getCurrentUserId()).thenReturn(1L);
        when(personalProfileService.getProfile()).thenReturn(profile);
        when(quickNoteRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(note));
        when(knowledgeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(theme));
        when(taskRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(task));
        when(aiService.answerQuestion(eq("我最近最该聚焦什么"), org.mockito.ArgumentMatchers.anyString(), eq(1L)))
                .thenReturn("建议继续聚焦个人第二大脑主线。");
        when(aiService.getModelName()).thenReturn("MockAI");

        PersonalChatReply reply = personalChatService.ask("我最近最该聚焦什么");

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).answerQuestion(eq("我最近最该聚焦什么"), contextCaptor.capture(), eq(1L));

        assertEquals("建议继续聚焦个人第二大脑主线。", reply.getAnswer());
        assertEquals("MockAI", reply.getModel());
        assertFalse(reply.getReferences().isEmpty());
        assertEquals("本次回答参考了 1 条笔记、1 个长期主题、1 个最近任务。", reply.getContextSummary());
        assertFalse(contextCaptor.getValue().contains("null"));
        assertFalse(reply.getSuggestions().isEmpty());
    }
}
