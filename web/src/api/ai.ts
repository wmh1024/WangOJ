// AiController => ai.ts

import request from '@/api/request'

export interface AiCodeVO {
  problemId: number
  code: string
  /** 可选：用户提出的具体问题（为空时后端只做注释与格式化） */
  aiPrompt?: string
}

export const aiCode = (aiCodeVO: AiCodeVO) => {
  return request.post('/api/ai/code', aiCodeVO)
}
