// UserController => api/user.ts

import request from '@/api/request'
import { AUTH } from '@/utils/constants'

/** 登录 / 注册表单（密码只在提交时存在，不进入任何持久化状态） */
export interface LoginForm {
  userAccount: string
  userPassword: string
}

/** 后端返回的脱敏用户信息 */
export interface UserDTO {
  id: number
  userAccount: string
  userRole: AUTH
}

/** 登录 / 注册响应 */
export interface TokenVO {
  token: string
  userVO: UserDTO
}

/** 管理端用户列表项 */
export interface UserByAdminVO {
  id: number
  userAccount: string
  userRole: AUTH
  createdTime: string
  lastLoginTime: string
}

export const postUserLogin = (form: LoginForm) => {
  return request.post('/api/user/login', form)
}

export const postUserRegister = (form: LoginForm) => {
  return request.post('/api/user/register', form)
}

export const getUserLogout = () => {
  return request.get('/api/user/logout')
}

export const getUserStatus = () => {
  // 路由守卫会自己决定公开页/保护页的跳转，避免 401 拦截器把公开页踢到登录页
  return request.get('/api/user/status', { skipAuthRedirect: true })
}

export const getUserList = (current: number, pageSize: number) => {
  return request.get('/api/admin/user', {
    params: {
      current,
      pageSize
    }
  })
}

export const putUserUpdate = (id: number, form: LoginForm) => {
  return request.put(`/api/admin/user/${id}`, form)
}

export const delUser = (id: number) => {
  return request.delete(`/api/admin/user/${id}`)
}
