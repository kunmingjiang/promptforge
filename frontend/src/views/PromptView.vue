<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { streamSse } from '../utils/sse'
import { promptVersions, promptTest, promptFinalize, promptAgentKit } from '../api'
import type { ConversationVersion, AgentKit } from '../types'
import ModelStatusBar from '../components/ModelStatusBar.vue'

const modelBar = ref<InstanceType<typeof ModelStatusBar>>()

/* ============ 左栏：需求输入 ============ */
const input = ref('')
const generating = ref(false)
const samples = [
  '帮我写一个面向程序员的周报生成提示词，要求输出结构化 Markdown',
  '生成一个代码评审助手提示词，按严重级别分级输出修改意见',
  '我要一个把会议记录转成待办清单的提示词，能识别负责人和截止时间'
]

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

async function generate() {
  if (!input.value.trim()) {
    ElMessage.warning('请先输入你的需求描述')
    return
  }
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  if (generating.value) return
  generating.value = true
  output.value = ''
  savedInfo.value = ''
  activeTab.value = 'result'

  await streamSse('/api/prompt/generate', { input: input.value }, {
    onMessage: (delta) => {
      output.value += delta
    },
    onDone: async (data) => {
      conversationId.value = data.conversationId
      finalized.value = false
      savedInfo.value = `已保存 · v${data.version ?? '-'} · 接下来可以「试运行」验证效果`
      await loadVersions('latest')
    },
    onError: (message) => {
      ElMessage.error(message)
      if (modelBar.value && message.includes('模型配置')) modelBar.value.open()
    }
  })
  generating.value = false
}

/* ============ 试运行 ============ */
const testInput = ref('')
const testOutput = ref('')
const testing = ref(false)
const testLatency = ref(0)

async function runTest() {
  if (!output.value) {
    ElMessage.warning('请先生成或选择一个提示词版本')
    activeTab.value = 'result'
    return
  }
  if (!testInput.value.trim()) {
    ElMessage.warning('请填写测试输入（模拟真实用户会说什么）')
    return
  }
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  testing.value = true
  testOutput.value = ''
  try {
    const res = await promptTest({ prompt: output.value, testInput: testInput.value })
    testOutput.value = res.data.output
    testLatency.value = res.data.latencyMs
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(msg || '试运行失败，请检查模型配置')
  } finally {
    testing.value = false
  }
}

function goOptimizeWithTestResult() {
  activeTab.value = 'optimize'
  feedback.value = '试运行输出不符合预期，请修复暴露的问题'
}

/* ============ 优化迭代 ============ */
const feedback = ref('')
const quickDirections = ['输出更简洁凝练', '指令约束更严格', '补充few-shot示例', '角色设定更鲜明', '强化输出格式', '修复试运行暴露的问题']
const optimizeTestOutput = computed(() => (testOutput.value ? testOutput.value : ''))

async function optimize() {
  if (!conversationId.value) {
    ElMessage.warning('请先生成一个提示词')
    activeTab.value = 'result'
    return
  }
  if (!feedback.value.trim()) {
    ElMessage.warning('请描述优化方向：点快捷标签或在下方自由填写')
    return
  }
  if (modelBar.value && !(await modelBar.value.ensureReady())) return
  optimizing.value = true
  optimizePreview.value = ''
  activeTab.value = 'optimize'

  await streamSse('/api/prompt/optimize', {
    conversationId: conversationId.value,
    feedback: feedback.value,
    testInput: testInput.value || undefined,
    testOutput: optimizeTestOutput.value || undefined
  }, {
    onMessage: (delta) => {
      optimizePreview.value += delta
    },
    onDone: async (data) => {
      feedback.value = ''
      optimizePreview.value = ''
      savedInfo.value = `优化完成 · 新版本 v${data.version ?? '-'} 已保存，可在「结果」标签回看全部版本`
      await loadVersions('latest')
      activeTab.value = 'result'
      ElMessage.success(`已生成新版本 v${data.version ?? ''}`)
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
    ElMessage.success('已定稿，可以拿去创建智能体了')
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
    ElMessage.warning('请先生成提示词')
    activeTab.value = 'result'
    return
  }
  if (!agentName.value.trim()) {
    ElMessage.warning('给你的智能体起个名字')
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
    ElMessage.error(msg || '生成上架材料失败')
  } finally {
    publishing.value = false
  }
}

async function copy(text: string | null | undefined, label = '已复制') {
  if (!text) return
  await navigator.clipboard.writeText(text)
  ElMessage.success(label)
}

function copyOutput() {
  copy(output.value, '提示词已复制到剪贴板')
}
</script>

<template>
  <div class="prompt-page">
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="page-card input-card">
          <div class="card-head">
            <h3 class="panel-title">你的需求</h3>
            <ModelStatusBar ref="modelBar" />
          </div>
          <el-input
            v-model="input"
            type="textarea"
            :rows="8"
            maxlength="4000"
            show-word-limit
            placeholder="用一句大白话描述你要的提示词，例如：帮我写一个周报生成器…"
          />
          <div class="actions">
            <el-button type="primary" size="large" :loading="generating" @click="generate">生成提示词</el-button>
            <el-button size="large" :disabled="!output" @click="copyOutput">复制结果</el-button>
          </div>
          <div v-if="versions.length" class="version-bar">
            <span class="version-label mono-tag">版本</span>
            <button
              v-for="(v, i) in versions"
              :key="v.id"
              type="button"
              class="version-chip"
              :class="{ active: i === currentIndex }"
              @click="selectVersion(i)"
            >v{{ v.version }}</button>
            <span v-if="finalized" class="finalized-badge">✓ 已定稿</span>
          </div>
          <p v-if="savedInfo" class="saved-info">{{ savedInfo }}</p>
        </div>
      </el-col>

      <el-col :span="12">
        <div class="page-card output-card">
          <el-tabs v-model="activeTab" class="workbench-tabs">
            <!-- 结果 -->
            <el-tab-pane label="结果" name="result">
              <div v-if="!output && !generating" class="empty">
                <p class="empty-title">还没有结果</p>
                <p class="empty-desc">在左侧输入需求并点击「生成提示词」，这里会流式输出。不知道写什么？点一条试试：</p>
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
              <div v-else class="stream-output">{{ output }}<span v-if="generating" class="cursor">▌</span></div>
              <div v-if="versions.length > 1" class="version-note">
                正在查看 v{{ currentVersion?.version }} / 共 {{ versions.length }} 版，点左下角版本号可回看历史版本
              </div>
            </el-tab-pane>

            <!-- 试运行 -->
            <el-tab-pane label="试运行" name="test">
              <p class="tab-hint">把当前版本的提示词当成真实系统跑一遍：输入一段模拟的用户话，看模型真实反馈。</p>
              <el-input
                v-model="testInput"
                type="textarea"
                :rows="3"
                maxlength="4000"
                placeholder="模拟用户输入，例如：帮我把今天的会议记录整理成周报…"
              />
              <div class="tab-actions">
                <el-button type="primary" :loading="testing" :disabled="!output" @click="runTest">运行测试</el-button>
                <el-button v-if="testOutput" @click="goOptimizeWithTestResult">不满意，带结果去优化 →</el-button>
              </div>
              <div v-if="testOutput" class="test-result">
                <div class="result-head">
                  <span class="mono-tag">模型输出 · {{ testLatency }}ms</span>
                </div>
                <div class="test-output">{{ testOutput }}</div>
              </div>
            </el-tab-pane>

            <!-- 优化 -->
            <el-tab-pane label="优化" name="optimize">
              <p class="tab-hint">告诉 AI 哪里不满意：可点快捷标签，也可自由描述。优化会生成新版本，随时可回看。</p>
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
                placeholder="例如：输出里不要出现免责声明；日期格式统一用 YYYY-MM-DD…"
              />
              <div class="tab-actions">
                <el-button type="primary" :loading="optimizing" :disabled="!conversationId" @click="optimize">
                  生成下一版
                </el-button>
                <span v-if="testOutput" class="carry-tag">✓ 将附带试运行结果作为优化依据</span>
              </div>
              <div v-if="optimizePreview" class="test-result">
                <div class="result-head"><span class="mono-tag">正在生成新版本…</span></div>
                <div class="test-output">{{ optimizePreview }}<span class="cursor">▌</span></div>
              </div>
            </el-tab-pane>

            <!-- 上架智能体 -->
            <el-tab-pane label="创建智能体" name="publish">
              <p class="tab-hint">把满意的提示词一键打包成目标平台的创建材料：开场白 + 创建步骤 + 调用示例。</p>
              <div class="publish-form">
                <el-input v-model="agentName" maxlength="64" placeholder="智能体名称，例如：周报小助手" />
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
                <el-input v-model="greeting" maxlength="500" placeholder="开场白（留空则 AI 自动生成）" />
                <el-button type="primary" :loading="publishing" :disabled="!output" @click="buildKit">
                  生成上架材料包
                </el-button>
              </div>
              <div v-if="kit" class="kit-card">
                <div class="kit-title">{{ kit.agentName }} · {{ kit.platformLabel }}</div>
                <ol class="kit-steps">
                  <li v-for="(s, i) in kit.steps" :key="i">{{ s }}</li>
                </ol>
                <a v-if="kit.platformSite" :href="kit.platformSite" target="_blank" class="kit-link" rel="noopener">
                  前往 {{ kit.platformLabel }} 创建 ↗
                </a>
                <div class="kit-block">
                  <div class="kit-block-head">
                    <span class="mono-tag">开场白</span>
                    <button type="button" class="copy-link" @click="copy(kit.greeting, '开场白已复制')">复制</button>
                  </div>
                  <div class="kit-block-body">{{ kit.greeting }}</div>
                </div>
                <div class="kit-block">
                  <div class="kit-block-head">
                    <span class="mono-tag">系统提示词（粘贴到平台设定/Instructions）</span>
                    <button type="button" class="copy-link" @click="copy(kit.systemPrompt, '提示词已复制')">复制</button>
                  </div>
                  <div class="kit-block-body pre">{{ kit.systemPrompt }}</div>
                </div>
                <div v-if="kit.callExample" class="kit-block">
                  <div class="kit-block-head">
                    <span class="mono-tag">调用示例</span>
                    <button type="button" class="copy-link" @click="copy(kit.callExample, '示例已复制')">复制</button>
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
