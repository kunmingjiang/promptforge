import http from '../utils/request'
import type {
  Conversation,
  ConversationDetail,
  ConversationVersion,
  DocumentVO,
  PageData,
  ProjectVO,
  Question,
  Stats
} from '../types'
import type { ModelStatus } from '../types'

export const getModelConfig = () =>
  http.get<unknown, { data: unknown }>('/admin/model-config') as Promise<{ data: unknown }>

export const updateModelConfig = (payload: {
  provider: string
  baseUrl: string
  apiKey?: string
  modelName: string
  enabled: boolean
}) => http.put('/admin/model-config', payload)

export const testModelConfig = (payload: {
  provider: string
  baseUrl: string
  apiKey: string
  modelName: string
}) => http.post('/admin/model-config/test', payload)

export const getStats = () =>
  http.get('/admin/stats') as Promise<{ data: Stats }>

export const getModelStatus = () =>
  http.get('/model/status') as Promise<{ data: ModelStatus }>

export const promptHistory = (page: number, size = 20) =>
  http.get('/prompt/history', { params: { page, size } }) as Promise<{ data: PageData<Conversation> }>

export const promptDetail = (id: number) =>
  http.get(`/prompt/${id}`) as Promise<{ data: ConversationDetail }>

export const promptVersions = (id: number) =>
  http.get(`/prompt/${id}/versions`) as Promise<{ data: ConversationVersion[] }>

export const conversationList = (page: number, size = 20, module?: string) =>
  http.get('/conversations', { params: { page, size, module } }) as Promise<{
    data: PageData<Conversation>
  }>

export const conversationDetail = (id: number) =>
  http.get(`/conversations/${id}`) as Promise<{ data: ConversationDetail }>

export const conversationVersions = (id: number) =>
  http.get(`/conversations/${id}/versions`) as Promise<{ data: ConversationVersion[] }>

export const createProject = (payload: { title: string; rawRequirement: string }) =>
  http.post('/requirement/projects', payload) as Promise<{ data: ProjectVO }>

export const getQuestions = (projectId: number) =>
  http.get(`/requirement/projects/${projectId}/questions`) as Promise<{ data: Question[] }>

export const submitAnswers = (
  projectId: number,
  answers: { answerId: number; answer?: string; skipped?: boolean }[]
) => http.post(`/requirement/projects/${projectId}/answers`, { answers }) as Promise<{ data: Question[] }>

export const getDocument = (projectId: number, version?: number) =>
  http.get(`/requirement/projects/${projectId}/document`, {
    params: version ? { version } : {}
  }) as Promise<{ data: DocumentVO }>
