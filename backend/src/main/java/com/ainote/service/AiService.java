package com.ainote.service;

public interface AiService {
    AnalyzeResult analyze(String content, Long userId);
    String generateDailyReport(Long userId, String date);
    String getModelName();
}