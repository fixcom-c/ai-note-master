package com.ainote.service;

import com.ainote.repository.AiCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MiniMaxAiServiceImplTest {

    @Mock
    private AiCallLogRepository aiCallLogRepository;

    @Test
    void shouldAnswerDifferentQuestionTypesWithDifferentMockReplies() {
        MiniMaxAiServiceImpl service = new MiniMaxAiServiceImpl(aiCallLogRepository, new ObjectMapper());
        ReflectionTestUtils.setField(service, "mockMode", true);
        ReflectionTestUtils.setField(service, "apiKey", "");

        String context = """
                个人画像
                - 称呼: 阿峰
                - 当前重点: 把 AI Note 打磨成每天都会打开的个人系统
                - 长期目标: 沉淀自己的长期节奏与判断
                - 工作风格: 先快速记录，再集中整理，再推进
                - AI 偏好: 直接、少空话

                最近长期主题
                1. 个人第二大脑 | 标签: 产品,复盘 | 摘要: 反复出现的主线是记录、复盘和 AI 理解
                2. 稳定日周节奏 | 标签: 计划,复盘 | 摘要: 建立每天能反复打开的节奏

                最近笔记
                1. 2026-05-09 日计划 | 分类: daily-plan | 摘要: 今天最重要的是检查前后端状态并继续推进个人助手体验

                最近任务
                1. [待推进] 整理接下来三件最重要的事 | 优先级: high | 描述: 收敛产品主线
                2. [已完成] 完成每日计划模板联调 | 优先级: medium | 描述: 已经联通前后端
                """;

        String focusAnswer = service.answerQuestion("我最近最该聚焦什么？", context, 1L);
        String themeAnswer = service.answerQuestion("最近反复出现的主题是什么？", context, 1L);

        assertTrue(focusAnswer.contains("每天都会打开的个人系统"));
        assertTrue(focusAnswer.contains("整理接下来三件最重要的事"));
        assertTrue(themeAnswer.contains("个人第二大脑"));
        assertTrue(themeAnswer.contains("稳定日周节奏"));
        assertNotEquals(focusAnswer, themeAnswer);
    }
}
