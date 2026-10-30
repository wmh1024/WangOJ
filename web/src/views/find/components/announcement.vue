<!--
  本文件改编自 Arco Design Pro
  （https://github.com/arco-design/arco-design-pro-vue，
  src/views/dashboard/workplace/components/announcement.vue）。
  Copyright (c) 2021 Bytedance, Inc. and its affiliates，MIT License；
  许可证全文见 web/LICENSE-ARCO-PRO，归属说明见仓库根目录 NOTICE。
-->
<template>
  <a-card class="general-card" hoverable title="公告">
    <a-scrollbar style="height: 250px; overflow: auto">
      <a-space direction="vertical">
        <div v-for="(item, idx) in list" :key="idx" class="item">
          <a-space style="margin-top: 5px;">
            <a-tag :color="item.type" size="small">{{ item.label }}</a-tag>
            <a-link style="color: #4e5969">
              {{ item.content }}
            </a-link>
          </a-space>
        </div>
      </a-space>
    </a-scrollbar>
  </a-card>
</template>

<script setup lang="ts">
const list = [
  {
    type: 'magenta',
    label: 'slogan',
    content: '🧑‍💻 Talk is easy, show me the code.'
  }
];
</script>

<style scoped lang="less">
.general-card {
  height: 350px;
  border-radius: 10px;
}

.general-card:hover {
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
}
</style>
