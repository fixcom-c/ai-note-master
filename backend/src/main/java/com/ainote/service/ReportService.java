package com.ainote.service;

import com.ainote.entity.Task;
import com.ainote.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final TaskRepository taskRepository;
    private final UserContextService userContextService;
    private final AiService aiService;

    public ReportService(TaskRepository taskRepository, UserContextService userContextService, AiService aiService) {
        this.taskRepository = taskRepository;
        this.userContextService = userContextService;
        this.aiService = aiService;
    }

    public DailyReport generateDailyReport(LocalDate date) {
        Long userId = userContextService.getCurrentUserId();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Task> todayTasks = taskRepository.findByUserIdAndCreatedAtBetweenAndIsDeletedFalse(userId, startOfDay, endOfDay);

        int total = todayTasks.size();
        int completed = (int) todayTasks.stream().filter(t -> "completed".equals(t.getStatus())).count();

        String taskSummary = todayTasks.stream()
                .map(t -> (t.getStatus().equals("completed") ? "[完成] " : "[待办] ") + t.getTitle())
                .collect(Collectors.joining("\n"));

        String aiContent = aiService.generateDailyReport(userId, date.toString());

        DailyReport report = new DailyReport();
        report.setDate(date.toString());
        report.setTaskSummary(taskSummary);
        report.setCompletedTaskCount(completed);
        report.setTotalTaskCount(total);
        report.setContent(aiContent);
        report.setCreatedAt(LocalDateTime.now());

        return report;
    }
}
