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

    private int estimateTokens(String content) {
        return content.length() / 2;
    }
}
