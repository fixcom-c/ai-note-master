<template>
  <div class="knowledge-page">
    <div class="page-header">
      <div class="header-left">
        <h1>知识库</h1>
        <p class="subtitle">共 {{ knowledgeList.length }} 条知识，持续沉淀可复用经验</p>
      </div>
      <div class="header-right">
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <el-card class="input-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Edit /></el-icon> 添加知识</span>
        </div>
      </template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="输入知识标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="输入知识内容" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple placeholder="选择标签" style="width: 100%">
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">
            <el-icon><Plus /></el-icon>
            保存
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        :prefix-icon="Search"
        placeholder="搜索知识标题或内容..."
        clearable
        class="search-input"
      />
      <el-select v-model="filterTag" placeholder="标签筛选" clearable class="tag-select">
        <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
      </el-select>
    </div>

    <div class="knowledge-grid" v-if="filteredKnowledge.length > 0">
      <div class="knowledge-card" v-for="item in filteredKnowledge" :key="item.id">
        <div class="card-header">
          <h3 class="card-title">{{ item.title }}</h3>
          <div class="card-actions">
            <el-button size="small" text @click="handleEdit(item)">
              <el-icon><Edit /></el-icon>
            </el-button>
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
          >
            {{ tag }}
          </el-tag>
        </div>
        <div class="card-meta">
          <span class="meta-time">{{ formatDateTime(item.createdAt) }}</span>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无知识库条目" :image-size="80" />

    <el-dialog v-model="showEditDialog" title="编辑知识" width="640px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="editForm.tags" multiple style="width: 100%">
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { knowledgeAPI } from '@/api'
import type { Knowledge } from '@/api/types'
import { Edit, Plus, Download, Search, Delete, CopyDocument } from '@element-plus/icons-vue'

const knowledgeList = ref<Knowledge[]>([])
const searchKeyword = ref('')
const filterTag = ref('')
const showEditDialog = ref(false)
const allTags = ['工作', '会议', '项目', '客户', '技术', '文档', '邮件', '报告', '计划', '总结']

const form = reactive({
  title: '',
  content: '',
  tags: [] as string[]
})

const editForm = reactive({
  id: null as number | null,
  title: '',
  content: '',
  tags: [] as string[]
})

const splitTags = (tags?: string) => (tags || '').split(',').map((tag) => tag.trim()).filter(Boolean)

const filteredKnowledge = computed(() => {
  let result = [...knowledgeList.value]

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
    ElMessage.error('加载知识库失败')
  }
}

const resetForm = () => {
  form.title = ''
  form.content = ''
  form.tags = []
}

const handleSave = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  try {
    await knowledgeAPI.create({
      title: form.title.trim(),
      content: form.content,
      tags: form.tags
    })
    ElMessage.success('保存成功')
    resetForm()
    await loadKnowledge()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleEdit = (item: Knowledge) => {
  editForm.id = item.id
  editForm.title = item.title
  editForm.content = item.content || ''
  editForm.tags = splitTags(item.tags)
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!editForm.id || !editForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  try {
    await knowledgeAPI.update(editForm.id, {
      title: editForm.title.trim(),
      content: editForm.content,
      tags: editForm.tags
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    await loadKnowledge()
  } catch {
    ElMessage.error('更新失败')
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定要删除该知识吗？', '提示', { type: 'warning' })
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

const handleExport = () => {
  const rows = [
    ['标题', '内容', '标签', '创建时间'],
    ...filteredKnowledge.value.map((item) => [
      item.title,
      item.content || '',
      item.tags || '',
      formatDateTime(item.createdAt)
    ])
  ]

  const csv = rows
    .map((row) => row.map((cell) => `"${String(cell || '').replace(/"/g, '""')}"`).join(','))
    .join('\n')

  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `知识库导出_${dayjs().format('YYYY-MM-DD')}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('导出成功')
}

const formatDateTime = (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm')

onMounted(loadKnowledge)
</script>

<style scoped>
.knowledge-page {
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

.subtitle {
  color: var(--text-secondary);
}

.input-card {
  margin-bottom: 24px;
  border-radius: 24px;
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.filter-bar {
  margin-bottom: 20px;
}

.search-input {
  max-width: 360px;
}

.tag-select {
  width: 160px;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.knowledge-card {
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: var(--shadow-card);
}

.card-title {
  font-size: 18px;
  margin: 0;
}

.card-content {
  min-height: 72px;
  margin: 16px 0;
  color: var(--text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.card-meta {
  margin-top: 14px;
  color: #7b8798;
  font-size: 13px;
}

@media (max-width: 720px) {
  .knowledge-page {
    padding: 16px;
  }

  .page-header,
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input,
  .tag-select {
    width: 100%;
    max-width: none;
  }
}
</style>
