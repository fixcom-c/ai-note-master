<template>
  <div class="tasks-page">
    <div class="page-header">
      <div class="header-left">
        <h1>任务列表</h1>
        <p class="subtitle">共 {{ filteredTasks.length }} 个任务</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleCreateTask">
          <el-icon><Plus /></el-icon>
          新建任务
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索任务标题或描述..."
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
      </div>
      <div class="filter-right">
        <el-radio-group v-model="filterStatus" @change="handleFilterChange">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="pending">待办</el-radio-button>
          <el-radio-button label="completed">已完成</el-radio-button>
        </el-radio-group>
        <el-select v-model="filterPriority" placeholder="优先级" clearable style="width: 120px">
          <el-option label="高" value="high" />
          <el-option label="中" value="medium" />
          <el-option label="低" value="low" />
        </el-select>
        <el-button-group>
          <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">
            <el-icon><List /></el-icon>
          </el-button>
          <el-button :type="viewMode === 'grid' ? 'primary' : ''" @click="viewMode = 'grid'">
            <el-icon><Grid /></el-icon>
          </el-button>
          <el-button :type="viewMode === 'calendar' ? 'primary' : ''" @click="viewMode = 'calendar'">
            <el-icon><Calendar /></el-icon>
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div class="tasks-container">
      <div v-if="filteredTasks.length > 0" class="tasks-content" :class="viewMode">
        <template v-if="viewMode === 'list'">
          <el-table :data="filteredTasks" style="width: 100%" stripe @row-click="handleRowClick">
            <el-table-column prop="title" label="标题" min-width="200">
              <template #default="{ row }">
                <div class="task-title-cell">
                  <el-checkbox 
                    :model-value="row.status === 'completed'"
                    @click.stop
                    @change="handleToggleTask(row)"
                  />
                  <span :class="{ completed: row.status === 'completed' }">{{ row.title }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="100">
              <template #default="{ row }">
                <el-tag :type="getPriorityType(row.priority)" size="small">
                  {{ getPriorityLabel(row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'completed' ? 'success' : 'info'">
                  {{ row.status === 'completed' ? '已完成' : '待办' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reminderTime" label="提醒时间" width="160">
              <template #default="{ row }">
                <span v-if="row.reminderTime" class="reminder-time">
                  <el-icon><Clock /></el-icon>
                  {{ formatDateTime(row.reminderTime) }}
                </span>
                <span v-else class="no-reminder">未设置</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="160">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <div class="action-btns" @click.stop>
                  <el-button type="primary" size="small" text @click="handleEdit(row)">编辑</el-button>
                  <el-button type="success" size="small" text v-if="row.status !== 'completed'" @click="handleToggleTask(row)">完成</el-button>
                  <el-button type="danger" size="small" text @click="handleDelete(row.id)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <template v-else-if="viewMode === 'grid'">
          <div class="tasks-grid">
            <div 
              v-for="task in filteredTasks" 
              :key="task.id" 
              class="task-card"
              :class="{ completed: task.status === 'completed' }"
            >
              <div class="card-header">
                <el-checkbox 
                  :model-value="task.status === 'completed'"
                  @change="handleToggleTask(task)"
                />
                <el-tag :type="getPriorityType(task.priority)" size="small">
                  {{ getPriorityLabel(task.priority) }}
                </el-tag>
              </div>
              <h3 class="card-title" @click="goToTaskDetail(task.id)">{{ task.title }}</h3>
              <p class="card-desc" v-if="task.description">{{ task.description }}</p>
              <div class="card-footer">
                <span class="card-time" v-if="task.reminderTime">
                  <el-icon><Clock /></el-icon>
                  {{ formatDateTime(task.reminderTime) }}
                </span>
                <span class="card-date">{{ formatDate(task.createdAt) }}</span>
              </div>
              <div class="card-actions" @click.stop>
                <el-button size="small" text @click="handleEdit(task)">编辑</el-button>
                <el-button type="danger" size="small" text @click="handleDelete(task.id)">删除</el-button>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="viewMode === 'calendar'">
          <div class="calendar-container">
            <div class="calendar-header">
              <el-button-group>
                <el-button @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
                <el-button @click="goToToday">今天</el-button>
                <el-button @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
              </el-button-group>
              <h3>{{ currentMonthTitle }}</h3>
            </div>
            <div class="calendar-grid">
              <div class="calendar-week">
                <div class="week-day" v-for="day in weekDays" :key="day">{{ day }}</div>
              </div>
              <div class="calendar-days">
                <div 
                  v-for="(day, index) in calendarDays" 
                  :key="index" 
                  class="calendar-day"
                  :class="{ 
                    'other-month': day.otherMonth,
                    'today': day.isToday,
                    'has-tasks': day.tasks.length > 0
                  }"
                >
                  <span class="day-number">{{ day.day }}</span>
                  <div class="day-tasks" v-if="day.tasks.length > 0">
                    <div 
                      v-for="task in day.tasks.slice(0, 2)" 
                      :key="task.id" 
                      class="task-dot"
                      :class="getPriorityClass(task.priority)"
                      :title="task.title"
                      @click="goToTaskDetail(task.id)"
                    />
                    <span v-if="day.tasks.length > 2" class="more-tasks">+{{ day.tasks.length - 2 }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
      <el-empty v-else description="暂无任务" :image-size="100">
        <el-button type="primary" @click="handleCreateTask">创建第一个任务</el-button>
      </el-empty>
    </div>

    <el-dialog v-model="showEditDialog" :title="isEditing ? '编辑任务' : '新建任务'" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="editForm.priority" style="width: 100%">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="提醒时间">
          <el-date-picker 
            v-model="editForm.reminderTime" 
            type="datetime" 
            placeholder="选择提醒时间" 
            style="width: 100%"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTaskDetail" title="任务详情" width="600px">
      <div v-if="taskDetail" class="task-detail-content">
        <div class="detail-header">
          <h2>{{ taskDetail.title }}</h2>
          <el-tag :type="taskDetail.status === 'completed' ? 'success' : 'info'">
            {{ taskDetail.status === 'completed' ? '已完成' : '待办' }}
          </el-tag>
        </div>
        <div class="detail-body">
          <div class="detail-item">
            <label>优先级</label>
            <el-tag :type="getPriorityType(taskDetail.priority)">
              {{ getPriorityLabel(taskDetail.priority) }}
            </el-tag>
          </div>
          <div class="detail-item" v-if="taskDetail.description">
            <label>描述</label>
            <p>{{ taskDetail.description }}</p>
          </div>
          <div class="detail-item" v-if="taskDetail.reminderTime">
            <label>提醒时间</label>
            <p>{{ formatDateTime(taskDetail.reminderTime) }}</p>
          </div>
          <div class="detail-item">
            <label>创建时间</label>
            <p>{{ formatDateTime(taskDetail.createdAt) }}</p>
          </div>
          <div class="detail-item" v-if="taskDetail.completedAt">
            <label>完成时间</label>
            <p>{{ formatDateTime(taskDetail.completedAt) }}</p>
          </div>
        </div>
        <div class="detail-actions">
          <el-button v-if="taskDetail.status !== 'completed'" type="success" @click="handleCompleteFromDetail">
            完成任务
          </el-button>
          <el-button type="primary" @click="handleEditFromDetail">编辑</el-button>
          <el-button type="danger" @click="handleDeleteFromDetail">删除</el-button>
          <el-button @click="showTaskDetail = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskAPI } from '@/api'
import type { Priority, Task } from '@/api/types'
import dayjs from 'dayjs'
import { 
  Plus, List, Grid, Calendar, Clock, Search, 
  ArrowLeft, ArrowRight 
} from '@element-plus/icons-vue'

const router = useRouter()

const tasks = ref<Task[]>([])
const searchKeyword = ref('')
const filterStatus = ref('')
const filterPriority = ref<Priority | ''>('')
const viewMode = ref<'list' | 'grid' | 'calendar'>('list')
const showEditDialog = ref(false)
const showTaskDetail = ref(false)
const isEditing = ref(false)
const taskDetail = ref<Task | null>(null)

const editForm = reactive({
  id: null as number | null,
  title: '',
  description: '',
  priority: 'medium' as Priority,
  reminderTime: null as string | null
})

const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const currentMonth = ref(dayjs())

const currentMonthTitle = computed(() => currentMonth.value.format('YYYY年MM月'))

const filteredTasks = computed(() => {
  return tasks.value.filter((task) => {
    const matchKeyword = !searchKeyword.value || 
      task.title.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      (task.description && task.description.toLowerCase().includes(searchKeyword.value.toLowerCase()))
    
    const matchStatus = !filterStatus.value || task.status === filterStatus.value
    const matchPriority = !filterPriority.value || task.priority === filterPriority.value
    
    return matchKeyword && matchStatus && matchPriority
  })
})

const calendarDays = computed(() => {
  const startOfMonth = currentMonth.value.startOf('month')
  const endOfMonth = currentMonth.value.endOf('month')
  const startDay = startOfMonth.day()
  const daysInMonth = endOfMonth.date()
  
  const days: Array<{
    day: number
    date: string
    otherMonth: boolean
    isToday: boolean
    tasks: Task[]
  }> = []
  
  for (let i = 0; i < startDay; i++) {
    const date = startOfMonth.subtract(startDay - i, 'day')
    days.push({
      day: date.date(),
      date: date.format('YYYY-MM-DD'),
      otherMonth: true,
      isToday: false,
      tasks: []
    })
  }
  
  const today = dayjs().format('YYYY-MM-DD')
  
  for (let i = 1; i <= daysInMonth; i++) {
    const date = currentMonth.value.date(i)
    const dateStr = date.format('YYYY-MM-DD')
    const dayTasks = tasks.value.filter((task) => {
      if (!task.reminderTime) return false
      return dayjs(task.reminderTime).format('YYYY-MM-DD') === dateStr
    })
    
    days.push({
      day: i,
      date: dateStr,
      otherMonth: false,
      isToday: dateStr === today,
      tasks: dayTasks
    })
  }
  
  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    const date = endOfMonth.add(i, 'day')
    days.push({
      day: date.date(),
      date: date.format('YYYY-MM-DD'),
      otherMonth: true,
      isToday: false,
      tasks: []
    })
  }
  
  return days
})

const loadTasks = async () => {
  try {
    tasks.value = await taskAPI.list()
  } catch (e) {
    ElMessage.error('加载任务失败')
  }
}

const handleFilterChange = () => {
  // filter is reactive via computed
}

const handleToggleTask = async (task: Task) => {
  try {
    await taskAPI.complete(task.id)
    ElMessage.success('任务已完成')
    loadTasks()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleRowClick = (row: Task) => {
  taskDetail.value = row
  showTaskDetail.value = true
}

const goToTaskDetail = (id: number) => {
  router.push(`/tasks/${id}`)
}

const handleCreateTask = () => {
  isEditing.value = false
  editForm.id = null
  editForm.title = ''
  editForm.description = ''
  editForm.priority = 'medium'
  editForm.reminderTime = null
  showEditDialog.value = true
}

const handleEdit = (task: Task) => {
  isEditing.value = true
  editForm.id = task.id
  editForm.title = task.title
  editForm.description = task.description || ''
  editForm.priority = task.priority
  editForm.reminderTime = task.reminderTime || null
  showEditDialog.value = true
  showTaskDetail.value = false
}

const handleSaveEdit = async () => {
  if (!editForm.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  
  try {
    if (isEditing.value && editForm.id) {
      await taskAPI.update(editForm.id, {
        title: editForm.title,
        description: editForm.description,
        priority: editForm.priority,
        reminderTime: editForm.reminderTime || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await taskAPI.create({
        title: editForm.title,
        description: editForm.description,
        priority: editForm.priority,
        reminderTime: editForm.reminderTime || undefined
      })
      ElMessage.success('创建成功')
    }
    showEditDialog.value = false
    loadTasks()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定要删除该任务吗？', '提示', { type: 'warning' })
  try {
    await taskAPI.delete(id)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const handleEditFromDetail = () => {
  if (!taskDetail.value) return
  showTaskDetail.value = false
  handleEdit(taskDetail.value)
}

const handleCompleteFromDetail = async () => {
  if (!taskDetail.value) return
  await handleToggleTask(taskDetail.value)
  showTaskDetail.value = false
}

const handleDeleteFromDetail = async () => {
  if (!taskDetail.value) return
  await handleDelete(taskDetail.value.id)
  showTaskDetail.value = false
}

const prevMonth = () => {
  currentMonth.value = currentMonth.value.subtract(1, 'month')
}

const nextMonth = () => {
  currentMonth.value = currentMonth.value.add(1, 'month')
}

const goToToday = () => {
  currentMonth.value = dayjs()
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

const getPriorityClass = (priority: Priority) => {
  const map: Record<Priority, string> = { high: 'high', medium: 'medium', low: 'low' }
  return map[priority]
}

const formatDate = (date: string) => {
  return dayjs(date).format('MM-DD HH:mm')
}

const formatDateTime = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.tasks-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
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

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.filter-left {
  flex: 1;
  max-width: 300px;
}

.filter-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.tasks-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  min-height: 400px;
}

.tasks-content.list :deep(.el-table) {
  border-radius: 12px;
}

.tasks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.task-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #f0f0f5 100%);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
  cursor: pointer;
}

.task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.task-card.completed {
  opacity: 0.6;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.task-card.completed .card-title {
  text-decoration: line-through;
}

.card-desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
}

.card-date {
  font-size: 12px;
  color: #888;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.task-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-title-cell span.completed {
  text-decoration: line-through;
  color: #999;
}

.reminder-time {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 13px;
}

.no-reminder {
  color: #ccc;
  font-size: 13px;
}

.action-btns {
  display: flex;
  gap: 4px;
}

.calendar-container {
  padding: 16px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.calendar-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1a1a2e;
}

.calendar-grid {
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
}

.calendar-week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
}

.week-day {
  padding: 12px;
  text-align: center;
  font-weight: 600;
  color: #666;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.calendar-day {
  min-height: 80px;
  padding: 8px;
  border-right: 1px solid #eee;
  border-bottom: 1px solid #eee;
}

.calendar-day:nth-child(7n) {
  border-right: none;
}

.calendar-day.other-month {
  background: #fafafa;
}

.calendar-day.today {
  background: #e6f7ff;
}

.day-number {
  font-weight: 600;
  color: #333;
}

.calendar-day.other-month .day-number {
  color: #ccc;
}

.calendar-day.today .day-number {
  color: #1890ff;
}

.day-tasks {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.task-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  cursor: pointer;
}

.task-dot.high {
  background: #ff4d4f;
}

.task-dot.medium {
  background: #faad14;
}

.task-dot.low {
  background: #52c41a;
}

.more-tasks {
  font-size: 10px;
  color: #888;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.detail-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1a1a2e;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item label {
  font-size: 13px;
  color: #888;
  font-weight: 500;
}

.detail-item p {
  margin: 0;
  font-size: 14px;
  color: #333;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    gap: 12px;
  }

  .filter-left {
    max-width: 100%;
    width: 100%;
  }

  .filter-right {
    flex-wrap: wrap;
  }

  .tasks-grid {
    grid-template-columns: 1fr;
  }
}
</style>
