package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.service.AiService;
import com.ainote.service.AnalyzeResult;
import com.ainote.service.UserContextService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final UserContextService userContextService;

    public AiController(AiService aiService, UserContextService userContextService) {
        this.aiService = aiService;
        this.userContextService = userContextService;
    }

    @PostMapping("/analyze")
    public Result<AnalyzeResult> analyze(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.isBlank()) {
            return Result.error(400, "内容不能为空");
        }
        Long userId = userContextService.getCurrentUserId();
        return Result.success(aiService.analyze(content, userId));
    }
}