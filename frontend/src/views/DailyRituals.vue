<template>
  <div class="rituals-page">
    <div class="page-header">
      <div>
        <h1>每日节奏</h1>
        <p class="subtitle">把计划、执行和复盘固定下来，让这个系统真正成为你每天都会打开的个人工作台。</p>
      </div>
      <div class="header-actions">
        <el-button @click="refreshAll">刷新</el-button>
        <el-button type="primary" @click="syncPlanToTasks" :loading="syncingTasks">
          同步计划为任务
        </el-button>
      </div>
    </div>

    <div class="overview-grid">
      <el-card class="overview-card">
        <strong>{{ todayLabel }}</strong>
        <span>今天</span>
      </el-card>
      <el-card class="overview-card">
        <strong>{{ todayTaskStats.total }}</strong>
        <span>今日计划任务</span>
      </el-card>
      <el-card class="overview-card">
        <strong>{{ todayTaskStats.completed }}</strong>
        <span>今日已完成</span>
      </el-card>
      <el-card class="overview-card">
        <strong>{{ completionRate }}%</strong>
        <span>今日完成率</span>
      </el-card>
    </div>

    <div class="rituals-grid">
      <div class="left-column">
        <el-card class="ritual-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Sunny /></el-icon> 今日计划</span>
              <div class="card-actions">
                <el-button text @click="insertPlanTemplate">重置模板</el-button>
                <el-button type="primary" text @click="savePlan" :loading="savingPlan">
                  保存计划
                </el-button>
              </div>
            </div>
          </template>

          <el-input
            v-model="planTitle"
            placeholder="计划标题"
            class="title-input"
          />
          <el-input
            v-model="planContent"
            type="textarea"
            :rows="16"
            class="content-input"
            placeholder="写下今天最重要的事..."
          />
        </el-card>

        <el-card class="ritual-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><MoonNight /></el-icon> 晚间复盘</span>
              <div class="card-actions">
                <el-button text @click="insertReviewTemplate">重置模板</el-button>
                <el-button type="primary" text @click="saveReview" :loading="savingReview">
                  保存复盘
                </el-button>
              </div>
            </div>
          </template>

          <el-input
            v-model="reviewTitle"
            placeholder="复盘标题"
            class="title-input"
          />
          <el-input
            v-model="reviewContent"
            type="textarea"
            :rows="16"
            class="content-input"
            placeholder="写下今天的完成情况、感受和明天要继续的事情..."
          />
        </el-card>
      </div>

      <div class="right-column">
        <el-card class="coach-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><MagicStick /></el-icon> AI 今日建议</span>
            </div>
          </template>

          <div v-if="insight" class="coach-content">
            <div class="coach-hero">
              <h3>{{ insight.headline }}</h3>
              <p>{{ insight.rhythm }}</p>
            </div>
            <ul class="coach-list">
              <li v-for="item in insight.suggestions" :key="item">{{ item }}</li>
            </ul>
          </div>
        </el-card>

        <el-card class="coach-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><List /></el-icon> 今天的任务面板</span>
            </div>
          </template>

          <div v-if="todayTasks.length" class="task-panel">
            <div v-for="task in todayTasks" :key="task.id" class="task-row">
              <div>
                <strong>{{ task.title }}</strong>
                <p>{{ task.description || '无补充描述' }}</p>
              </div>
              <el-tag :type="task.status === 'completed' ? 'success' : 'warning'">
                {{ task.status === 'completed' ? '已完成' : '待推进' }}
              </el-tag>
            </div>
          </div>
          <el-empty v-else description="今天还没有任务，先写计划再同步。" :image-size="60" />
        </el-card>

        <el-card class="coach-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Reading /></el-icon> 推荐使用方式</span>
            </div>
          </template>

          <div class="habit-list">
            <div class="habit-item">
              <strong>早上 3 分钟</strong>
              <p>只写今天最重要的 1 到 3 件事，不求完整，但求明确。</p>
            </div>
            <div class="habit-item">
              <strong>白天动态更新</strong>
              <p>想到阻碍、临时事项、完成进展，就补进计划或快速记录，不要憋到晚上。</p>
            </div>
            <div class="habit-item">
              <strong>晚上 5 分钟</strong>
              <p>写完成度、原因、感受和明天的第一步，让系统逐渐理解你的真实节奏。</p>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { aiAPI, noteAPI, taskAPI } from '@/api'
import type { Note, PersonalInsight, Task } from '@/api/types'
import { Sunny, MoonNight, MagicStick, List, Reading } from '@element-plus/icons-vue'

const today = dayjs()
const todayKey = today.format('YYYY-MM-DD')
const todayLabel = today.format('MM 月 DD 日')

const planNoteId = ref<number | null>(null)
const reviewNoteId = ref<number | null>(null)
const planTitle = ref('')
const planContent = ref('')
const reviewTitle = ref('')
const reviewContent = ref('')
const savingPlan = ref(false)
const savingReview = ref(false)
const syncingTasks = ref(false)

const insight = ref<PersonalInsight | null>(null)
const notes = ref<Note[]>([])
const todayTasks = ref<Task[]>([])

const completionRate = computed(() => {
  if (!todayTaskStats.value.total) return 0
  return Math.round((todayTaskStats.value.completed / todayTaskStats.value.total) * 100)
})

const todayTaskStats = computed(() => ({
  total: todayTasks.value.length,
  completed: todayTasks.value.filter((task) => task.status === 'completed').length
}))

const buildPlanTemplate = () => {
  return `# 今天最重要的 1-3 件事\n- [ ] \n- [ ] \n- [ ] \n\n# 今日推进\n- 关键任务：\n- 可以延后：\n- 需要沟通/确认：\n\n# 时间安排\n- 上午：\n- 下午：\n- 晚上：\n\n# 可能的阻碍\n- \n\n# 今天想保持的状态\n- `
}

const buildReviewTemplate = () => {
  return `# 今天完成了什么\n- \n\n# 哪些没有完成，原因是什么\n- \n\n# 今天最值得记录的一个发现\n- \n\n# 今天的状态 / 情绪 / 节奏\n- \n\n# 明天最重要的第一步\n- `
}

const insertPlanTemplate = () => {
  planTitle.value = `${todayKey} 日计划`
  planContent.value = buildPlanTemplate()
}

const insertReviewTemplate = () => {
  reviewTitle.value = `${todayKey} 晚间复盘`
  reviewContent.value = buildReviewTemplate()
}

const findTodayNote = (items: Note[], category: string) => {
  return items.find((note) => note.category === category && dayjs(note.createdAt).format('YYYY-MM-DD') === todayKey)
}

const loadNotesAndRituals = async () => {
  notes.value = await noteAPI.list()

  const planNote = findTodayNote(notes.value, 'daily-plan')
  const reviewNote = findTodayNote(notes.value, 'daily-review')

  if (planNote) {
    planNoteId.value = planNote.id
    planTitle.value = planNote.title
    planContent.value = planNote.content
  } else {
    planNoteId.value = null
    insertPlanTemplate()
  }

  if (reviewNote) {
    reviewNoteId.value = reviewNote.id
    reviewTitle.value = reviewNote.title
    reviewContent.value = reviewNote.content
  } else {
    reviewNoteId.value = null
    insertReviewTemplate()
  }
}

const loadTodayTasks = async () => {
  const tasks = await taskAPI.list()
  todayTasks.value = tasks.filter((task) => {
    const compareDate = task.reminderTime || task.createdAt
    return dayjs(compareDate).format('YYYY-MM-DD') === todayKey
  })
}

const loadInsight = async () => {
  insight.value = await aiAPI.personalInsight()
}

const refreshAll = async () => {
  await Promise.all([loadNotesAndRituals(), loadTodayTasks(), loadInsight()])
}

const savePlan = async () => {
  if (!planContent.value.trim()) {
    ElMessage.warning('请先写一点今天的计划')
    return
  }
  savingPlan.value = true
  try {
    const payload = {
      title: planTitle.value || `${todayKey} 日计划`,
      content: planContent.value,
      category: 'daily-plan',
      sourceType: 'manual'
    }
    const saved = planNoteId.value
      ? await noteAPI.update(planNoteId.value, payload)
      : await noteAPI.create(payload)
    planNoteId.value = saved.id
    ElMessage.success('今日计划已保存')
    await refreshAll()
  } catch {
    ElMessage.error('保存计划失败')
  } finally {
    savingPlan.value = false
  }
}

const saveReview = async () => {
  if (!reviewContent.value.trim()) {
    ElMessage.warning('请先写一点复盘内容')
    return
  }
  savingReview.value = true
  try {
    const payload = {
      title: reviewTitle.value || `${todayKey} 晚间复盘`,
      content: reviewContent.value,
      category: 'daily-review',
      sourceType: 'manual'
    }
    const saved = reviewNoteId.value
      ? await noteAPI.update(reviewNoteId.value, payload)
      : await noteAPI.create(payload)
    reviewNoteId.value = saved.id
    ElMessage.success('晚间复盘已保存')
    await refreshAll()
  } catch {
    ElMessage.error('保存复盘失败')
  } finally {
    savingReview.value = false
  }
}

const syncPlanToTasks = async () => {
  if (!planContent.value.trim()) {
    ElMessage.warning('请先保存今日计划')
    return
  }

  syncingTasks.value = true
  try {
    const result = await aiAPI.analyze({ content: planContent.value })
    const existingTitles = new Set(todayTasks.value.map((task) => task.title.trim()))
    let createdCount = 0

    for (const task of result.tasks || []) {
      const title = (task.title || '').trim()
      if (!title || existingTitles.has(title)) continue
      await taskAPI.create({
        title,
        description: task.description || '',
        priority: task.priority || 'medium'
      })
      createdCount += 1
      existingTitles.add(title)
    }

    await Promise.all([loadTodayTasks(), loadInsight()])
    ElMessage.success(createdCount > 0 ? `已同步 ${createdCount} 个任务` : '今天的计划任务已是最新')
  } catch {
    ElMessage.error('同步任务失败')
  } finally {
    syncingTasks.value = false
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.rituals-page {
  padding: 24px;
  max-width: 1360px;
  margin: 0 auto;
}

.page-header,
.card-header,
.card-actions,
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header,
.card-header {
  justify-content: space-between;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 8px;
}

.subtitle {
  color: var(--text-secondary);
  max-width: 760px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.overview-card,
.ritual-card,
.coach-card {
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.overview-card {
  padding: 8px;
}

.overview-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-card strong {
  font-size: 30px;
}

.overview-card span {
  color: var(--text-secondary);
  font-size: 13px;
}

.rituals-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 20px;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.title-input {
  margin-bottom: 14px;
}

.coach-content,
.habit-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.coach-hero {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.08), rgba(19, 184, 166, 0.08));
}

.coach-hero h3 {
  margin-bottom: 8px;
  font-size: 20px;
}

.coach-hero p,
.habit-item p,
.task-row p {
  color: var(--text-secondary);
  line-height: 1.8;
}

.coach-list {
  padding-left: 18px;
  color: var(--text-secondary);
  line-height: 1.9;
}

.task-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-row,
.habit-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.04);
}

.task-row strong,
.habit-item strong {
  display: block;
  margin-bottom: 6px;
}

.habit-item {
  flex-direction: column;
}

@media (max-width: 960px) {
  .rituals-page {
    padding: 16px;
  }

  .page-header,
  .header-actions,
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .overview-grid,
  .rituals-grid {
    grid-template-columns: 1fr;
  }
}
</style>
