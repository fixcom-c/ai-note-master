export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
}

export type Priority = 'high' | 'medium' | 'low'

export interface AnalyzeRequest {
  content: string
}

export interface AnalyzeResponse {
  summary: string
  tasks: TaskItem[]
  knowledgeTags: string[]
  reminderTime?: string
}

export interface TaskItem {
  title: string
  description: string
  priority: Priority
  reminderTime?: string
}

export interface Note {
  id: number
  content: string
  createdAt: string
  updatedAt: string
}

export interface Task {
  id: number
  title: string
  description: string
  status: 'pending' | 'completed'
  priority: Priority
  reminderTime?: string
  noteId?: number
  createdAt: string
  updatedAt: string
  completedAt?: string
}

export interface Knowledge {
  id: number
  title: string
  content: string
  tags: string
  createdAt: string
  updatedAt: string
}

export interface DailyReport {
  id: number
  date: string
  content: string
  taskSummary: string
  completedTaskCount: number
  totalTaskCount: number
  createdAt: string
}
