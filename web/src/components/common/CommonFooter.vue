<script lang="ts" setup>
const version = __APP_VERSION__
</script>

<template>
  <div id="CommonFooter">
    <a-divider />
    <span>WangOJ</span>
    <span class="sep">·</span>
    <span>基于 Apache-2.0 开源</span>
    <span class="sep">·</span>
    <span>v{{ version }}</span>
  </div>
</template>

<style scoped>
#CommonFooter {
  min-height: 20px;
  width: 80%;
  text-align: center;
  margin: 0 auto 20px;
  color: var(--color-text-3);
  font-size: 12px;
}
#CommonFooter .sep {
  margin: 0 6px;
}
</style>
