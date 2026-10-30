<!--
  本文件的卡片与列表样式参考自 Arco Design Pro
  （https://github.com/arco-design/arco-design-pro-vue）。
  Copyright (c) 2021 Bytedance, Inc. and its affiliates，MIT License；
  许可证全文见 web/LICENSE-ARCO-PRO，归属说明见仓库根目录 NOTICE。
-->
<template>
  <div id="ProblemTable">
    <a-card class="general-card" title="热门题目">
    <a-table :columns="columns" :data="data" :loading="loading" :pagination="false">
      <template #tags="{ record }">
        <a-space>
          <a-tag v-for="(item, idx) in record.tags" :key="idx" color="blue">{{ item }}</a-tag>
        </a-space>
      </template>
      <template #acceptPercent="{ record }">
        <a-progress
          :percent="
            Math.ceil(record.commitNum !== 0 ? (record.acceptNum * 100) / record.commitNum : 0) /
            100
          "
          size="large"
          style="width: 100px"
        />
      </template>
      <template #AcAndCommit="{ record }">
        {{ `${record.acceptNum} / ${record.commitNum}` }}
      </template>
      <template #optional="{ record }">
        <a-button shape="round" status="success" type="primary" @click="doCode(record.id)"
        >前往挑战
        </a-button>
      </template>
    </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHotProblemList } from '@/api/problem'
import { useRouter } from 'vue-router'

const columns = [
  {
    title: '编号',
    dataIndex: 'id',
    width: 100
  },
  {
    title: '题目',
    dataIndex: 'title'
  },
  {
    title: '标签',
    slotName: 'tags'
  },
  {
    title: '通过/提交',
    slotName: 'AcAndCommit'
  },
  {
    title: '通过率',
    slotName: 'acceptPercent'
  },
  {
    title: '操作',
    slotName: 'optional',
    width: 150
  }
]

const data = ref([])
const loading = ref(true)

const getData = async () => {
  const res = await getHotProblemList()
  data.value = res.data.data
  loading.value = false
}

onMounted(() => {
  getData()
})

const router = useRouter()

const doCode = (id: string) => {
  router.push(`/code?id=${id}`)
}

</script>

<style scoped lang="less">
#ProblemTable {
  padding-top: 20px;
}
.problem {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--color-bg-2);

  &-form {
    flex: 1;
    margin: 20px 0;
  }

  &-table {
    flex: 1;
    margin: 20px 0;
  }
}

.title {
  text-align: center;
  margin: 30px 0;
}

.pagination {
  display: flex;
  justify-content: center;
}
</style>
