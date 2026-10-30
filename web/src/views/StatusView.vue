<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { type CommitlogDTO, getCommitDTOList } from '@/api/commitlog'
import { useRoute, useRouter } from 'vue-router'
import { IconRefresh } from '@arco-design/web-vue/es/icon'
import { useUserStore } from '@/stores/user'

const data = ref<CommitlogDTO[]>([])
const loading = ref(true)
const current = ref(1)
const pageSize = ref(20)
const total = ref(0)
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 只看自己的提交（提交成功后带 ?mine=1 进入，自动勾选） */
const onlyMine = ref(route.query.mine === '1')

/** 判题是异步的：存在 Waiting 时自动轮询，全部结束后停止 */
const POLL_INTERVAL_MS = 3000
const MAX_POLL_TIMES = 40
let pollTimer: ReturnType<typeof setTimeout> | null = null
let pollCount = 0

const hasWaiting = computed(() => data.value.some((item) => item.status === 'Waiting'))

const columns = [
  {
    title: '提交编号',
    dataIndex: 'id',
    width: 100
  },
  {
    title: '题目',
    slotName: 'problem'
  },
  {
    title: '语言',
    dataIndex: 'lang',
    width: 100
  },
  {
    title: '判题状态',
    slotName: 'status',
    width: 180
  },
  {
    title: '运行时间',
    slotName: 'timeoutLimit',
    width: 120
  },
  {
    title: '运行空间',
    slotName: 'memoryLimit',
    width: 120
  },
  {
    title: '提交时间',
    dataIndex: 'createdTime',
    width: 160
  },
  {
    title: '操作',
    slotName: 'optional',
    width: 150
  }
]

const stopPolling = () => {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

const schedulePolling = () => {
  stopPolling()
  if (!hasWaiting.value || pollCount >= MAX_POLL_TIMES) {
    return
  }
  pollCount += 1
  pollTimer = setTimeout(async () => {
    await getData(false)
  }, POLL_INTERVAL_MS)
}

const getData = async (showLoading = true) => {
  if (showLoading) {
    loading.value = true
  }
  try {
    const userId = onlyMine.value ? userStore.userId : undefined
    const res = await getCommitDTOList(current.value, pageSize.value, userId)
    const page = res.data?.data
    if (!page) {
      return
    }
    total.value = page.total
    current.value = page.current
    pageSize.value = page.size
    data.value = page.t || []
    schedulePolling()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number) => {
  current.value = page
  pollCount = 0
  getData()
}

const onOnlyMineChange = () => {
  if (onlyMine.value && !userStore.userId) {
    onlyMine.value = false
    return
  }
  current.value = 1
  pollCount = 0
  router.replace({ path: '/status', query: onlyMine.value ? { mine: '1' } : {} })
  getData()
}

const toSubmission = (id: number) => {
  router.push(`/submission?id=${id}`)
}

const refresh = () => {
  pollCount = 0
  getData()
}

onMounted(() => {
  getData()
})

onUnmounted(stopPolling)
</script>

<template>
  <div id="StatusView">
    <h1 class="title">提交记录</h1>
    <a-row style="margin-bottom: 15px;" align="center">
      <a-col :flex="1">
        <a-checkbox v-model="onlyMine" :disabled="!userStore.userId" @change="onOnlyMineChange">
          只看我的提交
        </a-checkbox>
      </a-col>
      <a-col>
        <a-button type="primary" :loading="loading" @click="refresh">
          <template #icon>
            <icon-refresh />
          </template>
          刷新
        </a-button>
      </a-col>
    </a-row>

    <a-table :columns="columns" :data="data" :loading="loading" :pagination="false">
      <template #problem="{ record }">
        <a-link :href="`/code?id=${record.problemId}`">
          {{ `${record.problemId + '. ' + record.problemTitle}` }}
        </a-link>
      </template>
      <template #status="{ record }">
        <a-tag v-if="record.status === 'Accept'" color="#00b42a">{{ record.status }}</a-tag>
        <a-tag v-else-if="record.status === 'Waiting'" color="#165dff">{{ record.status }}</a-tag>
        <a-tag v-else-if="record.status === 'System Error'" color="#ff7d00">{{ record.status }}</a-tag>
        <a-tag v-else color="#f53f3f">{{ record.status }}</a-tag>
      </template>
      <template #timeoutLimit="{ record }">
        {{ record.timeoutLimit ? `${record.timeoutLimit} ms` : '-' }}
      </template>
      <template #memoryLimit="{ record }">
        {{ record.memoryLimit ? `${Math.round(record.memoryLimit / 1024)} KB` : '-' }}
      </template>
      <template #optional="{ record }">
        <a-button shape="round" type="primary" @click="toSubmission(record.id)">查看代码</a-button>
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
          @change="onPageChange"
        />
      </a-space>
    </div>
  </div>
</template>

<style scoped>
#StatusView {
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
