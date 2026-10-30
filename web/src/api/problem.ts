// ProblemController => api/problem.ts

import request from '@/api/request'

export class ProblemVO {
  id?: number
  title!: string
  content!: string
  tags!: string[]
  testCase!: TestCase[]
  limitOption!: LimitOption
}

interface TestCase {
  input: string
  output: string
}

interface LimitOption {
  timeoutLimit: number
  memoryLimit: number
}

export interface ProblemQuery {
  id: number | undefined
  title: string
  content: string
  tags: string[]
}

export const getProblemDTOList = (
  current: number,
  pageSize: number,
  problemQuery: ProblemQuery
) => {
  return request.post('/api/problem/list', problemQuery, {
    params: {
      current,
      pageSize
    }
  })
}

export const getProblemContentById = (id: number) => {
  return request.get('/api/problem/content', {
    params: {
      id
    }
  })
}

export const addProblem = (problemVO: ProblemVO) => {
  return request.post('/api/admin/problem/add', problemVO)
}

export const getProblemVOById = (id: number) => {
  return request.get(`/api/admin/problem/${id}`)
}

export const changeProblem = (problemVO: ProblemVO) => {
  return request.put('/api/admin/problem/change', problemVO)
}

export const delProblem = (id: number) => {
  return request.delete('/api/admin/problem/del', {
    params: {
      id
    }
  })
}

export const getHotProblemList = () => {
  return request.get('/api/problem/hot')
}