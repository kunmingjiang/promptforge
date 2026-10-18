<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { streamSse } from '../utils/sse'
import { promptVersions, promptTest, promptSelfCheck, promptClarify, promptFinalize, promptAgentKit } from '../api'
import type { ConversationVersion, AgentKit } from '../types'
import ModelStatusBar from '../components/ModelStatusBar.vue'

const { t } = useI18n()
const modelBar = ref<InstanceType<typeof ModelStatusBar>>()

/* ============ 左栏：需求输入 ============ */
const input = ref('')
const generating = ref(false)
const samples = computed(() => [
  t('prompt.sample1'),
  t('prompt.sample2'),
  t('prompt.sample3')
])

/* ============ 右栏工作台状态 ============ */
const activeTab = ref('result')
const conversationId = ref<number | null>(null)
const versions = ref<ConversationVersion[]>([])
const currentIndex = ref(0)
const finalized = ref(false)
const savedInfo = ref('')

const currentVersion = computed(() => versions.value[currentIndex.value] ?? null)
const output = ref('')
const optimizing = ref(false)
const optimizePreview = ref('')

function extractPrompt(version: ConversationVersion): string {
  try {
    const snapshot = JSON.parse(version.snapshot) as { messages: { role: string; content: string }[] }
    const assistants = snapshot.messages.filter((m) => m.role === 'assistant')
    return assistants.length ? assistants[assistants.length - 1].content : ''
  } catch {
    return ''
  }
}

async function loadVersions(select: 'latest' | 'keep' = 'latest') {
  if (!conversationId.value) return
  const res = await promptVersions(conversationId.value)
  versions.value = res.data
  if (!res.data.length) return
  currentIndex.value = select === 'latest' ? 0 : Math.min(currentIndex.value, res.data.length - 1)
  output.value = extractPrompt(versions.value[currentIndex.value])
}

function selectVersion(index: number) {
  currentIndex.value = index
  output.value = extractPrompt(versions.value[index])
}

async function generate(mergedInput?: string) {
  const finalInput = (mergedInput ?? input.value).trim()
  if (!finalInput) {
    ElMessage.warning(t('prompt.warnNeedInput'))
    return
  }
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  if (generating.value) return
  generating.value = true
  output.value = ''
  savedInfo.value = ''
  activeTab.value = 'result'
  showClarify.value = false

  await streamSse('/api/prompt/generate', {
    input: finalInput,
    images: imageDataUrls.value.map((i) => i.url)
  }, {
    onMessage: (delta) => {
      output.value += delta
    },
    onDone: async (data) => {
      conversationId.value = data.conversationId
      finalized.value = false
      savedInfo.value = t('prompt.savedGenerate', { v: data.version ?? '-' })
      await loadVersions('latest')
    },
    onError: (message) => {
      ElMessage.error(message)
      if (modelBar.value && message.includes('模型配置')) modelBar.value.open()
    }
  })
  generating.value = false
}

/* ============ 散信息梳理：让 AI 先提澄清问题，回答后再生成 ============ */
const clarifying = ref(false)
const showClarify = ref(false)
const questions = ref<string[]>([])
const clarifyAnswers = ref<Record<number, string>>({})

async function clarify() {
  if (!input.value.trim()) {
    ElMessage.warning(t('prompt.warnNeedInput'))
    return
  }
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  clarifying.value = true
  showClarify.value = true
  questions.value = []
  clarifyAnswers.value = {}
  try {
    const res = await promptClarify({ input: input.value, images: imageDataUrls.value.map((i) => i.url) })
    questions.value = res.data || []
    if (!questions.value.length) {
      ElMessage.info(t('prompt.clarifyEmpty'))
      showClarify.value = false
    }
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(msg || t('prompt.clarifyFailed'))
    showClarify.value = false
  } finally {
    clarifying.value = false
  }
}

/** 把澄清问答合并进需求描述，带着完整信息去生成 */
function generateWithAnswers() {
  const parts = [input.value.trim()]
  const answered = questions.value
    .map((q, i) => ({ q, a: (clarifyAnswers.value[i] || '').trim() }))
    .filter((x) => x.a)
  if (answered.length) {
    parts.push(t('prompt.clarifyMergeTitle'))
    answered.forEach((x, i) => parts.push(`${i + 1}. 问：${x.q}\n   答：${x.a}`))
  }
  void generate(parts.join('\n\n'))
}

/* ============ 附图：流程图 / 草图 / 设计稿 ============ */
interface ImageItem { uid: number; url: string }
const imageDataUrls = ref<ImageItem[]>([])

async function onImageChange(file: { uid: number; raw?: File }) {
  const raw = file.raw
  if (!raw) return
  if (!/^image\/(png|jpe?g|webp)$/i.test(raw.type)) {
    ElMessage.warning(t('prompt.imageTypeError'))
    removeImageByUid(file.uid)
    return
  }
  if (raw.size > 4 * 1024 * 1024) {
    ElMessage.warning(t('prompt.imageSizeError'))
    removeImageByUid(file.uid)
    return
  }
  const url = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(raw)
  })
  imageDataUrls.value.push({ uid: file.uid, url })
}

function onImageRemove(file: { uid: number }) {
  removeImageByUid(file.uid)
}

function removeImageByUid(uid: number) {
  imageDataUrls.value = imageDataUrls.value.filter((i) => i.uid !== uid)
}

/* ============ 试运行（多轮问答）：模型反问可继续回答，直到满意 ============ */
const testInput = ref('')
const testMessages = ref<{ role: 'user' | 'assistant'; content: string }[]>([])
const testing = ref(false)
const testLatency = ref(0)

const lastTestUser = computed(() => [...testMessages.value].reverse().find((m) => m.role === 'user')?.content || '')
const lastTestAssistant = computed(() => [...testMessages.value].reverse().find((m) => m.role === 'assistant')?.content || '')

async function runTest() {
  if (!output.value) {
    ElMessage.warning(t('prompt.warnNeedVersion'))
    activeTab.value = 'result'
    return
  }
  if (!testInput.value.trim()) {
    ElMessage.warning(t('prompt.warnNeedTestInput'))
    return
  }
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  testing.value = true
  try {
    const res = await promptTest({
      prompt: output.value,
      testInput: testInput.value.trim(),
      history: testMessages.value
    })
    testMessages.value.push({ role: 'user', content: testInput.value.trim() })
    testMessages.value.push({ role: 'assistant', content: res.data.output })
    testLatency.value = res.data.latencyMs
    testInput.value = ''
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(msg || t('prompt.testFailed'))
  } finally {
    testing.value = false
  }
}

function clearTest() {
  testMessages.value = []
  selfCheckResult.value = ''
  testInput.value = ''
}

function goOptimizeWithTestResult() {
  activeTab.value = 'optimize'
  feedback.value = t('prompt.goOptimizeFeedback')
}

/* ============ AI 自检：评审整段试运行问答，列出 AI 不确定、需人工确认的点 ============ */
const selfChecking = ref(false)
const selfCheckResult = ref('')
const selfCheckLatency = ref(0)

async function runSelfCheck() {
  if (!output.value || !testMessages.value.length) return
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  selfChecking.value = true
  selfCheckResult.value = ''
  try {
    const res = await promptSelfCheck({
      prompt: output.value,
      history: testMessages.value
    })
    selfCheckResult.value = res.data.output
    selfCheckLatency.value = res.data.latencyMs
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(msg || t('prompt.selfCheckFailed'))
  } finally {
    selfChecking.value = false
  }
}

/* ============ 优化迭代 ============ */
const feedback = ref('')
const quickDirections = computed(() => [
  t('prompt.direction1'),
  t('prompt.direction2'),
  t('prompt.direction3'),
  t('prompt.direction4'),
  t('prompt.direction5'),
  t('prompt.direction6')
])
const optimizeTestOutput = computed(() => lastTestAssistant.value)
const optimizeTestInput = computed(() => lastTestUser.value)

async function optimize() {
  if (!conversationId.value) {
    ElMessage.warning(t('prompt.warnNeedPrompt'))
    activeTab.value = 'result'
    return
  }
  if (!feedback.value.trim()) {
    ElMessage.warning(t('prompt.warnNeedFeedback'))
    return
  }
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  optimizing.value = true
  optimizePreview.value = ''
  activeTab.value = 'optimize'

  await streamSse('/api/prompt/optimize', {
    conversationId: conversationId.value,
    feedback: feedback.value,
    testInput: optimizeTestInput.value || undefined,
    testOutput: optimizeTestOutput.value || undefined
  }, {
    onMessage: (delta) => {
      optimizePreview.value += delta
    },
    onDone: async (data) => {
      feedback.value = ''
      optimizePreview.value = ''
      savedInfo.value = t('prompt.savedOptimize', { v: data.version ?? '-' })
      await loadVersions('latest')
      activeTab.value = 'result'
      ElMessage.success(t('prompt.newVersionMsg', { v: data.version ?? '' }))
    },
    onError: (message) => {
      ElMessage.error(message)
      if (modelBar.value && message.includes('模型配置')) modelBar.value.open()
    }
  })
  optimizing.value = false
}

/* ============ 定稿 ============ */
const finalizing = ref(false)

async function doFinalize() {
  if (!conversationId.value) return
  finalizing.value = true
  try {
    await promptFinalize(conversationId.value)
    finalized.value = true
    ElMessage.success(t('prompt.warnFinalizeDone'))
  } finally {
    finalizing.value = false
  }
}

/* ============ 上架智能体 ============ */
const platforms = [
  { key: 'coze', label: '扣子 Coze' },
  { key: 'dify', label: 'Dify' },
  { key: 'gpts', label: 'ChatGPT GPTs' },
  { key: 'yuanqi', label: '腾讯元器' },
  { key: 'custom', label: '自建 API' }
]
const agentName = ref('')
const platform = ref('coze')
const greeting = ref('')
const kit = ref<AgentKit | null>(null)
const publishing = ref(false)

async function buildKit() {
  if (!output.value) {
    ElMessage.warning(t('prompt.warnNeedPrompt'))
    activeTab.value = 'result'
    return
  }
  if (!agentName.value.trim()) {
    ElMessage.warning(t('prompt.warnNeedAgentName'))
    return
  }
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  publishing.value = true
  kit.value = null
  try {
    const res = await promptAgentKit({
      prompt: output.value,
      agentName: agentName.value.trim(),
      platform: platform.value,
      greeting: greeting.value.trim() || undefined
    })
    kit.value = res.data
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(msg || t('prompt.buildKitFailed'))
  } finally {
    publishing.value = false
  }
}

async function copy(text: string | null | undefined, label?: string) {
  if (!text) return
  await navigator.clipboard.writeText(text)
  ElMessage.success(label || t('prompt.copied'))
}

function copyOutput() {
  copy(output.value, t('prompt.copiedPrompt'))
}
</script>

<template>
  <div class="prompt-page">
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="page-card input-card">
          <div class="card-head">
            <h3 class="panel-title">{{ t('prompt.inputTitle') }}</h3>
            <ModelStatusBar ref="modelBar" />
          </div>
          <el-input
            v-model="input"
            type="textarea"
            :rows="8"
            maxlength="4000"
            show-word-limit
            :placeholder="t('prompt.inputPlaceholder')"
          />
          <div class="attach-row">
            <el-upload
              class="image-uploader"
              :auto-upload="false"
              list-type="picture-card"
              accept="image/png,image/jpeg,image/webp"
              :limit="3"
              :on-change="onImageChange"
              :on-remove="onImageRemove"
              :on-exceed="() => ElMessage.warning(t('prompt.imageLimitError'))"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="attach-tip">{{ t('prompt.attachTip') }}</div>
          </div>
          <div class="actions">
            <el-button type="primary" size="large" :loading="generating" @click="generate()">{{ t('prompt.generate') }}</el-button>
            <el-button size="large" :loading="clarifying" :disabled="generating" @click="clarify">{{ t('prompt.clarifyBtn') }}</el-button>
            <el-button size="large" :disabled="!output" @click="copyOutput">{{ t('prompt.copyResult') }}</el-button>
          </div>
          <div v-if="showClarify" class="clarify-panel">
            <p class="clarify-title">{{ t('prompt.clarifyTitle') }}</p>
            <div v-for="(q, i) in questions" :key="i" class="clarify-item">
              <p class="clarify-q">{{ i + 1 }}. {{ q }}</p>
              <el-input
                v-model="clarifyAnswers[i]"
                type="textarea"
                :rows="2"
                maxlength="1000"
                :placeholder="t('prompt.clarifyAnswerPlaceholder')"
              />
            </div>
            <div class="clarify-actions">
              <el-button type="primary" :loading="generating" @click="generateWithAnswers">{{ t('prompt.generateWithAnswers') }}</el-button>
              <el-button :disabled="generating" @click="showClarify = false">{{ t('prompt.clarifyCollapse') }}</el-button>
            </div>
          </div>
          <div v-if="versions.length" class="version-bar">
            <span class="version-label mono-tag">{{ t('prompt.version') }}</span>
            <button
              v-for="(v, i) in versions"
              :key="v.id"
              type="button"
              class="version-chip"
              :class="{ active: i === currentIndex }"
              @click="selectVersion(i)"
            >v{{ v.version }}</button>
            <span v-if="finalized" class="finalized-badge">{{ t('prompt.finalized') }}</span>
          </div>
          <p v-if="savedInfo" class="saved-info">{{ savedInfo }}</p>
        </div>
      </el-col>

      <el-col :span="12">
        <div class="page-card output-card">
          <el-tabs v-model="activeTab" class="workbench-tabs">
            <!-- 结果 -->
            <el-tab-pane :label="t('prompt.tabResult')" name="result">
              <div v-if="!output && !generating" class="empty">
                <p class="empty-title">{{ t('prompt.emptyTitle') }}</p>
                <p class="empty-desc">{{ t('prompt.emptyDesc') }}</p>
                <div class="samples">
                  <button
                    v-for="s in samples"
                    :key="s"
                    class="sample-chip"
                    type="button"
                    @click="input = s"
                  >{{ s }}</button>
                </div>
              </div>
              <div v-else class="stream-wrap">
                <div class="result-head">
                  <span class="mono-tag">{{ currentVersion ? 'v' + currentVersion.version : '' }}</span>
                  <el-button size="small" :disabled="!output || generating" @click="copy(output, t('prompt.copiedPrompt'))">{{ t('prompt.copy') }}</el-button>
                </div>
                <div class="stream-output">{{ output }}<span v-if="generating" class="cursor">▌</span></div>
              </div>
              <div v-if="versions.length > 1" class="version-note">
                {{ t('prompt.versionNote', { current: currentVersion?.version, total: versions.length }) }}
              </div>
            </el-tab-pane>

            <!-- 试运行（多轮问答） -->
            <el-tab-pane :label="t('prompt.tabTest')" name="test">
              <p class="tab-hint">{{ t('prompt.testHint') }}</p>
              <div v-if="testMessages.length" class="test-transcript">
                <template v-for="(m, i) in testMessages" :key="i">
                  <div v-if="m.role === 'user'" class="test-user-msg">{{ m.content }}</div>
                  <div v-else class="test-result">
                    <div class="result-head">
                      <span class="mono-tag">{{ t('prompt.modelReply') }}</span>
                      <el-button size="small" @click="copy(m.content, t('prompt.copiedOutput'))">{{ t('prompt.copy') }}</el-button>
                    </div>
                    <div class="test-output">{{ m.content }}</div>
                  </div>
                </template>
              </div>
              <el-input
                v-model="testInput"
                type="textarea"
                :rows="3"
                maxlength="4000"
                :placeholder="testMessages.length ? t('prompt.testReplyPlaceholder') : t('prompt.testInputPlaceholder')"
              />
              <div class="tab-actions">
                <el-button type="primary" :loading="testing" :disabled="!output || !testInput.trim()" @click="runTest">{{ t('prompt.testSend') }}</el-button>
                <el-button v-if="testMessages.length" :loading="selfChecking" @click="runSelfCheck">{{ t('prompt.selfCheck') }}</el-button>
                <el-button v-if="testMessages.length" @click="goOptimizeWithTestResult">{{ t('prompt.goOptimize') }}</el-button>
                <el-button v-if="testMessages.length" @click="clearTest">{{ t('prompt.testClear') }}</el-button>
              </div>
              <div v-if="testMessages.length" class="latency-note mono-tag">{{ t('prompt.modelOutput', { ms: testLatency }) }}</div>
              <div v-if="selfCheckResult || selfChecking" class="test-result self-check-card">
                <div class="result-head">
                  <span class="mono-tag">{{ t('prompt.selfCheckResult', { ms: selfCheckLatency }) }}</span>
                  <el-button v-if="selfCheckResult" size="small" @click="copy(selfCheckResult, t('prompt.copied'))">{{ t('prompt.copy') }}</el-button>
                </div>
                <div class="test-output">{{ selfCheckResult }}<span v-if="selfChecking" class="cursor">▌</span></div>
              </div>
            </el-tab-pane>

            <!-- 优化 -->
            <el-tab-pane :label="t('prompt.tabOptimize')" name="optimize">
              <p class="tab-hint">{{ t('prompt.optimizeHint') }}</p>
              <div class="direction-chips">
                <button
                  v-for="d in quickDirections"
                  :key="d"
                  type="button"
                  class="direction-chip"
                  @click="feedback = feedback ? feedback + '；' + d : d"
                >{{ d }}</button>
              </div>
              <el-input
                v-model="feedback"
                type="textarea"
                :rows="3"
                maxlength="2000"
                :placeholder="t('prompt.optimizeInputPlaceholder')"
              />
              <div class="tab-actions">
                <el-button type="primary" :loading="optimizing" :disabled="!conversationId" @click="optimize">
                  {{ t('prompt.generateNext') }}
                </el-button>
                <span v-if="testMessages.length" class="carry-tag">{{ t('prompt.carryTag') }}</span>
              </div>
              <div v-if="optimizePreview" class="test-result">
                <div class="result-head">
                  <span class="mono-tag">{{ t('prompt.generatingNew') }}</span>
                  <el-button size="small" @click="copy(optimizePreview, t('prompt.copiedPrompt'))">{{ t('prompt.copy') }}</el-button>
                </div>
                <div class="test-output">{{ optimizePreview }}<span class="cursor">▌</span></div>
              </div>
            </el-tab-pane>

            <!-- 上架智能体 -->
            <el-tab-pane :label="t('prompt.tabPublish')" name="publish">
              <p class="tab-hint">{{ t('prompt.publishHint') }}</p>
              <div class="publish-form">
                <el-input v-model="agentName" maxlength="64" :placeholder="t('prompt.agentNamePlaceholder')" />
                <div class="platform-row">
                  <button
                    v-for="p in platforms"
                    :key="p.key"
                    type="button"
                    class="platform-chip"
                    :class="{ active: platform === p.key }"
                    @click="platform = p.key"
                  >{{ p.label }}</button>
                </div>
                <el-input v-model="greeting" maxlength="500" :placeholder="t('prompt.greetingPlaceholder')" />
                <el-button type="primary" :loading="publishing" :disabled="!output" @click="buildKit">
                  {{ t('prompt.buildKit') }}
                </el-button>
              </div>
              <div v-if="kit" class="kit-card">
                <div class="kit-title">{{ kit.agentName }} · {{ kit.platformLabel }}</div>
                <ol class="kit-steps">
                  <li v-for="(s, i) in kit.steps" :key="i">{{ s }}</li>
                </ol>
                <a v-if="kit.platformSite" :href="kit.platformSite" target="_blank" class="kit-link" rel="noopener">
                  {{ t('prompt.goToPlatform', { name: kit.platformLabel }) }}
                </a>
                <div class="kit-block">
                  <div class="kit-block-head">
                    <span class="mono-tag">{{ t('prompt.kitGreeting') }}</span>
                    <button type="button" class="copy-link" @click="copy(kit.greeting, t('prompt.copiedGreeting'))">{{ t('prompt.copy') }}</button>
                  </div>
                  <div class="kit-block-body">{{ kit.greeting }}</div>
                </div>
                <div class="kit-block">
                  <div class="kit-block-head">
                    <span class="mono-tag">{{ t('prompt.kitSystemPrompt') }}</span>
                    <button type="button" class="copy-link" @click="copy(kit.systemPrompt, t('prompt.copiedPrompt'))">{{ t('prompt.copy') }}</button>
                  </div>
                  <div class="kit-block-body pre">{{ kit.systemPrompt }}</div>
                </div>
                <div v-if="kit.callExample" class="kit-block">
                  <div class="kit-block-head">
                    <span class="mono-tag">{{ t('prompt.kitCallExample') }}</span>
                    <button type="button" class="copy-link" @click="copy(kit.callExample, t('prompt.copiedExample'))">{{ t('prompt.copy') }}</button>
                  </div>
                  <div class="kit-block-body pre">{{ kit.callExample }}</div>
                </div>
                <div class="kit-tip">💡 {{ kit.tip }}</div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.input-card,
.output-card {
  min-height: 560px;
  display: flex;
  flex-direction: column;
}

.input-card :deep(.el-textarea) {
  flex: 1;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-head .panel-title {
  margin-bottom: 0;
}

.actions {
  margin-top: 14px;
  display: flex;
  gap: 12px;
}

/* 附图上传：小尺寸缩略图卡片 */
.attach-row {
  margin-top: 12px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.attach-tip {
  font-size: 13px;
  color: var(--pf-text-soft, #64748b);
  line-height: 1.6;
  padding-top: 4px;
}

.image-uploader :deep(.el-upload--picture-card) {
  width: 72px;
  height: 72px;
  border-radius: 10px;
}

.image-uploader :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 72px;
  height: 72px;
  border-radius: 10px;
}

/* 澄清梳理面板 */
.clarify-panel {
  margin-top: 14px;
  padding: 14px 16px;
  border: 1px solid var(--pf-border, #dbeafe);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.65);
}

.clarify-title {
  font-weight: 600;
  margin-bottom: 10px;
}

.clarify-item {
  margin-bottom: 12px;
}

.clarify-q {
  margin-bottom: 6px;
  line-height: 1.6;
}

.clarify-actions {
  display: flex;
  gap: 10px;
}

.version-bar {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.version-chip {
  border: 1px solid var(--line);
  background: var(--surface-2);
  color: var(--text-2);
  border-radius: 999px;
  padding: 4px 14px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.18s var(--ease);
}

.version-chip.active {
  background: var(--interactive);
  border-color: var(--interactive);
  color: #fff;
  font-weight: 600;
}

.finalized-badge {
  font-size: 14px;
  font-weight: 600;
  color: #059669;
  background: rgba(5, 150, 105, 0.08);
  border: 1px solid rgba(5, 150, 105, 0.25);
  border-radius: 999px;
  padding: 3px 12px;
}

.saved-info {
  margin: 12px 0 0;
  font-size: 14px;
  color: var(--interactive);
}

.workbench-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.workbench-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
}

.workbench-tabs :deep(.el-tabs__item) {
  font-size: 15.5px;
}

.tab-hint {
  font-size: 14.5px;
  color: var(--text-3);
  line-height: 1.7;
  margin: 0 0 14px;
}

.tab-actions {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.carry-tag {
  font-size: 13.5px;
  color: #059669;
}

.empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 24px 8px;
}

.empty-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0 0 6px;
}

.empty-desc {
  font-size: 15px;
  color: var(--text-3);
  line-height: 1.7;
  margin: 0 0 16px;
}

.samples {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: stretch;
}

.sample-chip {
  text-align: left;
  font-size: 14.5px;
  line-height: 1.6;
  color: var(--text-2);
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  cursor: pointer;
  transition: border-color 0.18s var(--ease), background-color 0.18s var(--ease), color 0.18s var(--ease);
}

.sample-chip:hover {
  border-color: var(--line-strong);
  background: var(--surface-3);
  color: var(--text-1);
}

.stream-output,
.test-output {
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-1);
}

.cursor {
  animation: blink 0.8s infinite;
  color: var(--accent);
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.version-note {
  margin-top: 14px;
  font-size: 13.5px;
  color: var(--text-3);
}

.test-result {
  margin-top: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  padding: 12px 16px;
}

.result-head {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.stream-wrap .stream-output {
  margin-top: 0;
}

/* 多轮问答记录 */
.test-transcript {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.test-user-msg {
  align-self: flex-end;
  max-width: 85%;
  padding: 8px 14px;
  border-radius: 14px 14px 4px 14px;
  background: var(--pf-accent-soft, #e0f2fe);
  color: var(--pf-text, #0e2a4d);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.latency-note {
  margin-top: 10px;
  opacity: 0.75;
}

.direction-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.direction-chip {
  border: 1px solid var(--line);
  background: var(--surface-2);
  color: var(--text-2);
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.18s var(--ease);
}

.direction-chip:hover {
  border-color: var(--interactive);
  color: var(--interactive);
  background: rgba(37, 99, 235, 0.06);
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.platform-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.platform-chip {
  border: 1px solid var(--line);
  background: var(--surface-2);
  color: var(--text-2);
  border-radius: var(--radius-sm);
  padding: 8px 16px;
  font-size: 14.5px;
  cursor: pointer;
  transition: all 0.18s var(--ease);
}

.platform-chip.active {
  background: var(--interactive);
  border-color: var(--interactive);
  color: #fff;
  font-weight: 600;
}

.kit-card {
  margin-top: 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  padding: 16px 20px;
}

.kit-title {
  font-size: 16.5px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 10px;
}

.kit-steps {
  margin: 0 0 12px;
  padding-left: 22px;
  font-size: 14.5px;
  line-height: 1.9;
  color: var(--text-2);
}

.kit-link {
  display: inline-block;
  font-size: 14.5px;
  font-weight: 600;
  color: var(--interactive);
  text-decoration: none;
  margin-bottom: 14px;
}

.kit-link:hover {
  text-decoration: underline;
}

.kit-block {
  margin-bottom: 12px;
}

.kit-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.copy-link {
  border: none;
  background: none;
  color: var(--interactive);
  font-size: 13.5px;
  cursor: pointer;
}

.copy-link:hover {
  text-decoration: underline;
}

.kit-block-body {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-2);
  background: var(--surface-3);
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 10px 12px;
  max-height: 180px;
  overflow-y: auto;
}

.kit-block-body.pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  font-size: 13px;
}

.kit-tip {
  font-size: 14px;
  color: var(--text-2);
  background: rgba(14, 165, 233, 0.07);
  border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: 6px;
  padding: 10px 14px;
}
</style>
