<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, FormInstance } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { t } = useI18n()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', email: '' })

async function onLogin() {
  await loginFormRef.value?.validate()
  loading.value = true
  try {
    await auth.login(loginForm)
    ElMessage.success(t('login.loginSuccess'))
    router.push((route.query.redirect as string) || '/')
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  await registerFormRef.value?.validate()
  loading.value = true
  try {
    await auth.register(registerForm)
    ElMessage.success(t('login.registerSuccess'))
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左：品牌叙事 -->
    <div class="showcase">
      <div class="showcase-inner">
        <div class="mark-row">
          <span class="brand-mark">城</span>
          <span class="mono-tag">PROMPTFORGE · AI WORKSPACE</span>
        </div>
        <h1 class="headline">{{ t('login.headlinePre') }}<br /><span class="accent">{{ t('login.headlineAccent') }}</span></h1>
        <p class="sub">{{ t('login.sub') }}</p>

        <div class="feature-list">
          <div class="feature">
            <span class="feature-tag mono-tag">01</span>
            <div>
              <div class="feature-title">{{ t('login.feature1Title') }}</div>
              <div class="feature-desc">{{ t('login.feature1Desc') }}</div>
            </div>
          </div>
          <div class="feature">
            <span class="feature-tag mono-tag">02</span>
            <div>
              <div class="feature-title">{{ t('login.feature2Title') }}</div>
              <div class="feature-desc">{{ t('login.feature2Desc') }}</div>
            </div>
          </div>
          <div class="feature">
            <span class="feature-tag mono-tag">03</span>
            <div>
              <div class="feature-title">{{ t('login.feature3Title') }}</div>
              <div class="feature-desc">{{ t('login.feature3Desc') }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="glow"></div>
      <div class="grid-mask"></div>
    </div>

    <!-- 右：表单 -->
    <div class="form-side">
      <div class="form-box">
        <div class="form-brand">
          <span class="brand-mark">铸</span>
          <h2 class="form-title">{{ t('login.formTitle') }}</h2>
          <p class="form-sub">{{ t('login.formSub') }}</p>
        </div>

        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane :label="t('login.tabLogin')" name="login">
            <el-form ref="loginFormRef" :model="loginForm" label-position="top" @keyup.enter="onLogin">
              <el-form-item prop="username" required>
                <el-input v-model="loginForm.username" :placeholder="t('login.username')" size="large" />
              </el-form-item>
              <el-form-item prop="password" required>
                <el-input v-model="loginForm.password" type="password" show-password :placeholder="t('login.password')" size="large" />
              </el-form-item>
              <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onLogin">
                {{ t('login.loginBtn') }}
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane :label="t('login.tabRegister')" name="register">
            <el-form ref="registerFormRef" :model="registerForm" label-position="top" @keyup.enter="onRegister">
              <el-form-item
                prop="username"
                required
                :rules="[
                  { required: true, message: t('login.ruleUsernameRequired') },
                  { min: 3, max: 32, message: t('login.ruleUsernameLen') },
                  { pattern: /^[a-zA-Z0-9_-]+$/, message: t('login.ruleUsernamePattern') }
                ]"
              >
                <el-input v-model="registerForm.username" :placeholder="t('login.username')" size="large" />
              </el-form-item>
              <el-form-item
                prop="password"
                required
                :rules="[
                  { required: true, message: t('login.rulePasswordRequired') },
                  { min: 6, max: 64, message: t('login.rulePasswordLen') }
                ]"
              >
                <el-input v-model="registerForm.password" type="password" show-password :placeholder="t('login.passwordRegister')" size="large" />
              </el-form-item>
              <el-form-item prop="email">
                <el-input v-model="registerForm.email" :placeholder="t('login.emailOptional')" size="large" />
              </el-form-item>
              <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onRegister">
                {{ t('login.registerBtn') }}
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <p class="footer mono-tag">© CHENG-RONG-JIE-YUE · OPEN SOURCE</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
}

/* ---------- 左侧品牌叙事 ---------- */
.showcase {
  flex: 1.1;
  position: relative;
  display: flex;
  align-items: center;
  padding: 0 72px;
  overflow: hidden;
  background: linear-gradient(160deg, #e8f4ff 0%, #d3e9fc 55%, #bfe0fa 100%);
  border-right: 1px solid var(--line);
}

.showcase-inner {
  position: relative;
  z-index: 2;
  max-width: 480px;
}

.mark-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 36px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #38bdf8 0%, #2563eb 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
}

.brand-mark.small {
  width: 34px;
  height: 34px;
  font-size: 17px;
}

.headline {
  font-size: 42px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: var(--text-1);
  margin: 0 0 16px;
}

.accent {
  color: var(--interactive);
}

.sub {
  font-size: 17px;
  color: var(--text-2);
  line-height: 1.75;
  margin: 0 0 46px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.feature {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.feature-tag {
  padding-top: 3px;
  color: var(--accent);
  opacity: 0.85;
}

.feature-title {
  font-size: 16.5px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 15px;
  color: var(--text-2);
  line-height: 1.65;
}

/* 背景光晕：只此一处用大渐变，落在品牌侧 */
.glow {
  position: absolute;
  width: 640px;
  height: 640px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.85) 0%, rgba(56, 189, 248, 0.2) 40%, transparent 70%);
  top: -160px;
  right: -180px;
  pointer-events: none;
}

.grid-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(37, 99, 235, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.05) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 80% 70% at 30% 40%, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 80% 70% at 30% 40%, #000 30%, transparent 75%);
  pointer-events: none;
}

/* ---------- 右侧表单 ---------- */
.form-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-box {
  width: 400px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 32px 34px 24px;
  box-shadow: 0 1px 2px rgba(30, 80, 160, 0.06), 0 16px 48px rgba(30, 90, 180, 0.14);
}

.form-brand {
  text-align: center;
  margin-bottom: 14px;
}

.form-title {
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--text-1);
  margin: 12px 0 4px;
}

.form-sub {
  font-size: 12.5px;
  color: var(--text-3);
  margin: 0 0 10px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.footer {
  margin-top: 26px;
}

@media (max-width: 900px) {
  .showcase {
    display: none;
  }
}
</style>
