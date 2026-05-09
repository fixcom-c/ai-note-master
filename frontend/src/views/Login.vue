<template>
  <div class="login-page">
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>

    <section class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">AI NOTE MASTER</p>
        <h1>把随手记，变成可推进的行动系统。</h1>
        <p class="description">
          记录灵感、拆解任务、沉淀知识、生成日报。这个工作空间会帮你把零散输入整理成真正可执行的节奏。
        </p>
      </div>

      <div class="hero-metrics">
        <div class="metric-card">
          <strong>1 次输入</strong>
          <span>自动提取任务与标签</span>
        </div>
        <div class="metric-card">
          <strong>多视图协同</strong>
          <span>任务、知识、日报闭环联动</span>
        </div>
        <div class="metric-card">
          <strong>专注今天</strong>
          <span>更适合个人执行与复盘</span>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="panel-header">
        <div class="panel-badge">Sign In</div>
        <h2>进入你的智能工作台</h2>
        <p>首次使用直接注册，系统会自动创建账号。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" label-width="0">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            :prefix-icon="User"
            placeholder="用户名"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>

        <div class="register-row">
          <span>还没有账号？</span>
          <el-link type="primary" @click="handleRegister">立即注册</el-link>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authAPI } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateForm = async () => {
  if (!formRef.value) return false
  return formRef.value.validate().catch(() => false)
}

const handleLogin = async () => {
  const valid = await validateForm()
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  const valid = await validateForm()
  if (!valid) return

  loading.value = true
  try {
    const response = await authAPI.register(form)
    userStore.logout()
    ElMessage.success(`注册成功，欢迎你 ${response.username}`)
    form.password = ''
  } catch (error) {
    ElMessage.error('注册失败，用户名可能已存在')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 460px);
  gap: 28px;
  align-items: center;
  padding: 32px;
  overflow: hidden;
}

.ambient {
  position: absolute;
  border-radius: 999px;
  filter: blur(12px);
  opacity: 0.7;
}

.ambient-left {
  width: 360px;
  height: 360px;
  left: -80px;
  top: 10%;
  background: rgba(31, 111, 235, 0.22);
}

.ambient-right {
  width: 320px;
  height: 320px;
  right: -60px;
  bottom: 8%;
  background: rgba(19, 184, 166, 0.2);
}

.hero-panel,
.login-panel {
  position: relative;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.52);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(26px);
}

.hero-panel {
  padding: 42px;
  border-radius: 34px;
  background:
    linear-gradient(140deg, rgba(255, 255, 255, 0.9) 0%, rgba(240, 247, 255, 0.8) 55%, rgba(227, 252, 248, 0.72) 100%);
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--brand-primary);
  margin-bottom: 18px;
}

.hero-copy h1 {
  max-width: 680px;
  font-size: clamp(38px, 5vw, 62px);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.description {
  max-width: 560px;
  margin-top: 18px;
  font-size: 17px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 36px;
}

.metric-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.metric-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 20px;
}

.metric-card span {
  color: var(--text-secondary);
  line-height: 1.6;
}

.login-panel {
  padding: 34px;
  border-radius: 30px;
  background: rgba(10, 18, 33, 0.88);
  color: white;
}

.panel-header {
  margin-bottom: 28px;
}

.panel-badge {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  margin-bottom: 16px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  background: rgba(31, 111, 235, 0.18);
  color: #8dc0ff;
}

.panel-header h2 {
  font-size: 28px;
  margin-bottom: 10px;
}

.panel-header p {
  color: rgba(255, 255, 255, 0.66);
  line-height: 1.7;
}

:deep(.el-input__wrapper) {
  min-height: 50px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.06);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

:deep(.el-input__inner) {
  color: white;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.submit-btn {
  width: 100%;
  min-height: 50px;
  margin-top: 8px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #1f6feb, #13b8a6);
}

.register-row {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.66);
}

@media (max-width: 1080px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
