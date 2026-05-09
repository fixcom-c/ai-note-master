package com.ainote.service;

public interface AiService {
    AnalyzeResult analyze(String content, Long userId);
    String generateDailyReport(Long userId, String date);
    String answerQuestion(String question, String context, Long userId);
    String getModelName();
}
