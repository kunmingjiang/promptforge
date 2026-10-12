export interface UserVO {
  id: number
  username: string
  email: string
  roles: string[]
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: UserVO
}

export interface ModelConfig {
  id: number
  provider: string
  baseUrl: string
  modelName: string
  enabled: boolean
  apiKeyMasked: string
  updatedAt: string
}

export interface ModelConfigTestResult {
  success: boolean
  message: string
  latencyMs: number
}

export interface ModelStatus {
  configured: boolean
  enabled: boolean
  provider: string
  modelName: string
  message: string
}

export interface Stats {
  userCount: number
  conversationCount: number
  messageCount: number
  requirementProjectCount: number
}

export interface Conversation {
  id: number
  title: string
  module: string
  createdAt: string
}

export interface Message {
  id: number
  role: string
  content: string
  createdAt: string
}

export interface ConversationDetail {
  id: number
  title: string
  module: string
  createdAt: string
  messages: Message[]
}

export interface ConversationVersion {
  id: number
  version: number
  createdAt: string
  snapshot: string
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface Question {
  id: number
  question: string
  answer: string
  skipped: boolean
}

export interface ProjectVO {
  id: number
  title: string
  status: string
  createdAt: string
}

export interface DocumentVersion {
  versionId: number
  version: number
  createdAt: string
}

export interface DocumentVO {
  projectId: number
  version: number
  content: string
  versions: DocumentVersion[]
}
