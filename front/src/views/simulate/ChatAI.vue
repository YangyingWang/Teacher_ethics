<script setup lang="ts">
import type { BubbleListProps, ConversationsProps, PromptsProps } from 'ant-design-x-vue'
import type { VNode } from 'vue'
import { CommentOutlined, FireOutlined, HeartOutlined, DeleteOutlined, EditOutlined, UserOutlined,
        PlusOutlined, ReadOutlined, SmileOutlined, TrophyOutlined,  PlayCircleOutlined, SyncOutlined, LoadingOutlined, 
        CheckCircleFilled, CheckCircleOutlined, FileTextOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { Select, Button, Flex, Space, Spin, Typography, theme, Progress, Tag, InputNumber, Input, Modal} from 'ant-design-vue'
import { Bubble, Conversations, Prompts, Sender, Welcome} from 'ant-design-x-vue'
import { ElMessage } from 'element-plus'

import { computed, h, ref, onMounted, watch } from 'vue'
import { sessionListService, createSessionService, deleteSessionService, updateSessionTitleService, messageListService, sendChatService, ensureEvaluationService} from '@/api/simulate.js'
import { sceneCategoriesService } from '@/api/test.js'
import { useRouter, useRoute } from 'vue-router'
const router = useRouter()
const route = useRoute()

import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
const md = new MarkdownIt({
  html: false,      // 重要：不允许 Markdown 内嵌 HTML，降低风险
  linkify: true,
  breaks: true,     // 换行更符合聊天显示
})

const { token } = theme.useToken()
const styles = computed(() => {
  return {
    'layout': {
      'width': '100%',
      'height': '100%',
      'margin': '0',
      'display': 'flex',
      'background': `${token.value.colorBgContainer}`,
      'font-family': `AlibabaPuHuiTi, ${token.value.fontFamily}, sans-serif`,
    },
    'menu': {
      'background': `${token.value.colorBgLayout}80`,
      'width': '20%',
      'display': 'flex',
      'flex-direction': 'column',
    },
    'logo': {
      'display': 'flex',
      'height': '72px',
      'align-items': 'center',
      'justify-content': 'center',
    },
    'logo-span': {
      'display': 'inline-block',
      'font-weight': 'bold',
      'color': token.value.colorText,
      'font-size': '20px',
    },
    'addBtn': {
      'background': '#1677ff0f',
      'border': '1px solid #1677ff34',
      'width': 'calc(100% - 24px)',
      'margin': '0 12px 24px 12px',
    },
    'conversations': {
      'padding': '0 12px',
      'flex': 1,
      'overflow-y': 'auto',
    },
    'chat': {
      'height': '100%',
      'width': '100%',
      'max-width': '700px',
      'margin': '0 auto',
      'box-sizing': 'border-box',
      'display': 'flex',
      'flex-direction': 'column',
      'padding': `${token.value.paddingLG}px`,
      'gap': '16px',
    },
    'messages': {
      'flex': 1,
    },
    'placeholder': {
      'padding-top': '32px',
      'text-align': 'left',
      'flex': 1,
    },
    'sender': {
      'box-shadow': token.value.boxShadow,
    },
  } as const
})

//辅助函数，生成带有图标和标题的元素，返回一个 VNode，用于自定义渲染
function renderTitle(icon: VNode, title: string) {
  return h(Space, { align: 'start' }, [icon, h('span', title)])
}

function renderMarkdown(src: string) {
  const raw = md.render(src ?? '')
  const safe = DOMPurify.sanitize(raw) // 重要：再做一次清洗
  return h('div', { class: 'md-content', innerHTML: safe })
}

// ==================== 类型定义 ====================
interface Conversation {
  key: string
  label: string
  status?: 'active' | 'completed' | 'pending'
  phase?: 'init' | 'dialogue' | 'evaluating' | 'completed'
  step?: number
  maxSteps?: number
  group?: string
  timestamp?: number
}
interface UiMsg {
  id: string
  role: 'local' | 'ai'
  content: string
}
// ==================== Prompts ====================
const placeholderPromptsItems: PromptsProps['items'] = [{
    key: '1',
    label: renderTitle(h(FireOutlined, { style: { color: '#FF4D4F' } }), '常见师德困境'),
    description: '选择或输入一个主题',
    children: [{ key: '1-1', description: `网络舆情应对?`},
               { key: '1-2', description: `职称评审争议?`},
               { key: '1-3', description: `师生关系紧张?`}]
  },{ key: '2',
    label: renderTitle(h(ReadOutlined, { style: { color: '#1890FF' } }), '决策训练示例'),
    description: '如何提升决策能力',
    children: [{ key: '2-1', icon: h(HeartOutlined), description: `学生请求修改成绩`},
               { key: '2-2', icon: h(CommentOutlined), description: `强导师署名争议`},
               { key: '2-3', icon: h(SmileOutlined), description: `课堂言论引发投诉`} ]
  }
]
const senderPromptsItems: PromptsProps['items'] = [
  { key: '1', description: '学生请求修改成绩', icon: h(FireOutlined, { style: { color: '#FF4D4F' } }) },
  { key: '2', description: '科研署名争议', icon: h(ReadOutlined, { style: { color: '#1890FF' } }) },
]

// ==================== Bubble Roles ====================
const roles: BubbleListProps['roles'] = {
  ai: {
    placement: 'start',
    avatar: { icon: h(UserOutlined), style: { background: '#fde3cf' } },
    typing: { step: 5, interval: 20 },
    styles: { content: { borderRadius: '16px'} },
    loadingRender: () => h(Space, null, () => [h(Spin, { size: 'small' }), '模拟推演中...']),
  },
  local: {
    placement: 'end',
    avatar: { icon: h(UserOutlined), style: { background: '#87d068' } },
    variant: 'shadow',
  },
}

// ==================== State ====================
const content = ref('') //输入框的内容
const agentRequestLoading = ref(false) //表示是否正在请求数据（等待AI回复）
const evaluationGenerating = ref(false)
const conversationsItems = ref<Conversation[]>([])
const activeKey = ref<string>('')
const uiMessages = ref<UiMsg[]>([])
const ethicalPolicies = [
  "《研究生导师指导行为准则》",
  "《关于落实研究生导师立德树人职责的意见》",
  "《新时代高校教师职业行为十项准则》",
  "《关于加强和改进新时代师德师风建设的意见》",
  "《高等学校教师职业道德规范》",
  "《教育部关于高校教师师德失范行为处理的指导意见》",
]
const currentConv = computed(() =>
  conversationsItems.value.find(c => c.key === activeKey.value),
)
const senderPlaceholder = computed(() => {
  const conv = currentConv.value
  if (!conv) return '加载会话中...'

  const phase = conv.phase ?? 'init'
  if (phase === 'init') return '请输入师德困境主题，如：学生请求修改成绩...'
  if (phase === 'dialogue') return '请输入您的处置决策...'
  if (phase === 'evaluating') return '系统正在评估此次模拟训练...'
  return '当前会话已完成，可查看评估或新建下一轮训练...'
})
const senderDisabled = computed(() => {
  const phase = currentConv.value?.phase
  return phase === 'evaluating' || phase === 'completed'
})
const sceneCategories = ref([])

// ==================== 后端数据加载 ====================
async function loadSceneCategories() {
  try {
    const result = await sceneCategoriesService()
    sceneCategories.value = result.data || []
  } catch (error) {
    ElMessage.error('加载情景类别失败，请稍后重试')
  }
}

async function loadSessions() {
  try {
    const result = await sessionListService()
    const list = result.data || []
    conversationsItems.value = list.map((s: any) => ({
      key: String(s.id),
      label: s.title,
      status: s.status,
      phase: s.phase,
      step: s.step,
      maxSteps: s.maxSteps,
      timestamp: new Date(s.updatedAt).getTime(),
      group: toGroup(new Date(s.updatedAt).getTime()),
    }))
    // 默认选中第一个
    if (!activeKey.value) {
      if (conversationsItems.value.length > 0) {
        activeKey.value = conversationsItems.value[0].key
        await loadMessages(Number(activeKey.value))
      } else {
        await onAddConversation()
        ElMessage.warning('当前没有会话，请新建会话')
      }
    }
  } catch (error) {
    console.error('加载会话失败:', error)
    ElMessage.error('加载会话失败，请稍后重试!')
  }
}

async function loadMessages(sessionId: number) {
  const res = await messageListService(sessionId)
  const list = res.data || []
  uiMessages.value = list.map((m: any) => ({
    id: String(m.id),
    role: m.role === 'user' ? 'local' : 'ai',
    content: m.content ?? '',
  }))
}

async function restoreSessionFromRoute() {
  const sessionId = Number(route.query.sessionId)
  if (!sessionId) return

  const matched = conversationsItems.value.find(item => Number(item.key) === sessionId)
  if (!matched) return

  if (activeKey.value !== String(sessionId)) {
    activeKey.value = String(sessionId)
  }
  await loadMessages(sessionId)
}

async function request(nextContent: string) {
  const text = (nextContent ?? '').trim()
  if (!text) {
    ElMessage.warning('请输入内容')
    return
  }
  const conv = currentConv.value
  if (!conv) {
    ElMessage.warning('当前会话不存在，请刷新后重试')
    return
  }
  if (conv.phase === 'evaluating') {
    ElMessage.warning('当前正在生成评估，请稍候')
    return
  }
  if (conv.phase === 'completed') {
    ElMessage.warning('当前会话已完成，请新建会话开始新的演练')
    return
  }

  const sessionId = Number(activeKey.value)
  uiMessages.value.push({
    id: `u-${Date.now()}`,
    role: 'local',
    content: text,
  })

  content.value = ''
  agentRequestLoading.value = true
  try {
    const result = await sendChatService({ sessionId, message: text})
    const data = result.data || {}
    const aiText = data.aiMessage ?? ''

    uiMessages.value.push({
      id: `a-${Date.now()}`,
      role: 'ai',
      content: aiText,
    })

    updateCurrentConversationPatch({
      phase: data.phase,
      step: data.step,
      status: data.phase === 'completed' ? 'completed' : 'active',
    })

    if (data.phase === 'evaluating') {
      await generateEvaluationAndJump(sessionId)
    }
  } catch (e: any) {
    console.error('提交失败:', e)
    ElMessage.error(`请求失败: ${e?.message ?? '未知错误'}`)
    await loadMessages(sessionId)
  } finally {
    agentRequestLoading.value = false
  }
}

function updateCurrentConversationPatch(patch: Partial<Conversation>) {
  const current = conversationsItems.value.find(c => c.key === activeKey.value)
  if (!current) return
  Object.assign(current, patch)
}

// ==================== 生成评估并跳转 ====================
async function generateEvaluationAndJump(sessionId: number) {
    if (evaluationGenerating.value) return

    evaluationGenerating.value = true
    try {
        ElMessage.info('推演完成，正在生成评估报告...')
        const res = await ensureEvaluationService(sessionId)
        const evaluationId = res?.data
        updateCurrentConversationPatch({ phase: 'completed', status: 'completed', })

        await loadSessions()
        ElMessage.success('评估报告生成成功')
        router.push({ path: '/evaluation/detail', query: { id: sessionId} })
    } catch (error) {
        console.error('生成评估失败:', error)
        ElMessage.error(error?.message ?? '生成评估失败')
        updateCurrentConversationPatch({ phase: 'evaluating' })
    } finally {
        evaluationGenerating.value = false
    }
}

const goToEvaluation = (conversationKey?: string) => {
  const sid = Number(conversationKey || activeKey.value)
  if (!sid) {
    ElMessage.warning('当前没有可查看的会话')
    return
  }
  router.push({ path: '/evaluation/detail', query: { id: sid} })
}

// ==================== 会话：新建/切换/删除 ====================
async function onAddConversation() {
  const maxSteps = ref<number>(6)
  const title = ref<string>('新决策训练')
  const selectedCategory = ref<number>()
    await loadSceneCategories()

  Modal.confirm({
    title: '新建师德困境模拟训练',
    okText: '创建',
    cancelText: '取消',
    content: () =>
      h('div', { style: 'display:flex;flex-direction:column;gap:12px;' }, [
        h('div', null, [
          h('div', { style: 'margin-bottom:6px;color:rgba(0,0,0,0.65);' }, '模拟训练标题（可选）'),
          h(Input, {
            value: title.value,
            maxlength: 30,
            showCount: true,
            'onUpdate:value': (v: string) => (title.value = v),
            placeholder: '例如：学生请求修改成绩',
          }),
        ]),
        h('div', null, [
          h('div', { style: 'margin-bottom:6px;color:rgba(0,0,0,0.65);' }, '选择情景类别（可选）'),
          h(Select, {
              value: selectedCategory.value,
              style: 'width: 100%;',
              allowClear: true,
              'onUpdate:value': (v: number) => (selectedCategory.value = v),
              options: sceneCategories.value.map((category: { id: number; name: string }) => ({
                label: category.name,
                value: category.id,
              })),
            },
          ),
        ]),
        h('div', null, [
          h('div', { style: 'margin-bottom:6px;color:rgba(0,0,0,0.65);' }, '情景推演轮次（可选）'),
          h(InputNumber, {
            min: 1,
            max: 10,
            value: maxSteps.value,
            style: 'width: 100%;',
            'onUpdate:value': (v: number) => (maxSteps.value = v),
          }),
        ]),
        h('div', { style: 'color:rgba(0,0,0,0.45);font-size:12px;' },
          '轮次越多，推演更充分，但耗时更长。建议 4-8 轮。'
        ),
    ]),
    onOk: async () => {
      const t = title.value?.trim() || undefined
      const ms = maxSteps.value
      const cat = selectedCategory.value || undefined
      const result = await createSessionService(t, ms, cat)
      const s = result.data
      ElMessage.success('新建模拟训练成功！')
      try {
        await loadSessions()
        activeKey.value = String(s.id)
        await loadMessages(Number(activeKey.value))
      } catch (e: any) {
        console.error('创建后刷新失败', e)
        ElMessage.warning('会话已创建，但刷新列表失败，请手动刷新或重新进入页面')
      }
      return true
    },
  })
}

const onConversationClick: ConversationsProps['onActiveChange'] = async (key) => {
  activeKey.value = key
  const conv = conversationsItems.value.find(c => c.key === key)

  await loadMessages(Number(key))
  ElMessage.info("切换到模拟训练：" + (conv?.label || ''))
}

async function deleteConversation(conversationKey: string) {
  const sid = Number(conversationKey)
  const result = await deleteSessionService(sid)

  conversationsItems.value = conversationsItems.value.filter(c => c.key !== conversationKey)

  if (activeKey.value === conversationKey) {
    if (conversationsItems.value.length > 0) {
      activeKey.value = conversationsItems.value[0].key
      await loadMessages(Number(activeKey.value))
    } else {
      await onAddConversation()
    }
  }
}

const menuConfig: ConversationsProps['menu'] = (conversation) => ({
  items: [
    { label: '编辑', key: 'edit', icon: h(EditOutlined) },
    { label: '删除', key: 'delete', icon: h(DeleteOutlined), danger: true },
    { label: '查看评估', key: 'view-eval', icon: h(TrophyOutlined),
      disabled: !['completed'].includes((conversation.phase as string) || ''),
    },
  ],
  onClick: async (menuInfo) => {
    if (menuInfo.key === 'delete') {
      Modal.confirm({
        title: '确认删除该事件模拟训练？',
        content: '删除后该模拟训练下的所有记录也会一起删除，且不可恢复。',
        okText: '删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
          try {
            await deleteConversation(conversation.key as string)
            ElMessage.success('删除成功')
          } catch (e) {
            ElMessage.error(e?.message ?? '删除失败')
          }
        },
      })
    } else if (menuInfo.key === 'view-eval') {
        goToEvaluation(conversation.key as string)
    }else if(menuInfo.key === 'edit'){
      const val = ref(conversation.label)
      Modal.confirm({
        title: '编辑模拟训练标题',
        content: () => h(Input, {
          value: val.value,
          'onUpdate:value': (v: string) => (val.value = v),
          maxlength: 30,
          showCount: true,
        }),
        okText: '确认',
        cancelText: '取消',
        onOk: async () => {
          const newTitle = val.value
          if (!newTitle) {
            ElMessage.warning('标题不能为空')
            return false // 阻止对话框关闭
          }

          await updateSessionTitleService(Number(conversation.key), newTitle)
          conversation.label = newTitle
          ElMessage.success('该训练标题已更新')
        },
      })
    }
  },
})

const groupable: ConversationsProps['groupable'] = {
  sort(a, b) {
    const order = ['今天', '昨天', '近7天', '更早']
    return order.indexOf(a as string) - order.indexOf(b as string)
  },
  title: (group, { components: { GroupTitle } }) => group ? h(GroupTitle, null, () => 
      h(Space, null, () => [h(CommentOutlined), h('span', null, group as string)]) ) : h(GroupTitle),
}

function toGroup(ts: number) {
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const startOfYesterday = startOfToday - 24 * 60 * 60 * 1000
  const startOf7Days = startOfToday - 7 * 24 * 60 * 60 * 1000

  if (ts >= startOfToday) return '今天'
  if (ts >= startOfYesterday) return '昨天'
  if (ts >= startOf7Days) return '近7天'
  return '更早'
}
// ==================== Prompts 点击====================
const onPromptsItemClick: PromptsProps['onItemClick'] = (info) => {
  const text = (info.data.description ?? '').toString().trim()
  if (!text) return
  request(text)
}

// ==================== UI组件添加 ===================
const progressStatus = computed(() => {
  const conv = currentConv.value
  switch (conv.phase) {
    case 'init':  return { percent: 0}
    case 'dialogue': 
        const percent = conv.maxSteps > 0 ? 15 + (conv.step / conv.maxSteps) * 70 : 15
        return { percent}
    case 'evaluating': return { percent: 90}
    case 'completed': return { percent: 100}
    default:  return { percent: 0}
  }
})

const PhasePanel = () => {
  const conv = currentConv.value
  if (!conv) return null

  const phaseConfig = {
    init: {
      icon: h(PlayCircleOutlined),
      title: '准备开始',
      desc: '输入一个师德困境主题，开始模拟推演',
      color: '#1890ff',
    },
    dialogue: {
      icon: h(SyncOutlined),
      title: `推演中 (${conv.step}/${conv.maxSteps})`,
      desc: conv.label || '正在模拟推演',
      color: '#faad14',
    },
    evaluating: {
      icon: h(LoadingOutlined),
      title: '评估生成中',
      desc: 'AI 正在分析您的决策表现，请稍候...',
      color: '#52c41a',
    },
    completed: {
      icon: h(CheckCircleFilled),
      title: '演练完成',
      desc: conv.label || '师德困境模拟训练',
      color: '#52c41a',
    },
  }[conv.phase || 'init']

  return h('div', {
    style: {
      background: '#ffffff',
      borderRadius: '12px',
      padding: '20px 24px',
      marginBottom: '16px',
      boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
      border: '1px solid #f0f0f0',
    },
  }, [
    // 头部区域：图标 + 标题 + 状态描述
    h('div', { style: 'display: flex; align-items: center; margin-bottom: 16px;' }, [
      h('div', {
        style: {
          width: '48px',
          height: '48px',
          borderRadius: '50%',
          background: `${phaseConfig.color}10`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginRight: '16px',
          color: phaseConfig.color,
          fontSize: '24px',
        },
      }, phaseConfig.icon),
      h('div', { style: 'flex: 1;' }, [
        h('div', { style: 'font-size: 18px; font-weight: 600; line-height: 1.4;' }, phaseConfig.title),
        h('div', { style: 'font-size: 14px; color: #666; margin-top: 4px;' }, phaseConfig.desc),
      ]),
      conv.phase === 'completed' && h(Tag, { color: 'success', icon: h(CheckCircleOutlined) }, '已完成'),
    ]),
    // 进度条区域（非 init 状态显示进度）
    conv.phase !== 'init' && h(Progress, {
      percent: Number(progressStatus.value.percent.toFixed(0)),
      status: conv.phase === 'completed' ? 'success' : 'active',
      showInfo: false,
      strokeColor: phaseConfig.color,
      style: { marginBottom: '20px' },
    }),

    // 操作按钮区域
    conv.phase === 'completed' && h('div', { style: 'display: flex; gap: 12px; justify-content: flex-end;' }, [
      h(Button, { type: 'primary', ghost: true, icon: h(PlusOutlined), onClick: () => onAddConversation() }, '新建训练'),
      h(Button, { type: 'primary', icon: h(FileTextOutlined), onClick: () => goToEvaluation() }, '查看评估'),
    ]),

    // 如果是 evaluating，显示加载提示或重试按钮
    conv.phase === 'evaluating' && h('div', { style: 'text-align: center; color: #999;' }, [
      evaluationGenerating.value
          ? [h(Spin, { size: 'small' }), ' 正在生成评估报告，请稍候...']
          : h(Button, {
              type: 'primary',
              size: 'small',
              icon: h(ReloadOutlined),
              onClick: () => generateEvaluationAndJump(Number(activeKey.value)),
            }, '重新评估')
      ]),
  ])
}

// ==================== Nodes ====================
const placeholderNode = computed(() => h(Space,{direction: "vertical", size: 16, style: styles.value.placeholder }, () => [
    h( Welcome, {
        variant: "borderless",
        icon: "https://mdn.alipayobjects.com/huamei_iwk9zp/afts/img/A*s5sNRo5LjfQAAAAAAAAAAAAADgCCAQ/fmt.webp",
        title: "你好!我是高校师德治理数字沙盘AI助手",
        description: "提出一个师德突发事件,我会将其具体化并通过沉浸式对话引导你处理~",
    }),
    h( Prompts, {
        title: '你想从哪个主题开始？',
        items: placeholderPromptsItems,
        styles: {
          list: { width: '100%',},
          item: { flex: 1, },
        },
        onItemClick: onPromptsItemClick,
    }),
    h(Typography.Text, { type: 'secondary' }, '系统将生成逼真情景，并在完成全部轮次后生成决策评估报告。'),
    h(Flex, { wrap: 'wrap', gap: 'small' }, () => ethicalPolicies.map(policy => h(Tag, { color: 'blue' }, policy)) ),
    h('div', { style: { marginTop: '20px', padding: '16px', background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: '8px' } }, [
        h(Typography.Title, { level: 5, style: { marginBottom: '8px' } }, '📊 历史评估记录'),
        h(Button, { type: 'primary', size: 'small', onClick: () => router.push('/study/evaluation')}, '查看详细评估报告')
      ]
    )
  ]
))

const items = computed<BubbleListProps['items']>(() => {
  if (uiMessages.value.length === 0) {
    return [{ content: placeholderNode.value, variant: 'borderless' }]
  }
  return uiMessages.value.map(m => ({
    key: m.id,
    role: m.role,
    content: m.content,
    messageRender: (c: string) => renderMarkdown(c),
  }))
})

// ==================== 生命周期 ====================
onMounted(async () => {
  try {
    await loadSessions()
    await restoreSessionFromRoute()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载会话失败')
  }
})
watch(
  () => route.query.sessionId,
  async (val) => {
    if (!val) return
    if (conversationsItems.value.length === 0) return
    await restoreSessionFromRoute()
  }
)
</script>

<template>
  <div :style="styles.layout">
    <div :style="styles.menu">
      <div :style="styles.logo"><span :style="styles['logo-span']">数字沙盘AI助手</span></div>
      <!-- 🌟 添加会话 -->
      <Button type="link" :style="styles.addBtn" @click="onAddConversation"><PlusOutlined />新建决策训练</Button>
      <!-- 🌟 会话管理 -->
      <Conversations :items="conversationsItems" :style="styles.conversations" :menu="menuConfig" :groupable="groupable"
        :active-key="activeKey" @active-change="onConversationClick">
        <template #title="conversation">
          <Space>
            <span>{{ conversation.label }}</span>
            <Tag v-if="conversation.phase === 'completed'" color="green">已完成</Tag>
            <Tag v-else-if="conversation.phase === 'evaluating'" color="orange">评估中</Tag>
            <Tag v-else color="blue">进行中</Tag>
          </Space>
        </template>
      </Conversations>
    </div>

    <div :style="styles.chat">
      <PhasePanel v-if="currentConv?.phase !== 'init'" />

      <!-- 🌟 消息列表 -->
      <Bubble.List :items="items" :roles="roles" :style="styles.messages" />
      <!-- 🌟 提示词 -->
      <Prompts v-if="currentConv?.phase === 'init'" :items="senderPromptsItems" @item-click="onPromptsItemClick" />
      <!-- 🌟 输入框 -->
      <Sender :value="content" :style="styles.sender" :loading="agentRequestLoading || evaluationGenerating" :allow-speech="true"
        :disabled="senderDisabled" :placeholder="senderPlaceholder"
        @submit="request" @change="value => content = value" 
      >
        <template #suffix>
          <Typography.Text type="secondary" style="font-size: 12px;">
            {{ currentConv?.phase === 'dialogue' 
                ? `第 ${currentConv?.step ?? 0}/${currentConv?.maxSteps ?? 6} 轮推演`
                : currentConv?.phase === 'evaluating' ? 
                '评估生成中' : currentConv?.phase === 'completed'
                ? '已完成' : '开始模拟'
            }}
          </Typography.Text>
        </template>
      </Sender>
    </div>
  </div>
</template>

<style scoped>
/* .md-content {
  line-height: 1.5;
  word-break: break-word;
}

.md-content p { margin: 0.4em 0; }
.md-content h1, .md-content h2, .md-content h3 { margin: 0.6em 0 0.4em; }
.md-content ul, .md-content ol { margin: 0.4em 0 0.4em 1.2em; }
.md-content blockquote {
  margin: 0.6em 0;
  padding-left: 0.8em;
  border-left: 3px solid rgba(0,0,0,0.15);
  opacity: 0.9;
}
.md-content pre {
  padding: 0.8em;
  border-radius: 8px;
  overflow: auto;
}
.md-content code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
} */
</style>