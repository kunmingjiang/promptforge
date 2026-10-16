<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getStats } from '../../api'
import type { Stats } from '../types'

const { t } = useI18n()

const stats = ref<Stats | null>(null)

onMounted(async () => {
  const result: any = await getStats()
  stats.value = result.data as Stats
})

const cards = [
  { key: 'userCount', labelKey: 'admin.userCount', icon: 'User' },
  { key: 'conversationCount', labelKey: 'admin.conversationCount', icon: 'ChatDotRound' },
  { key: 'messageCount', labelKey: 'admin.messageCount', icon: 'ChatLineRound' },
  { key: 'requirementProjectCount', labelKey: 'admin.requirementProjectCount', icon: 'Document' }
] as const
</script>

<template>
  <div class="stats-page">
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.key">
        <div class="stat-card page-card">
          <div class="stat-icon">
            <el-icon :size="20"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-value">{{ stats ? stats[card.key] : '-' }}</div>
          <div class="stat-label">{{ t(card.labelKey) }}</div>
        </div>
      </el-col>
    </el-row>
    <el-alert
      class="note"
      type="info"
      :closable="false"
      show-icon
      :title="t('admin.statsNote')"
    />
  </div>
</template>

<style scoped>
.stat-card {
  text-align: left;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--interactive);
  background: var(--surface-3);
  border: 1px solid var(--line);
  margin-bottom: 14px;
}

.stat-value {
  font-size: 34px;
  font-weight: 600;
  letter-spacing: -0.02em;
  font-family: var(--font-mono);
  color: var(--text-1);
  margin: 0 0 4px;
  line-height: 1.2;
}

.stat-label {
  font-size: 15px;
  color: var(--text-2);
}

.note {
  margin-top: 20px;
}
</style>
