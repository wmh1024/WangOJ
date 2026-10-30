<script lang="ts" setup>
import 'bytemd/dist/index.css'
import highlight from '@bytemd/plugin-highlight'
import { Viewer } from '@bytemd/vue-next'
import zhHans from 'bytemd/lib/locales/zh_Hans.json'
import 'highlight.js/styles/default.css'
import { ref } from 'vue'
import gfm from '@bytemd/plugin-gfm'

withDefaults(
  defineProps<{
    content?: string
  }>(),
  {
    content: ''
  }
)

const plugins = ref([highlight(), gfm()])
</script>

<template>
  <div id="MarkdownEditor">
    <Viewer :locale="zhHans" :plugins="plugins" :value="content" />
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
