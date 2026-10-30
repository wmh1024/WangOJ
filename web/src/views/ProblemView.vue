<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { getProblemDTOList, type ProblemQuery } from '@/api/problem'
import { useRouter } from 'vue-router'
import { IconRefresh, IconSearch } from '@arco-design/web-vue/es/icon'

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
const current = ref(1)
const pageSize = ref(10)
const total = ref(0)

const problemQuery = ref({
  id: undefined,
  title: '',
  content: '',
  tags: []
} as ProblemQuery)

const getData = async () => {
  const res = await getProblemDTOList(current.value, pageSize.value, problemQuery.value)
  total.value = res.data.data.total
  current.value = res.data.data.current
  pageSize.value = res.data.data.size
  data.value = res.data.data.t
  loading.value = false
}

onMounted(() => {
  getData()
})

const router = useRouter()

const doCode = (id: string) => {
  router.push(`/code?id=${id}`)
}

const search = () => {
  current.value = 1
  getData()
}

const reset = () => {
  problemQuery.value.id = undefined
  problemQuery.value.title = ''
  problemQuery.value.content = ''
  problemQuery.value.tags = []
  current.value = 1
  getData()
}
</script>

<template>
  <div id="ProblemView">
    <h1 class="title">题目列表</h1>
    <a-row>
      <a-col :flex="1">
        <a-form
          :model="problemQuery"
          :label-col-props="{ span: 6 }"
          :wrapper-col-props="{ span: 18 }"
          label-align="right"
        >
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item field="number" label="序号">
                <a-input-number v-model="problemQuery.id" placeholder="" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item field="name" label="题目">
                <a-input v-model="problemQuery.title" placeholder="" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item field="name" label="标签">
                <a-input-tag v-model="problemQuery.tags" placeholder="" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-col>
      <a-divider style="height: 42px" direction="vertical" />
      <a-col :flex="'172px'" style="text-align: right">
        <a-space :size="9">
          <a-button type="primary" @click="search">
            <template #icon>
              <IconSearch />
            </template>
            搜索
          </a-button>
          <a-button @click="reset">
            <template #icon>
              <IconRefresh />
            </template>
            重置
          </a-button>
        </a-space>
      </a-col>
    </a-row>
    <a-divider style="margin-top: 0" />
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
    <div class="pagination">
      <a-space direction="vertical" size="large" style="margin-top: 30px">
        <a-pagination
          :current="current"
          :page-size="pageSize"
          :total="total"
          show-jumper
          show-total
          size="medium"
          @change="(page: number) => {
            current = page
            getData()
          }"
        />
      </a-space>
    </div>
  </div>
</template>

<style scoped>
#ProblemView {
  width: 80%;
  margin: 0 auto;
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
