<template>
  <div class="rituals-page">
    <div class="page-header">
      <div>
        <h1>每日节奏</h1>
        <p class="subtitle">把日计划、晚间复盘和周总结固定下来，让这个系统真正成为你长期会打开的个人工作台。</p>
      </div>
      <div class="header-actions">
        <div class="mode-switch">
          <button
            type="button"
            class="mode-pill"
            :class="{ active: activeMode === 'daily' }"
            @click="activeMode = 'daily'"
          >
            日节奏
          </button>
          <button
            type="button"
            class="mode-pill"
            :class="{ active: activeMode === 'weekly' }"
            @click="activeMode = 'weekly'"
          >
            周复盘
          </button>
        </div>
        <el-button @click="refreshAll">刷新</el-button>
        <el-button
          v-if="activeMode === 'daily'"
          type="primary"
          @click="syncPlanToTasks"
          :loading="syncingTasks"
        >
          同步计划为任务
        </el-button>
      </div>
    </div>

    <template v-if="activeMode === 'daily'">
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
    </template>

    <template v-else>
      <div class="overview-grid">
        <el-card class="overview-card">
          <strong>{{ weekRangeShortLabel }}</strong>
          <span>本周范围</span>
        </el-card>
        <el-card class="overview-card">
          <strong>{{ weeklyTaskStats.total }}</strong>
          <span>本周任务数</span>
        </el-card>
        <el-card class="overview-card">
          <strong>{{ weeklyReviewCount }}</strong>
          <span>本周日复盘次数</span>
        </el-card>
        <el-card class="overview-card">
          <strong>{{ weeklyCompletionRate }}%</strong>
          <span>本周完成率</span>
        </el-card>
      </div>

      <div class="rituals-grid">
        <div class="left-column">
          <el-card class="ritual-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><Calendar /></el-icon> 本周复盘</span>
                <div class="card-actions">
                  <el-button text @click="insertWeeklyReviewTemplate">重置模板</el-button>
                  <el-button type="primary" text @click="saveWeeklyReview" :loading="savingWeeklyReview">
                    保存周复盘
                  </el-button>
                </div>
              </div>
            </template>

            <div class="weekly-cover">
              <strong>{{ weekRangeLabel }}</strong>
              <span>把这一周的推进、卡点、节奏和下周重点沉淀下来。</span>
            </div>
            <el-input
              v-model="weeklyReviewTitle"
              placeholder="周复盘标题"
              class="title-input"
            />
            <el-input
              v-model="weeklyReviewContent"
              type="textarea"
              :rows="24"
              class="content-input"
              placeholder="写下这周最值得总结的事..."
            />
          </el-card>
        </div>

        <div class="right-column">
          <el-card class="coach-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><MagicStick /></el-icon> 本周观察</span>
              </div>
            </template>

            <div class="coach-content">
              <div class="coach-hero weekly-hero">
                <h3>{{ weeklyCoachHeadline }}</h3>
                <p>{{ weeklyCoachSummary }}</p>
              </div>
              <ul class="coach-list">
                <li v-for="item in weeklyCoachSuggestions" :key="item">{{ item }}</li>
              </ul>
            </div>
          </el-card>

          <el-card class="coach-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><Histogram /></el-icon> 本周推进概览</span>
              </div>
            </template>

            <div class="mini-stat-grid">
              <div class="mini-stat">
                <strong>{{ weeklyTaskStats.completed }}</strong>
                <span>已完成</span>
              </div>
              <div class="mini-stat">
                <strong>{{ weeklyTaskStats.pending }}</strong>
                <span>待继续</span>
              </div>
              <div class="mini-stat">
                <strong>{{ weeklyCaptureCount }}</strong>
                <span>本周记录</span>
              </div>
            </div>

            <div v-if="weeklyTaskHighlights.length" class="task-panel">
              <div v-for="task in weeklyTaskHighlights" :key="task.id" class="task-row">
                <div>
                  <strong>{{ task.title }}</strong>
                  <p>{{ task.description || '无补充描述' }}</p>
                </div>
                <el-tag :type="task.status === 'completed' ? 'success' : 'warning'">
                  {{ task.status === 'completed' ? '已完成' : '待推进' }}
                </el-tag>
              </div>
            </div>
            <el-empty v-else description="本周还没有任务沉淀，先从每日计划开始。" :image-size="60" />
          </el-card>

          <el-card class="coach-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><Reading /></el-icon> 本周沉淀主题</span>
              </div>
            </template>

            <div class="weekly-meta-card">
              <div class="tag-cloud">
                <el-tag
                  v-for="item in weeklyCategorySummary"
                  :key="item.label"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  {{ item.label }} {{ item.count }}
                </el-tag>
              </div>
              <div class="habit-list compact">
                <div class="habit-item">
                  <strong>这周最适合回看的内容</strong>
                  <p>{{ weeklyHighlightText }}</p>
                </div>
                <div class="habit-item">
                  <strong>下周继续使用的建议</strong>
                  <p>保留日计划入口不变，周末只需要补一篇周复盘，系统就能开始形成你的长期节奏记忆。</p>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { aiAPI, noteAPI, taskAPI } from '@/api'
import type { Note, PersonalInsight, Task } from '@/api/types'
import {
  Sunny,
  MoonNight,
  MagicStick,
  List,
  Reading,
  Calendar,
  Histogram
} from '@element-plus/icons-vue'

const activeMode = ref<'daily' | 'weekly'>('daily')
const today = dayjs()
const todayKey = today.format('YYYY-MM-DD')
const todayLabel = today.format('MM 月 DD 日')

const mondayOffset = (today.day() + 6) % 7
const weekStart = today.subtract(mondayOffset, 'day').startOf('day')
const weekEnd = weekStart.add(6, 'day').endOf('day')
const weekRangeLabel = `${weekStart.format('MM 月 DD 日')} - ${weekEnd.format('MM 月 DD 日')}`
const weekRangeShortLabel = `${weekStart.format('MM/DD')} - ${weekEnd.format('MM/DD')}`
const weekReviewTitleFallback = `${weekStart.format('YYYY-MM-DD')} 至 ${weekEnd.format('YYYY-MM-DD')} 周复盘`

const planNoteId = ref<number | null>(null)
const reviewNoteId = ref<number | null>(null)
const weeklyReviewNoteId = ref<number | null>(null)
const planTitle = ref('')
const planContent = ref('')
const reviewTitle = ref('')
const reviewContent = ref('')
const weeklyReviewTitle = ref('')
const weeklyReviewContent = ref('')
const savingPlan = ref(false)
const savingReview = ref(false)
const savingWeeklyReview = ref(false)
const syncingTasks = ref(false)

const insight = ref<PersonalInsight | null>(null)
const notes = ref<Note[]>([])
const tasks = ref<Task[]>([])

const todayTasks = computed(() =>
  tasks.value.filter((task) => dayjs(task.reminderTime || task.createdAt).format('YYYY-MM-DD') === todayKey)
)

const weeklyTasks = computed(() =>
  tasks.value.filter((task) => {
    const compareDate = dayjs(task.reminderTime || task.createdAt)
    return !compareDate.isBefore(weekStart) && !compareDate.isAfter(weekEnd)
  })
)

const weeklyNotes = computed(() =>
  notes.value.filter((note) => {
    const created = dayjs(note.createdAt)
    return !created.isBefore(weekStart) && !created.isAfter(weekEnd)
  })
)

const completionRate = computed(() => {
  if (!todayTaskStats.value.total) return 0
  return Math.round((todayTaskStats.value.completed / todayTaskStats.value.total) * 100)
})

const todayTaskStats = computed(() => ({
  total: todayTasks.value.length,
  completed: todayTasks.value.filter((task) => task.status === 'completed').length
}))

const weeklyTaskStats = computed(() => ({
  total: weeklyTasks.value.length,
  completed: weeklyTasks.value.filter((task) => task.status === 'completed').length,
  pending: weeklyTasks.value.filter((task) => task.status !== 'completed').length
}))

const weeklyCompletionRate = computed(() => {
  if (!weeklyTaskStats.value.total) return 0
  return Math.round((weeklyTaskStats.value.completed / weeklyTaskStats.value.total) * 100)
})

const weeklyReviewCount = computed(() =>
  weeklyNotes.value.filter((note) => note.category === 'daily-review').length
)

const weeklyCaptureCount = computed(() =>
  weeklyNotes.value.filter((note) => note.category !== 'weekly-review').length
)

const weeklyTaskHighlights = computed(() =>
  [...weeklyTasks.value]
    .sort((a, b) => {
      if (a.status !== b.status) {
        return a.status === 'completed' ? 1 : -1
      }
      return dayjs(b.updatedAt || b.createdAt).valueOf() - dayjs(a.updatedAt || a.createdAt).valueOf()
    })
    .slice(0, 5)
)

const weeklyCategorySummary = computed(() => {
  const labelMap: Record<string, string> = {
    inbox: '收件箱',
    'daily-plan': '日计划',
    'daily-review': '晚间复盘',
    'weekly-review': '周复盘',
    work: '工作',
    life: '生活',
    learning: '学习',
    idea: '灵感',
    reference: '资料'
  }

  const counts = new Map<string, number>()
  for (const note of weeklyNotes.value) {
    const label = labelMap[note.category] || note.category || '记录'
    counts.set(label, (counts.get(label) || 0) + 1)
  }

  return Array.from(counts.entries())
    .map(([label, count]) => ({ label, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)
})

const weeklyHighlightText = computed(() => {
  if (!weeklyCategorySummary.value.length) {
    return '这周记录还不多，先保留一篇周复盘，后面再让主题慢慢浮现。'
  }
  return `这周你最常沉淀的是 ${weeklyCategorySummary.value.map((item) => item.label).join('、')}，很适合回看这些主题里的重复问题和稳定推进点。`
})

const weeklyCoachHeadline = computed(() => {
  if (!weeklyTaskStats.value.total && !weeklyCaptureCount.value) {
    return '这周还比较轻，先补一次周复盘，把最近真实状态收进系统。'
  }
  if (weeklyCompletionRate.value >= 70) {
    return '这周执行已经比较稳，适合把有效方法写清楚，变成下周的固定动作。'
  }
  if (weeklyTaskStats.value.pending >= 5) {
    return '这周待推进的事情偏多，周复盘里更适合先砍掉次要项。'
  }
  return '这周已经有一些行动和记录，最值得做的是把分散内容整理成 2 到 3 条结论。'
})

const weeklyCoachSummary = computed(() => {
  const chunks = [
    `本周完成了 ${weeklyTaskStats.value.completed}/${weeklyTaskStats.value.total || 0} 个任务`,
    `写了 ${weeklyReviewCount.value} 次晚间复盘`,
    `新增了 ${weeklyCaptureCount.value} 条记录`
  ]
  return chunks.join('，') + '。'
})

const weeklyCoachSuggestions = computed(() => {
  const items: string[] = []

  if (weeklyReviewCount.value < 2) {
    items.push('如果这周晚间复盘偏少，周复盘里优先补“哪些事反复拖延、为什么”。')
  }
  if (weeklyTaskStats.value.pending >= 5) {
    items.push('下周只保留最关键的 3 个推进目标，其余先放进收件箱，别让任务列表持续膨胀。')
  }
  if (weeklyCategorySummary.value.length) {
    items.push(`围绕“${weeklyCategorySummary.value[0].label}”继续沉淀，下周更容易看出你稳定在投入什么。`)
  }
  if (weeklyCompletionRate.value >= 70) {
    items.push('这周有效的方法别只停在感觉上，写进周复盘，后面才能复用。')
  }
  if (!items.length) {
    items.push('先完成一篇周复盘，系统接下来才能更像了解你，而不是只保存内容。')
  }

  return items.slice(0, 4)
})

const buildPlanTemplate = () => {
  return `# 今天最重要的 1-3 件事\n- [ ] \n- [ ] \n- [ ] \n\n# 今日推进\n- 关键任务：\n- 可以延后：\n- 需要沟通/确认：\n\n# 时间安排\n- 上午：\n- 下午：\n- 晚上：\n\n# 可能的阻碍\n- \n\n# 今天想保持的状态\n- `
}

const buildReviewTemplate = () => {
  return `# 今天完成了什么\n- \n\n# 哪些没有完成，原因是什么\n- \n\n# 今天最值得记录的一个发现\n- \n\n# 今天的状态 / 情绪 / 节奏\n- \n\n# 明天最重要的第一步\n- `
}

const buildWeeklyReviewTemplate = () => {
  return `# 这周最值得肯定的 3 件事\n- \n- \n- \n\n# 这周没完成的事，以及原因\n- \n\n# 这周最常出现的卡点 / 情绪 / 节奏问题\n- \n\n# 这周对自己新的理解\n- \n\n# 下周最重要的 3 个推进点\n- [ ] \n- [ ] \n- [ ] \n\n# 下周想保持的状态\n- `
}

const insertPlanTemplate = () => {
  planTitle.value = `${todayKey} 日计划`
  planContent.value = buildPlanTemplate()
}

const insertReviewTemplate = () => {
  reviewTitle.value = `${todayKey} 晚间复盘`
  reviewContent.value = buildReviewTemplate()
}

const insertWeeklyReviewTemplate = () => {
  weeklyReviewTitle.value = weekReviewTitleFallback
  weeklyReviewContent.value = buildWeeklyReviewTemplate()
}

const findTodayNote = (items: Note[], category: string) => {
  return items.find((note) => note.category === category && dayjs(note.createdAt).format('YYYY-MM-DD') === todayKey)
}

const findCurrentWeekNote = (items: Note[], category: string) => {
  return items.find((note) => {
    if (note.category !== category) return false
    const created = dayjs(note.createdAt)
    return !created.isBefore(weekStart) && !created.isAfter(weekEnd)
  })
}

const loadNotesAndRituals = async () => {
  notes.value = await noteAPI.list()

  const planNote = findTodayNote(notes.value, 'daily-plan')
  const reviewNote = findTodayNote(notes.value, 'daily-review')
  const weekNote = findCurrentWeekNote(notes.value, 'weekly-review')

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

  if (weekNote) {
    weeklyReviewNoteId.value = weekNote.id
    weeklyReviewTitle.value = weekNote.title
    weeklyReviewContent.value = weekNote.content
  } else {
    weeklyReviewNoteId.value = null
    insertWeeklyReviewTemplate()
  }
}

const loadTasks = async () => {
  tasks.value = await taskAPI.list()
}

const loadInsight = async () => {
  insight.value = await aiAPI.personalInsight()
}

const refreshAll = async () => {
  await Promise.all([loadNotesAndRituals(), loadTasks(), loadInsight()])
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

const saveWeeklyReview = async () => {
  if (!weeklyReviewContent.value.trim()) {
    ElMessage.warning('请先写一点本周复盘')
    return
  }
  savingWeeklyReview.value = true
  try {
    const payload = {
      title: weeklyReviewTitle.value || weekReviewTitleFallback,
      content: weeklyReviewContent.value,
      category: 'weekly-review',
      sourceType: 'manual'
    }
    const saved = weeklyReviewNoteId.value
      ? await noteAPI.update(weeklyReviewNoteId.value, payload)
      : await noteAPI.create(payload)
    weeklyReviewNoteId.value = saved.id
    ElMessage.success('周复盘已保存')
    await refreshAll()
  } catch {
    ElMessage.error('保存周复盘失败')
  } finally {
    savingWeeklyReview.value = false
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

    await Promise.all([loadTasks(), loadInsight()])
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
.header-actions,
.mode-switch {
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
  max-width: 780px;
}

.mode-switch {
  padding: 4px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
}

.mode-pill {
  border: 0;
  border-radius: 999px;
  padding: 10px 16px;
  background: transparent;
  color: var(--text-secondary);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.24s ease, color 0.24s ease, transform 0.24s ease;
}

.mode-pill.active {
  background: linear-gradient(135deg, #1f6feb, #13b8a6);
  color: #fff;
  transform: translateY(-1px);
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

.weekly-hero {
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.06), rgba(31, 111, 235, 0.08), rgba(19, 184, 166, 0.1));
}

.coach-hero h3 {
  margin-bottom: 8px;
  font-size: 20px;
}

.coach-hero p,
.habit-item p,
.task-row p,
.weekly-cover span {
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

.habit-item,
.habit-list.compact .habit-item {
  flex-direction: column;
}

.weekly-cover {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.08), rgba(19, 184, 166, 0.08));
}

.weekly-cover strong {
  font-size: 18px;
}

.mini-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.04);
}

.mini-stat strong {
  font-size: 24px;
}

.mini-stat span {
  color: var(--text-secondary);
  font-size: 13px;
}

.weekly-meta-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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

  .mode-switch {
    width: 100%;
    justify-content: stretch;
  }

  .mode-pill {
    flex: 1;
  }

  .overview-grid,
  .rituals-grid,
  .mini-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
