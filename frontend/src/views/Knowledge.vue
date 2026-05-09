<template>
  <div class="knowledge-page">
    <div class="page-header">
      <div>
        <h1>长期主题</h1>
        <p class="subtitle">这里不需要像维护资料库一样整理，只要把最近反复出现的想法、方法和结论慢慢沉淀下来。</p>
      </div>
    </div>

    <div class="summary-grid">
      <el-card class="summary-card">
        <strong>{{ knowledgeList.length }}</strong>
        <span>主题沉淀</span>
      </el-card>
      <el-card class="summary-card">
        <strong>{{ activeThemeCount }}</strong>
        <span>活跃标签</span>
      </el-card>
      <el-card class="summary-card">
        <strong>{{ recentThemeCount }}</strong>
        <span>近 7 天新增</span>
      </el-card>
      <el-card class="summary-card">
        <strong>{{ focusTopicLabel }}</strong>
        <span>当前主线</span>
      </el-card>
    </div>

    <el-card class="capture-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Reading /></el-icon> 新增一条主题沉淀</span>
        </div>
      </template>

      <div class="capture-copy">
        <strong>更适合记录什么？</strong>
        <p>最近重复出现的思考、一个正在形成的方法、你反复在投入的方向，或者一句以后还会想回看的结论。</p>
      </div>

      <el-input
        v-model="capture.content"
        type="textarea"
        :rows="6"
        class="capture-textarea"
        placeholder="例如：最近发现自己真正会长期投入的不是“做更多功能”，而是把日常记录、周复盘和 AI 理解形成一个稳定闭环。"
      />

      <div class="capture-meta">
        <el-input
          v-model="capture.title"
          class="capture-title"
          placeholder="可选标题，不填会自动生成"
        />
        <el-select
          v-model="capture.tags"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="给它选 1-3 个主题标签"
          class="capture-tags"
        >
          <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
      </div>

      <div class="capture-actions">
        <span class="capture-hint">不用追求完整，先让长期主线浮出来。</span>
        <el-button type="primary" :loading="saving" @click="handleSave">
          <el-icon><Plus /></el-icon>
          沉淀为主题
        </el-button>
      </div>
    </el-card>

    <div class="filter-block">
      <el-input
        v-model="searchKeyword"
        :prefix-icon="Search"
        placeholder="搜索主题标题或内容..."
        clearable
        class="search-input"
      />
      <div class="tag-filter-row">
        <button
          type="button"
          class="theme-chip"
          :class="{ active: filterTag === '' }"
          @click="filterTag = ''"
        >
          全部
        </button>
        <button
          v-for="tag in topTags"
          :key="tag.name"
          type="button"
          class="theme-chip"
          :class="{ active: filterTag === tag.name }"
          @click="filterTag = filterTag === tag.name ? '' : tag.name"
        >
          {{ tag.name }} {{ tag.count }}
        </button>
      </div>
    </div>

    <div class="knowledge-grid" v-if="filteredKnowledge.length > 0">
      <div class="knowledge-card" v-for="item in filteredKnowledge" :key="item.id">
        <div class="card-header top-align">
          <div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-time">{{ formatDateTime(item.createdAt) }}</p>
          </div>
          <div class="card-actions">
            <el-button size="small" text @click="handleCopy(item.content)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
            <el-button size="small" text type="danger" @click="handleDelete(item.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <p class="card-content">{{ item.content || '暂无内容' }}</p>

        <div class="card-footer">
          <el-tag
            v-for="tag in splitTags(item.tags)"
            :key="tag"
            size="small"
            type="info"
            effect="plain"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </div>

    <el-empty v-else description="还没有长期主题，先沉淀最近最常出现的一条想法。" :image-size="80" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { knowledgeAPI } from '@/api'
import type { Knowledge } from '@/api/types'
import { Reading, Plus, Search, Delete, CopyDocument } from '@element-plus/icons-vue'

const knowledgeList = ref<Knowledge[]>([])
const searchKeyword = ref('')
const filterTag = ref('')
const saving = ref(false)
const allTags = ['产品', '开发', '复盘', '计划', '学习', '生活', '决策', '习惯', '情绪', '灵感']

const capture = reactive({
  title: '',
  content: '',
  tags: [] as string[]
})

const splitTags = (tags?: string) => (tags || '').split(',').map((tag) => tag.trim()).filter(Boolean)

const sortedKnowledge = computed(() =>
  [...knowledgeList.value].sort((a, b) => dayjs(b.updatedAt || b.createdAt).valueOf() - dayjs(a.updatedAt || a.createdAt).valueOf())
)

const tagCounts = computed(() => {
  const counts = new Map<string, number>()
  for (const item of knowledgeList.value) {
    for (const tag of splitTags(item.tags)) {
      counts.set(tag, (counts.get(tag) || 0) + 1)
    }
  }
  return counts
})

const topTags = computed(() =>
  Array.from(tagCounts.value.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 8)
)

const activeThemeCount = computed(() => tagCounts.value.size)

const recentThemeCount = computed(() =>
  knowledgeList.value.filter((item) => dayjs(item.createdAt).isAfter(dayjs().subtract(7, 'day'))).length
)

const focusTopicLabel = computed(() => topTags.value[0]?.name || '待形成')

const filteredKnowledge = computed(() => {
  let result = [...sortedKnowledge.value]

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter((item) =>
      item.title.toLowerCase().includes(keyword) ||
      (item.content || '').toLowerCase().includes(keyword)
    )
  }

  if (filterTag.value) {
    result = result.filter((item) => splitTags(item.tags).includes(filterTag.value))
  }

  return result
})

const loadKnowledge = async () => {
  try {
    knowledgeList.value = await knowledgeAPI.list()
  } catch {
    ElMessage.error('加载长期主题失败')
  }
}

const buildTitle = (title: string, content: string, tags: string[]) => {
  if (title.trim()) return title.trim()
  const firstLine = content.split(/\r?\n/, 1)[0].trim()
  if (firstLine) {
    return firstLine.length <= 30 ? firstLine : `${firstLine.slice(0, 30)}...`
  }
  if (tags.length) {
    return `${tags[0]}主题沉淀`
  }
  return `主题沉淀 ${dayjs().format('MM-DD HH:mm')}`
}

const resetCapture = () => {
  capture.title = ''
  capture.content = ''
  capture.tags = []
}

const handleSave = async () => {
  if (!capture.content.trim()) {
    ElMessage.warning('先写下一点最近反复出现的想法')
    return
  }

  saving.value = true
  try {
    await knowledgeAPI.create({
      title: buildTitle(capture.title, capture.content, capture.tags),
      content: capture.content.trim(),
      tags: capture.tags
    })
    ElMessage.success('已经沉淀进长期主题')
    resetCapture()
    await loadKnowledge()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定要删除这条主题沉淀吗？', '提示', { type: 'warning' })
  try {
    await knowledgeAPI.delete(id)
    ElMessage.success('删除成功')
    await loadKnowledge()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleCopy = async (text: string) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const formatDateTime = (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm')

onMounted(loadKnowledge)
</script>

<style scoped>
.knowledge-page {
  padding: 24px;
  max-width: 1320px;
  margin: 0 auto;
}

.page-header,
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
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
  line-height: 1.8;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card,
.capture-card,
.knowledge-card {
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.summary-card {
  padding: 8px;
}

.summary-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-card strong {
  font-size: 28px;
}

.summary-card span {
  color: var(--text-secondary);
  font-size: 13px;
}

.capture-card {
  margin-bottom: 20px;
}

.capture-copy {
  margin-bottom: 16px;
}

.capture-copy strong {
  display: block;
  margin-bottom: 6px;
}

.capture-copy p,
.card-content,
.card-time,
.capture-hint {
  color: var(--text-secondary);
  line-height: 1.8;
}

.capture-textarea {
  margin-bottom: 14px;
}

.capture-meta {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(260px, 1.1fr);
  gap: 14px;
  margin-bottom: 14px;
}

.capture-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.filter-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 20px;
}

.search-input {
  max-width: 420px;
}

.tag-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.theme-chip {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  padding: 9px 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.24s ease;
}

.theme-chip.active {
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.12), rgba(19, 184, 166, 0.12));
  color: var(--text-primary);
  border-color: rgba(31, 111, 235, 0.18);
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 18px;
}

.knowledge-card {
  padding: 20px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.card-header.top-align {
  align-items: flex-start;
}

.card-title {
  font-size: 18px;
  margin: 0 0 4px;
}

.card-time {
  font-size: 13px;
}

.card-actions {
  display: flex;
  gap: 4px;
}

.card-content {
  min-height: 96px;
  margin: 16px 0;
  white-space: pre-wrap;
}

.card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 920px) {
  .summary-grid,
  .capture-meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .knowledge-page {
    padding: 16px;
  }

  .capture-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: none;
  }
}
</style>
