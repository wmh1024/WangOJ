import { describe, expect, it } from 'vitest'
import { AUTH, isAdmin } from '../constants'

describe('权限常量与工具函数', () => {
  it('角色标识与后端保持一致', () => {
    expect(AUTH.ADMIN).toBe('admin')
    expect(AUTH.USER).toBe('user')
  })

  it('isAdmin 只认管理员角色', () => {
    expect(isAdmin(AUTH.ADMIN)).toBe(true)
    expect(isAdmin(AUTH.USER)).toBe(false)
    expect(isAdmin(undefined)).toBe(false)
    expect(isAdmin(null)).toBe(false)
  })
})
