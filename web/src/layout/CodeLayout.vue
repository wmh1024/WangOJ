<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { Message, Notification } from '@arco-design/web-vue'
import MarkdownViewer from '@/components/editor/MarkdownViewer.vue'
import CodeEditor from '@/components/editor/CodeEditor.vue'
import { IconArrowLeft, IconRobot, IconSend } from '@arco-design/web-vue/es/icon'
import type { DescData } from '@arco-design/web-vue'
import { useRoute, useRouter } from 'vue-router'
import { getProblemContentById } from '@/api/problem'
import type { CommitlogVO } from '@/api/commitlog'
import { commitCode } from '@/api/commitlog'
import { aiCode, type AiCodeVO } from '@/api/ai'
import { useUserStore } from '@/stores/user'

/**
 * 可选语言，value 必须与后端/沙箱约定一致
 */
const LANGUAGE_OPTIONS = [
  { label: 'C++', value: 'c++', ace: 'c_cpp' },
  { label: 'C', value: 'c', ace: 'c_cpp' },
  { label: 'Java', value: 'java', ace: 'java' },
  { label: 'Python', value: 'python', ace: 'python' },
  { label: 'JavaScript', value: 'javascript', ace: 'javascript' },
  { label: 'Golang', value: 'golang', ace: 'golang' }
]

const size = ref(0.5)

const limitOptionFormat = ref<DescData[]>()

const title = ref('')
const tags = ref([])
const content = ref('')
const lang = ref('c++')

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const problemID = ref<string>('')

onMounted(async () => {
  const queryId = route.query['id']
  problemID.value = typeof queryId === 'string' ? queryId : ''
  const numericId = Number.parseInt(problemID.value)
  if (!Number.isFinite(numericId)) {
    Message.warning('缺少题目 ID')
    router.replace('/problem')
    return
  }
  let problem
  try {
    const res = await getProblemContentById(numericId)
    problem = res.data?.data
  } catch {
    Message.error('题目加载失败')
    return
  }
  if (!problem) {
    Message.error('题目不存在')
    router.replace('/problem')
    return
  }
  tags.value = problem.tags || []
  limitOptionFormat.value = [
    {
      label: '时间限制(ms)',
      value: problem.limitOption?.timeoutLimit ?? '-'
    },
    {
      label: '内存限制(MB)',
      value: problem.limitOption?.memoryLimit ?? '-'
    },
    {
      label: '总通过数',
      value: problem.acceptNum ?? 0
    },
    {
      label: '总提交数',
      value: problem.commitNum ?? 0
    }
  ]
  title.value = problem.title || ''
  content.value = problem.content || ''
})

const code = ref('')
const submitting = ref(false)

const getCode = (content: string) => {
  code.value = content
}

const doCommitCode = async () => {
  if (submitting.value) {
    return
  }
  if (!code.value || !code.value.trim()) {
    Message.warning('代码不能为空')
    return
  }
  const problemId = Number.parseInt(String(problemID))
  if (!Number.isFinite(problemId)) {
    Message.error('题目 ID 不合法')
    return
  }
  const commitlogVO: CommitlogVO = {
    code: code.value,
    problemId,
    lang: lang.value
  }
  submitting.value = true
  try {
    await commitCode(commitlogVO)
    Message.success('提交成功，正在判题')
    // 只过滤自己的提交，避免在公共列表里找自己
    router.push({ path: '/status', query: userStore.userId ? { mine: '1' } : {} })
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

// 添加新的响应式变量
const aiPrompt = ref('')
const aiResponse = ref('')
const isLoading = ref(false)
// 添加 AI 优化方法
const handleAiOptimize = async () => {
  if (isLoading.value) {
    return
  }
  if (!aiPrompt.value || !code.value) {
    Message.warning('请先填写优化需求并编写代码')
    return
  }
  const problemId = Number.parseInt(String(problemID))
  if (!Number.isFinite(problemId)) {
    Message.error('题目 ID 不合法')
    return
  }
  isLoading.value = true
  try {
    Notification.info('AI请求已提交！')
    const aiCodeVO: AiCodeVO = {
      problemId,
      code: code.value,
      aiPrompt: aiPrompt.value
    }
    const res = await aiCode(aiCodeVO)
    // 确保响应内容是 markdown 格式
    aiResponse.value = res.data.data
    Notification.success('代码解释已生成！')
  } catch {
    Notification.error('生成失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div id="CodeLayout">
    <a-split
      v-model:size="size"
      :style="{
        height: '100%',
        width: '100%',
        minWidth: '500px',
        border: '1px solid var(--color-border)'
      }"
      min="80px"
    >
      <!-- 左侧面板 -->
      <template #first>
        <div class="problem-panel">
          <a-card :style="{ height: '100%' }">
            <template #title>
              <a-space>
                <a-button status="warning" style="width: 70px" @click="router.back()">
                  <template #icon>
                    <icon-arrow-left />
                  </template>
                  返回
                </a-button>
                {{ title }}
              </a-space>
            </template>
            <template #extra>
              <a-space>
                <a-tag v-for="(item, idx) in tags" :key="idx" color="blue">{{ item }}</a-tag>
              </a-space>
            </template>
            <div class="problem-content">
              <a-descriptions
                :column="2"
                :data="limitOptionFormat"
                size="large"
                style="margin-bottom: 16px"
                title=""
              />
              <MarkdownViewer :content="content" />
            </div>
          </a-card>
        </div>
      </template>

      <!-- 右侧面板 -->
      <template #second>
        <a-split
          direction="vertical"
          :default-size="0.7"
          :style="{ height: '100%' }"
        >
          <template #first>
            <div class="code-panel">
              <a-card :style="{ height: '100%' }" title="代码">
                <template #extra>
                  <a-space>
                    <a-button
                      status="success"
                      type="primary"
                      :loading="submitting"
                      @click="doCommitCode"
                    >
                      提交代码
                    </a-button>
                    <a-select v-model="lang" style="width: 140px">
                      <a-option
                        v-for="item in LANGUAGE_OPTIONS"
                        :key="item.value"
                        :value="item.value"
                      >
                        {{ item.label }}
                      </a-option>
                    </a-select>
                  </a-space>
                </template>
                <CodeEditor
                  :lang="LANGUAGE_OPTIONS.find((i) => i.value === lang)?.ace || 'c_cpp'"
                  @get-code="getCode"
                />
              </a-card>
            </div>
          </template>
          <template #second>
            <div class="output-panel">
              <a-card :style="{ height: '100%' }" title="AI优化">
                <div class="ai-panel">
                  <div class="ai-input-wrapper">
                    <a-input
                      v-model="aiPrompt"
                      placeholder="请描述你的代码优化需求..."
                      :style="{ backgroundColor: 'var(--color-fill-2)' }"
                      allow-clear
                    >
                      <template #prefix>
                        <icon-robot />
                      </template>
                      <template #append>
                        <a-button
                          type="primary"
                          status="success"
                          :loading="isLoading"
                          @click="handleAiOptimize"
                          
                        >
                          <template #icon>
                            <icon-send />
                          </template>
                          优化
                        </a-button>
                      </template>
                    </a-input>
                  </div>
                  <div class="ai-response-wrapper">
                    <div v-if="aiResponse" class="ai-response">
                      <div class="response-header">
                        <icon-robot />
                        <span>AI 优化建议</span>
                      </div>
                      <div class="response-content">
                        <MarkdownViewer :content="aiResponse" />
                      </div>
                    </div>
                    <div v-else class="empty-state">
                      <icon-robot :style="{ fontSize: '32px', marginBottom: '8px' }" />
                      <div class="empty-text">AI 助手将帮助你优化代码</div>
                    </div>
                  </div>
                </div>
              </a-card>
            </div>
          </template>
        </a-split>
      </template>
    </a-split>
  </div>
</template>

<style scoped>
#CodeLayout {
  height: 100vh;
  overflow: hidden;
}

.problem-panel,
.code-panel,
.output-panel {
  height: 100%;
  overflow: hidden;
}

.problem-content {
  padding: 16px;
  height: 100%;
  overflow-y: auto;
}

:deep(.arco-card) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

:deep(.arco-card-body) {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

/* 设置滚动条样式 */
.problem-content::-webkit-scrollbar {
  width: 6px;
}

.problem-content::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.problem-content::-webkit-scrollbar-track {
  background-color: transparent;
}

.ai-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  gap: 16px;
}

.ai-input-wrapper :deep(.arco-input-wrapper) {
  background-color: var(--color-fill-2);
  border: 1px solid var(--color-neutral-3);
  transition: all 0.2s ease;
}

.ai-input-wrapper :deep(.arco-input-wrapper:hover) {
  border-color: rgb(var(--success-6));
  background-color: var(--color-fill-3);
}

.ai-response-wrapper {
  flex: 1;
  overflow: hidden;
  border-radius: 8px;
  background: var(--color-fill-2);
}

.ai-response {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.response-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-neutral-3);
  color: var(--color-text-2);
  font-weight: 500;
}

.response-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  line-height: 1.6;
  color: var(--color-text-2);
}

/* 调整 markdown 查看器的样式 */
.response-content :deep(.markdown-body) {
  background: transparent;
  padding: 0;
}

/* 确保代码块样式正确 */
.response-content :deep(pre) {
  background-color: var(--color-fill-1);
  border-radius: 4px;
  margin: 8px 0;
}

/* 调整链接颜色 */
.response-content :deep(a) {
  color: rgb(var(--primary-6));
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-3);
}

.empty-text {
  margin-top: 8px;
  font-size: 14px;
}

/* 美化滚动条 */
.response-content::-webkit-scrollbar {
  width: 4px;
}

.response-content::-webkit-scrollbar-thumb {
  background-color: var(--color-neutral-4);
  border-radius: 2px;
}

.response-content::-webkit-scrollbar-track {
  background-color: transparent;
}

:deep(.arco-btn-success) {
  background-color: rgb(var(--success-6));
}

:deep(.arco-btn-success:hover) {
  background-color: rgb(var(--success-7));
}
:deep(.arco-input-append) {
  background-color: #fff;
}
</style>
