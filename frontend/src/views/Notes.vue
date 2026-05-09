<template>
  <div class="notes-page">
    <div class="page-header">
      <div class="header-left">
        <h1>快速记录</h1>
        <p class="subtitle">共 {{ notes.length }} 条记录，可导入 `txt / md`，把日常、资料、计划都沉淀进你的个人系统</p>
      </div>
      <div class="header-right">
        <input ref="fileInputRef" type="file" accept=".txt,.md,.markdown,.text" class="file-input" @change="handleFileImport" />
        <el-button @click="triggerFileImport">
          <el-icon><Upload /></el-icon>
          导入笔记
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <div class="input-section">
      <el-card class="input-card">
        <div class="input-area">
          <div class="meta-row">
            <el-input v-model="noteTitle" placeholder="给这条记录起个标题（可选）" class="title-input" />
            <el-select v-model="noteCategory" placeholder="选择分类" class="category-select">
              <el-option label="收件箱" value="inbox" />
              <el-option label="日计划" value="daily-plan" />
              <el-option label="晚间复盘" value="daily-review" />
              <el-option label="周复盘" value="weekly-review" />
              <el-option label="工作" value="work" />
              <el-option label="生活" value="life" />
              <el-option label="学习" value="learning" />
              <el-option label="灵感" value="idea" />
              <el-option label="资料" value="reference" />
            </el-select>
          </div>
          <el-input
            v-model="content"
            type="textarea"
            :rows="4"
            placeholder="快速记录今天的工作、想法、会议内容、生活事项... 输入后点击「一键 AI 分析」"
            @keydown.ctrl.enter="handleAnalyze"
          />
          <div class="input-actions">
            <span class="hint">
              <el-icon><Lightning /></el-icon>
              AI 将自动提取任务、生成摘要，并基于你的个人上下文持续给建议
            </span>
            <div class="btns">
              <el-button :disabled="!content.trim()" @click="handleSave">
                仅保存
              </el-button>
              <el-button
                type="primary"
                size="large"
                :loading="analyzing"
                :disabled="!content.trim()"
                @click="handleAnalyze"
              >
                <el-icon><MagicStick /></el-icon>
                {{ analyzing ? 'AI 分析中...' : '一键 AI 分析' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        :prefix-icon="Search"
        placeholder="搜索笔记内容..."
        clearable
        class="search-input"
      />
      <el-select v-model="filterType" placeholder="筛选类型" clearable style="width: 150px">
        <el-option label="全部" value="" />
        <el-option label="普通记录" value="note" />
        <el-option label="AI分析" value="analyzed" />
      </el-select>
      <el-button text @click="sortOrder = sortOrder === 'desc' ? 'asc' : 'desc'">
        <el-icon><Sort /></el-icon>
        {{ sortOrder === 'desc' ? '最新在前' : '最旧在前' }}
      </el-button>
    </div>

    <div class="notes-grid" v-if="filteredNotes.length > 0">
      <div class="note-card" v-for="note in filteredNotes" :key="note.id">
        <div class="card-header">
          <div class="card-heading">
            <strong class="card-title">{{ note.title || '未命名记录' }}</strong>
            <span class="card-time">{{ formatDateTime(note.createdAt) }}</span>
          </div>
          <div class="card-actions">
            <el-tag v-if="note.hasTasks" size="small" type="success" class="task-badge">
              <el-icon><List /></el-icon>
              {{ note.taskCount }} 个任务
            </el-tag>
            <el-button size="small" text @click="handleCopy(note.content)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
            <el-button size="small" text type="danger" @click="handleDelete(note.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="card-meta">
          <el-tag size="small" effect="plain">{{ categoryLabelMap[note.category] || note.category || '收件箱' }}</el-tag>
          <el-tag size="small" type="info" effect="plain">{{ note.sourceType === 'imported' ? '导入' : '手动记录' }}</el-tag>
        </div>
        <p class="card-content">{{ note.content }}</p>
        <div class="card-tags" v-if="note.tags.length">
          <el-tag v-for="tag in note.tags" :key="tag" size="small">{{ tag }}</el-tag>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无笔记，试试输入内容点击「一键 AI 分析」" :image-size="80">
      <el-button type="primary" :disabled="!content.trim()" @click="handleAnalyze">创建第一条笔记</el-button>
    </el-empty>

    <el-dialog
      v-model="showAnalyzeResult"
      title="AI 智能分析结果"
      width="700px"
      :close-on-click-modal="false"
      class="ai-result-dialog"
    >
      <div v-if="analyzeResult" class="analyze-result">
        <el-steps :active="stepActive" finish-status="success" class="step-indicator">
          <el-step title="AI 分析" description="提取任务和标签" />
          <el-step title="保存笔记" description="记录内容" />
          <el-step title="创建任务" :description="`${analyzeResult.tasks?.length || 0} 个任务`" />
          <el-step title="添加标签" :description="`${analyzeResult.knowledgeTags?.length || 0} 个标签`" />
        </el-steps>

        <el-divider />

        <div class="result-section">
          <h4>内容摘要</h4>
          <div class="summary-box">{{ analyzeResult.summary }}</div>
        </div>

        <div class="result-section" v-if="analyzeResult.tasks?.length">
          <h4>提取的任务 ({{ analyzeResult.tasks.length }})</h4>
          <div class="tasks-list">
            <div v-for="(task, index) in analyzeResult.tasks" :key="index" class="task-item">
              <el-tag :type="getPriorityType(task.priority)" size="small">{{ getPriorityLabel(task.priority) }}</el-tag>
              <span class="task-title">{{ task.title }}</span>
            </div>
          </div>
        </div>

        <div class="result-section" v-if="analyzeResult.knowledgeTags?.length">
          <h4>知识标签</h4>
          <div class="tags-row">
            <el-tag v-for="tag in analyzeResult.knowledgeTags" :key="tag" type="info">{{ tag }}</el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="viewInTasks">查看任务列表</el-button>
        <el-button @click="viewInKnowledge">查看长期主题</el-button>
        <el-button type="primary" @click="showAnalyzeResult = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { noteAPI, aiAPI, taskAPI, knowledgeAPI } from '@/api'
import type { AnalyzeResponse, Note, Priority, Task, Knowledge } from '@/api/types'
import {
  MagicStick,
  Download,
  Upload,
  Lightning,
  Search,
  Sort,
  Delete,
  CopyDocument,
  List
} from '@element-plus/icons-vue'

type NoteCard = Note & {
  hasTasks: boolean
  taskCount: number
  tags: string[]
}

const router = useRouter()

const fileInputRef = ref<HTMLInputElement | null>(null)
const noteTitle = ref('')
const content = ref('')
const noteCategory = ref('inbox')
const noteSourceType = ref<'manual' | 'imported'>('manual')
const analyzing = ref(false)
const showAnalyzeResult = ref(false)
const analyzeResult = ref<AnalyzeResponse | null>(null)
const stepActive = ref(3)
const notes = ref<NoteCard[]>([])
const searchKeyword = ref('')
const filterType = ref('')
const sortOrder = ref<'desc' | 'asc'>('desc')
const categoryLabelMap: Record<string, string> = {
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

const filteredNotes = computed(() => {
  let result = [...notes.value]

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter((note) =>
      `${note.title} ${note.content} ${note.category}`.toLowerCase().includes(keyword)
    )
  }

  if (filterType.value === 'note') {
    result = result.filter((note) => !note.hasTasks && note.tags.length === 0)
  }

  if (filterType.value === 'analyzed') {
    result = result.filter((note) => note.hasTasks || note.tags.length > 0)
  }

  result.sort((a, b) => {
    const timeA = new Date(a.createdAt).getTime()
    const timeB = new Date(b.createdAt).getTime()
    return sortOrder.value === 'desc' ? timeB - timeA : timeA - timeB
  })

  return result
})

const getPriorityType = (priority: Priority) => {
  const types: Record<Priority, 'danger' | 'warning' | 'info'> = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return types[priority]
}

const getPriorityLabel = (priority: Priority) => {
  const labels: Record<Priority, string> = {
    high: '高优先级',
    medium: '中优先级',
    low: '低优先级'
  }
  return labels[priority]
}

const loadNotes = async () => {
  try {
    const [noteList, tasks, knowledgeList] = await Promise.all([
      noteAPI.list(),
      taskAPI.list(),
      knowledgeAPI.list()
    ])

    const taskCountByNoteId = tasks.reduce<Record<number, number>>((acc, task: Task) => {
      if (task.noteId) {
        acc[task.noteId] = (acc[task.noteId] || 0) + 1
      }
      return acc
    }, {})

    const knowledgeTagsByContent = knowledgeList.reduce<Record<string, string[]>>((acc, item: Knowledge) => {
      if (item.content) {
        acc[item.content] = (item.tags || '').split(',').map((tag) => tag.trim()).filter(Boolean)
      }
      return acc
    }, {})

    notes.value = noteList.map((note) => {
      const taskCount = taskCountByNoteId[note.id] || 0
      const tags = knowledgeTagsByContent[note.content] || []
      return {
        ...note,
        hasTasks: taskCount > 0,
        taskCount,
        tags
      }
    })
  } catch (error) {
    console.error('加载笔记失败', error)
  }
}

const handleAnalyze = async () => {
  if (!content.value.trim()) {
    ElMessage.warning('请先输入内容')
    return
  }

  analyzing.value = true
  analyzeResult.value = null

  try {
    const result = await aiAPI.analyze({ content: content.value })
    analyzeResult.value = result

    const note = await noteAPI.create({
      title: noteTitle.value,
      content: content.value,
      category: noteCategory.value,
      sourceType: noteSourceType.value
    })

    for (const task of result.tasks || []) {
      await taskAPI.create({
        title: task.title,
        description: task.description || '',
        priority: task.priority || 'medium',
        noteId: note.id
      })
    }

    if (result.knowledgeTags?.length) {
      await knowledgeAPI.create({
        title: (result.summary || content.value.slice(0, 30)).slice(0, 50),
        content: content.value,
        tags: result.knowledgeTags
      })
    }

    ElMessage.success(`分析完成，已创建 ${result.tasks?.length || 0} 个任务`)
    stepActive.value = 3
    showAnalyzeResult.value = true
    resetDraft()
    await loadNotes()
  } catch (error: any) {
    ElMessage.error(error?.message || 'AI 分析失败，请重试')
  } finally {
    analyzing.value = false
  }
}

const handleSave = async () => {
  if (!content.value.trim()) {
    ElMessage.warning('请先输入内容')
    return
  }

  try {
    await noteAPI.create({
      title: noteTitle.value,
      content: content.value,
      category: noteCategory.value,
      sourceType: noteSourceType.value
    })
    ElMessage.success('已保存到个人笔记')
    resetDraft()
    await loadNotes()
  } catch {
    ElMessage.error('保存失败')
  }
}

const triggerFileImport = () => {
  fileInputRef.value?.click()
}

const handleFileImport = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    const text = await file.text()
    content.value = text
    noteTitle.value = file.name.replace(/\.[^.]+$/, '')
    noteCategory.value = 'reference'
    noteSourceType.value = 'imported'
    ElMessage.success(`已导入 ${file.name}`)
  } catch {
    ElMessage.error('导入失败，请重试')
  } finally {
    target.value = ''
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定要删除该笔记吗？', '提示', { type: 'warning' })
  try {
    await noteAPI.delete(id)
    ElMessage.success('删除成功')
    await loadNotes()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleCopy = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const handleExport = () => {
  const rows = [
    ['标题', '分类', '来源', '内容', '标签', '任务数', '创建时间'],
    ...filteredNotes.value.map((note) => [
      note.title,
      categoryLabelMap[note.category] || note.category,
      note.sourceType === 'imported' ? '导入' : '手动记录',
      note.content,
      note.tags.join(','),
      String(note.taskCount),
      formatDateTime(note.createdAt)
    ])
  ]

  const csv = rows
    .map((row) => row.map((cell) => `"${String(cell || '').replace(/"/g, '""')}"`).join(','))
    .join('\n')

  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `笔记导出_${dayjs().format('YYYY-MM-DD_HH-mm')}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('导出成功')
}

const viewInTasks = () => {
  showAnalyzeResult.value = false
  router.push('/tasks')
}

const viewInKnowledge = () => {
  showAnalyzeResult.value = false
  router.push('/knowledge')
}

const formatDateTime = (date: string) => dayjs(date).format('MM-DD HH:mm')

const resetDraft = () => {
  noteTitle.value = ''
  content.value = ''
  noteCategory.value = 'inbox'
  noteSourceType.value = 'manual'
}

onMounted(loadNotes)
</script>

<style scoped>
.notes-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header,
.filter-bar {
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

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.subtitle {
  color: var(--text-secondary);
}

.input-card {
  margin-bottom: 24px;
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.meta-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 14px;
  margin-bottom: 14px;
}

.file-input {
  display: none;
}

.input-actions {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
}

.search-input {
  max-width: 360px;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 18px;
}

.note-card {
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: var(--shadow-card);
}

.card-header,
.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-heading {
  min-width: 0;
}

.card-title {
  display: block;
  font-size: 17px;
  line-height: 1.4;
}

.card-time {
  display: block;
  margin-top: 6px;
  color: #7b8798;
  font-size: 13px;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.card-content {
  margin: 16px 0;
  line-height: 1.8;
  white-space: pre-wrap;
}

.card-tags,
.tags-row,
.tasks-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-box {
  padding: 16px;
  border-radius: 18px;
  background: rgba(31, 111, 235, 0.06);
  line-height: 1.7;
}

.result-section {
  margin-bottom: 18px;
}

.task-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
}

@media (max-width: 720px) {
  .notes-page {
    padding: 16px;
  }

  .page-header,
  .filter-bar,
  .meta-row,
  .input-actions {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: none;
  }
}
</style>
