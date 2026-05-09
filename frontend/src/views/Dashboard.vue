<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div class="header-left">
        <h1>工作台</h1>
        <p class="subtitle">{{ greeting }}，{{ username }} 👋</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleQuickCapture">
          <el-icon><Plus /></el-icon>
          快速记录
        </el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card" v-for="(stat, index) in statsData" :key="index" :style="{ '--delay': index * 0.1 + 's' }">
        <div class="stat-icon" :style="{ background: stat.color }">
          <el-icon :size="24"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'" v-if="stat.trend !== undefined">
          <el-icon><TrendCharts /></el-icon>
          {{ Math.abs(stat.trend) }}%
        </div>
      </div>
    </div>

    <div class="main-grid">
      <div class="grid-left">
        <el-card class="quick-input-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Edit /></el-icon> 快速记录</span>
            </div>
          </template>
          <div class="quick-input-area">
            <el-input
              v-model="quickInput"
              type="textarea"
              :rows="4"
              placeholder="输入你想记录的内容，AI会自动分析并生成任务...（支持 Ctrl+Enter 快速提交）"
              @keydown.ctrl.enter="handleQuickAnalyze"
            />
            <div class="quick-actions">
              <span class="ai-hint">AI 将自动提取任务和知识标签</span>
              <div class="action-buttons">
                <el-button type="primary" :loading="analyzing" @click="handleQuickAnalyze">
                  <el-icon><MagicStick /></el-icon>
                  AI 分析
                </el-button>
                <el-button @click="handleQuickSave">
                  <el-icon><DocumentAdd /></el-icon>
                  仅保存
                </el-button>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="today-tasks-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><List /></el-icon> 今日任务</span>
              <el-tag type="warning" size="small">待完成 {{ pendingTasks.length }}</el-tag>
            </div>
          </template>
          <div class="task-list" v-if="todayTasks.length > 0">
            <div 
              class="task-item" 
              v-for="task in todayTasks" 
              :key="task.id"
              :class="{ completed: task.status === 'completed' }"
            >
              <el-checkbox 
                :model-value="task.status === 'completed'"
                @change="handleToggleTask(task)"
                :disabled="task.status === 'completed'"
              />
              <div class="task-info" @click="goToTaskDetail(task.id)">
                <span class="task-title">{{ task.title }}</span>
                <span class="task-desc" v-if="task.description">{{ task.description }}</span>
              </div>
              <div class="task-meta">
                <el-tag size="small" :type="getPriorityType(task.priority)">
                  {{ getPriorityLabel(task.priority) }}
                </el-tag>
                <span class="task-time" v-if="task.reminderTime">{{ formatTime(task.reminderTime) }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="今日暂无任务" :image-size="80" />
          <div class="card-footer" v-if="pendingTasks.length > 0">
            <router-link to="/tasks" class="view-more">查看全部 {{ pendingTasks.length }} 个待办 →</router-link>
          </div>
        </el-card>
      </div>

      <div class="grid-right">
        <el-card class="ai-summary-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><DataLine /></el-icon> 今日个人洞察</span>
              <div class="summary-actions">
                <el-button text size="small" @click="$router.push('/profile')">个人空间</el-button>
                <el-button text size="small" @click="loadAISummary">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </div>
          </template>
          <div class="ai-summary" v-if="personalInsight">
            <div class="summary-hero">
              <h3>{{ personalInsight.headline }}</h3>
              <p>{{ personalInsight.understanding }}</p>
            </div>
            <div class="summary-item">
              <div class="summary-label">今日完成度</div>
              <div class="summary-value">{{ personalInsight.todayCompleted }}/{{ personalInsight.todayPlanned || 0 }}</div>
              <el-progress
                :percentage="personalInsight.todayPlanned ? Math.round((personalInsight.todayCompleted / personalInsight.todayPlanned) * 100) : 0"
                :stroke-width="8"
                :show-text="false"
              />
            </div>
            <div class="summary-item">
              <div class="summary-label">当前节奏</div>
              <p class="summary-copy">{{ personalInsight.rhythm }}</p>
            </div>
            <div class="summary-item">
              <div class="summary-label">给你的建议</div>
              <ul class="summary-list">
                <li v-for="item in personalInsight.suggestions.slice(0, 3)" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div class="summary-item">
              <div class="summary-label">最近主题</div>
              <div class="tag-cloud">
                <el-tag v-for="tag in personalInsight.focusTags" :key="tag" size="small" type="info">
                  {{ tag }}
                </el-tag>
                <el-tag v-for="tag in hotTags" :key="tag.name" size="small" :type="tag.type">
                  {{ tag.name }} ({{ tag.count }})
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="recent-knowledge-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Reading /></el-icon> 最近知识</span>
              <router-link to="/knowledge" class="view-all">查看全部</router-link>
            </div>
          </template>
          <div class="knowledge-list" v-if="recentKnowledge.length > 0">
            <div class="knowledge-item" v-for="item in recentKnowledge" :key="item.id">
              <div class="knowledge-title">{{ item.title }}</div>
              <div class="knowledge-tags">
                <el-tag size="small" v-for="tag in (item.tags || '').split(',').filter(Boolean)" :key="tag">
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无知识库条目" :image-size="60" />
        </el-card>

        <el-card class="quick-actions-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Lightning /></el-icon> 快捷操作</span>
            </div>
          </template>
          <div class="quick-btns">
            <el-button @click="$router.push('/notes')">
              <el-icon><Edit /></el-icon>
              记笔记
            </el-button>
            <el-button @click="$router.push('/rituals')">
              <el-icon><Calendar /></el-icon>
              每日节奏
            </el-button>
            <el-button @click="handleNewTask">
              <el-icon><Plus /></el-icon>
              新任务
            </el-button>
            <el-button @click="handleGenerateReport">
              <el-icon><Document /></el-icon>
              生成日报
            </el-button>
            <el-button @click="$router.push('/knowledge')">
              <el-icon><Collection /></el-icon>
              添知识
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="showQuickCapture" title="快速记录" width="600px">
      <el-input v-model="quickInput" type="textarea" :rows="6" placeholder="输入内容..." autofocus />
      <template #footer>
        <el-button @click="showQuickCapture = false">取消</el-button>
        <el-button type="info" @click="handleQuickSave">仅保存</el-button>
        <el-button type="primary" :loading="analyzing" @click="handleQuickAnalyze">AI 分析</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAnalyzeResult" title="AI 分析结果" width="700px">
      <div v-if="analyzeResult" class="analyze-result">
        <el-form label-width="100px">
          <el-form-item label="摘要">
            <el-input v-model="analyzeResult.summary" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="提取任务">
            <div v-for="(task, index) in analyzeResult.tasks" :key="index" class="task-edit-item">
              <el-input v-model="task.title" placeholder="任务标题" />
              <el-input v-model="task.description" placeholder="描述" />
              <el-select v-model="task.priority" placeholder="优先级">
                <el-option label="高" value="high" />
                <el-option label="中" value="medium" />
                <el-option label="低" value="low" />
              </el-select>
              <el-button type="danger" @click="analyzeResult.tasks.splice(index, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button plain @click="analyzeResult.tasks.push({ title: '', description: '', priority: 'medium' })">
              + 添加任务
            </el-button>
          </el-form-item>
          <el-form-item label="知识标签">
            <el-select v-model="analyzeResult.knowledgeTags" multiple style="width: 100%">
              <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showAnalyzeResult = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleConfirmAnalyze">确认保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showNewTask" title="创建新任务" width="500px">
      <el-form :model="newTaskForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="newTaskForm.title" placeholder="任务标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newTaskForm.description" type="textarea" :rows="3" placeholder="任务描述" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="newTaskForm.priority">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="提醒时间">
          <el-date-picker v-model="newTaskForm.reminderTime" type="datetime" placeholder="选择提醒时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNewTask = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { taskAPI, noteAPI, knowledgeAPI, aiAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import type { AnalyzeResponse, Knowledge, PersonalInsight, Priority, Task } from '@/api/types'
import dayjs from 'dayjs'
import {
  List, Edit, DocumentAdd, MagicStick, DataLine, Reading, Lightning, 
  Collection, Plus, TrendCharts, Refresh, Delete, Clock, CircleCheck, Calendar
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const username = computed(() => userStore.username || '用户')
const quickInput = ref('')
const analyzing = ref(false)
const saving = ref(false)
const showQuickCapture = ref(false)
const showAnalyzeResult = ref(false)
const showNewTask = ref(false)
const analyzeResult = ref<AnalyzeResponse | null>(null)
const personalInsight = ref<PersonalInsight | null>(null)

const todayTasks = ref<Task[]>([])
const recentKnowledge = ref<Knowledge[]>([])
const allTags = ['工作', '会议', '项目', '客户', '技术', '文档', '邮件', '报告', '计划', '总结']

const newTaskForm = reactive({
  title: '',
  description: '',
  priority: 'medium' as Priority,
  reminderTime: null as string | null
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const statsData = computed(() => [
  {
    label: '总任务',
    value: stats.total,
    icon: List,
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    trend: 12
  },
  {
    label: '待完成',
    value: stats.pending,
    icon: Clock,
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    trend: -5
  },
  {
    label: '已完成',
    value: stats.completed,
    icon: CircleCheck,
    color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    trend: 25
  },
  {
    label: '知识条目',
    value: stats.knowledge,
    icon: Reading,
    color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
])

const stats = reactive({
  total: 0,
  pending: 0,
  completed: 0,
  knowledge: 0
})

const pendingTasks = computed(() => todayTasks.value.filter((task) => task.status !== 'completed'))

const hotTags = computed(() => {
  const tagCounts: any = {}
  recentKnowledge.value.forEach(item => {
    (item.tags || '').split(',').filter(Boolean).forEach((tag: string) => {
      tagCounts[tag] = (tagCounts[tag] || 0) + 1
    })
  })
  const types: Array<'primary' | 'success' | 'warning' | 'info' | 'danger'> = [
    'primary',
    'success',
    'warning',
    'info',
    'danger'
  ]
  return Object.entries(tagCounts)
    .sort((a, b) => (b[1] as number) - (a[1] as number))
    .slice(0, 5)
    .map(([name, count], i) => ({ name, count, type: types[i % types.length] }))
})

const loadData = async () => {
  try {
    const [tasks, knowledge] = await Promise.all([
      taskAPI.list(),
      knowledgeAPI.list()
    ])

    stats.total = tasks.length
    stats.pending = tasks.filter((task) => task.status === 'pending').length
    stats.completed = tasks.filter((task) => task.status === 'completed').length
    stats.knowledge = knowledge.length

    const today = dayjs().format('YYYY-MM-DD')
    todayTasks.value = tasks
      .filter((task: Task) => {
        const compareDate = task.reminderTime || task.createdAt
        return dayjs(compareDate).format('YYYY-MM-DD') === today
      })
      .slice(0, 5)
    recentKnowledge.value = knowledge.slice(0, 5)
    personalInsight.value = await aiAPI.personalInsight()
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

const handleQuickCapture = () => {
  showQuickCapture.value = true
}

const handleQuickSave = async () => {
  if (!quickInput.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  try {
    await noteAPI.create({ content: quickInput.value })
    ElMessage.success('保存成功')
    quickInput.value = ''
    showQuickCapture.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleQuickAnalyze = async () => {
  if (!quickInput.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  analyzing.value = true
  try {
    const result = await aiAPI.analyze({ content: quickInput.value })
    analyzeResult.value = result
    showAnalyzeResult.value = true
    showQuickCapture.value = false
  } catch (e) {
    ElMessage.error('AI分析失败')
  } finally {
    analyzing.value = false
  }
}

const handleConfirmAnalyze = async () => {
  if (!analyzeResult.value) return

  saving.value = true
  try {
    const note = await noteAPI.create({ content: quickInput.value })

    for (const task of analyzeResult.value.tasks) {
      await taskAPI.create({
        title: task.title,
        description: task.description,
        priority: task.priority,
        noteId: note.id
      })
    }

    if (analyzeResult.value.knowledgeTags?.length) {
      await knowledgeAPI.create({
        title: analyzeResult.value.summary?.slice(0, 50) || '未知',
        content: quickInput.value,
        tags: analyzeResult.value.knowledgeTags
      })
    }

    ElMessage.success('保存成功')
    showAnalyzeResult.value = false
    quickInput.value = ''
    analyzeResult.value = null
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleToggleTask = async (task: Task) => {
  try {
    await taskAPI.complete(task.id)
    ElMessage.success('任务已完成')
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const goToTaskDetail = (id: number) => {
  router.push(`/tasks/${id}`)
}

const handleNewTask = () => {
  newTaskForm.title = ''
  newTaskForm.description = ''
  newTaskForm.priority = 'medium'
  newTaskForm.reminderTime = null
  showNewTask.value = true
}

const handleCreateTask = async () => {
  if (!newTaskForm.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  try {
    await taskAPI.create({
      title: newTaskForm.title,
      description: newTaskForm.description,
      priority: newTaskForm.priority,
      reminderTime: newTaskForm.reminderTime || undefined
    })
    ElMessage.success('任务创建成功')
    showNewTask.value = false
    loadData()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

const handleGenerateReport = () => {
  router.push('/reports')
}

const loadAISummary = () => {
  loadData()
  ElMessage.success('已刷新')
}

const getPriorityType = (priority: Priority) => {
  const map: Record<Priority, 'danger' | 'warning' | 'info'> = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return map[priority]
}

const getPriorityLabel = (priority: Priority) => {
  const map: Record<Priority, string> = { high: '高', medium: '中', low: '低' }
  return map[priority]
}

const formatTime = (time: string) => {
  return dayjs(time).format('HH:mm')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.dashboard-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: #1a1a2e;
}

.subtitle {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.summary-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  animation: fadeInUp 0.5s ease forwards;
  animation-delay: var(--delay);
  opacity: 0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
}

.stat-label {
  font-size: 13px;
  color: #888;
  margin-top: 2px;
}

.summary-hero {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.08), rgba(19, 184, 166, 0.08));
}

.summary-hero h3 {
  margin-bottom: 8px;
  font-size: 19px;
}

.summary-hero p,
.summary-copy {
  color: var(--text-secondary);
  line-height: 1.8;
}

.summary-list {
  padding-left: 18px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.stat-trend {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-trend.up {
  background: #e6fff4;
  color: #52c41a;
}

.stat-trend.down {
  background: #fff1f0;
  color: #ff4d4f;
}

.main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.grid-left, .grid-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

:deep(.el-card) {
  border: none;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-card__body) {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a2e;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-input-area textarea {
  border: none;
  font-size: 15px;
  resize: none;
}

.quick-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.ai-hint {
  font-size: 12px;
  color: #888;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 10px;
  transition: all 0.2s;
  cursor: pointer;
}

.task-item:hover {
  background: #f0f0f5;
}

.task-item.completed {
  opacity: 0.6;
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-weight: 500;
  color: #1a1a2e;
  display: block;
}

.task-desc {
  font-size: 12px;
  color: #888;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.task-time {
  font-size: 11px;
  color: #888;
}

.card-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.view-more, .view-all {
  color: #409eff;
  font-size: 13px;
  text-decoration: none;
}

.view-more:hover, .view-all:hover {
  text-decoration: underline;
}

.ai-summary {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-item {
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #f0f0f5 100%);
  border-radius: 12px;
}

.summary-label {
  font-size: 12px;
  color: #888;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.knowledge-item {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 10px;
}

.knowledge-title {
  font-weight: 500;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.knowledge-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.quick-btns {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-btns .el-button {
  height: 44px;
  border-radius: 10px;
}

.analyze-result {
  padding: 0 8px;
}

.task-edit-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.task-edit-item .el-input {
  flex: 1;
}

.task-edit-item .el-select {
  width: 120px;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .dashboard {
    padding: 16px;
  }
}
</style>
