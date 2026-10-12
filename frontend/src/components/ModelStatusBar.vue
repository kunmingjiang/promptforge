<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getModelConfig, getModelStatus, testModelConfig, updateModelConfig } from '../api'
import { useAuthStore } from '../stores/auth'
import { PROVIDERS, providerOptions } from '../providers'
import type { ModelConfig, ModelConfigTestResult, ModelStatus } from '../types'

/**
 * 模型状态条 + 就地配置弹框（多供应商）：
 * - 供应商可切换：DeepSeek / 智谱 GLM / Kimi / MiniMax / 自定义（OpenAI 兼容）
 * - 选择供应商自动填充 Base URL 与默认模型名，教程按供应商动态切换
 * - 状态就绪：显示「供应商 · 模型名 · 就绪」，点击可查看/修改
 * - 未配置：显示警示态，点生成时自动弹出配置框
 */
const props = defineProps<{ autoOpen?: boolean }>()
const auth = useAuthStore()

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
    const providerLabel = PROVIDERS[status.value?.provider || '']?.label || status.value?.provider
    return `${providerLabel} · ${status.value?.modelName || ''} · 就绪`
  }
  return '模型未配置'
})

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
    ElMessage.warning('请填写 Base URL 与模型名')
    return
  }
  if (!masked.value && !form.apiKey.trim()) {
    ElMessage.warning('首次配置需要填写 API Key')
    return
  }
  saving.value = true
  try {
    await updateModelConfig({
      provider: form.provider,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey || undefined,
      modelName: form.modelName,
      enabled: form.enabled
    })
    ElMessage.success('模型配置已保存，API Key 已加密存储')
    masked.value = '****'
    loadedAdminConfig.value = true
    form.apiKey = ''
    await refresh()
    dialogVisible.value = false
  } finally {
    saving.value = false
  }
}

async function onTest() {
  if (!form.apiKey.trim()) {
    ElMessage.warning('测试连接需要输入 API Key（已保存的 Key 出于安全不回显，重新粘贴一次即可）')
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
      ElMessage.success(`连接成功，耗时 ${testResult.value.latencyMs}ms`)
    } else {
      ElMessage.error(testResult.value?.message || '连接失败')
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
    <span class="chip-action">{{ ready ? '管理' : '去配置' }}</span>
  </button>

  <!-- 就地配置弹框 -->
  <el-dialog v-model="dialogVisible" title="模型配置" width="600px" append-to-body>
    <el-alert
      v-if="status && !status.configured"
      type="warning"
      :closable="false"
      show-icon
      :title="status.message || '模型尚未配置，配置完成后即可开始生成'"
      style="margin-bottom: 16px"
    />

    <!-- 管理员：可编辑表单 -->
    <template v-if="auth.isAdmin">
      <el-form label-position="top" v-loading="loading">
        <el-form-item label="模型提供商" required>
          <div class="provider-grid">
            <button
              v-for="p in providerOptions"
              :key="p.key"
              type="button"
              class="provider-item"
              :class="{ active: form.provider === p.key }"
              @click="form.provider = p.key"
            >{{ p.label }}</button>
          </div>
        </el-form-item>
        <el-form-item label="Base URL" required>
          <el-input v-model="form.baseUrl" :placeholder="preset.baseUrl || 'https://your-gateway.example.com/v1'" />
        </el-form-item>
        <el-form-item :label="masked ? `API Key（已保存：${masked}）` : 'API Key'" required>
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="masked ? '留空则保留原 Key' : (preset.siteName ? `在 ${preset.siteName} 创建的 Key` : 'sk-...')"
          />
        </el-form-item>
        <el-form-item label="模型名" required>
          <el-input v-model="form.modelName" :placeholder="preset.model || '例如 gpt-4o / qwen-max'" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <div class="dialog-actions">
          <el-button :loading="testing" @click="onTest">测试连接</el-button>
          <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
        </div>
      </el-form>
      <el-alert
        v-if="testResult"
        :type="testResult.success ? 'success' : 'error'"
        :title="testResult.message"
        :description="`耗时 ${testResult.latencyMs}ms`"
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
        title="模型配置由管理员统一管理"
        description="请联系管理员在「管理后台 → 模型配置」中完成配置；如果你自己就是管理员，用管理员账号登录后即可在此直接填写。"
        style="margin-bottom: 16px"
      />
    </template>

    <!-- 获取 Key 引导（按所选供应商动态切换） -->
    <el-collapse v-if="preset.steps.length" class="guide">
      <el-collapse-item :title="`不知道怎么获取 ${preset.label} 的 API Key？看这里（3 步搞定）`" name="guide">
        <ol class="guide-steps">
          <li v-for="[t, d] in preset.steps" :key="t">
            <b>{{ t }}</b>：{{ d }}
          </li>
        </ol>
        <div class="guide-tips">
          <div v-for="tip in preset.tips" :key="tip">· {{ tip }}</div>
          <div>· Base URL 填 <code>{{ preset.baseUrl }}</code></div>
          <div>· Key 只保存在你自己的服务器里（AES-256 加密），不会上传任何第三方</div>
        </div>
        <el-button size="small" tag="a" :href="preset.site" target="_blank" rel="noopener">
          打开 {{ preset.siteName }} ↗
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
