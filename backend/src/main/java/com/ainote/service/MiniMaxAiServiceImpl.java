package com.ainote.service;

import com.ainote.common.BusinessException;
import com.ainote.entity.AiCallLog;
import com.ainote.repository.AiCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Primary
public class MiniMaxAiServiceImpl implements AiService {

    private final AiCallLogRepository aiCallLogRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${ai.minimax.api-key:}")
    private String apiKey;

    @Value("${ai.minimax.group-id:}")
    private String groupId;

    @Value("${ai.minimax.model:abab6.5s-chat}")
    private String model;

    @Value("${ai.minimax.mock-mode:true}")
    private boolean mockMode;

    public MiniMaxAiServiceImpl(AiCallLogRepository aiCallLogRepository, ObjectMapper objectMapper) {
        this.aiCallLogRepository = aiCallLogRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public AnalyzeResult analyze(String content, Long userId) {
        String modelName = mockMode ? "MockAI" : "MiniMax-abab6.5s";
        
        try {
            AnalyzeResult result;
            int tokenCount;
            
            if (mockMode || apiKey == null || apiKey.isEmpty()) {
                result = mockAnalyze(content);
                tokenCount = estimateTokens(content);
            } else {
                result = callMiniMaxApi(content);
                tokenCount = estimateTokens(content) + estimateTokens(objectMapper.writeValueAsString(result));
            }

            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setModel(modelName);
            logEntry.setInputContent(content);
            logEntry.setOutputContent(objectMapper.writeValueAsString(result));
            logEntry.setTokenCount(tokenCount);
            logEntry.setSuccess(true);
            aiCallLogRepository.save(logEntry);

            return result;
        } catch (Exception e) {
            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setModel(modelName);
            logEntry.setInputContent(content);
            logEntry.setSuccess(false);
            logEntry.setErrorMessage(e.getMessage());
            aiCallLogRepository.save(logEntry);

            throw new BusinessException("AI分析失败: " + e.getMessage());
        }
    }

    @Override
    public String generateDailyReport(Long userId, String date) {
        if (mockMode || apiKey == null || apiKey.isEmpty()) {
            return "今日工作总结：\n1. 完成了主要任务推进\n2. 解决了多个关键问题\n3. 明日计划继续优化";
        }

        try {
            String prompt = String.format(
                "请根据以下日期%s的任务完成情况，生成一份简洁的每日工作总结。要求：\n" +
                "1. 总结今日完成的主要工作\n" +
                "2. 列出遇到的问题或待改进之处\n" +
                "3. 提出明日的工作计划\n" +
                "使用中文回复，格式清晰，总字数控制在200字以内。",
                date
            );

            String response = callMiniMaxForText(prompt);
            return response.isEmpty() ? "今日工作总结：\n1. 完成了主要任务推进\n2. 解决了多个关键问题\n3. 明日计划继续优化" : response;
        } catch (Exception e) {
            return "今日工作总结：\n1. 完成了主要任务推进\n2. 解决了多个关键问题\n3. 明日计划继续优化";
        }
    }

    @Override
    public String answerQuestion(String question, String context, Long userId) {
        String modelName = mockMode ? "MockAI" : "MiniMax-abab6.5s";

        try {
            String answer;
            if (mockMode || apiKey == null || apiKey.isEmpty()) {
                answer = buildMockPersonalAnswer(question, context);
            } else {
                String prompt = "你是用户的个人知识助手。请只基于下面提供的个人上下文回答问题，不要编造上下文中不存在的事实。"
                        + "如果上下文不足，要明确说“现有记录里还看不出来”。"
                        + "回答要求：中文、直接、少空话、优先可执行，控制在 220 字以内。\n\n"
                        + "用户问题：\n" + question + "\n\n"
                        + "个人上下文：\n" + context;
                answer = callMiniMaxForText(prompt);
                if (answer == null || answer.isBlank()) {
                    answer = "现有记录里还看不出来特别明确的答案。你可以再补一点最近笔记、周复盘或长期主题，我会更容易给出贴近你的建议。";
                }
            }

            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setModel(modelName);
            logEntry.setInputContent("问题: " + question + "\n\n上下文:\n" + context);
            logEntry.setOutputContent(answer);
            logEntry.setTokenCount(estimateTokens(question + context + answer));
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
        return mockMode ? "MockAI" : "MiniMax-abab6.5s";
    }

    private AnalyzeResult callMiniMaxApi(String content) throws Exception {
        String prompt = String.format(
            "你是一个智能任务提取助手。请分析用户输入的内容，提取任务信息并返回JSON格式结果。\n\n" +
            "用户输入：%s\n\n" +
            "请按以下JSON格式返回结果（只返回JSON，不要其他内容）：\n" +
            "{\n" +
            "  \"summary\": \"内容摘要（不超过100字）\",\n" +
            "  \"tasks\": [\n" +
            "    {\n" +
            "      \"title\": \"任务标题\",\n" +
            "      \"description\": \"任务描述\",\n" +
            "      \"priority\": \"high/medium/low\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"knowledgeTags\": [\"标签1\", \"标签2\"],\n" +
            "  \"reminderTime\": \"提醒时间（可选，格式YYYY-MM-DD HH:mm）\"\n" +
            "}\n\n" +
            "规则：\n" +
            "1. 根据内容判断任务优先级（高/中/低）\n" +
            "2. 提取相关知识标签（工作、会议、项目、客户、技术等）\n" +
            "3. 如果内容提到具体时间，设置提醒时间\n" +
            "4. 如果没有明确任务，返回一个默认任务\"处理记录内容\"\n" +
            "5. 知识标签从以下选项中选择：工作,会议,项目,客户,技术,文档,邮件,报告,计划,总结,日常",
            content
        );

        String response = callMiniMaxForText(prompt);
        return parseAnalyzeResponse(response, content);
    }

    private String callMiniMaxForText(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个智能助手，请严格按照用户要求返回JSON格式数据。");
            messages.add(systemMessage);
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // MiniMax 国际版 API 端点（兼容 OpenAI 格式）
            String url = "https://api.minimax.io/v1/chat/completions";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            }
        } catch (Exception e) {
            throw new BusinessException("MiniMax API调用失败: " + e.getMessage());
        }
        
        return "";
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            
            System.err.println("MiniMax API Response: " + responseBody);
            
            if (response.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) response.get("error");
                String errorMsg = error != null ? (String) error.get("message") : "未知错误";
                System.err.println("MiniMax API Error: " + errorMsg);
                return "";
            }
            
            if (response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    if (firstChoice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message != null && message.containsKey("content")) {
                            String content = message.get("content") != null ? message.get("content").toString() : "";
                            
                            content = extractJsonFromResponse(content);
                            
                            if (!content.isEmpty()) {
                                return content;
                            }
                        }
                    }
                    if (firstChoice.containsKey("delta")) {
                        Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");
                        if (delta != null && delta.containsKey("content")) {
                            Object content = delta.get("content");
                            if (content != null) {
                                return content.toString();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析响应失败: " + e.getMessage());
        }
        return "";
    }
    
    private String extractJsonFromResponse(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        content = content.trim();
        
        int thinkStart = content.indexOf("<think>");
        int thinkEnd = content.indexOf("</think>");
        
        String jsonPart;
        if (thinkStart >= 0 && thinkEnd > thinkStart) {
            jsonPart = content.substring(thinkEnd + 9).trim();
        } else {
            jsonPart = content;
        }
        
        jsonPart = jsonPart.replaceAll("```json\\s*", "").replaceAll("\\s*```", "");
        jsonPart = jsonPart.replaceAll("```\\s*", "").replaceAll("\\s*```", "");
        
        if (jsonPart.contains("{")) {
            int start = jsonPart.indexOf("{");
            int end = jsonPart.lastIndexOf("}");
            if (end > start) {
                return jsonPart.substring(start, end + 1);
            }
        }
        
        if (content.contains("{")) {
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");
            if (end > start) {
                return content.substring(start, end + 1);
            }
        }
        
        return jsonPart;
    }

    private AnalyzeResult parseAnalyzeResponse(String response, String originalContent) {
        AnalyzeResult result = new AnalyzeResult();
        
        try {
            if (response.contains("{")) {
                int start = response.indexOf("{");
                int end = response.lastIndexOf("}") + 1;
                String jsonStr = response.substring(start, end);
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);
                
                result.setSummary((String) parsed.getOrDefault("summary", originalContent.substring(0, Math.min(50, originalContent.length()))));
                
                List<Map<String, Object>> tasksList = (List<Map<String, Object>>) parsed.get("tasks");
                List<AnalyzeResult.TaskInfo> tasks = new ArrayList<>();
                if (tasksList != null) {
                    for (Map<String, Object> taskMap : tasksList) {
                        AnalyzeResult.TaskInfo task = new AnalyzeResult.TaskInfo();
                        task.setTitle((String) taskMap.getOrDefault("title", "处理任务"));
                        task.setDescription((String) taskMap.getOrDefault("description", ""));
                        task.setPriority((String) taskMap.getOrDefault("priority", "medium"));
                        tasks.add(task);
                    }
                }
                result.setTasks(tasks);
                
                List<String> tags = (List<String>) parsed.get("knowledgeTags");
                result.setKnowledgeTags(tags != null ? tags : extractTagsFromContent(originalContent));
                
                result.setReminderTime((String) parsed.get("reminderTime"));
            } else {
                setDefaultResult(result, originalContent);
            }
        } catch (Exception e) {
            setDefaultResult(result, originalContent);
        }
        
        return result;
    }

    private void setDefaultResult(AnalyzeResult result, String content) {
        result.setSummary(content.length() > 100 ? content.substring(0, 100) : content);
        
        AnalyzeResult.TaskInfo task = new AnalyzeResult.TaskInfo();
        task.setTitle("处理记录内容");
        task.setDescription(content.length() > 100 ? content.substring(0, 100) : content);
        task.setPriority("medium");
        result.setTasks(List.of(task));
        
        result.setKnowledgeTags(extractTagsFromContent(content));
    }

    private AnalyzeResult mockAnalyze(String content) {
        AnalyzeResult result = new AnalyzeResult();
        
        result.setSummary(content.length() > 100 ? content.substring(0, 100) + "..." : content);
        
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
        
        result.setTasks(tasks);
        result.setKnowledgeTags(extractTagsFromContent(content));
        
        return result;
    }

    private List<String> extractTagsFromContent(String content) {
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

    private String buildMockPersonalAnswer(String question, String context) {
        String name = cleanPhrase(blankToFallback(extractField(context, "- 称呼:"), "你"));
        String focus = cleanPhrase(blankToFallback(extractField(context, "- 当前重点:"), "把当前分散内容收敛成稳定主线"));
        String goal = cleanPhrase(blankToFallback(extractField(context, "- 长期目标:"), "沉淀自己的长期系统"));
        String workStyle = cleanPhrase(blankToFallback(extractField(context, "- 工作风格:"), "先记录，再整理，再推进"));

        List<String> themes = extractSectionEntries(context, "最近长期主题");
        List<String> notes = extractSectionEntries(context, "最近笔记");
        List<String> tasks = extractSectionEntries(context, "最近任务");

        String topTheme = firstMeaningfulEntry(themes, "最近的长期主题还不够清晰");
        String secondTheme = themes.size() > 1 ? summarizeEntry(themes.get(1)) : "";
        String topNote = firstMeaningfulEntry(notes, "最近记录还不多");
        String pendingTask = firstMatchingEntry(tasks, "[待推进]", "当前待推进事项还比较少");
        String completedTask = firstMatchingEntry(tasks, "[已完成]", "最近完成项还不够集中");

        if (containsAny(question, "主题", "反复", "长期", "主线")) {
            return name + "，最近反复出现的主题更像是“" + summarizeEntry(topTheme) + "”"
                    + appendIfPresent("，其次是“" + secondTheme + "”", secondTheme)
                    + "。它们同时和你的当前重点“" + focus + "”贴得很近，说明这不是临时兴趣，而是在形成长期投入。"
                    + "\n\n下一步别急着继续扩张，先给最核心的 1 个主题补齐三件事：为什么重要、现在卡在哪、这周下一步是什么。";
        }

        if (containsAny(question, "聚焦", "重点", "优先", "最该")) {
            return name + "，如果这阶段只保留一条主线，我更建议你继续围绕“" + focus + "”推进。"
                    + "\n\n现在最值得盯住的线索，一条来自主题“" + summarizeEntry(topTheme) + "”，一条来自最近记录“" + summarizeEntry(topNote) + "”，再加上待推进事项“" + summarizeEntry(pendingTask) + "”。"
                    + "\n\n先把它收敛成今天、这周、下一步 3 个动作，暂时不要再开新坑。";
        }

        if (containsAny(question, "下一步", "接下来", "明天", "计划", "推进")) {
            return name + "，接下来最值得推进的不是再想更多，而是把“" + focus + "”落成一个明确动作。"
                    + "\n\n我会优先从待推进事项“" + summarizeEntry(pendingTask) + "”开始，再参考最近笔记“" + summarizeEntry(topNote) + "”做收敛。"
                    + "\n\n做法上保持你的节奏就好：" + workStyle + "。";
        }

        if (containsAny(question, "复盘", "总结", "回顾")) {
            return name + "，如果把最近这段记录压成一次复盘，你的主线还是“" + focus + "”。"
                    + "\n\n值得肯定的是你已经推进了“" + summarizeEntry(completedTask) + "”，同时最近最有代表性的记录是“" + summarizeEntry(topNote) + "”。"
                    + "\n\n接下来最需要补的不是更多结论，而是给这条主线建立连续几天都能重复的推进动作。";
        }

        if (containsAny(question, "了解我", "我是谁", "怎么理解我", "适合我")) {
            return name + "，从现有记录看，你不是在做一次性的工具，而是在围绕“" + goal + "”搭自己的长期个人系统。"
                    + "\n\n你的当前重点是“" + focus + "”，而且做事方式更偏向“" + workStyle + "”。这意味着 AI 最适合帮你做的，是整理主线、提示偏航、辅助复盘，而不是替你堆更多复杂功能。";
        }

        return name + "，我现在会把你这阶段理解成“" + focus + "”。"
                + "\n\n最近最有代表性的内容主要来自主题“" + summarizeEntry(topTheme) + "”、记录“" + summarizeEntry(topNote) + "”和待推进事项“" + summarizeEntry(pendingTask) + "”。"
                + "\n\n如果你愿意，我下一步可以继续把这些内容整理成周复盘、优先级排序，或者一段更像你的个人画像。";
    }

    private List<String> extractSectionEntries(String context, String sectionTitle) {
        List<String> entries = new ArrayList<>();
        boolean inSection = false;

        for (String rawLine : context.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.equals(sectionTitle)) {
                inSection = true;
                continue;
            }
            if (!inSection) {
                continue;
            }
            if (line.isBlank()) {
                break;
            }
            if (Character.isDigit(line.charAt(0))) {
                int dotIndex = line.indexOf('.');
                entries.add(dotIndex >= 0 ? line.substring(dotIndex + 1).trim() : line);
            }
        }

        return entries;
    }

    private String firstMeaningfulEntry(List<String> entries, String fallback) {
        return entries.isEmpty() ? fallback : summarizeEntry(entries.get(0));
    }

    private String firstMatchingEntry(List<String> entries, String marker, String fallback) {
        for (String entry : entries) {
            if (entry.contains(marker)) {
                return summarizeEntry(entry);
            }
        }
        return entries.isEmpty() ? fallback : summarizeEntry(entries.get(0));
    }

    private String summarizeEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return "";
        }

        String summary = entry.trim();
        if (summary.startsWith("[待推进]")) {
            summary = summary.substring(5).trim();
        } else if (summary.startsWith("[已完成]")) {
            summary = summary.substring(5).trim();
        }

        int separatorIndex = summary.indexOf(" | ");
        if (separatorIndex > 0) {
            summary = summary.substring(0, separatorIndex).trim();
        }

        return summary.isBlank() ? entry.trim() : summary;
    }

    private String extractField(String context, String prefix) {
        for (String rawLine : context.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() || "暂无".equals(value) || "无".equals(value) ? fallback : value;
    }

    private String appendIfPresent(String value, String condition) {
        return condition == null || condition.isBlank() ? "" : value;
    }

    private String cleanPhrase(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[。！？!?,，；;：:]+$", "");
    }

    private int estimateTokens(String content) {
        return content.length() / 2;
    }
}
