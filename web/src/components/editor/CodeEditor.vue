<script lang="ts" setup>
import { VAceEditor } from 'vue3-ace-editor'
import { reactive, ref } from 'vue'
import { Ace } from 'ace-builds'
import '@/utils/aceConfig.js'

const emit = defineEmits(['get-code'])

const props = defineProps<{
  lang?: string
}>()

const content = ref('')

const onChange = () => {
  if (emit) {
    emit('get-code', content.value)
  }
}

const options: Partial<Ace.EditorOptions> = reactive({
  useWorker: true, // 启用语法检查,必须为true
  enableBasicAutocompletion: true, // 自动补全
  enableLiveAutocompletion: true, // 智能补全
  enableSnippets: true, // 启用代码段
  showPrintMargin: false, // 去掉灰色的线，printMarginColumn
  highlightActiveLine: true, // 高亮行
  highlightSelectedWord: true, // 高亮选中的字符
  tabSize: 4, // tab锁进字符
  fontSize: 16, // 设置字号
  wrap: false, // 是否换行
  readonly: false // 是否可编辑
})
</script>

<template>
  <div id="CodeEditor">
    <v-ace-editor
      v-model:value="content"
      :options="options"
      :lang="props.lang || 'c_cpp'"
      style="height: 100%"
      theme="chrome"
      @change="onChange"
    />
  </div>
</template>

<style scoped>
#CodeEditor {
  position: absolute;
  width: 100%;
  height: 100%;
}
</style>
