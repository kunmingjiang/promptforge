<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getModelConfig, testModelConfig, updateModelConfig } from '../../api'
import { PROVIDERS, providerOptions } from '../../providers'
import type { ModelConfig, ModelConfigTestResult } from '../types'

const form = reactive({
  provider: 'deepseek',
  baseUrl: 'https://api.deepseek.com',
  modelName: 'deepseek-chat',
  enabled: false,
  apiKey: ''
})
const masked = ref('')
const saving = ref(false)
const testing = ref(false)
const testResult = ref<ModelConfigTestResult | null>(null)

const preset = computed(() => PROVIDERS[form.provider] || PROVIDERS.custom)

/** 切换供应商：自动填充该家的 Base URL 与默认模型名 */
watch(() => form.provider, () => {
  form.baseUrl = preset.value.baseUrl
  form.modelName = preset.value.model
  testResult.value = null
})

onMounted(async () => {
  const result: any = await getModelConfig()
  const config = result.data as ModelConfig | null
  if (config) {
    form.provider = PROVIDERS[config.provider] ? config.provider : 'custom'
    // 直接恢复已保存值（自定义网关地址不被预设覆盖）
    form.baseUrl = config.baseUrl
    form.modelName = config.modelName
    form.enabled = config.enabled
    masked.value = config.apiKeyMasked
  }
})

async function onSave() {
  if (!form.baseUrl.trim() || !form.modelName.trim()) {
    ElMessage.warning('请填写 Base URL 与模型名')
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
    ElMessage.success('配置已保存，API Key 已加密存储')
    const result: any = await getModelConfig()
    masked.value = (result.data as ModelConfig)?.apiKeyMasked || ''
    form.apiKey = ''
  } finally {
    saving.value = false
  }
}

async function onTest() {
  if (!form.apiKey.trim()) {
    ElMessage.warning('测试连接需要输入 API Key')
    return
  }
  testing.value = true
  testResult.value = null
  try {
    const result: any = await testModelConfig({
      provider: form.provider,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
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
</script>

<template>
  <div class="admin-page">
    <div class="page-card" style="max-width: 640px">
      <h3 class="panel-title">模型配置</h3>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="支持 DeepSeek / 智谱 GLM / Kimi / MiniMax 及任意 OpenAI 兼容网关。API Key 使用 AES-256 加密存储，保存后仅显示末 4 位；更换 Key 时重新填写即可。"
        style="margin-bottom: 16px"
      />

      <el-form label-position="top">
        <el-form-item label="模型提供商" required>
          <el-select v-model="form.provider" style="width: 100%">
            <el-option v-for="p in providerOptions" :key="p.key" :label="p.label" :value="p.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL" required>
          <el-input v-model="form.baseUrl" :placeholder="preset.baseUrl || 'https://your-gateway.example.com/v1'" />
        </el-form-item>
        <el-form-item label="模型名" required>
          <el-input v-model="form.modelName" :placeholder="preset.model || '例如 gpt-4o / qwen-max'" />
        </el-form-item>
        <el-form-item :label="masked ? `API Key（已保存：${masked}）` : 'API Key'">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="masked ? '留空则保留原 Key' : (preset.siteName ? `在 ${preset.siteName} 创建的 Key` : 'sk-...')"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <div class="actions">
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
    </div>
  </div>
</template>

<style scoped>
.actions {
  display: flex;
  gap: 10px;
}
</style>
