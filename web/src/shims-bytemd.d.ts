/**
 * 修正 @bytemd/vue-next 的类型声明。
 *
 * <p>该包把 .vue 组件作为值导出，但其 .d.ts 实际声明的是一个类型，
 * 在 verbatimModuleSyntax 下会被要求用 <code>import type</code> 引入，而运行期又必须作为值使用。
 * 这里显式声明为组件值，避免 TS1484 与"导入不存在"之间的两难。</p>
 */
declare module '@bytemd/vue-next' {
  import type { DefineComponent } from 'vue'

  export const Editor: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export const Viewer: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
}
