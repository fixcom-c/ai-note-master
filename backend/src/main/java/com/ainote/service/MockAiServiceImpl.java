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
    public String answerQuestion(String question, String context, Long userId) {
        String modelName = "MockAI";

        try {
            String answer = buildMockAnswer(question, context);

            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setModel(modelName);
            logEntry.setInputContent("问题: " + question + "\n\n上下文:\n" + context);
            logEntry.setOutputContent(answer);
            logEntry.setTokenCount(estimateTokens(question + context));
            logEntry.setSuccess(true);
            aiCallLogRepository.save(logEntry);

            return answer;
        } catch (Exception e) {
            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setModel(modelName);
            logEntry.setInputContent("问题: " + question);
            logEntry.setSuccess(false);
            logEntry.setErrorMessage(e.getMessage());
            aiCallLogRepository.save(logEntry);

            throw new BusinessException("AI回答失败: " + e.getMessage());
        }
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

    private String buildMockAnswer(String question, String context) {
        String name = extractField(context, "- 称呼:");
        String focus = extractField(context, "- 当前重点:");
        String goal = extractField(context, "- 长期目标:");
        String topTheme = extractFirstEntry(context, "最近长期主题");
        String topNote = extractFirstEntry(context, "最近笔记");
        String topTask = extractFirstEntry(context, "最近任务");

        StringBuilder builder = new StringBuilder();
        builder.append((name == null || name.isBlank() ? "你" : name))
                .append("，");

        if (containsAny(question, "聚焦", "重点", "优先", "最该")) {
            builder.append("如果只看你最近这批记录，现在最值得继续盯住的还是“")
                    .append(blankToFallback(focus, "把当前分散内容收敛成更明确的行动"))
                    .append("”。");
            builder.append("\n\n我会优先参考你最近的主题和待推进任务：");
            builder.append(blankToFallback(topTheme, blankToFallback(topTask, "先把今天最重要的一件事推进到底")));
        } else if (containsAny(question, "复盘", "总结", "回顾")) {
            builder.append("从最近笔记、主题和任务来看，你已经有一些稳定主线了。");
            builder.append("\n\n这次最值得回看的内容是：");
            builder.append(blankToFallback(topNote, blankToFallback(topTheme, "最近记录还不多，先补连续几天的计划与复盘")));
        } else if (containsAny(question, "接下来", "下一步", "明天", "计划")) {
            builder.append("接下来更适合先把长期方向和最近待推进事项对齐。");
            builder.append("\n\n你可以先从这件事推进：");
            builder.append(blankToFallback(topTask, blankToFallback(focus, "把当前重点拆成 1 到 3 个明确动作")));
        } else {
            builder.append("我会把你当前阶段理解成“")
                    .append(blankToFallback(focus, blankToFallback(goal, "正在持续搭建自己的个人系统")))
                    .append("”。");
            builder.append("\n\n如果看最近上下文，最有代表性的线索是：");
            builder.append(blankToFallback(topTheme, blankToFallback(topNote, "你最近还在建立稳定记录节奏，后面会越来越清晰。")));
        }

        builder.append("\n\n基于这些内容，我更建议你优先做能形成连续反馈的动作，而不是再开新的大块任务。");
        return builder.toString();
    }

    private String extractField(String context, String prefix) {
        for (String line : context.split("\\R")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private String extractFirstEntry(String context, String sectionTitle) {
        boolean section = false;
        for (String line : context.split("\\R")) {
            if (line.startsWith(sectionTitle)) {
                section = true;
                continue;
            }
            if (section) {
                if (line.isBlank()) {
                    break;
                }
                if (Character.isDigit(line.charAt(0))) {
                    return line.substring(line.indexOf('.') + 1).trim();
                }
            }
        }
        return "";
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() || "暂无".equals(value) || "无".equals(value) ? fallback : value;
    }
}
