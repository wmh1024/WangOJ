// CommitController => api/commitlog.ts
import request from '@/api/request'

export interface CommitlogDTO {
  id: number
  userId: number
  problemId: number
  problemTitle: string
  code: string
  // 判题状态：Accept / Waiting / Wrong Answer / Compilation Error /
  // Time Limit Exceeded / Memory Limit Exceeded / Runtime Error / System Error
  status: string
  timeoutLimit: number
  memoryLimit: number
  createdTime: string
  lang?: string
}

export interface CommitlogVO {
  problemId: number
  code: string
  lang: string
}

/** 单个测试点的判题结论（后端 caseResult 反序列化后） */
export interface CaseOutcome {
  index: number
  verdict: string
  timeMs?: number | null
  memoryBytes?: number | null
  accepted: boolean
}

/** 提交详情（/api/log/{id}） */
export interface CommitlogDetail {
  id: number
  userId: number
  problemId: number
  problemTitle: string
  problemTags: string[]
  code: string
  lang: string
  status: string
  timeoutLimit?: number
  memoryLimit?: number
  createdTime: string
  errorMessage?: string
  caseResult?: string
}

export const commitCode = (commitlogVO: CommitlogVO) => {
  return request.post('/api/commit/code', commitlogVO)
}

export const getCommitDTOList = (current: number, pageSize: number, userId?: number) => {
  return request.get('/api/commit/list', {
    params: {
      current,
      pageSize,
      // 仅传当前用户 ID 时后端会按用户过滤，便于"只看我的提交"
      ...(userId ? { userId } : {})
    }
  })
}


export const getCommitlogById = (id: number) => {
  return request.get(`/api/log/${id}`)
}

/** 排行榜条目：ac = 通过题目数，ans = AC 次数 */
export interface RankItem {
  id: number
  name: string
  ac: number
  ans: number
}

export const getRankList = () => {
  return request.get('/api/rank')
}