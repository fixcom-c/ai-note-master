package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.dto.PersonalChatRequest;
import com.ainote.service.AiService;
import com.ainote.service.AnalyzeResult;
import com.ainote.service.PersonalChatReply;
import com.ainote.service.PersonalChatService;
import com.ainote.service.PersonalInsight;
import com.ainote.service.PersonalInsightService;
import com.ainote.service.UserContextService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final UserContextService userContextService;
    private final PersonalInsightService personalInsightService;
    private final PersonalChatService personalChatService;

    public AiController(AiService aiService,
                        UserContextService userContextService,
                        PersonalInsightService personalInsightService,
                        PersonalChatService personalChatService) {
        this.aiService = aiService;
        this.userContextService = userContextService;
        this.personalInsightService = personalInsightService;
        this.personalChatService = personalChatService;
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

    @GetMapping("/personal-insight")
    public Result<PersonalInsight> personalInsight() {
        return Result.success(personalInsightService.generate());
    }

    @PostMapping("/personal-chat")
    public Result<PersonalChatReply> personalChat(@RequestBody PersonalChatRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return Result.error(400, "问题不能为空");
        }
        return Result.success(personalChatService.ask(request.getQuestion()));
    }
}
