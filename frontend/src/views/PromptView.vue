<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { streamSse } from '../utils/sse'
import ModelStatusBar from '../components/ModelStatusBar.vue'

const modelBar = ref<InstanceType<typeof ModelStatusBar>>()
const input = ref('')
const output = ref('')
const generating = ref(false)
const savedInfo = ref('')

const samples = [
  '帮我写一个面向程序员的周报生成提示词，要求输出结构化 Markdown',
  '生成一个代码评审助手提示词，按严重级别分级输出修改意见',
  '我要一个把会议记录转成待办清单的提示词，能识别负责人和截止时间'
]

async function generate() {
  if (!input.value.trim()) {
    ElMessage.warning('请先输入你的需求描述')
    return
  }
  // 模型未配置时当场弹出配置引导，不打无效请求
  if (modelBar.value && !(await modelBar.value.ensureReady())) {
    ElMessage.warning('请先完成模型配置')
    return
  }
  if (generating.value) return
  generating.value = true
  output.value = ''
  savedInfo.value = ''

  await streamSse('/api/prompt/generate', { input: input.value }, {
    onMessage: (delta) => {
      output.value += delta
    },
    onDone: (data) => {
      savedInfo.value = `已自动保存到「对话历史」 · 会话 ${data.conversationId} · 版本 v${data.version ?? '-'}`
    },
    onError: (message) => {
      ElMessage.error(message)
      if (modelBar.value && message.includes('模型配置')) modelBar.value.open()
    }
  })
  generating.value = false
}

async function copyOutput() {
  await navigator.clipboard.writeText(output.value)
  ElMessage.success('已复制到剪贴板')
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
          <p v-if="savedInfo" class="saved-info">{{ savedInfo }}</p>
        </div>
      </el-col>

      <el-col :span="12">
        <div class="page-card output-card">
          <h3 class="panel-title">生成结果</h3>
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
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.input-card,
.output-card {
  min-height: 480px;
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

.saved-info {
  margin: 12px 0 0;
  font-size: 14px;
  color: var(--interactive);
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

.cursor {
  animation: blink 0.8s infinite;
  color: var(--accent);
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
