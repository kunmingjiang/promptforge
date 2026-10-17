/**
 * 中文语言包
 * 结构：menu 侧边栏/路由 / layout 布局 / login 登录页 / home 工作台首页 /
 *       prompt 提示词工作台 / requirement 需求助手 / history 对话历史 /
 *       admin 后台（配置+统计） / status 模型状态条 / common 公共 / providers 供应商教程
 */
export default {
  menu: {
    home: '工作台首页',
    prompt: '提示词生成',
    requirement: '需求助手',
    history: '对话历史',
    adminConfig: '模型配置',
    adminStats: '使用统计',
    workspace: '工作台',
    admin: '管理'
  },
  layout: {
    userDefault: '用户',
    logout: '退出登录',
    langName: '简体中文'
  },
  login: {
    headlinePre: '把一句大白话，',
    headlineAccent: '变成大模型听得懂的专业指令',
    sub: '自部署的开源 AI 提示词与需求助手。管理员配置一次 DeepSeek，全员直接使用。',
    feature1Title: '提示词引擎',
    feature1Desc: '一句需求，产出结构化、可复用的工程化提示词',
    feature2Title: '需求引导链',
    feature2Desc: 'AI 多轮追问澄清，生成 Markdown 需求文档',
    feature3Title: '版本可回溯',
    feature3Desc: '每次生成都落版本，随时回看与切换',
    formTitle: '铸语 PromptForge',
    formSub: '提示词生成 · 需求文档一步到位',
    tabLogin: '登录',
    tabRegister: '注册',
    username: '用户名',
    password: '密码',
    passwordRegister: '密码（至少 6 位）',
    emailOptional: '邮箱（选填）',
    loginBtn: '登录',
    registerBtn: '注册并登录',
    loginSuccess: '登录成功',
    registerSuccess: '注册成功，已自动登录',
    ruleUsernameRequired: '请输入用户名',
    ruleUsernameLen: '长度 3-32 位',
    ruleUsernamePattern: '仅支持字母、数字、下划线和短横线',
    rulePasswordRequired: '请输入密码',
    rulePasswordLen: '长度 6-64 位'
  },
  home: {
    hello: '你好，{name}。',
    heroPre: '今天要把',
    heroAccent: '什么想法',
    heroPost: '交给 AI？',
    heroDesc: '选择一个入口开始，所有生成内容都会自动保存到「对话历史」，可随时回看。',
    entry1Title: '提示词生成',
    entry1Desc: '输入一句需求，生成结构化、可直接复用的高质量提示词',
    entry2Title: '需求助手',
    entry2Desc: '提交原始需求，AI 追问澄清问题，生成 Markdown 需求文档',
    startUsing: '开始使用'
  },
  prompt: {
    inputTitle: '你的需求',
    inputPlaceholder: '用一句大白话描述你要的提示词，也可以随手把零散要点倒进来，例如：帮我写一个周报生成器…',
    generate: '生成提示词',
    copyResult: '复制结果',
    clarifyBtn: '信息太散？让 AI 帮我梳理',
    clarifyTitle: 'AI 想先确认这几件事（答完再生成，效果更好）：',
    clarifyAnswerPlaceholder: '回答（可以留空，留空的问题 AI 会用合理默认值）',
    generateWithAnswers: '带着回答生成',
    clarifyCollapse: '先不用梳理',
    clarifyMergeTitle: '【澄清补充】',
    clarifyEmpty: '你的信息已经很清楚了，直接生成就好',
    clarifyFailed: '梳理失败，请稍后再试',
    attachTip: '可附最多 3 张图（流程图 / 草图 / 设计稿，4MB 内 png·jpg·webp）。图片识别需当前模型支持视觉：GLM、GPT-4o、Claude、Gemini、Kimi 视觉版均可；DeepSeek 仅支持文字。',
    imageTypeError: '仅支持 png / jpg / webp 图片',
    imageSizeError: '单张图片不能超过 4MB',
    imageLimitError: '最多上传 3 张图片',
    version: '版本',
    finalized: '✓ 已定稿',
    savedGenerate: '已保存 · v{v} · 接下来可以「试运行」验证效果',
    savedOptimize: '优化完成 · 新版本 v{v} 已保存，可在「结果」标签回看全部版本',
    newVersionMsg: '已生成新版本 v{v}',
    sample1: '帮我写一个面向程序员的周报生成提示词，要求输出结构化 Markdown',
    sample2: '生成一个代码评审助手提示词，按严重级别分级输出修改意见',
    sample3: '我要一个把会议记录转成待办清单的提示词，能识别负责人和截止时间',
    tabResult: '结果',
    tabTest: '试运行',
    tabOptimize: '优化',
    tabPublish: '创建智能体',
    emptyTitle: '还没有结果',
    emptyDesc: '在左侧输入需求并点击「生成提示词」，这里会流式输出。不知道写什么？点一条试试：',
    versionNote: '正在查看 v{current} / 共 {total} 版，点左下角版本号可回看历史版本',
    testHint: '把当前版本的提示词当成真实系统跑一遍：输入一段模拟的用户话，看模型真实反馈。跑完可点「AI 自检」，由 AI 评审输出并列出它不确定、需要人工确认的点。',
    testInputPlaceholder: '模拟用户输入，例如：帮我把今天的会议记录整理成周报…',
    runTest: '运行测试',
    selfCheck: 'AI 自检',
    selfCheckResult: '自检结论 · {ms}ms',
    selfCheckFailed: '自检失败，请稍后再试',
    copy: '复制',
    copiedOutput: '输出已复制到剪贴板',
    goOptimize: '不满意，带结果去优化 →',
    modelOutput: '模型输出 · {ms}ms',
    optimizeHint: '告诉 AI 哪里不满意：可点快捷标签，也可自由描述。优化会生成新版本，随时可回看。',
    optimizeInputPlaceholder: '例如：输出里不要出现免责声明；日期格式统一用 YYYY-MM-DD…',
    generateNext: '生成下一版',
    carryTag: '✓ 将附带试运行结果作为优化依据',
    generatingNew: '正在生成新版本…',
    direction1: '输出更简洁凝练',
    direction2: '指令约束更严格',
    direction3: '补充few-shot示例',
    direction4: '角色设定更鲜明',
    direction5: '强化输出格式',
    direction6: '修复试运行暴露的问题',
    publishHint: '把满意的提示词一键打包成目标平台的创建材料：开场白 + 创建步骤 + 调用示例。',
    agentNamePlaceholder: '智能体名称，例如：周报小助手',
    greetingPlaceholder: '开场白（留空则 AI 自动生成）',
    buildKit: '生成上架材料包',
    goToPlatform: '前往 {name} 创建 ↗',
    kitGreeting: '开场白',
    kitSystemPrompt: '系统提示词（粘贴到平台设定/Instructions）',
    kitCallExample: '调用示例',
    copy: '复制',
    warnNeedInput: '请先输入你的需求描述',
    warnNeedVersion: '请先生成或选择一个提示词版本',
    warnNeedTestInput: '请填写测试输入（模拟真实用户会说什么）',
    testFailed: '试运行失败，请检查模型配置',
    goOptimizeFeedback: '试运行输出不符合预期，请修复暴露的问题',
    warnNeedPrompt: '请先生成一个提示词',
    warnNeedFeedback: '请描述优化方向：点快捷标签或在下方自由填写',
    warnFinalizeDone: '已定稿，可以拿去创建智能体了',
    warnNeedAgentName: '给你的智能体起个名字',
    buildKitFailed: '生成上架材料失败',
    copiedPrompt: '提示词已复制到剪贴板',
    copiedGreeting: '开场白已复制',
    copiedExample: '示例已复制',
    copied: '已复制'
  },
  requirement: {
    stepSubmit: '提交需求',
    stepQa: '澄清问答',
    stepDoc: '生成文档',
    infoTitle: '需求信息',
    projectTitle: '项目标题',
    titlePlaceholder: '例如：停车缴费小程序',
    rawRequirement: '原始需求',
    rawPlaceholder: '用大白话描述你想做的东西，AI 会帮你追问关键细节',
    genQuestions: '生成澄清问题',
    qaTitle: 'AI 需要澄清 {n} 个问题（最多 5 个，可跳过）',
    answerPlaceholder: '填写你的回答',
    skipQuestion: '跳过此问题',
    submitAnswers: '提交回答',
    restart: '重新开始',
    docTitle: '需求文档',
    genDoc: '生成需求文档',
    regenDoc: '重新生成文档',
    newProject: '新建项目',
    emptyDesc: '点击「生成需求文档」，右侧将流式渲染 Markdown 文档',
    warnFillTitleRaw: '请填写项目标题和原始需求',
    warnConfigFirst: '请先完成模型配置',
    docGenerated: '需求文档已生成并保存版本'
  },
  history: {
    filterAll: '全部',
    filterPrompt: '提示词',
    filterRequirement: '需求助手',
    empty: '还没有对话记录',
    loadMore: '加载更多（{loaded}/{total}）',
    selectVersion: '选择版本',
    inputTag: '【输入】',
    roleUser: '我',
    roleAI: 'AI',
    noMessages: '该会话暂无消息',
    pickConversation: '从左侧选择一条会话查看详情',
    modulePrompt: '提示词生成',
    moduleRequirement: '需求助手'
  },
  admin: {
    configTitle: '模型配置',
    configAlert: '支持 DeepSeek / 智谱 GLM / Kimi / MiniMax 及任意 OpenAI 兼容网关。API Key 使用 AES-256 加密存储，保存后仅显示末 4 位；更换 Key 时重新填写即可。',
    provider: '模型提供商',
    baseUrl: 'Base URL',
    modelName: '模型名',
    apiKeySaved: 'API Key（已保存：{masked}）',
    apiKey: 'API Key',
    enabled: '启用',
    testConn: '测试连接',
    saveConfig: '保存配置',
    latency: '耗时 {ms}ms',
    statsTitle: '使用统计',
    userCount: '用户数',
    conversationCount: '会话数',
    messageCount: '消息数',
    requirementProjectCount: '需求项目数',
    statsNote: '管理后台为只读统计，不提供任何用户数据明细，满足最小可见原则。',
    warnFillBaseUrlModel: '请填写 Base URL 与模型名',
    savedEncrypted: '配置已保存，API Key 已加密存储',
    warnNeedKeyForTest: '测试连接需要输入 API Key',
    connSuccess: '连接成功，耗时 {ms}ms',
    connFailed: '连接失败',
    keyKeepOriginal: '留空则保留原 Key',
    keyCreatedAt: '在 {site} 创建的 Key',
    modelPlaceholder: '例如 gpt-4o / qwen-max'
  },
  status: {
    ready: '就绪',
    statusReady: '{provider} · {model} · 就绪',
    notConfigured: '模型未配置',
    manage: '管理',
    goConfig: '去配置',
    dialogTitle: '模型配置',
    notConfiguredAlert: '模型尚未配置，配置完成后即可开始生成',
    userAlertTitle: '模型配置由管理员统一管理',
    userAlertDesc: '请联系管理员在「管理后台 → 模型配置」中完成配置；如果你自己就是管理员，用管理员账号登录后即可在此直接填写。',
    guideTitle: '不知道怎么获取 {provider} 的 API Key？看这里（3 步搞定）',
    baseUrlTip: 'Base URL 填 {url}',
    keySafeTip: 'Key 只保存在你自己的服务器里（AES-256 加密），不会上传任何第三方',
    openSite: '打开 {site} ↗',
    savedEncrypted: '模型配置已保存，API Key 已加密存储',
    warnFillBaseUrlModel: '请填写 Base URL 与模型名',
    warnNeedApiKeyFirst: '首次配置需要填写 API Key',
    warnNeedKeyForTest: '测试连接需要输入 API Key（已保存的 Key 出于安全不回显，重新粘贴一次即可）',
    connSuccess: '连接成功，耗时 {ms}ms',
    connFailed: '连接失败'
  },
  common: {
    appTitle: 'PROMPTFORGE'
  },
  providers: {
    deepseek: {
      steps: [
        { t: '注册账号', d: '打开 platform.deepseek.com，用手机号或邮箱注册并登录' },
        { t: '充值', d: '进入「充值」页面，按 tokens 用量计费，最低可充 10 元，个人使用足够很久' },
        { t: '创建 API Key', d: '进入「API Keys」→「创建 API key」，复制生成的 sk-... 字符串（只显示一次，务必保存好）' }
      ],
      tips: ['模型名填 deepseek-chat（V3 通用模型）或 deepseek-reasoner（R1 推理模型）']
    },
    glm: {
      steps: [
        { t: '注册账号', d: '打开 open.bigmodel.cn（智谱开放平台），注册并登录' },
        { t: '实名认证', d: '按指引完成实名认证，新用户会赠送免费 tokens 额度' },
        { t: '创建 API Key', d: '进入「控制台」→「API Keys」，复制生成的 Key（只显示一次，务必保存好）' }
      ],
      tips: ['模型名可填 glm-4.5、glm-4.5-air（更便宜）或 glm-4-flash（免费）']
    },
    kimi: {
      steps: [
        { t: '注册账号', d: '打开 platform.moonshot.cn（月之暗面开放平台），注册并登录' },
        { t: '充值', d: '新用户会赠送免费额度，用完后按 tokens 计费' },
        { t: '创建 API Key', d: '进入「API Key 管理」→「新建」，复制生成的 Key（只显示一次，务必保存好）' }
      ],
      tips: ['模型名可填 kimi-k2 系列，长文本场景可用 moonshot-v1-128k']
    },
    minimax: {
      steps: [
        { t: '注册账号', d: '打开 platform.minimaxi.com（MiniMax 开放平台），注册并登录' },
        { t: '充值', d: '新用户会赠送免费额度，用完后按 tokens 计费' },
        { t: '创建 API Key', d: '进入「账户管理」→「API Keys」，复制生成的 Key（只显示一次，务必保存好）' }
      ],
      tips: ['模型名填 MiniMax-Text-01 或 MiniMax-M1（推理模型）']
    },
    openai: {
      steps: [
        { t: '注册账号', d: '打开 platform.openai.com 注册并登录（需海外网络环境）' },
        { t: '绑定付款方式', d: '进入 Settings → Billing，绑定国际信用卡（Visa/Mastercard）并购买 API 额度' },
        { t: '创建 API Key', d: '进入「API keys」→「Create new secret key」，复制 sk-... 字符串（只显示一次，务必保存好）' }
      ],
      tips: ['需要海外网络与国际信用卡', '模型名 gpt-4o 或 gpt-4o-mini（更便宜）']
    },
    claude: {
      steps: [
        { t: '注册账号', d: '打开 console.anthropic.com 注册并登录（需海外网络环境）' },
        { t: '绑定付款方式', d: '进入 Settings → Billing，绑定国际信用卡并购买额度' },
        { t: '创建 API Key', d: '进入「API Keys」→「Create Key」，复制 sk-ant-... 字符串（只显示一次，务必保存好）' }
      ],
      tips: [
        '模型名 claude-sonnet-4-5（均衡）、claude-haiku 系列（快、便宜）、claude-opus（能力最强）',
        '走 Anthropic 官方 OpenAI 兼容端点，无需额外适配'
      ]
    },
    gemini: {
      steps: [
        { t: '打开 Google AI Studio', d: '访问 aistudio.google.com 并登录 Google 账号（需海外网络环境）' },
        { t: '创建 API Key', d: '点「Get API key」→「Create API key」，免费额度即可开发调试' },
        { t: '复制 Key', d: '复制生成的 API Key（后续可在 AI Studio 随时查看）' }
      ],
      tips: [
        '免费额度充足，适合个人与调试',
        '模型名 gemini-2.5-flash（快）或 gemini-2.5-pro（更强）',
        '走 Google 官方 OpenAI 兼容端点'
      ]
    },
    custom: {
      label: '自定义',
      tips: ['适用于 OneAPI / NewAPI / vLLM / 公司内网网关等任意 OpenAI 兼容服务']
    }
  }
}
