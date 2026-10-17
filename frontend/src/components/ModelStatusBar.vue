<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getModelConfig, getModelStatus, testModelConfig, updateModelConfig } from '../api'
import { useAuthStore } from '../stores/auth'
import { PROVIDERS, providerOptions } from '../providers'
import type { ModelConfig, ModelConfigTestResult, ModelStatus } from '../types'

/**
 * 模型状态条 + 就地配置弹框（多供应商）：
 * - 供应商可切换：DeepSeek / 智谱 GLM / Kimi / MiniMax / OpenAI / Claude / Gemini / 自定义（OpenAI 兼容）
 * - 选择供应商自动填充 Base URL 与默认模型名，教程按供应商从 i18n 动态读取
 * - 状态就绪：显示「供应商 · 模型名 · 就绪」，点击可查看/修改
 * - 未配置：显示警示态，点生成时自动弹出配置框
 */
const props = defineProps<{ autoOpen?: boolean }>()
const auth = useAuthStore()
const { t, tm } = useI18n()

const status = ref<ModelStatus | null>(null)
const dialogVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const testResult = ref<ModelConfigTestResult | null>(null)
const masked = ref('')
const loadedAdminConfig = ref(false)

const form = reactive({
  provider: 'deepseek',
  baseUrl: 'https://api.deepseek.com',
  apiKey: '',
  modelName: 'deepseek-chat',
  enabled: true
})

const preset = computed(() => PROVIDERS[form.provider] || PROVIDERS.custom)
const ready = computed(() => !!status.value?.configured)
const statusText = computed(() => {
  if (ready.value) {
    const key = status.value?.provider || ''
    const providerLabel = key === 'custom' ? t('providers.custom.label') : (PROVIDERS[key]?.label || status.value?.provider)
    return t('status.statusReady', { provider: providerLabel, model: status.value?.modelName || '' })
  }
  return t('status.notConfigured')
})

/** 当前供应商的教程（steps / tips 从 i18n 读取，随语言切换；数组消息须用 tm()） */
const guideSteps = computed<{ t: string; d: string }[]>(() =>
  preset.value.hasGuide ? ((tm('providers.' + form.provider + '.steps') || []) as unknown as { t: string; d: string }[]) : []
)
const guideTips = computed<string[]>(() =>
  preset.value.hasGuide ? ((tm('providers.' + form.provider + '.tips') || []) as unknown as string[]) : []
)

/** 切换供应商：自动填充该家的 Base URL 与默认模型名 */
watch(() => form.provider, (key) => {
  const p = PROVIDERS[key]
  if (!p) return
  form.baseUrl = p.baseUrl
  form.modelName = p.model
  testResult.value = null
})

async function refresh() {
  try {
    const result: any = await getModelStatus()
    status.value = result.data as ModelStatus
  } catch {
    status.value = null
  }
}

function open() {
  testResult.value = null
  form.apiKey = ''
  if (auth.isAdmin && !loadedAdminConfig.value) {
    loading.value = true
    getModelConfig()
      .then((result: any) => {
        const config = result.data as ModelConfig | null
        if (config) {
          form.provider = PROVIDERS[config.provider] ? config.provider : 'custom'
          // 直接恢复已保存值（自定义网关地址不被预设覆盖）
          form.baseUrl = config.baseUrl
          form.modelName = config.modelName
          form.enabled = config.enabled
          masked.value = config.apiKeyMasked
          loadedAdminConfig.value = true
        }
      })
      .finally(() => {
        loading.value = false
      })
  }
  dialogVisible.value = true
}

/** 供页面在生成动作前调用：就绪返回 true，否则弹配置框并返回 false */
async function ensureReady(): Promise<boolean> {
  await refresh()
  if (ready.value) return true
  open()
  return false
}

async function onSave() {
  if (!form.baseUrl.trim() || !form.modelName.trim()) {
    ElMessage.warning(t('status.warnFillBaseUrlModel'))
    return
  }
  if (!masked.value && !form.apiKey.trim()) {
    ElMessage.warning(t('status.warnNeedApiKeyFirst'))
    return
  }
  saving.value = true
  try {
    const result: any = await updateModelConfig({
      provider: form.provider,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey || undefined,
      modelName: form.modelName,
      enabled: form.enabled
    })
    // 用服务端回传的掩码回填，让「已保存」看得见（如 ****8fce）
    const saved = result?.data as ModelConfig | null
    masked.value = saved?.apiKeyMasked || '****'
    loadedAdminConfig.value = true
    form.apiKey = ''
    dialogVisible.value = false
    await refresh()
    ElMessage.success(t('status.savedEncrypted'))
  } catch (e: any) {
    // 保存失败必须明确报错，不能静默
    ElMessage.error(e?.response?.data?.message || e?.message || t('status.connFailed'))
  } finally {
    saving.value = false
  }
}

async function onTest() {
  if (!form.apiKey.trim()) {
    ElMessage.warning(t('status.warnNeedKeyForTest'))
    return
  }
  testing.value = true
  testResult.value = null
  try {
    const result: any = await testModelConfig({
      provider: form.provider,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey.trim(),
      modelName: form.modelName
    })
    testResult.value = result.data as ModelConfigTestResult
    if (testResult.value?.success) {
      ElMessage.success(t('status.connSuccess', { ms: testResult.value.latencyMs }))
    } else {
      ElMessage.error(testResult.value?.message || t('status.connFailed'))
    }
  } finally {
    testing.value = false
  }
}

defineExpose({ ensureReady, open, refresh })

refresh()
</script>

<template>
  <!-- 状态条 -->
  <button class="model-chip" :class="{ ready }" type="button" @click="open">
    <span class="chip-dot"></span>
    <span class="chip-text">{{ statusText }}</span>
    <span class="chip-action">{{ ready ? t('status.manage') : t('status.goConfig') }}</span>
  </button>

  <!-- 就地配置弹框 -->
  <el-dialog v-model="dialogVisible" :title="t('status.dialogTitle')" width="600px" append-to-body>
    <el-alert
      v-if="status && !status.configured"
      type="warning"
      :closable="false"
      show-icon
      :title="status.message || t('status.notConfiguredAlert')"
      style="margin-bottom: 16px"
    />

    <!-- 管理员：可编辑表单 -->
    <template v-if="auth.isAdmin">
      <el-form label-position="top" v-loading="loading">
        <el-form-item :label="t('admin.provider')" required>
          <div class="provider-grid">
            <button
              v-for="p in providerOptions"
              :key="p.key"
              type="button"
              class="provider-item"
              :class="{ active: form.provider === p.key }"
              @click="form.provider = p.key"
            >{{ p.key === 'custom' ? t('providers.custom.label') : p.label }}</button>
          </div>
        </el-form-item>
        <el-form-item :label="t('admin.baseUrl')" required>
          <el-input v-model="form.baseUrl" :placeholder="preset.baseUrl || 'https://your-gateway.example.com/v1'" />
        </el-form-item>
        <el-form-item :label="masked ? t('admin.apiKeySaved', { masked }) : t('admin.apiKey')" required>
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="masked ? t('admin.keyKeepOriginal') : (preset.siteName ? t('admin.keyCreatedAt', { site: preset.siteName }) : 'sk-...')"
          />
        </el-form-item>
        <el-form-item :label="t('admin.modelName')" required>
          <el-input v-model="form.modelName" :placeholder="preset.model || t('admin.modelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('admin.enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <div class="dialog-actions">
          <el-button :loading="testing" @click="onTest">{{ t('admin.testConn') }}</el-button>
          <el-button type="primary" :loading="saving" @click="onSave">{{ t('admin.saveConfig') }}</el-button>
        </div>
      </el-form>
      <el-alert
        v-if="testResult"
        :type="testResult.success ? 'success' : 'error'"
        :title="testResult.message"
        :description="t('admin.latency', { ms: testResult.latencyMs })"
        :closable="false"
        show-icon
        style="margin-top: 8px"
      />
    </template>

    <!-- 普通用户：只读状态 + 引导 -->
    <template v-else>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        :title="t('status.userAlertTitle')"
        :description="t('status.userAlertDesc')"
        style="margin-bottom: 16px"
      />
    </template>

    <!-- 获取 Key 引导（按所选供应商动态切换，文案走 i18n） -->
    <el-collapse v-if="preset.hasGuide" class="guide">
      <el-collapse-item :title="t('status.guideTitle', { provider: preset.label })" name="guide">
        <ol class="guide-steps">
          <li v-for="s in guideSteps" :key="s.t">
            <b>{{ s.t }}</b>：{{ s.d }}
          </li>
        </ol>
        <div class="guide-tips">
          <div v-for="tip in guideTips" :key="tip">· {{ tip }}</div>
          <div>· {{ t('status.baseUrlTip', { url: preset.baseUrl }) }}</div>
          <div>· {{ t('status.keySafeTip') }}</div>
        </div>
        <el-button size="small" tag="a" :href="preset.site" target="_blank" rel="noopener">
          {{ t('status.openSite', { site: preset.siteName }) }}
        </el-button>
      </el-collapse-item>
    </el-collapse>
  </el-dialog>
</template>

<style scoped>
.model-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  color: var(--text-2);
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.18s var(--ease), background-color 0.18s var(--ease);
}

.model-chip:hover {
  border-color: var(--line-strong);
  background: var(--surface-3);
}

.chip-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f59e0b;
  flex-shrink: 0;
}

.model-chip.ready .chip-dot {
  background: #10b981;
}

.chip-text {
  font-weight: 500;
}

.model-chip.ready .chip-text {
  color: var(--text-1);
}

.chip-action {
  font-size: 12.5px;
  color: var(--interactive);
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  width: 100%;
}

.provider-item {
  padding: 8px 4px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line);
  background: var(--surface-2);
  color: var(--text-2);
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.15s var(--ease), background-color 0.15s var(--ease), color 0.15s var(--ease);
}

.provider-item:hover {
  border-color: var(--line-strong);
  color: var(--text-1);
}

.provider-item.active {
  border-color: var(--interactive);
  background: rgba(37, 99, 235, 0.1);
  color: var(--interactive);
  font-weight: 600;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.guide {
  margin-top: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
}

.guide :deep(.el-collapse-item__header) {
  background: transparent;
  border-bottom: 1px solid var(--line);
  padding: 0 14px;
  font-size: 14px;
  color: var(--text-2);
}

.guide :deep(.el-collapse-item__content) {
  padding: 14px;
  color: var(--text-2);
  font-size: 14px;
  line-height: 1.8;
}

.guide-steps {
  margin: 0 0 12px;
  padding-left: 20px;
}

.guide-steps li {
  margin-bottom: 6px;
}

.guide-tips {
  margin-bottom: 12px;
  color: var(--text-3);
  font-size: 13.5px;
}

code {
  background: var(--accent-dim);
  color: #0277bd;
  padding: 1px 6px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 0.9em;
}

a {
  color: var(--interactive);
}
</style>
