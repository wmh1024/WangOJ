import { describe, expect, it, vi } from 'vitest'
import { AUTH } from '@/utils/constants'
import type { UserDTO } from '@/api/user'
import { createNavigationGuard } from '../guard'

const admin: UserDTO = { id: 1, userAccount: 'admin', userRole: AUTH.ADMIN }
const user: UserDTO = { id: 2, userAccount: 'alice', userRole: AUTH.USER }

function createStore(token?: string) {
  return {
    token,
    setUser: vi.fn(),
    resetUser: vi.fn()
  }
}

const publicTarget = { meta: {}, fullPath: '/problem' }
const userTarget = { meta: { auth: AUTH.USER }, fullPath: '/code?id=1' }
const adminTarget = { meta: { auth: AUTH.ADMIN }, fullPath: '/create' }

describe('路由守卫 C-2 回归点', () => {
  it('公开页即使 Token 失效也只清理本地态，不跳登录页', async () => {
    const store = createStore('stale-token')
    const guard = createNavigationGuard(store, () => Promise.reject(new Error('401')))

    await expect(guard(publicTarget)).resolves.toBe(true)
    expect(store.resetUser).toHaveBeenCalledOnce()
    expect(store.setUser).not.toHaveBeenCalled()
  })

  it('公开页无 Token 时不请求用户状态', async () => {
    const store = createStore()
    const fetchUserStatus = vi.fn()
    const guard = createNavigationGuard(store, fetchUserStatus)

    await expect(guard(publicTarget)).resolves.toBe(true)
    expect(fetchUserStatus).not.toHaveBeenCalled()
  })

  it('公开页携带有效 Token 时刷新用户信息', async () => {
    const store = createStore('token')
    const guard = createNavigationGuard(store, () => Promise.resolve({ data: { data: user } }))

    await expect(guard(publicTarget)).resolves.toBe(true)
    expect(store.setUser).toHaveBeenCalledWith(user)
  })

  it('登录页缺少 Token 时带上 redirect', async () => {
    const store = createStore()
    const guard = createNavigationGuard(store, () => Promise.resolve({}))

    await expect(guard(userTarget)).resolves.toEqual({
      path: '/login',
      query: { redirect: '/code?id=1' }
    })
  })

  it('登录页 Token 有效时放行普通用户', async () => {
    const store = createStore('token')
    const guard = createNavigationGuard(store, () => Promise.resolve({ data: { data: user } }))

    await expect(guard(userTarget)).resolves.toBe(true)
    expect(store.setUser).toHaveBeenCalledWith(user)
  })

  it('管理员页拒绝普通用户', async () => {
    const store = createStore('token')
    const guard = createNavigationGuard(store, () => Promise.resolve({ data: { data: user } }))

    await expect(guard(adminTarget)).resolves.toEqual({ path: '/403' })
  })

  it('管理员页放行管理员', async () => {
    const store = createStore('token')
    const guard = createNavigationGuard(store, () => Promise.resolve({ data: { data: admin } }))

    await expect(guard(adminTarget)).resolves.toBe(true)
  })

  it('受保护页拉取用户失败时清理并跳登录', async () => {
    const store = createStore('token')
    const guard = createNavigationGuard(store, () => Promise.reject(new Error('network')))

    await expect(guard(userTarget)).resolves.toEqual({
      path: '/login',
      query: { redirect: '/code?id=1' }
    })
    expect(store.resetUser).toHaveBeenCalledOnce()
  })
})
