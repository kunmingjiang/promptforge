/**
 * 模型供应商预设（全站共享）：
 * 国内外主流大模型均提供 OpenAI 兼容端点，后端 OpenAiCompatProvider 按
 * provider 别名统一适配（deepseek / glm / kimi / minimax / openai / claude / gemini / custom）。
 *
 * 教程文案（steps / tips）走 i18n：locale 文件里 providers.<key>.steps / .tips，
 * 本文件只保留与技术参数（label / baseUrl / model / 官网）。
 */
export interface ProviderPreset {
  /** 品牌名（各语言一致，不翻译） */
  label: string
  baseUrl: string
  model: string
  site: string
  siteName: string
  /** 是否有「获取 Key」教程（custom 无） */
  hasGuide: boolean
}

export const PROVIDERS: Record<string, ProviderPreset> = {
  deepseek: {
    label: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com',
    model: 'deepseek-chat',
    site: 'https://platform.deepseek.com',
    siteName: 'platform.deepseek.com',
    hasGuide: true
  },
  glm: {
    label: '智谱 GLM',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    model: 'glm-4.5',
    site: 'https://open.bigmodel.cn',
    siteName: 'open.bigmodel.cn',
    hasGuide: true
  },
  kimi: {
    label: 'Kimi',
    baseUrl: 'https://api.moonshot.cn/v1',
    model: 'kimi-k2-0905-preview',
    site: 'https://platform.moonshot.cn',
    siteName: 'platform.moonshot.cn',
    hasGuide: true
  },
  minimax: {
    label: 'MiniMax',
    baseUrl: 'https://api.minimax.chat/v1',
    model: 'MiniMax-Text-01',
    site: 'https://platform.minimaxi.com',
    siteName: 'platform.minimaxi.com',
    hasGuide: true
  },
  openai: {
    label: 'OpenAI',
    baseUrl: 'https://api.openai.com/v1',
    model: 'gpt-4o',
    site: 'https://platform.openai.com',
    siteName: 'platform.openai.com',
    hasGuide: true
  },
  claude: {
    label: 'Claude',
    baseUrl: 'https://api.anthropic.com/v1',
    model: 'claude-sonnet-4-5',
    site: 'https://console.anthropic.com',
    siteName: 'console.anthropic.com',
    hasGuide: true
  },
  gemini: {
    label: 'Gemini',
    baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai',
    model: 'gemini-2.5-flash',
    site: 'https://aistudio.google.com',
    siteName: 'aistudio.google.com',
    hasGuide: true
  },
  custom: {
    label: '自定义',
    baseUrl: '',
    model: '',
    site: '',
    siteName: '',
    hasGuide: false
  }
}

/** 供应商展示顺序（配置弹框/管理后台共用） */
export const providerOrder = [
  'deepseek',
  'glm',
  'kimi',
  'minimax',
  'openai',
  'claude',
  'gemini',
  'custom'
]

export const providerOptions = providerOrder.map((key) => ({
  key,
  label: PROVIDERS[key].label
}))
