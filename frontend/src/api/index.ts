import request from './request'
import type {
  LoginRequest,
  LoginResponse,
  AnalyzeRequest,
  AnalyzeResponse,
  Note,
  Task,
  Knowledge,
  PersonalInsight,
  PersonalProfile
} from './types'

export const authAPI = {
  login: (data: LoginRequest) => request.post<any, LoginResponse>('/auth/login', data),
  register: (data: LoginRequest) => request.post<any, LoginResponse>('/auth/register', data)
}

export const aiAPI = {
  analyze: (data: AnalyzeRequest) => request.post<any, AnalyzeResponse>('/ai/analyze', data),
  personalInsight: () => request.get<any, PersonalInsight>('/ai/personal-insight')
}

export const noteAPI = {
  list: () => request.get<any, Note[]>('/notes'),
  create: (data: { title?: string; content: string; category?: string; sourceType?: string }) => request.post<any, Note>('/notes', data),
  update: (id: number, data: { title?: string; content: string; category?: string; sourceType?: string }) => request.put<any, Note>(`/notes/${id}`, data),
  delete: (id: number) => request.delete(`/notes/${id}`)
}

export const taskAPI = {
  list: () => request.get<any, Task[]>('/tasks'),
  create: (data: Partial<Task>) => request.post<any, Task>('/tasks', data),
  get: (id: number) => request.get<any, Task>(`/tasks/${id}`),
  update: (id: number, data: Partial<Task>) => request.put<any, Task>(`/tasks/${id}`, data),
  complete: (id: number) => request.put<any, Task>(`/tasks/${id}/complete`, {}),
  delete: (id: number) => request.delete(`/tasks/${id}`)
}

export const knowledgeAPI = {
  list: () => request.get<any, Knowledge[]>('/knowledge'),
  create: (data: { title: string; content: string; tags: string[] | string }) => request.post<any, Knowledge>('/knowledge', {
    title: data.title,
    content: data.content,
    tags: Array.isArray(data.tags) ? data.tags.join(',') : data.tags
  }),
  update: (id: number, data: { title: string; content: string; tags: string[] | string }) => request.put<any, Knowledge>(`/knowledge/${id}`, {
    title: data.title,
    content: data.content,
    tags: Array.isArray(data.tags) ? data.tags.join(',') : data.tags
  }),
  delete: (id: number) => request.delete(`/knowledge/${id}`)
}

export const profileAPI = {
  get: () => request.get<any, PersonalProfile>('/profile'),
  update: (data: PersonalProfile) => request.put<any, PersonalProfile>('/profile', data)
}
