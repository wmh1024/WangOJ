/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端 API 前缀；留空表示同源相对路径 */
  readonly VITE_API_BASE_URL?: string
  /** 开发环境代理目标（仅 vite dev server 使用） */
  readonly VITE_PROXY_TARGET?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

/**
 * 让 .vue 单文件组件在 tsconfig.app.json（未引入 vue-tsc 的 shim 时）也能被类型系统识别。
 * 使用泛型返回值，避免组件被推断成 never 导致模板内的属性访问报错。
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<object, object, unknown>
  export default component
}

declare const __APP_VERSION__: string
