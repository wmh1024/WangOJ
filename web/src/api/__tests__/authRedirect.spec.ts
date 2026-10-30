import { describe, expect, it } from 'vitest'
import { shouldRedirectToLogin } from '../authRedirect'

describe('401 登录跳转判定', () => {
  it('公开页不跳转', () => {
    expect(shouldRedirectToLogin({ path: '/problem' })).toBe(false)
    expect(shouldRedirectToLogin({ path: '/home', requiresAuth: undefined })).toBe(false)
  })

  it('需要登录的页面跳转', () => {
    expect(shouldRedirectToLogin({ path: '/code', requiresAuth: 'user' })).toBe(true)
    expect(shouldRedirectToLogin({ path: '/create', requiresAuth: 'admin' })).toBe(true)
  })

  it('守卫刷新用户状态时静默处理 401', () => {
    expect(
      shouldRedirectToLogin({ path: '/code', requiresAuth: 'user', skipAuthRedirect: true })
    ).toBe(false)
  })

  it('登录页自身不重复跳转', () => {
    expect(shouldRedirectToLogin({ path: '/login', requiresAuth: 'user' })).toBe(false)
  })
})
