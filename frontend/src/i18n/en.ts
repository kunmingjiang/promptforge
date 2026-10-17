/**
 * English locale
 * Sections: menu sidebar/routes / layout / login / home /
 *           prompt workbench / requirement assistant / history /
 *           admin (config + stats) / status model bar / common / providers guides
 */
export default {
  menu: {
    home: 'Home',
    prompt: 'Prompt Generator',
    requirement: 'Requirement Assistant',
    history: 'History',
    adminConfig: 'Model Config',
    adminStats: 'Usage Stats',
    workspace: 'WORKSPACE',
    admin: 'ADMIN'
  },
  layout: {
    userDefault: 'User',
    logout: 'Sign out',
    langName: 'English'
  },
  login: {
    headlinePre: 'Turn plain words into',
    headlineAccent: 'professional instructions AI understands',
    sub: 'A self-hosted, open-source AI prompt & requirement assistant. Admin configures DeepSeek once, and the whole team is ready to go.',
    feature1Title: 'Prompt Engine',
    feature1Desc: 'One sentence in, a structured and reusable engineering-grade prompt out',
    feature2Title: 'Requirement Guidance Chain',
    feature2Desc: 'AI clarifies with follow-up questions, then generates a Markdown requirement document',
    feature3Title: 'Versioned History',
    feature3Desc: 'Every generation is versioned — review and switch anytime',
    formTitle: 'PromptForge',
    formSub: 'Prompt generation · Requirement docs in one place',
    tabLogin: 'Sign in',
    tabRegister: 'Sign up',
    username: 'Username',
    password: 'Password',
    passwordRegister: 'Password (at least 6 characters)',
    emailOptional: 'Email (optional)',
    loginBtn: 'Sign in',
    registerBtn: 'Sign up & sign in',
    loginSuccess: 'Signed in successfully',
    registerSuccess: 'Registered successfully, signed in automatically',
    ruleUsernameRequired: 'Please enter a username',
    ruleUsernameLen: 'Length must be 3-32 characters',
    ruleUsernamePattern: 'Only letters, digits, underscores and hyphens are allowed',
    rulePasswordRequired: 'Please enter a password',
    rulePasswordLen: 'Length must be 6-64 characters'
  },
  home: {
    hello: 'Hello, {name}.',
    heroPre: 'What ',
    heroAccent: 'idea',
    heroPost: ' will you hand to AI today?',
    heroDesc: 'Pick an entry to start — everything you generate is saved to "History" automatically and can be revisited anytime.',
    entry1Title: 'Prompt Generator',
    entry1Desc: 'Describe a need in one sentence and get a structured, ready-to-reuse, high-quality prompt',
    entry2Title: 'Requirement Assistant',
    entry2Desc: 'Submit a raw requirement, AI asks clarifying questions, and a Markdown requirement doc is generated',
    startUsing: 'Get started'
  },
  prompt: {
    inputTitle: 'Your requirement',
    inputPlaceholder: 'Describe the prompt you want in plain words, e.g. "Write me a weekly report generator…"',
    generate: 'Generate prompt',
    copyResult: 'Copy result',
    version: 'Version',
    finalized: '✓ Finalized',
    savedGenerate: 'Saved · v{v} · Next, try "Test run" to verify the result',
    savedOptimize: 'Optimization done · New version v{v} saved — review all versions in the "Result" tab',
    newVersionMsg: 'New version v{v} generated',
    sample1: 'Write a weekly-report generator prompt for programmers, output must be structured Markdown',
    sample2: 'Create a code-review assistant prompt that gives feedback ranked by severity',
    sample3: 'I want a prompt that turns meeting notes into a to-do list, identifying owners and deadlines',
    tabResult: 'Result',
    tabTest: 'Test run',
    tabOptimize: 'Optimize',
    tabPublish: 'Create agent',
    emptyTitle: 'No result yet',
    emptyDesc: 'Enter a requirement on the left and click "Generate prompt" — output streams here. Not sure what to write? Try one of these:',
    versionNote: 'Viewing v{current} / {total} versions in total. Click a version chip at the bottom left to revisit history.',
    testHint: 'Run the current prompt as if it were a real system: type a simulated user message and see the model\'s actual response. Then run "AI self-check" to review the output and list points the AI is unsure about for human confirmation.',
    testInputPlaceholder: 'Simulated user input, e.g. "Help me turn today\'s meeting notes into a weekly report…"',
    runTest: 'Run test',
    selfCheck: 'AI self-check',
    selfCheckResult: 'Self-check review · {ms}ms',
    selfCheckFailed: 'Self-check failed, please retry',
    copy: 'Copy',
    copiedOutput: 'Output copied to clipboard',
    goOptimize: 'Not satisfied — optimize with this result →',
    modelOutput: 'Model output · {ms}ms',
    optimizeHint: 'Tell AI what you are not satisfied with: click a quick tag or describe freely. Each optimization creates a new version you can revisit anytime.',
    optimizeInputPlaceholder: 'E.g. no disclaimer in the output; always use YYYY-MM-DD for dates…',
    generateNext: 'Generate next version',
    carryTag: '✓ The test-run result will be attached as optimization context',
    generatingNew: 'Generating new version…',
    direction1: 'Make output more concise',
    direction2: 'Tighten instruction constraints',
    direction3: 'Add few-shot examples',
    direction4: 'Sharpen the persona',
    direction5: 'Reinforce output format',
    direction6: 'Fix issues exposed by the test run',
    publishHint: 'Package your polished prompt into launch materials for the target platform: greeting + setup steps + API example.',
    agentNamePlaceholder: 'Agent name, e.g. Weekly Report Helper',
    greetingPlaceholder: 'Greeting (leave empty for AI to generate)',
    buildKit: 'Generate launch kit',
    goToPlatform: 'Create on {name} ↗',
    kitGreeting: 'Greeting',
    kitSystemPrompt: 'System prompt (paste into the platform\'s Instructions)',
    kitCallExample: 'API example',
    copy: 'Copy',
    warnNeedInput: 'Please describe your requirement first',
    warnNeedVersion: 'Please generate or select a prompt version first',
    warnNeedTestInput: 'Please fill in the test input (what a real user would say)',
    testFailed: 'Test run failed — please check the model configuration',
    goOptimizeFeedback: 'The test-run output did not meet expectations. Please fix the exposed issues.',
    warnNeedPrompt: 'Please generate a prompt first',
    warnNeedFeedback: 'Describe the optimization direction: click a quick tag or write freely below',
    warnFinalizeDone: 'Finalized — ready to create an agent',
    warnNeedAgentName: 'Give your agent a name',
    buildKitFailed: 'Failed to generate the launch kit',
    copiedPrompt: 'Prompt copied to clipboard',
    copiedGreeting: 'Greeting copied',
    copiedExample: 'Example copied',
    copied: 'Copied'
  },
  requirement: {
    stepSubmit: 'Submit requirement',
    stepQa: 'Clarification Q&A',
    stepDoc: 'Generate document',
    infoTitle: 'Requirement details',
    projectTitle: 'Project title',
    titlePlaceholder: 'E.g. Parking payment mini-program',
    rawRequirement: 'Raw requirement',
    rawPlaceholder: 'Describe what you want to build in plain words — AI will ask about key details',
    genQuestions: 'Generate clarifying questions',
    qaTitle: 'AI needs to clarify {n} question(s) (up to 5, skippable)',
    answerPlaceholder: 'Type your answer',
    skipQuestion: 'Skip this question',
    submitAnswers: 'Submit answers',
    restart: 'Start over',
    docTitle: 'Requirement document',
    genDoc: 'Generate document',
    regenDoc: 'Regenerate document',
    newProject: 'New project',
    emptyDesc: 'Click "Generate document" and the Markdown document will stream in on the right',
    warnFillTitleRaw: 'Please fill in the project title and raw requirement',
    warnConfigFirst: 'Please complete the model configuration first',
    docGenerated: 'Requirement document generated and version saved'
  },
  history: {
    filterAll: 'All',
    filterPrompt: 'Prompt',
    filterRequirement: 'Requirement',
    empty: 'No conversations yet',
    loadMore: 'Load more ({loaded}/{total})',
    selectVersion: 'Select version',
    inputTag: '[Input] ',
    roleUser: 'Me',
    roleAI: 'AI',
    noMessages: 'No messages in this conversation',
    pickConversation: 'Select a conversation on the left to view details',
    modulePrompt: 'Prompt Generator',
    moduleRequirement: 'Requirement Assistant'
  },
  admin: {
    configTitle: 'Model Configuration',
    configAlert: 'Supports DeepSeek / Zhipu GLM / Kimi / MiniMax and any OpenAI-compatible gateway. API keys are encrypted with AES-256 — only the last 4 characters are shown after saving. Re-enter the key when rotating it.',
    provider: 'Model provider',
    baseUrl: 'Base URL',
    modelName: 'Model name',
    apiKeySaved: 'API Key (saved: {masked})',
    apiKey: 'API Key',
    enabled: 'Enabled',
    testConn: 'Test connection',
    saveConfig: 'Save configuration',
    latency: 'Latency {ms}ms',
    statsTitle: 'Usage Stats',
    userCount: 'Users',
    conversationCount: 'Conversations',
    messageCount: 'Messages',
    requirementProjectCount: 'Requirement projects',
    statsNote: 'The admin console shows read-only aggregate statistics only — no user-level data is exposed, following the minimum-visibility principle.',
    warnFillBaseUrlModel: 'Please fill in the Base URL and model name',
    savedEncrypted: 'Configuration saved. API key stored encrypted',
    warnNeedKeyForTest: 'Testing the connection requires an API key',
    connSuccess: 'Connection successful, {ms}ms',
    connFailed: 'Connection failed',
    keyKeepOriginal: 'Leave empty to keep the existing key',
    keyCreatedAt: 'Key created at {site}',
    modelPlaceholder: 'E.g. gpt-4o / qwen-max'
  },
  status: {
    ready: 'Ready',
    statusReady: '{provider} · {model} · Ready',
    notConfigured: 'Model not configured',
    manage: 'Manage',
    goConfig: 'Set up',
    dialogTitle: 'Model Configuration',
    notConfiguredAlert: 'Model not configured yet. You can start generating once it is set up.',
    userAlertTitle: 'Model configuration is managed by the admin',
    userAlertDesc: 'Please ask the admin to complete the configuration in "Admin console → Model Config". If you are the admin, sign in with the admin account to fill it in right here.',
    guideTitle: 'Don\'t know how to get an API key for {provider}? Here\'s how (3 steps)',
    baseUrlTip: 'Fill the Base URL with {url}',
    keySafeTip: 'Your key is stored only on your own server (AES-256 encrypted) — never uploaded to any third party',
    openSite: 'Open {site} ↗',
    savedEncrypted: 'Model configuration saved. API key stored encrypted',
    warnFillBaseUrlModel: 'Please fill in the Base URL and model name',
    warnNeedApiKeyFirst: 'First-time setup requires an API key',
    warnNeedKeyForTest: 'Testing the connection requires an API key (saved keys are never echoed back for security — just paste it again)',
    connSuccess: 'Connection successful, {ms}ms',
    connFailed: 'Connection failed'
  },
  common: {
    appTitle: 'PROMPTFORGE'
  },
  providers: {
    deepseek: {
      steps: [
        { t: 'Register an account', d: 'Open platform.deepseek.com, sign up with a phone number or email and sign in' },
        { t: 'Top up', d: 'Go to the "Top up" page. Billing is per token; the minimum is ¥10, which is plenty for personal use' },
        { t: 'Create an API Key', d: 'Go to "API Keys" → "Create API key" and copy the generated sk-... string (shown only once — keep it safe)' }
      ],
      tips: ['Use model name deepseek-chat (V3 general) or deepseek-reasoner (R1 reasoning)']
    },
    glm: {
      steps: [
        { t: 'Register an account', d: 'Open open.bigmodel.cn (Zhipu Open Platform), sign up and sign in' },
        { t: 'Identity verification', d: 'Complete real-name verification as guided — new users get free token credits' },
        { t: 'Create an API Key', d: 'Go to "Console" → "API Keys" and copy the generated key (shown only once — keep it safe)' }
      ],
      tips: ['Use model name glm-4.5, glm-4.5-air (cheaper) or glm-4-flash (free)']
    },
    kimi: {
      steps: [
        { t: 'Register an account', d: 'Open platform.moonshot.cn (Moonshot AI Open Platform), sign up and sign in' },
        { t: 'Top up', d: 'New users get free credits; afterwards billing is per token' },
        { t: 'Create an API Key', d: 'Go to "API Key Management" → "New" and copy the generated key (shown only once — keep it safe)' }
      ],
      tips: ['Use a kimi-k2 model; for long-context scenarios moonshot-v1-128k is available']
    },
    minimax: {
      steps: [
        { t: 'Register an account', d: 'Open platform.minimaxi.com (MiniMax Open Platform), sign up and sign in' },
        { t: 'Top up', d: 'New users get free credits; afterwards billing is per token' },
        { t: 'Create an API Key', d: 'Go to "Account" → "API Keys" and copy the generated key (shown only once — keep it safe)' }
      ],
      tips: ['Use model name MiniMax-Text-01 or MiniMax-M1 (reasoning)']
    },
    openai: {
      steps: [
        { t: 'Register an account', d: 'Open platform.openai.com, sign up and sign in (requires international network access)' },
        { t: 'Add a payment method', d: 'Go to Settings → Billing, add an international credit card (Visa/Mastercard) and purchase API credits' },
        { t: 'Create an API Key', d: 'Go to "API keys" → "Create new secret key" and copy the sk-... string (shown only once — keep it safe)' }
      ],
      tips: ['Requires international network access and an international credit card', 'Use model name gpt-4o or gpt-4o-mini (cheaper)']
    },
    claude: {
      steps: [
        { t: 'Register an account', d: 'Open console.anthropic.com, sign up and sign in (requires international network access)' },
        { t: 'Add a payment method', d: 'Go to Settings → Billing, add an international credit card and purchase credits' },
        { t: 'Create an API Key', d: 'Go to "API Keys" → "Create Key" and copy the sk-ant-... string (shown only once — keep it safe)' }
      ],
      tips: [
        'Model names: claude-sonnet-4-5 (balanced), claude-haiku series (fast, cheap), claude-opus (most capable)',
        'Uses Anthropic\'s official OpenAI-compatible endpoint — no extra adaptation needed'
      ]
    },
    gemini: {
      steps: [
        { t: 'Open Google AI Studio', d: 'Visit aistudio.google.com and sign in with a Google account (requires international network access)' },
        { t: 'Create an API Key', d: 'Click "Get API key" → "Create API key" — the free tier is enough for development and debugging' },
        { t: 'Copy the Key', d: 'Copy the generated API key (you can view it anytime later in AI Studio)' }
      ],
      tips: [
        'Generous free tier, great for individuals and debugging',
        'Model names: gemini-2.5-flash (fast) or gemini-2.5-pro (more capable)',
        'Uses Google\'s official OpenAI-compatible endpoint'
      ]
    },
    custom: {
      label: 'Custom',
      tips: ['Works with any OpenAI-compatible service such as OneAPI / NewAPI / vLLM / corporate intranet gateways']
    }
  }
}
