<template>
  <div class="reports-page">
    <div class="page-header">
      <div class="header-left">
        <h1>日报</h1>
        <p class="subtitle">每日工作总结与智能复盘</p>
      </div>
      <div class="header-right">
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <div class="report-actions">
      <el-button type="primary" :loading="generating" @click="handleGenerate">
        <el-icon><MagicStick /></el-icon>
        生成今日日报
      </el-button>
      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="选择日期"
        style="width: 170px"
        @change="loadReportByDate"
      />
    </div>

    <el-card class="report-card" v-if="report">
      <template #header>
        <div class="report-header">
          <div class="report-title">
            <el-icon><Document /></el-icon>
            {{ report.date }} 日报
          </div>
          <el-tag :type="report.completedTaskCount > 0 ? 'success' : 'info'">
            {{ report.completedTaskCount }}/{{ report.totalTaskCount }} 任务完成
          </el-tag>
        </div>
      </template>

      <div class="report-content">
        <div class="report-section">
          <h3><el-icon><DataLine /></el-icon> 任务概览</h3>
          <div class="progress-stats">
            <div class="stat-item">
              <span class="stat-label">完成率</span>
              <span class="stat-value">{{ completionRate }}%</span>
              <el-progress :percentage="completionRate" :stroke-width="10" />
            </div>
          </div>
          <div class="task-summary" v-if="report.taskSummary">
            <pre>{{ report.taskSummary }}</pre>
          </div>
          <el-empty v-else description="该日期暂无任务" :image-size="60" />
        </div>

        <div class="report-section">
          <h3><el-icon><Cpu /></el-icon> AI 总结</h3>
          <div class="ai-content" v-if="report.content">
            <pre>{{ report.content }}</pre>
          </div>
          <el-empty v-else description="暂无 AI 总结" :image-size="60" />
        </div>

        <div class="report-meta">
          <span><el-icon><Clock /></el-icon> 生成时间: {{ formatDateTime(report.createdAt) }}</span>
        </div>
      </div>
    </el-card>

    <el-card v-else class="empty-card">
      <el-empty description="暂无日报数据">
        <el-button type="primary" @click="handleGenerate">生成今日日报</el-button>
      </el-empty>
    </el-card>

    <div class="history-section" v-if="recentReports.length > 0">
      <h3>历史日报</h3>
      <div class="history-list">
        <div
          class="history-item"
          v-for="item in recentReports"
          :key="item.date"
          @click="selectReport(item.date)"
        >
          <div class="history-date">{{ item.date }}</div>
          <div class="history-stats">
            <el-tag size="small" :type="item.completedTaskCount > 0 ? 'success' : 'info'">
              {{ item.completedTaskCount }}/{{ item.totalTaskCount }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { reportAPI, taskAPI } from '@/api'
import type { DailyReport, Task } from '@/api/types'
import { MagicStick, Document, DataLine, Cpu, Clock, Download } from '@element-plus/icons-vue'

type ReportSummary = {
  date: string
  completedTaskCount: number
  totalTaskCount: number
}

const report = ref<DailyReport | null>(null)
const generating = ref(false)
const selectedDate = ref<Date | null>(null)
const recentReports = ref<ReportSummary[]>([])

const completionRate = computed(() => {
  if (!report.value || report.value.totalTaskCount === 0) return 0
  return Math.round((report.value.completedTaskCount / report.value.totalTaskCount) * 100)
})

const loadRecentReports = async () => {
  const tasks = await taskAPI.list()
  const dates = [...new Set(tasks.map((task: Task) => dayjs(task.createdAt).format('YYYY-MM-DD')))]

  recentReports.value = dates.slice(0, 7).map((date) => ({
    date,
    completedTaskCount: tasks.filter((task) =>
      dayjs(task.createdAt).format('YYYY-MM-DD') === date && task.status === 'completed'
    ).length,
    totalTaskCount: tasks.filter((task) => dayjs(task.createdAt).format('YYYY-MM-DD') === date).length
  }))
}

const loadCurrentReport = async () => {
  try {
    report.value = await reportAPI.getDaily()
  } catch {
    report.value = null
  }
}

const loadReportByDate = async () => {
  if (!selectedDate.value) {
    await loadCurrentReport()
    return
  }

  const date = dayjs(selectedDate.value).format('YYYY-MM-DD')
  try {
    report.value = await reportAPI.getDaily(date)
  } catch {
    ElMessage.error('加载日报失败')
  }
}

const handleGenerate = async () => {
  generating.value = true
  try {
    report.value = await reportAPI.generateDaily()
    await loadRecentReports()
    ElMessage.success('日报生成成功')
  } catch {
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

const selectReport = async (date: string) => {
  selectedDate.value = new Date(date)
  try {
    report.value = await reportAPI.getDaily(date)
  } catch {
    ElMessage.error('加载日报失败')
  }
}

const handleExport = () => {
  if (!report.value) {
    ElMessage.warning('请先生成日报')
    return
  }

  const rows = [
    ['日期', '任务完成', '任务总数', '完成率', '任务概览', 'AI总结', '生成时间'],
    [
      report.value.date,
      String(report.value.completedTaskCount),
      String(report.value.totalTaskCount),
      `${completionRate.value}%`,
      (report.value.taskSummary || '').replace(/\n/g, ' '),
      (report.value.content || '').replace(/\n/g, ' '),
      formatDateTime(report.value.createdAt)
    ]
  ]

  const csv = rows
    .map((row) => row.map((cell) => `"${String(cell || '').replace(/"/g, '""')}"`).join(','))
    .join('\n')

  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `日报_${report.value.date}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('导出成功')
}

const formatDateTime = (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm')

onMounted(async () => {
  await Promise.all([loadCurrentReport(), loadRecentReports()])
})
</script>

<style scoped>
.reports-page {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header,
.report-actions,
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 6px;
}

.subtitle {
  color: var(--text-secondary);
}

.report-actions {
  margin-bottom: 20px;
}

.report-card,
.empty-card {
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.report-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.report-section h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.task-summary pre,
.ai-content pre {
  padding: 18px;
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.04);
  white-space: pre-wrap;
  line-height: 1.8;
  font-family: inherit;
}

.report-meta {
  color: #7b8798;
}

.history-section {
  margin-top: 28px;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.history-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

@media (max-width: 720px) {
  .reports-page {
    padding: 16px;
  }

  .page-header,
  .report-actions,
  .report-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
