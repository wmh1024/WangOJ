<script lang="ts" setup>
import { RouterView } from 'vue-router'
import BaseHeader from '@/components/base_layout/BaseHeader.vue'
import CommonFooter from '@/components/common/CommonFooter.vue'
</script>

<template>
  <div id="BaseLayout">
    <a-layout style="height: 400px">
      <a-affix>
        <a-layout-header style="min-height: 50px">
          <BaseHeader />
        </a-layout-header>
      </a-affix>
      <a-layout-content>
        <RouterView />
      </a-layout-content>
      <a-layout-footer>
        <CommonFooter />
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
.layout-demo :deep(.arco-layout-header),
.layout-demo :deep(.arco-layout-footer),
.layout-demo :deep(.arco-layout-sider-children),
.layout-demo :deep(.arco-layout-content) {
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: var(--color-white);
  font-size: 16px;
  font-stretch: condensed;
  text-align: center;
}

.layout-demo :deep(.arco-layout-header),
.layout-demo :deep(.arco-layout-footer) {
  height: 64px;
  background-color: var(--color-primary-light-4);
}

.layout-demo :deep(.arco-layout-sider) {
  width: 206px;
  background-color: var(--color-primary-light-3);
}

.layout-demo :deep(.arco-layout-content) {
  background-color: rgb(var(--arcoblue-6));
}
</style>
