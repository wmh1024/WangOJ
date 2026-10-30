<script lang="ts" setup>
import 'bytemd/dist/index.css'
import { ref } from 'vue'
import { Editor } from '@bytemd/vue-next'
import zhHans from 'bytemd/lib/locales/zh_Hans.json'

const plugins = ref()

/** 草稿内容只在本组件内维护，通过 get-content 事件交给父组件 */
const content = ref<string>('')

const emit = defineEmits<{ (e: 'get-content', value: string): void }>()
const handleChange = (v: string) => {
  content.value = v
  emit('get-content', v)
}
</script>

<template>
  <div id="MarkdownEditor">
    <Editor :locale="zhHans" :plugins="plugins" :value="content" @change="handleChange" />
  </div>
</template>

<style scoped>
:deep(.bytemd-toolbar-icon):last-child,
:deep(.bytemd-toolbar-icon):nth-child(5) {
  display: none;
}

#MarkdownEditor {
  width: 100%;
  height: 100%;
}
</style>
