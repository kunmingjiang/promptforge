<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getModelConfig, testModelConfig, updateModelConfig } from '../../api'
import { PROVIDERS, providerOptions } from '../../providers'
import type { ModelConfig, ModelConfigTestResult } from '../types'

const { t } = useI18n()

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
    ElMessage.warning(t('admin.warnFillBaseUrlModel'))
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
    ElMessage.success(t('admin.savedEncrypted'))
    const result: any = await getModelConfig()
    masked.value = (result.data as ModelConfig)?.apiKeyMasked || ''
    form.apiKey = ''
  } catch (e: any) {
    // 保存失败必须明确报错，不能静默
    ElMessage.error(e?.response?.data?.message || e?.message || t('admin.connFailed'))
  } finally {
    saving.value = false
  }
}

async function onTest() {
  if (!form.apiKey.trim()) {
    ElMessage.warning(t('admin.warnNeedKeyForTest'))
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
      ElMessage.success(t('admin.connSuccess', { ms: testResult.value.latencyMs }))
    } else {
      ElMessage.error(testResult.value?.message || t('admin.connFailed'))
    }
  } finally {
    testing.value = false
  }
}
</script>

<template>
  <div class="admin-page">
    <div class="page-card" style="max-width: 640px">
      <h3 class="panel-title">{{ t('admin.configTitle') }}</h3>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        :title="t('admin.configAlert')"
        style="margin-bottom: 16px"
      />

      <el-form label-position="top">
        <el-form-item :label="t('admin.provider')" required>
          <el-select v-model="form.provider" style="width: 100%">
            <el-option
              v-for="p in providerOptions"
              :key="p.key"
              :label="p.key === 'custom' ? t('providers.custom.label') : p.label"
              :value="p.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.baseUrl')" required>
          <el-input v-model="form.baseUrl" :placeholder="preset.baseUrl || 'https://your-gateway.example.com/v1'" />
        </el-form-item>
        <el-form-item :label="t('admin.modelName')" required>
          <el-input v-model="form.modelName" :placeholder="preset.model || t('admin.modelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="masked ? t('admin.apiKeySaved', { masked }) : t('admin.apiKey')">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            autocomplete="new-password"
            name="pf-api-key"
            :placeholder="masked ? t('admin.keyKeepOriginal') : (preset.siteName ? t('admin.keyCreatedAt', { site: preset.siteName }) : 'sk-...')"
          />
        </el-form-item>
        <el-form-item :label="t('admin.enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <div class="actions">
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
    </div>
  </div>
</template>

<style scoped>
.actions {
  display: flex;
  gap: 10px;
}
</style>
