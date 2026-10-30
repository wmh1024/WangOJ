<!--
  本文件的卡片与列表样式参考自 Arco Design Pro
  （https://github.com/arco-design/arco-design-pro-vue）。
  Copyright (c) 2021 Bytedance, Inc. and its affiliates，MIT License；
  许可证全文见 web/LICENSE-ARCO-PRO，归属说明见仓库根目录 NOTICE。
-->
<template>
  <a-card class="general-card" title="排行榜">

    <div style="text-align: center;">AC：通过题目数，ANS：AC 次数</div>
    <a-list :max-height="300" style="margin-top: 10px;">
      <a-list-item v-for="(item, idx) of data" :key="idx">
        <a-tag color="arcoblue" style="margin-right: 3px;">{{ idx + 1 }}</a-tag>
        {{ item.name }}
        <template #actions>
          {{ `AC: ${item.ac} | ANS: ${item.ans}` }}
        </template>
      </a-list-item>
    </a-list>
  </a-card>
</template>

<script setup lang="ts">

import { onMounted, ref } from 'vue'
import { getRankList, type RankItem } from '@/api/commitlog'

const data = ref<RankItem[]>([])
onMounted(async () => {
  try {
    const res = await getRankList()
    data.value = res.data?.data || []
  } catch {
    data.value = []
  }
})


</script>

<style scoped lang="less">
.general-card {
  border-radius: 10px;
}

.a-link {
  color: #4e5969;
}
</style>
