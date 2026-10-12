<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const entries = [
  {
    path: '/prompt',
    title: '提示词生成',
    desc: '输入一句需求，生成结构化、可直接复用的高质量提示词',
    icon: 'MagicStick',
    tag: 'SINGLE-STEP',
    num: '01'
  },
  {
    path: '/requirement',
    title: '需求助手',
    desc: '提交原始需求，AI 追问澄清问题，生成 Markdown 需求文档',
    icon: 'Document',
    tag: 'MULTI-STEP',
    num: '02'
  }
]
</script>

<template>
  <div class="home">
    <div class="hero">
      <span class="mono-tag hero-tag">WORKSPACE</span>
      <h1 class="hero-title">
        你好，{{ auth.user?.username || '' }}。
        <br />今天要把<span class="accent">什么想法</span>交给 AI？
      </h1>
      <p class="hero-desc">选择一个入口开始，所有生成内容都会自动保存到「对话历史」，可随时回看。</p>
    </div>

    <div class="entries">
      <div v-for="entry in entries" :key="entry.path" class="entry" @click="router.push(entry.path)">
        <div class="entry-top">
          <span class="entry-num mono-tag">{{ entry.num }}</span>
          <span class="entry-tag mono-tag">{{ entry.tag }}</span>
        </div>
        <div class="entry-icon-wrap">
          <el-icon :size="24">
            <component :is="entry.icon" />
          </el-icon>
        </div>
        <h3 class="entry-title">{{ entry.title }}</h3>
        <p class="entry-desc">{{ entry.desc }}</p>
        <span class="entry-go">开始使用
          <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
            <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  max-width: 1060px;
  margin: 0 auto;
}

.hero {
  padding: 46px 0 38px;
}

.hero-tag {
  display: block;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: var(--text-1);
  margin: 0 0 14px;
}

.accent {
  color: var(--interactive);
  background: linear-gradient(120deg, rgba(56, 189, 248, 0.16), rgba(37, 99, 235, 0.1));
  border-radius: 6px;
  padding: 0 6px;
}

.hero-desc {
  font-size: 17px;
  color: var(--text-2);
  margin: 0;
}

.entries {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.entry {
  position: relative;
  cursor: pointer;
  min-height: 240px;
  padding: 26px 28px;
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--line);
  box-shadow: 0 1px 2px rgba(30, 80, 160, 0.05), 0 10px 28px rgba(30, 90, 180, 0.1);
  transition: border-color 0.2s var(--ease), transform 0.2s var(--ease), box-shadow 0.2s var(--ease);
  display: flex;
  flex-direction: column;
}

.entry:hover {
  border-color: var(--line-strong);
  transform: translateY(-2px);
  box-shadow: 0 2px 6px rgba(30, 80, 160, 0.08), 0 18px 44px rgba(30, 90, 180, 0.16);
}

.entry-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 26px;
}

.entry-num {
  color: var(--text-3);
  opacity: 0.7;
}

.entry-tag {
  color: var(--accent);
  opacity: 0.8;
}

.entry-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--interactive);
  background: var(--surface-3);
  border: 1px solid var(--line);
  margin-bottom: 16px;
  transition: color 0.2s var(--ease), border-color 0.2s var(--ease);
}

.entry:hover .entry-icon-wrap {
  color: #fff;
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  border-color: transparent;
}

.entry-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0 0 10px;
  color: var(--text-1);
}

.entry-desc {
  font-size: 15.5px;
  color: var(--text-2);
  line-height: 1.7;
  margin: 0 0 auto;
  padding-bottom: 18px;
}

.entry-go {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-2);
  transition: color 0.2s var(--ease);
}

.entry:hover .entry-go {
  color: var(--accent);
}

@media (max-width: 760px) {
  .entries {
    grid-template-columns: 1fr;
  }
}
</style>
