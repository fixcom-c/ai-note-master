package com.ainote.service;

import com.ainote.common.BusinessException;
import com.ainote.entity.AiCallLog;
import com.ainote.repository.AiCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MockAiServiceImpl implements AiService {

    private final AiCallLogRepository aiCallLogRepository;
    private final ObjectMapper objectMapper;

    public MockAiServiceImpl(AiCallLogRepository aiCallLogRepository, ObjectMapper objectMapper) {
        this.aiCallLogRepository = aiCallLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalyzeResult analyze(String content, Long userId) {
        String modelName = "MockAI";
        String outputContent = "";
        boolean success = true;
        String errorMessage = null;

        try {
            AnalyzeResult result = new AnalyzeResult();

            String summary = generateSummary(content);
            result.setSummary(summary);

            List<AnalyzeResult.TaskInfo> tasks = extractTasks(content);
            result.setTasks(tasks);

            List<String> tags = extractTags(content);
            result.setKnowledgeTags(tags);

            String reminderTime = extractReminderTime(content);
            result.setReminderTime(reminderTime);

            outputContent = objectMapper.writeValueAsString(result);

            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setModel(modelName);
            logEntry.setInputContent(content);
            logEntry.setOutputContent(outputContent);
            logEntry.setTokenCount(estimateTokens(content));
            logEntry.setSuccess(true);
            aiCallLogRepository.save(logEntry);

            return result;
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();

            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setModel(modelName);
            logEntry.setInputContent(content);
            logEntry.setSuccess(false);
            logEntry.setErrorMessage(errorMessage);
            aiCallLogRepository.save(logEntry);

            throw new BusinessException("AI分析失败: " + e.getMessage());
        }
    }

    @Override
    public String generateDailyReport(Long userId, String date) {
        return "今日工作总结：\n1. 完成了主要任务推进\n2. 解决了多个关键问题\n3. 明日计划继续优化";
    }

    @Override
    public String getModelName() {
        return "MockAI";
    }

    private String generateSummary(String content) {
        if (content.length() > 50) {
            return content.substring(0, 50) + "...";
        }
        return content;
    }

    private List<AnalyzeResult.TaskInfo> extractTasks(String content) {
        List<AnalyzeResult.TaskInfo> tasks = new ArrayList<>();
        String lowerContent = content.toLowerCase();

        if (lowerContent.contains("会议") || lowerContent.contains("meeting")) {
            AnalyzeResult.TaskInfo task = new AnalyzeResult.TaskInfo();
            task.setTitle("参加会议");
            task.setDescription("根据记录参加会议");
            task.setPriority("medium");
            tasks.add(task);
        }

        if (lowerContent.contains("报告") || lowerContent.contains("写")) {
            AnalyzeResult.TaskInfo task = new AnalyzeResult.TaskInfo();
            task.setTitle("撰写报告");
            task.setDescription("完成相关报告撰写");
            task.setPriority("high");
            tasks.add(task);
        }

        if (lowerContent.contains("回复") || lowerContent.contains("邮件")) {
            AnalyzeResult.TaskInfo task = new AnalyzeResult.TaskInfo();
            task.setTitle("回复邮件");
            task.setDescription("处理邮件回复");
            task.setPriority("medium");
            tasks.add(task);
        }

        if (tasks.isEmpty()) {
            AnalyzeResult.TaskInfo task = new AnalyzeResult.TaskInfo();
            task.setTitle("处理记录内容");
            task.setDescription(content.length() > 100 ? content.substring(0, 100) : content);
            task.setPriority("medium");
            tasks.add(task);
        }

        return tasks;
    }

    private List<String> extractTags(String content) {
        List<String> tags = new ArrayList<>();
        String[] keywords = {"工作", "会议", "项目", "客户", "技术", "文档", "邮件", "报告", "计划", "总结"};

        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                tags.add(keyword);
            }
        }

        if (tags.isEmpty()) {
            tags.add("日常记录");
        }

        return tags;
    }

    private String extractReminderTime(String content) {
        return null;
    }

    private int estimateTokens(String content) {
        return content.length() / 2;
    }
}