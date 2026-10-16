<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { conversationDetail, conversationList, conversationVersions } from '../api'
import type { Conversation, ConversationVersion, Message } from '../types'

const { t } = useI18n()

const module = ref('')
const list = ref<Conversation[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const current = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const versions = ref<ConversationVersion[]>([])
const selectedVersion = ref<number | null>(null)
const versionContent = ref('')

const renderedVersion = ref('')

async function load(reset = true) {
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      list.value = []
    }
    const result: any = await conversationList(page.value, 20, module.value || undefined)
    list.value = list.value.concat(result.data.records as Conversation[])
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

async function onModuleChange() {
  current.value = null
  messages.value = []
  versions.value = []
  versionContent.value = ''
  await load(true)
}

async function open(item: Conversation) {
  current.value = item
  const detail: any = await conversationDetail(item.id)
  messages.value = detail.data.messages as Message[]
  const vs: any = await conversationVersions(item.id)
  versions.value = vs.data as ConversationVersion[]
  selectedVersion.value = versions.value[0]?.version ?? null
  versionContent.value = ''
  renderedVersion.value = ''
}

function onVersionChange(version: number) {
  const target = versions.value.find((v) => v.version === version)
  versionContent.value = ''
  renderedVersion.value = ''
  if (!target) return
  try {
    const snapshot = JSON.parse(target.snapshot)
    const msgs: { role: string; content: string }[] = snapshot.messages || []
    versionContent.value = msgs
      .map((m) => (m.role === 'user' ? t('history.inputTag') + m.content : m.content))
      .join('\n\n')
    const last = msgs[msgs.length - 1]
    if (last && last.role === 'assistant' && last.content.trim().startsWith('#')) {
      renderedVersion.value = marked.parse(last.content) as string
    }
  } catch {
    versionContent.value = target.snapshot
  }
}

async function loadMore() {
  page.value++
  await load(false)
}

onMounted(() => load(true))

function moduleLabel(m: string) {
  return m === 'requirement' ? t('history.moduleRequirement') : t('history.modulePrompt')
}
</script>

<template>
  <div class="history-page">
    <div class="page-card list-side">
      <el-radio-group v-model="module" class="filter" @change="onModuleChange">
        <el-radio-button value="">{{ t('history.filterAll') }}</el-radio-button>
        <el-radio-button value="prompt">{{ t('history.filterPrompt') }}</el-radio-button>
        <el-radio-button value="requirement">{{ t('history.filterRequirement') }}</el-radio-button>
      </el-radio-group>

      <div v-if="!list.length && !loading" class="empty">
        <el-empty :description="t('history.empty')" />
      </div>

      <div
        v-for="item in list"
        :key="item.id"
        class="conv-item"
        :class="{ active: current?.id === item.id }"
        @click="open(item)"
      >
        <div class="conv-title">{{ item.title }}</div>
        <div class="conv-meta">
          <el-tag size="small" :type="item.module === 'requirement' ? 'success' : 'primary'">
            {{ moduleLabel(item.module) }}
          </el-tag>
          <span class="conv-time">{{ item.createdAt }}</span>
        </div>
      </div>

      <el-button
        v-if="list.length < total"
        class="load-more"
        text
        :loading="loading"
        @click="loadMore"
      >
        {{ t('history.loadMore', { loaded: list.length, total }) }}
      </el-button>
    </div>

    <div class="page-card detail-side">
      <template v-if="current">
        <div class="detail-header">
          <h3 class="detail-title">{{ current.title }}</h3>
          <el-select
            v-if="versions.length"
            v-model="selectedVersion"
            :placeholder="t('history.selectVersion')"
            class="version-select"
            @change="onVersionChange"
          >
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="`v${v.version} · ${v.createdAt}`"
              :value="v.version"
            />
          </el-select>
        </div>

        <div v-if="versionContent || renderedVersion" class="version-view">
          <div v-if="renderedVersion" class="markdown-body" v-html="renderedVersion"></div>
          <div v-else class="stream-output">{{ versionContent }}</div>
        </div>

        <template v-else>
          <div v-for="msg in messages" :key="msg.id" class="msg" :class="msg.role">
            <div class="msg-role">{{ msg.role === 'user' ? t('history.roleUser') : t('history.roleAI') }}</div>
            <div class="msg-content">{{ msg.content }}</div>
          </div>
          <el-alert
            v-if="!messages.length"
            :title="t('history.noMessages')"
            type="info"
            :closable="false"
          />
        </template>
      </template>
      <el-empty v-else :description="t('history.pickConversation')" />
    </div>
  </div>
</template>

<style scoped>
.history-page {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  height: calc(100vh - 180px);
}

.list-side {
  width: 320px;
  flex-shrink: 0;
  overflow-y: auto;
}

.filter {
  margin-bottom: 12px;
}

.conv-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 6px;
}

.conv-item:hover {
  background: rgba(37, 99, 235, 0.07);
}

.conv-item.active {
  background: rgba(37, 99, 235, 0.12);
  box-shadow: inset 2px 0 0 var(--interactive);
}

.conv-title {
  font-size: 15px;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.conv-time {
  font-size: 13px;
  color: var(--text-3);
}

.load-more {
  width: 100%;
}

.detail-side {
  flex: 1;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.detail-title {
  font-size: 15px;
  font-weight: 500;
  margin: 0;
  color: var(--text-1);
}

.version-select {
  width: 220px;
}

.msg {
  margin-bottom: 14px;
}

.msg-role {
  font-size: 13px;
  color: var(--text-3);
  margin-bottom: 4px;
}

.msg-content {
  font-size: 15px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f6fbff;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  color: var(--text-1);
}

.msg.user .msg-content {
  background: rgba(37, 99, 235, 0.08);
  border-color: rgba(37, 99, 235, 0.24);
}

.version-view {
  margin-top: 4px;
}
</style>
