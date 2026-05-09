package com.ainote.service;

import com.ainote.entity.Task;
import com.ainote.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserContextService userContextService;

    @Mock
    private AiService aiService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void shouldGenerateReportForRequestedDate() {
        LocalDate date = LocalDate.of(2026, 5, 9);
        when(userContextService.getCurrentUserId()).thenReturn(7L);
        when(taskRepository.findByUserIdAndCreatedAtBetweenAndIsDeletedFalse(
                eq(7L),
                eq(date.atStartOfDay()),
                eq(date.atTime(23, 59, 59, 999999999))
        )).thenReturn(List.of(
                buildTask("完成接口联调", "completed"),
                buildTask("整理次日计划", "pending")
        ));
        when(aiService.generateDailyReport(7L, "2026-05-09")).thenReturn("AI 总结");

        DailyReport report = reportService.generateDailyReport(date);

        assertEquals("2026-05-09", report.getDate());
        assertEquals(2, report.getTotalTaskCount());
        assertEquals(1, report.getCompletedTaskCount());
        assertEquals("AI 总结", report.getContent());
        assertTrue(report.getTaskSummary().contains("[完成] 完成接口联调"));
        assertTrue(report.getTaskSummary().contains("[待办] 整理次日计划"));
        verify(aiService).generateDailyReport(7L, "2026-05-09");
    }

    private Task buildTask(String title, String status) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus(status);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }
}
