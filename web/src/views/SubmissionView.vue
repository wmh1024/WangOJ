<script setup lang="ts">
import MarkdownViewer from '@/components/editor/MarkdownViewer.vue'
import { computed, onMounted, ref } from 'vue'
import { aiCode, type AiCodeVO } from '@/api/ai'
import { Notification } from '@arco-design/web-vue'
import { useRoute, useRouter } from 'vue-router'
import { getCommitlogById, type CaseOutcome, type CommitlogDetail } from '@/api/commitlog'
import { getProblemContentById } from '@/api/problem'

const route = useRoute()
const router = useRouter()

const commitLog = ref<CommitlogDetail | null>(null)
const mdContent = ref('')
const aiContent = ref('')
const codeContent = ref('')
const useAi = ref(false)
const aiLoading = ref(false)

/** 逐测试点结果（后端以 JSON 字符串落库） */
const caseOutcomes = computed<CaseOutcome[]>(() => {
  const raw = commitLog.value?.caseResult
  if (!raw) {
    return []
  }
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? (parsed as CaseOutcome[]) : []
  } catch {
    return []
  }
})

const descriptionsData = computed(() => {
  const log = commitLog.value
  if (!log) {
    return [] as { label: string; value: string }[]
  }
  return [
    { label: '语言', value: log.lang || '-' },
    { label: '运行时间', value: log.timeoutLimit ? `${log.timeoutLimit} ms` : '-' },
    { label: '内存占用', value: log.memoryLimit ? `${Math.round(log.memoryLimit / 1024)} KB` : '-' },
    { label: '判题状态', value: log.status || '-' },
    { label: '提交时间', value: log.createdTime || '-' }
  ]
})

const descriptionsTitle = computed(() =>
  commitLog.value ? `user-${commitLog.value.userId} 提交的代码` : ''
)

const title = computed(() => {
  const log = commitLog.value
  if (!log) {
    return ''
  }
  return `${log.problemId}. ${log.problemTitle || ''}`
})

const statusColor = (status?: string) => {
  if (status === 'Accept') return '#00b42a'
  if (status === 'Waiting') return '#165dff'
  if (status === 'System Error') return '#ff7d00'
  return '#f53f3f'
}

const submitAiCode = async () => {
  const log = commitLog.value
  if (!log || !log.code) {
    Notification.warning('没有可解释的代码')
    return
  }
  if (aiLoading.value) {
    return
  }
  aiLoading.value = true
  Notification.info('AI 请求已提交')
  const aiCodeVO: AiCodeVO = {
    problemId: log.problemId,
    code: log.code
  }
  try {
    const res = await aiCode(aiCodeVO)
    aiContent.value = res.data?.data || ''
    useAi.value = true
    Notification.success('代码解释已生成')
  } catch {
    Notification.error('AI 生成失败，请稍后重试')
  } finally {
    aiLoading.value = false
  }
}

/**
 * 加载题面。失败不阻断页面（代码与判题结果照常展示）。
 */
const loadProblemContent = async (problemId?: number) => {
  if (!problemId) {
    return
  }
  try {
    const res = await getProblemContentById(problemId)
    mdContent.value = res.data?.data?.content || ''
  } catch {
    mdContent.value = ''
  }
}

onMounted(async () => {
  const id = Number.parseInt(String(route.query['id']))
  if (!Number.isFinite(id)) {
    Notification.error('提交记录 ID 不合法')
    router.replace('/status')
    return
  }
  try {
    const res = await getCommitlogById(id)
    const log: CommitlogDetail | undefined = res.data?.data
    if (!log) {
      Notification.error('提交记录不存在')
      router.replace('/status')
      return
    }
    commitLog.value = log
    // 题面不再随提交详情下发，单独按 problemId 取（题面接口有缓存，同一题多次查看只请求一次）
    await loadProblemContent(log.problemId)
    const fence = String.fromCharCode(96, 96, 96)
    // 代码里如果本身包含 ``` 会破坏 Markdown 围栏，替换成视觉相同的安全序列
    const safeCode = (log.code || '').split(fence).join('``\u200b`')
    codeContent.value = [fence, log.lang || '', safeCode, fence].join(String.fromCharCode(10))
  } catch {
    Notification.error('提交记录加载失败')
  }
})
</script>

<template>
  <div id="SubmissionView">
    <h2 style="text-align:center;">{{ title }}</h2>
    <div style="text-align:center;">
      <a-space>
        <a-tag v-for="(item, idx) in commitLog?.problemTags || []" :key="idx" color="blue">
          {{ item }}
        </a-tag>
      </a-space>
    </div>
    <MarkdownViewer :content="mdContent"></MarkdownViewer>
    <a-divider />
    <a-descriptions :title="descriptionsTitle">
      <a-descriptions-item v-for="(item, index) in descriptionsData" :label="item.label" :key="index">
        <a-tag v-if="item.label === '判题状态'" :color="statusColor(item.value)">{{ item.value }}</a-tag>
        <span v-else>{{ item.value }}</span>
      </a-descriptions-item>
    </a-descriptions>

    <a-alert
      v-if="commitLog?.errorMessage"
      type="error"
      :title="commitLog.status"
      style="margin: 16px 0"
    >
      <pre class="error-output">{{ commitLog.errorMessage }}</pre>
    </a-alert>

    <a-table
      v-if="caseOutcomes.length"
      :data="caseOutcomes"
      :pagination="false"
      size="small"
      style="margin: 16px 0"
    >
      <template #columns>
        <a-table-column title="测试点" data-index="index" :width="100" />
        <a-table-column title="结果" :width="200">
          <template #cell="{ record }">
            <a-tag :color="record.accepted ? '#00b42a' : '#f53f3f'">{{ record.verdict }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="耗时" :width="120">
          <template #cell="{ record }">
            {{ record.timeMs == null ? '-' : `${record.timeMs} ms` }}
          </template>
        </a-table-column>
        <a-table-column title="内存" :width="120">
          <template #cell="{ record }">
            {{ record.memoryBytes == null ? '-' : `${Math.round(record.memoryBytes / 1024)} KB` }}
          </template>
        </a-table-column>
      </template>
    </a-table>

    <a-space>
      <a-button
        type="primary"
        :disabled="!commitLog"
        @click="router.push('/code?id=' + (commitLog?.problemId ?? ''))"
      >
        前往挑战
      </a-button>
      <a-button type="primary" status="success" :loading="aiLoading" @click="submitAiCode">
        AI 解释代码
      </a-button>
    </a-space>
    <a-space direction="vertical" :size="16" style="display: block; margin-top: 16px">
      <MarkdownViewer v-if="!useAi" :content="codeContent"></MarkdownViewer>
      <a-row v-else class="grid-demo" :gutter="{ md: 8, lg: 24, xl: 32 }">
        <a-col :span="12">
          <MarkdownViewer :content="codeContent"></MarkdownViewer>
        </a-col>
        <a-col :span="12">
          <MarkdownViewer style="width: 100%" :content="aiContent"></MarkdownViewer>
        </a-col>
      </a-row>
    </a-space>
  </div>
</template>

<style scoped>
#SubmissionView {
  width: 70%;
  margin: 0 auto;
}

.error-output {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
}
</style>
