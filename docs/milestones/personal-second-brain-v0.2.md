# Personal Second Brain Milestone

Version: `0.2.0`  
Date: `2026-05-09`

## Milestone Theme

把项目从“通用 AI 记录工具”推进到“适合个人每天使用的 AI 工作台”。

这一版最重要的变化，不是单纯多了几个页面，而是产品主线开始稳定下来：

- 先记录
- 再拆成行动
- 然后复盘
- 最后让 AI 逐渐理解你

## What This Version Delivers

### 1. 个人长期上下文

新增 `个人空间`，可以沉淀这些长期信息：

- 你是谁
- 当前重点
- 长期目标
- 工作风格
- 常见生活/工作主题
- 你希望 AI 如何给建议

这让系统不再只是处理单次输入，而是开始积累“对你这个人”的理解。

### 2. 每日节奏固定下来

新增 `每日节奏` 页面，建立最适合个人高频使用的固定入口：

- 早上写 `今日计划`
- 晚上写 `晚间复盘`
- 中间可以把计划同步成任务
- 页面同时显示今日任务和 AI 建议

这一步很关键，因为它让产品从“偶尔记录”变成“每天会打开”。

### 3. 笔记不再只是纯文本

笔记现在支持：

- 标题
- 分类
- 来源类型
- 文本文件导入

这让你的记录开始更像个人资产，而不是一堆零散内容。

### 4. AI 建议开始更像私人系统

AI 洞察现在会综合：

- 任务状态
- 知识标签
- 最近笔记
- 个人画像

输出结果更接近“理解你的节奏后给建议”，而不是一次性的通用摘要。

## Recommended Daily Usage

### Morning

- 打开 `每日节奏`
- 只写今天最重要的 1 到 3 件事
- 必要时同步成任务

### Daytime

- 所有灵感、会议、资料、待办都先放进 `快速记录`
- 不要求整理完成，只要求先收进系统

### Evening

- 回到 `每日节奏`
- 写完成情况、卡点、状态和明天第一步

### Weekly

- 导入旧笔记、资料、复盘、规划
- 让 AI 更快看出你的长期主题

## What Was Validated

- Backend tests passed
- Frontend build passed
- Login API verified
- Profile save verified
- Daily plan create and update verified
- Evening review save verified
- Personal insight API verified

## Next Best Priorities

### Priority 1

做 `周复盘` 模板页，让每天的记录开始沉淀成周级总结。

### Priority 2

做“和我的笔记聊天”，让 AI 能基于个人资料、计划、复盘和历史笔记直接回答问题。

### Priority 3

支持更丰富的导入能力，例如：

- PDF
- DOCX
- 图片 OCR
- 批量导入历史 Markdown

### Priority 4

增加长期趋势视图，例如：

- 完成率走势
- 常见主题变化
- 最常卡住的任务类型
- 最近一周 / 一月的节奏总结

## Release Summary

`0.2.0` 是这个项目第一次真正具备“个人第二大脑”雏形的版本。

如果 `0.1.0` 解决的是“能不能用”，那 `0.2.0` 解决的是“值不值得每天用”。
