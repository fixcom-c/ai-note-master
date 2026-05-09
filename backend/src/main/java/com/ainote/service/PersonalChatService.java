package com.ainote.service;

import com.ainote.common.BusinessException;
import com.ainote.entity.Knowledge;
import com.ainote.entity.PersonalProfile;
import com.ainote.entity.QuickNote;
import com.ainote.entity.Task;
import com.ainote.repository.KnowledgeRepository;
import com.ainote.repository.QuickNoteRepository;
import com.ainote.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonalChatService {

    private final QuickNoteRepository quickNoteRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final TaskRepository taskRepository;
    private final PersonalProfileService personalProfileService;
    private final UserContextService userContextService;
    private final AiService aiService;

    public PersonalChatService(QuickNoteRepository quickNoteRepository,
                               KnowledgeRepository knowledgeRepository,
                               TaskRepository taskRepository,
                               PersonalProfileService personalProfileService,
                               UserContextService userContextService,
                               AiService aiService) {
        this.quickNoteRepository = quickNoteRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.taskRepository = taskRepository;
        this.personalProfileService = personalProfileService;
        this.userContextService = userContextService;
        this.aiService = aiService;
    }

    public PersonalChatReply ask(String question) {
        if (question == null || question.isBlank()) {
            throw new BusinessException("问题不能为空");
        }

        Long userId = userContextService.getCurrentUserId();
        String trimmedQuestion = question.trim();
        PersonalProfile profile = personalProfileService.getProfile();
        List<QuickNote> notes = quickNoteRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        List<Knowledge> knowledgeList = knowledgeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        List<Task> tasks = taskRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        List<QuickNote> selectedNotes = selectRelevantNotes(trimmedQuestion, notes, 5);
        List<Knowledge> selectedKnowledge = selectRelevantKnowledge(trimmedQuestion, knowledgeList, 4);
        List<Task> selectedTasks = selectRelevantTasks(trimmedQuestion, tasks, 4);

        String context = buildContext(profile, selectedNotes, selectedKnowledge, selectedTasks);

        PersonalChatReply reply = new PersonalChatReply();
        reply.setAnswer(aiService.answerQuestion(trimmedQuestion, context, userId));
        reply.setContextSummary(buildContextSummary(selectedNotes, selectedKnowledge, selectedTasks));
        reply.setModel(aiService.getModelName());
        reply.setReferences(buildReferences(selectedNotes, selectedKnowledge, selectedTasks));
        reply.setSuggestions(buildSuggestions(trimmedQuestion, selectedKnowledge, selectedTasks));
        return reply;
    }

    private List<QuickNote> selectRelevantNotes(String question, List<QuickNote> notes, int limit) {
        return notes.stream()
                .sorted(Comparator.comparingInt((QuickNote note) -> scoreNote(question, note)).reversed()
                        .thenComparing(QuickNote::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private List<Knowledge> selectRelevantKnowledge(String question, List<Knowledge> knowledgeList, int limit) {
        return knowledgeList.stream()
                .sorted(Comparator.comparingInt((Knowledge item) -> scoreKnowledge(question, item)).reversed()
                        .thenComparing(Knowledge::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private List<Task> selectRelevantTasks(String question, List<Task> tasks, int limit) {
        return tasks.stream()
                .sorted(Comparator.comparingInt((Task task) -> scoreTask(question, task)).reversed()
                        .thenComparing(task -> task.getUpdatedAt() != null ? task.getUpdatedAt() : task.getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private String buildContext(PersonalProfile profile,
                                List<QuickNote> notes,
                                List<Knowledge> knowledgeList,
                                List<Task> tasks) {
        StringBuilder builder = new StringBuilder();
        builder.append("个人画像\n");
        builder.append("- 称呼: ").append(valueOrDefault(profile.getDisplayName(), "用户")).append('\n');
        builder.append("- 当前重点: ").append(valueOrDefault(profile.getCurrentFocus(), "暂无")).append('\n');
        builder.append("- 长期目标: ").append(valueOrDefault(profile.getLongTermGoals(), "暂无")).append('\n');
        builder.append("- 工作风格: ").append(valueOrDefault(profile.getWorkStyle(), "暂无")).append('\n');
        builder.append("- AI 偏好: ").append(valueOrDefault(profile.getAiPreference(), "直接、实用")).append("\n\n");

        builder.append("最近长期主题\n");
        if (knowledgeList.isEmpty()) {
            builder.append("无\n");
        } else {
            for (int i = 0; i < knowledgeList.size(); i++) {
                Knowledge item = knowledgeList.get(i);
                builder.append(i + 1)
                        .append(". ")
                        .append(item.getTitle())
                        .append(" | 标签: ")
                        .append(valueOrDefault(item.getTags(), "无"))
                        .append(" | 摘要: ")
                        .append(clip(item.getContent(), 100))
                        .append('\n');
            }
        }

        builder.append("\n最近笔记\n");
        if (notes.isEmpty()) {
            builder.append("无\n");
        } else {
            for (int i = 0; i < notes.size(); i++) {
                QuickNote note = notes.get(i);
                builder.append(i + 1)
                        .append(". ")
                        .append(note.getTitle())
                        .append(" | 分类: ")
                        .append(valueOrDefault(note.getCategory(), "收件箱"))
                        .append(" | 摘要: ")
                        .append(clip(note.getContent(), 100))
                        .append('\n');
            }
        }

        builder.append("\n最近任务\n");
        if (tasks.isEmpty()) {
            builder.append("无\n");
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                builder.append(i + 1)
                        .append(". [")
                        .append("completed".equals(task.getStatus()) ? "已完成" : "待推进")
                        .append("] ")
                        .append(task.getTitle())
                        .append(" | 优先级: ")
                        .append(valueOrDefault(task.getPriority(), "medium"))
                        .append(" | 描述: ")
                        .append(clip(task.getDescription(), 80))
                        .append('\n');
            }
        }

        return builder.toString();
    }

    private String buildContextSummary(List<QuickNote> notes, List<Knowledge> knowledgeList, List<Task> tasks) {
        return "本次回答参考了 "
                + notes.size() + " 条笔记、"
                + knowledgeList.size() + " 个长期主题、"
                + tasks.size() + " 个最近任务。";
    }

    private List<PersonalChatReference> buildReferences(List<QuickNote> notes,
                                                        List<Knowledge> knowledgeList,
                                                        List<Task> tasks) {
        List<PersonalChatReference> references = new ArrayList<>();

        for (QuickNote note : notes.stream().limit(2).toList()) {
            PersonalChatReference reference = new PersonalChatReference();
            reference.setType("note");
            reference.setTitle(note.getTitle());
            reference.setSummary(clip(note.getContent(), 70));
            references.add(reference);
        }

        for (Knowledge item : knowledgeList.stream().limit(2).toList()) {
            PersonalChatReference reference = new PersonalChatReference();
            reference.setType("theme");
            reference.setTitle(item.getTitle());
            reference.setSummary(clip(item.getContent(), 70));
            references.add(reference);
        }

        for (Task task : tasks.stream().limit(2).toList()) {
            PersonalChatReference reference = new PersonalChatReference();
            reference.setType("task");
            reference.setTitle(task.getTitle());
            reference.setSummary(("completed".equals(task.getStatus()) ? "已完成" : "待推进") + " · " + clip(task.getDescription(), 60));
            references.add(reference);
        }

        return references;
    }

    private List<String> buildSuggestions(String question, List<Knowledge> knowledgeList, List<Task> tasks) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        String normalized = question.toLowerCase(Locale.ROOT);

        if (normalized.contains("聚焦") || normalized.contains("重点") || normalized.contains("优先")) {
            suggestions.add("结合最近任务，帮我拆成接下来 3 个最值得做的动作");
            suggestions.add("基于我的长期主题，提醒我哪些事其实可以先不做");
        } else if (normalized.contains("复盘") || normalized.contains("总结")) {
            suggestions.add("把我最近一周的记录整理成一段周复盘草稿");
            suggestions.add("告诉我最近反复出现的卡点是什么");
        } else {
            suggestions.add("结合我的最近任务，告诉我下一步最值得推进什么");
            suggestions.add("从我的长期主题里，帮我总结最近真正稳定在投入的方向");
        }

        if (!knowledgeList.isEmpty()) {
            suggestions.add("围绕“" + knowledgeList.get(0).getTitle() + "”，我接下来最该继续沉淀什么");
        }
        if (!tasks.isEmpty()) {
            suggestions.add("把最近待推进的事收敛成 1 到 3 个明确优先级");
        }

        return new ArrayList<>(suggestions).stream().limit(4).toList();
    }

    private int scoreNote(String question, QuickNote note) {
        int score = scoreText(question, note.getTitle(), note.getContent(), note.getCategory());
        if (question.contains("复盘") && "daily-review".equals(note.getCategory())) score += 4;
        if (question.contains("周") && "weekly-review".equals(note.getCategory())) score += 5;
        if (question.contains("计划") && "daily-plan".equals(note.getCategory())) score += 4;
        return score;
    }

    private int scoreKnowledge(String question, Knowledge item) {
        return scoreText(question, item.getTitle(), item.getContent(), item.getTags()) + 2;
    }

    private int scoreTask(String question, Task task) {
        int score = scoreText(question, task.getTitle(), task.getDescription(), task.getPriority(), task.getStatus());
        if ("pending".equals(task.getStatus())) score += 1;
        return score;
    }

    private int scoreText(String question, String... parts) {
        String haystack = Arrays.stream(parts)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        int score = 0;

        if (!haystack.isBlank() && haystack.contains(normalizedQuestion)) {
            score += 8;
        }

        for (String token : extractKeywords(question)) {
            if (haystack.contains(token.toLowerCase(Locale.ROOT))) {
                score += token.length() >= 4 ? 4 : 2;
            }
        }

        return score;
    }

    private List<String> extractKeywords(String question) {
        return Arrays.stream(question.split("[\\s，。！？、,.!?：:；;\\-_/]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .distinct()
                .toList();
    }

    private String clip(String value, int limit) {
        if (value == null || value.isBlank()) {
            return "暂无";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit) + "...";
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
