import request from '@/utils/request.js'

export const chatAskService = (message) => {
  return request.get('/study/chat/ask', { params: { message } })
}

// ============= 会话相关接口 =============
export const sessionListService = () => {
  return request.get('/simulate/chat/session/list')
}

export const createSessionService = (title, maxSteps, sceneCategoryId) => {
  return request.post('/simulate/chat/session', null, {
    params: { title, maxSteps, sceneCategoryId }
  })
}

export const deleteSessionService = (id) => {
  return request.delete('/simulate/chat/session?id=' + id)
}

export const updateSessionTitleService = (id, title) => {
  return request.put('/simulate/chat/session/title', null, {
    params: { id, title }
  })
}

// ============= 消息相关接口 =============
export const messageListService = (sessionId) => {
  return request.get('/simulate/chat/message', { params: { sessionId } })
}

// 发送聊天
export const sendChatService = (req) => {
  // payload: { sessionId, message }
  return request.post('/simulate/chat/ask', req)
}

// ============= 决策评估相关接口 =============

// 生成/刷新某次会话的评估
export const ensureEvaluationService = (sessionId) => {
  return request.post('/simulate/evaluation', null, { params: { sessionId } })
}

// 获取单次评估详情（按 sessionId）
export const getEvaluationDetailService = (sessionId) => {
  return request.get('/simulate/evaluation', { params: { sessionId } })
}

// 获取评估维度列表
export const getEvaluationDimensionsService = () => {
  return request.get('/simulate/evaluation/dimensions')
}

// 获取最近评估记录
export const getRecentEvaluationService = () => {
  return request.get('/simulate/evaluation/recent')
}

// 获取评估主页聚合数据
export const getEvaluationHomeService = () => {
  return request.get('/simulate/evaluation/home')
}