package com.ainote.service;

import com.ainote.entity.Knowledge;
import com.ainote.entity.PersonalProfile;
import com.ainote.entity.QuickNote;
import com.ainote.entity.Task;
import com.ainote.repository.KnowledgeRepository;
import com.ainote.repository.QuickNoteRepository;
import com.ainote.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonalInsightService {

    private final TaskRepository taskRepository;
    private final QuickNoteRepository quickNoteRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final PersonalProfileService personalProfileService;
    private final UserContextService userContextService;

    public PersonalInsightService(TaskRepository taskRepository,
                                  QuickNoteRepository quickNoteRepository,
                                  KnowledgeRepository knowledgeRepository,
                                  PersonalProfileService personalProfileService,
                                  UserContextService userContextService) {
        this.taskRepository = taskRepository;
        this.quickNoteRepository = quickNoteRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.personalProfileService = personalProfileService;
        this.userContextService = userContextService;
    }

    public PersonalInsight generate() {
        Long userId = userContextService.getCurrentUserId();
        PersonalProfile profile = personalProfileService.getProfile();
        List<Task> tasks = taskRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        List<QuickNote> notes = quickNoteRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        List<Knowledge> knowledge = knowledgeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        LocalDate today = LocalDate.now();
        List<Task> todayTasks = tasks.stream()
                .filter(task -> {
                    LocalDate compareDate = task.getReminderTime() != null
                            ? task.getReminderTime().toLocalDate()
                            : task.getCreatedAt().toLocalDate();
                    return today.equals(compareDate);
                })
                .toList();

        int todayCompleted = (int) todayTasks.stream()
                .filter(task -> "completed".equals(task.getStatus()))
                .count();
        int pendingTasks = (int) tasks.stream()
                .filter(task -> "pending".equals(task.getStatus()))
                .count();

        PersonalInsight insight = new PersonalInsight();
        insight.setTodayPlanned(todayTasks.size());
        insight.setTodayCompleted(todayCompleted);
        insight.setPendingTasks(pendingTasks);
        insight.setFocusTags(extractFocusTags(knowledge));
        insight.setHeadline(buildHeadline(profile, todayTasks.size(), todayCompleted, pendingTasks));
        insight.setUnderstanding(buildUnderstanding(profile, notes, insight.getFocusTags()));
        insight.setRhythm(buildRhythm(notes, todayTasks.size(), todayCompleted));
        insight.setSuggestions(buildSuggestions(profile, notes, todayTasks, pendingTasks, insight.getFocusTags()));
        return insight;
    }

    private List<String> extractFocusTags(List<Knowledge> knowledge) {
        Map<String, Long> counts = knowledge.stream()
                .limit(12)
                .flatMap(item -> Arrays.stream(Optional.ofNullable(item.getTags()).orElse("").split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(4)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String buildHeadline(PersonalProfile profile, int todayPlanned, int todayCompleted, int pendingTasks) {
        String name = safeValue(profile.getDisplayName(), "你");
        if (todayPlanned == 0 && pendingTasks == 0) {
            return name + "，今天适合轻量记录和整理想法。";
        }
        if (todayCompleted >= Math.max(1, todayPlanned)) {
            return name + "，你今天的执行节奏很稳，可以开始为明天铺路。";
        }
        if (pendingTasks >= 5) {
            return name + "，当前待办偏多，先收敛优先级会更轻松。";
        }
        return name + "，今天最值得做的是把记录转成 1 到 2 个明确行动。";
    }

    private String buildUnderstanding(PersonalProfile profile, List<QuickNote> notes, List<String> focusTags) {
        List<String> chunks = new ArrayList<>();
        if (!blank(profile.getIdentitySummary())) {
            chunks.add("我正在把你当作“" + profile.getIdentitySummary().trim() + "”来理解");
        }
        if (!blank(profile.getCurrentFocus())) {
            chunks.add("当前阶段更关注“" + profile.getCurrentFocus().trim() + "”");
        }
        if (!focusTags.isEmpty()) {
            chunks.add("最近最常出现的主题是 " + String.join(" / ", focusTags));
        }
        if (notes.stream().anyMatch(note -> "imported".equals(note.getSourceType()))) {
            chunks.add("你已经开始把外部资料也纳入这套个人系统");
        }
        if (chunks.isEmpty()) {
            return "先补充一点你的个人背景和目标，我就能给出更贴近你的建议。";
        }
        return String.join("，", chunks) + "。";
    }

    private String buildRhythm(List<QuickNote> notes, int todayPlanned, int todayCompleted) {
        long recentNotes = notes.stream().limit(10).count();
        if (recentNotes >= 5 && todayPlanned > todayCompleted) {
            return "你记录很积极，但执行层还可以更聚焦，建议每天只盯住最重要的 1 到 3 件事。";
        }
        if (recentNotes <= 2) {
            return "你的记录密度还不高，适合把这里当作日常 inbox，先记碎片，不必等完整想法。";
        }
        return "你的记录和行动正在形成闭环，继续保持“先记录，再拆任务，最后复盘”的节奏。";
    }

    private List<String> buildSuggestions(PersonalProfile profile,
                                          List<QuickNote> notes,
                                          List<Task> todayTasks,
                                          int pendingTasks,
                                          List<String> focusTags) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        if (blank(profile.getCurrentFocus()) || blank(profile.getLongTermGoals())) {
            suggestions.add("去个人空间补全“当前重点”和“长期目标”，AI 给出的建议会更像你的私人教练。");
        }
        if (pendingTasks >= 5) {
            suggestions.add("把今天待办压缩到 3 件以内，先完成一个最容易推进的任务，建立行动势能。");
        }
        if (notes.stream().noneMatch(note -> "imported".equals(note.getSourceType()))) {
            suggestions.add("导入一份你的旧笔记或资料，让系统开始理解你长期关心的话题。");
        }
        if (todayTasks.isEmpty()) {
            suggestions.add("把今天的计划单独记成一条“日计划”笔记，晚上更容易做完成度复盘。");
        }
        if (!focusTags.isEmpty()) {
            suggestions.add("围绕“" + focusTags.get(0) + "”持续沉淀，连续几天之后会更容易看出你的稳定兴趣。");
        }
        if (!blank(profile.getAiPreference())) {
            suggestions.add("后续建议会优先遵循你的偏好： " + profile.getAiPreference().trim());
        }
        return new ArrayList<>(suggestions);
    }

    private String safeValue(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
