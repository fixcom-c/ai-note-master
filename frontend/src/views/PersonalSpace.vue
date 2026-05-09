<template>
  <div class="personal-space">
    <div class="page-header">
      <div>
        <h1>个人空间</h1>
        <p class="subtitle">把你的目标、偏好和生活/工作节奏交给系统，AI 才能越来越懂你。</p>
      </div>
      <el-button type="primary" :loading="saving" @click="handleSave">
        <el-icon><Check /></el-icon>
        保存画像
      </el-button>
    </div>

    <div class="personal-grid">
      <div class="left-column">
        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><User /></el-icon> 你的长期背景</span>
            </div>
          </template>

          <el-form label-position="top" class="profile-form">
            <el-form-item label="你希望 AI 怎么称呼你">
              <el-input v-model="profile.displayName" placeholder="例如：阿峰 / Qifeng / Fix" />
            </el-form-item>
            <el-form-item label="你现在是谁 / 处于什么阶段">
              <el-input
                v-model="profile.identitySummary"
                type="textarea"
                :rows="3"
                placeholder="例如：正在做个人产品的独立开发者，希望把工作、学习、生活记录沉淀为长期资产。"
              />
            </el-form-item>
            <el-form-item label="你当前最想推进的重点">
              <el-input
                v-model="profile.currentFocus"
                type="textarea"
                :rows="3"
                placeholder="例如：把 AI Note 打磨成每天都会打开的个人第二大脑。"
              />
            </el-form-item>
            <el-form-item label="长期目标">
              <el-input
                v-model="profile.longTermGoals"
                type="textarea"
                :rows="4"
                placeholder="例如：建立一个能长期复用我知识、帮我做计划和复盘的私人系统。"
              />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Collection /></el-icon> 你的使用节奏</span>
            </div>
          </template>

          <el-form label-position="top" class="profile-form">
            <el-form-item label="你的工作/行动风格">
              <el-input
                v-model="profile.workStyle"
                type="textarea"
                :rows="3"
                placeholder="例如：喜欢先快速记下碎片，再集中整理；不喜欢复杂流程。"
              />
            </el-form-item>
            <el-form-item label="你最常记录的生活/工作主题">
              <el-input
                v-model="profile.lifeAreas"
                type="textarea"
                :rows="3"
                placeholder="例如：产品、开发、复盘、财务、健康、阅读、生活琐事。"
              />
            </el-form-item>
            <el-form-item label="你希望 AI 给建议的方式">
              <el-input
                v-model="profile.aiPreference"
                type="textarea"
                :rows="3"
                placeholder="例如：直接、少空话、优先给 1-3 条可执行建议。"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <div class="right-column">
        <el-card class="insight-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><MagicStick /></el-icon> AI 对你的当前理解</span>
              <el-button text size="small" @click="loadInsight">
                刷新洞察
              </el-button>
            </div>
          </template>

          <div v-if="insight" class="insight-content">
            <div class="hero-block">
              <h3>{{ insight.headline }}</h3>
              <p>{{ insight.understanding }}</p>
            </div>

            <div class="stat-grid">
              <div class="mini-stat">
                <strong>{{ insight.todayPlanned }}</strong>
                <span>今日计划</span>
              </div>
              <div class="mini-stat">
                <strong>{{ insight.todayCompleted }}</strong>
                <span>今日完成</span>
              </div>
              <div class="mini-stat">
                <strong>{{ insight.pendingTasks }}</strong>
                <span>待完成</span>
              </div>
            </div>

            <div class="section-block">
              <h4>当前节奏</h4>
              <p>{{ insight.rhythm }}</p>
            </div>

            <div class="section-block" v-if="insight.focusTags.length">
              <h4>最近主题</h4>
              <div class="tag-row">
                <el-tag v-for="tag in insight.focusTags" :key="tag" type="info">{{ tag }}</el-tag>
              </div>
            </div>

            <div class="section-block">
              <h4>更适合你持续使用的方式</h4>
              <ul class="suggestion-list">
                <li v-for="item in insight.suggestions" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </el-card>

        <el-card class="habit-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Reading /></el-icon> 我建议你这样用</span>
            </div>
          </template>

          <div class="habit-list">
            <div class="habit-item">
              <strong>早上</strong>
              <p>写一条日计划笔记，列今天最重要的 1 到 3 件事。</p>
            </div>
            <div class="habit-item">
              <strong>白天</strong>
              <p>任何碎片想法、会议、灵感、生活事项，先扔进快速记录，不要等整理好再写。</p>
            </div>
            <div class="habit-item">
              <strong>晚上</strong>
              <p>看完成度、补复盘、写下明天第一步，让系统形成连续的个人上下文。</p>
            </div>
            <div class="habit-item">
              <strong>每周</strong>
              <p>导入 1 到 2 份旧笔记或资料，让 AI 更快识别你的长期主题和决策偏好。</p>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { profileAPI, aiAPI } from '@/api'
import type { PersonalInsight, PersonalProfile } from '@/api/types'
import { Check, User, Collection, MagicStick, Reading } from '@element-plus/icons-vue'

const saving = ref(false)
const insight = ref<PersonalInsight | null>(null)

const profile = reactive<PersonalProfile>({
  displayName: '',
  identitySummary: '',
  currentFocus: '',
  longTermGoals: '',
  workStyle: '',
  lifeAreas: '',
  aiPreference: ''
})

const loadProfile = async () => {
  const data = await profileAPI.get()
  Object.assign(profile, data)
}

const loadInsight = async () => {
  insight.value = await aiAPI.personalInsight()
}

const handleSave = async () => {
  saving.value = true
  try {
    await profileAPI.update(profile)
    await Promise.all([loadProfile(), loadInsight()])
    ElMessage.success('个人画像已更新')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadProfile(), loadInsight()])
})
</script>

<style scoped>
.personal-space {
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
  max-width: 760px;
}

.personal-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(340px, 0.8fr);
  gap: 20px;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card,
.insight-card,
.habit-card {
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.insight-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-block {
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.1), rgba(19, 184, 166, 0.08));
}

.hero-block h3 {
  margin-bottom: 10px;
  font-size: 22px;
}

.hero-block p,
.section-block p {
  line-height: 1.8;
  color: var(--text-secondary);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.mini-stat {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.mini-stat strong {
  display: block;
  font-size: 28px;
  margin-bottom: 6px;
}

.mini-stat span {
  color: var(--text-secondary);
  font-size: 13px;
}

.section-block h4 {
  margin-bottom: 10px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestion-list {
  padding-left: 18px;
  color: var(--text-secondary);
  line-height: 1.9;
}

.habit-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.habit-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.04);
}

.habit-item strong {
  display: block;
  margin-bottom: 8px;
}

.habit-item p {
  color: var(--text-secondary);
  line-height: 1.8;
}

@media (max-width: 960px) {
  .personal-space {
    padding: 16px;
  }

  .page-header,
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .personal-grid,
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
