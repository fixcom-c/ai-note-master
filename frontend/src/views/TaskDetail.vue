<template>
  <div class="task-detail">
    <el-card v-if="task">
      <template #header>
        <div class="header">
          <span>任务详情</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="task.title" :disabled="!editing" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="task.description" type="textarea" :rows="4" :disabled="!editing" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="task.priority" :disabled="!editing">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-tag :type="task.status === 'completed' ? 'success' : 'info'">
            {{ task.status === 'completed' ? '已完成' : '待办' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="创建时间">
          <span>{{ task.createdAt }}</span>
        </el-form-item>
        <el-form-item v-if="task.completedAt" label="完成时间">
          <span>{{ task.completedAt }}</span>
        </el-form-item>
      </el-form>

      <div class="actions">
        <template v-if="!editing">
          <el-button v-if="task.status !== 'completed'" type="success" @click="handleComplete">完成任务</el-button>
          <el-button type="primary" @click="editing = true">编辑</el-button>
          <el-button type="danger" @click="handleDelete">删除</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="handleSave">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskAPI } from '@/api'

const route = useRoute()
const router = useRouter()
const task = ref<any>(null)
const editing = ref(false)
const originalTask = ref<any>(null)

const loadTask = async () => {
  try {
    const id = Number(route.params.id)
    task.value = await taskAPI.get(id)
    originalTask.value = { ...task.value }
  } catch (e) {
    ElMessage.error('加载失败')
    router.push('/tasks')
  }
}

const handleBack = () => {
  router.push('/tasks')
}

const handleComplete = async () => {
  try {
    await taskAPI.complete(task.value.id)
    ElMessage.success('任务已完成')
    loadTask()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleSave = async () => {
  try {
    await taskAPI.update(task.value.id, task.value)
    ElMessage.success('保存成功')
    editing.value = false
    loadTask()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleCancel = () => {
  task.value = { ...originalTask.value }
  editing.value = false
}

const handleDelete = async () => {
  await ElMessageBox.confirm('确定要删除吗？', '提示', { type: 'warning' })
  try {
    await taskAPI.delete(task.value.id)
    ElMessage.success('删除成功')
    router.push('/tasks')
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(loadTask)
</script>

<style scoped>
.task-detail {
  max-width: 800px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  margin-top: 20px;
  text-align: right;
}
</style>