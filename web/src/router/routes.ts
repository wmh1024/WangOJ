import { AUTH } from '@/utils/constants'

export const routes = [
  {
    path: '/',
    name: '基础布局',
    component: () => import('@/layout/BaseLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: '/home',
        name: '主页',
        component: () => import('@/views/find/index.vue')
      },
      {
        path: '/problem',
        name: '题库',
        component: () => import('@/views/ProblemView.vue')
      },
      {
        path: '/status',
        name: '提交记录',
        component: () => import('@/views/StatusView.vue')
      },
      {
        path: '/create',
        name: '创建题目',
        component: () => import('@/views/CreateView.vue'),
        meta: {
          auth: AUTH.ADMIN
        }
      },
      {
        path: '/manage',
        name: '题目管理',
        component: () => import('@/views/ManageView.vue'),
        meta: {
          auth: AUTH.ADMIN
        }
      },
      {
        path: '/user',
        name: '用户管理',
        component: () => import('@/views/UserManageView.vue'),
        meta: {
          auth: AUTH.ADMIN
        }
      },
      {
        path: '/submission',
        name: '代码查看',
        component: () => import('@/views/SubmissionView.vue'),
        meta: {
          hide: true
        }
      },
      {
        path: '/userinfo',
        name: '个人信息',
        component: () => import('@/views/UserInfoView.vue'),
        meta: {
          hide: true
        }
      },
      {
        path: '/setting',
        name: '个人设置',
        component: () => import('@/views/UserSettingsView.vue'),
        meta: {
          hide: true
        }
      },
      {
        path: '/403',
        name: '403',
        component: () => import('@/views/exception/403.vue'),
        meta: {
          hide: true
        }
      },
      {
        path: '/404',
        name: '404',
        component: () => import('@/views/exception/404.vue'),
        meta: {
          hide: true
        }
      },
      {
        path: '/500',
        name: '500',
        component: () => import('@/views/exception/500.vue'),
        meta: {
          hide: true
        }
      }
    ]
  },
  {
    path: '/code',
    name: '代码编辑',
    component: () => import('@/layout/CodeLayout.vue'),
    meta: {
      auth: AUTH.USER
    }
  },
  {
    path: '/login',
    name: '登录页',
    component: () => import('@/layout/LoginLayout.vue')
  }
]
