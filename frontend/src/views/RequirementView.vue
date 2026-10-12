<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { streamSse } from '../utils/sse'
import { createProject, getDocument, getQuestions, submitAnswers } from '../api'
import type { Question } from '../types'
import ModelStatusBar from '../components/ModelStatusBar.vue'

const modelBar = ref<InstanceType<typeof ModelStatusBar>>()

const step = ref(0)
const title = ref('')
const raw = ref('')
const projectId = ref<number | null>(null)
const questions = ref<Question[]>([])
const docContent = ref('')
const docVersion = ref<number | null>(null)
const versions = ref<{ versionId: number; version: number; createdAt: string }[]>([])
const loading = ref(false)
const generating = ref(false)

const renderedDoc = computed(() => (docContent.value ? marked.parse(docContent.value) as string : ''))

async function onCreate() {
  if (!title.value.trim() || !raw.value.trim()) {
    ElMessage.warning('请填写项目标题和原始需求')
    return
  }
  // 模型未配置时当场弹出配置引导
  if (modelBar.value && !(await modelBar.value.ensureReady())) {
    ElMessage.warning('请先完成模型配置')
    return
  }
  loading.value = true
  try {
    const created: any = await createProject({ title: title.value, rawRequirement: raw.value })
    projectId.value = created.data.id
    const qs: any = await getQuestions(created.data.id)
    questions.value = (qs.data as Question[]).map((q) => ({ ...q }))
    step.value = 1
  } finally {
    loading.value = false
  }
}

async function onSubmitAnswers() {
  if (!projectId.value) return
  loading.value = true
  try {
    const payload = questions.value.map((q) => ({
      answerId: q.id,
      answer: q.answer,
      skipped: q.skipped || !q.answer
    }))
    const result: any = await submitAnswers(projectId.value, payload)
    questions.value = result.data as Question[]
    step.value = 2
  } finally {
    loading.value = false
  }
}

async function onGenerateDoc() {
  if (!projectId.value || generating.value) return
  if (modelBar.value && !(await modelBar.value.ensureReady())) {
    ElMessage.warning('请先完成模型配置')
    return
  }
  generating.value = true
  docContent.value = ''
  await streamSse(`/api/requirement/projects/${projectId.value}/document`, {}, {
    onMessage: (delta) => {
      docContent.value += delta
    },
    onDone: async (data) => {
      docVersion.value = data.version
      const detail: any = await getDocument(projectId.value!)
      docVersion.value = detail.data.version
      versions.value = detail.data.versions
      ElMessage.success('需求文档已生成并保存版本')
    },
    onError: (message) => {
      ElMessage.error(message)
    }
  })
  generating.value = false
}

async function onSwitchVersion(version: number) {
  if (!projectId.value) return
  const detail: any = await getDocument(projectId.value, version)
  docContent.value = detail.data.content
  docVersion.value = detail.data.version
}

function reset() {
  step.value = 0
  title.value = ''
  raw.value = ''
  projectId.value = null
  questions.value = []
  docContent.value = ''
  versions.value = []
}

const answerState = reactive({})
</script>

<template>
  <div class="req-page">
    <div class="page-card steps-card">
      <el-steps :active="step" align-center finish-status="success">
        <el-step title="提交需求" />
        <el-step title="澄清问答" />
        <el-step title="生成文档" />
      </el-steps>
    </div>

    <!-- 第 1 步：提交需求 -->
    <div v-if="step === 0" class="page-card">
      <div class="card-head">
        <h3 class="panel-title">需求信息</h3>
        <ModelStatusBar ref="modelBar" />
      </div>
      <el-form label-position="top">
        <el-form-item label="项目标题" required>
          <el-input v-model="title" maxlength="64" show-word-limit placeholder="例如：停车缴费小程序" />
        </el-form-item>
        <el-form-item label="原始需求" required>
          <el-input
            v-model="raw"
            type="textarea"
            :rows="8"
            maxlength="8000"
            show-word-limit
            placeholder="用大白话描述你想做的东西，AI 会帮你追问关键细节"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="onCreate">生成澄清问题</el-button>
      </el-form>
    </div>

    <!-- 第 2 步：澄清问答 -->
    <div v-else-if="step === 1" class="page-card">
      <h3 class="panel-title">AI 需要澄清 {{ questions.length }} 个问题（最多 5 个，可跳过）</h3>
      <div v-for="(q, index) in questions" :key="q.id" class="question-item">
        <div class="question-label">{{ index + 1 }}. {{ q.question }}</div>
        <el-input
          v-model="q.answer"
          type="textarea"
          :rows="2"
          :disabled="q.skipped"
          placeholder="填写你的回答"
        />
        <el-checkbox v-model="q.skipped" class="skip-check">跳过此问题</el-checkbox>
      </div>
      <div class="actions">
        <el-button type="primary" :loading="loading" @click="onSubmitAnswers">提交回答</el-button>
        <el-button @click="reset">重新开始</el-button>
      </div>
    </div>

    <!-- 第 3 步：文档预览 -->
    <div v-else class="doc-layout">
      <div class="page-card doc-side">
        <h3 class="panel-title">需求文档</h3>
        <el-select
          v-if="versions.length"
          :model-value="docVersion"
          class="version-select"
          @change="onSwitchVersion"
        >
          <el-option
            v-for="v in versions"
            :key="v.versionId"
            :label="`v${v.version} · ${v.createdAt}`"
            :value="v.version"
          />
        </el-select>
        <el-button
          type="primary"
          class="gen-btn"
          :loading="generating"
          :disabled="generating"
          @click="onGenerateDoc"
        >
          {{ docContent ? '重新生成文档' : '生成需求文档' }}
        </el-button>
        <el-button :disabled="!docContent" @click="reset">新建项目</el-button>
      </div>
      <div class="page-card doc-main">
        <div v-if="!docContent && !generating" class="empty">
          <el-empty description="点击「生成需求文档」，右侧将流式渲染 Markdown 文档" />
        </div>
        <div v-else class="markdown-body doc-body" v-html="renderedDoc"></div>
        <span v-if="generating" class="cursor">▌</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-head .panel-title {
  margin-bottom: 0;
}

.steps-card {
  margin-bottom: 20px;
}

.question-item {
  margin-bottom: 16px;
}

.question-label {
  font-size: 15.5px;
  font-weight: 500;
  color: var(--text-1);
  margin-bottom: 6px;
}

.skip-check {
  margin-top: 4px;
}

.actions {
  margin-top: 8px;
}

.doc-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.doc-side {
  width: 260px;
  flex-shrink: 0;
}

.doc-main {
  flex: 1;
  min-height: 400px;
}

.version-select {
  width: 100%;
  margin-bottom: 12px;
}

.gen-btn {
  width: 100%;
  margin-bottom: 8px;
}

.doc-body {
  font-size: 15.5px;
  line-height: 1.8;
}

.empty {
  padding-top: 100px;
}

.cursor {
  color: var(--accent);
  animation: blink 0.8s infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
