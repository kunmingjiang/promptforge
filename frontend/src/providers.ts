/**
 * 模型供应商预设（全站共享）：
 * 所有主流国产大模型均提供 OpenAI 兼容端点，后端 OpenAiCompatProvider 按
 * provider 别名统一适配（deepseek / glm / kimi / minimax / custom）。
 */
export interface ProviderPreset {
  label: string
  baseUrl: string
  model: string
  site: string
  siteName: string
  /** 获取 Key 的 3 步引导；空数组表示自定义网关不展示教程 */
  steps: [string, string][]
  tips: string[]
}

export const PROVIDERS: Record<string, ProviderPreset> = {
  deepseek: {
    label: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com',
    model: 'deepseek-chat',
    site: 'https://platform.deepseek.com',
    siteName: 'platform.deepseek.com',
    steps: [
      ['注册账号', '打开 platform.deepseek.com，用手机号或邮箱注册并登录'],
      ['充值', '进入「充值」页面，按 tokens 用量计费，最低可充 10 元，个人使用足够很久'],
      ['创建 API Key', '进入「API Keys」→「创建 API key」，复制生成的 sk-... 字符串（只显示一次，务必保存好）']
    ],
    tips: ['模型名填 deepseek-chat（V3 通用模型）或 deepseek-reasoner（R1 推理模型）']
  },
  glm: {
    label: '智谱 GLM',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    model: 'glm-4.5',
    site: 'https://open.bigmodel.cn',
    siteName: 'open.bigmodel.cn',
    steps: [
      ['注册账号', '打开 open.bigmodel.cn（智谱开放平台），注册并登录'],
      ['实名认证', '按指引完成实名认证，新用户会赠送免费 tokens 额度'],
      ['创建 API Key', '进入「控制台」→「API Keys」，复制生成的 Key（只显示一次，务必保存好）']
    ],
    tips: ['模型名可填 glm-4.5、glm-4.5-air（更便宜）或 glm-4-flash（免费）']
  },
  kimi: {
    label: 'Kimi',
    baseUrl: 'https://api.moonshot.cn/v1',
    model: 'kimi-k2-0905-preview',
    site: 'https://platform.moonshot.cn',
    siteName: 'platform.moonshot.cn',
    steps: [
      ['注册账号', '打开 platform.moonshot.cn（月之暗面开放平台），注册并登录'],
      ['充值', '新用户会赠送免费额度，用完后按 tokens 计费'],
      ['创建 API Key', '进入「API Key 管理」→「新建」，复制生成的 Key（只显示一次，务必保存好）']
    ],
    tips: ['模型名可填 kimi-k2 系列，长文本场景可用 moonshot-v1-128k']
  },
  minimax: {
    label: 'MiniMax',
    baseUrl: 'https://api.minimax.chat/v1',
    model: 'MiniMax-Text-01',
    site: 'https://platform.minimaxi.com',
    siteName: 'platform.minimaxi.com',
    steps: [
      ['注册账号', '打开 platform.minimaxi.com（MiniMax 开放平台），注册并登录'],
      ['充值', '新用户会赠送免费额度，用完后按 tokens 计费'],
      ['创建 API Key', '进入「账户管理」→「API Keys」，复制生成的 Key（只显示一次，务必保存好）']
    ],
    tips: ['模型名填 MiniMax-Text-01 或 MiniMax-M1（推理模型）']
  },
  custom: {
    label: '自定义',
    baseUrl: '',
    model: '',
    site: '',
    siteName: '',
    steps: [],
    tips: ['适用于 OneAPI / NewAPI / vLLM / 公司内网网关等任意 OpenAI 兼容服务']
  }
}

export const providerOptions = Object.keys(PROVIDERS).map((key) => ({
  key,
  label: PROVIDERS[key].label
}))
