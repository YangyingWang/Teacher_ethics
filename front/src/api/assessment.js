import request from '@/utils/request.js'
import axios from 'axios'
import { useTokenStore } from '@/stores/token.js'

// 获取评估首页数据
export const assessmentHomeService = (params) => {
  return request.get('/assessment/home', { params })
}

// 刷新评估数据
export const assessmentRefreshService = (params) => {
  return request.post('/assessment/refresh', null, { params })
}

// 生成评估报告
export const assessmentReportService = (params) => {
  return request.post('/assessment/report', null, { params })
}

// 导出 Excel
export const assessmentExportService = async (params) => {
  const tokenStore = useTokenStore()

  const response = await axios({
    url: '/api/assessment/export',
    method: 'get',
    params,
    responseType: 'blob',
    headers: tokenStore.token
      ? { Authorization: tokenStore.token }
      : {}
  })

  let fileName = 'assessment.xlsx'
  const disposition = response.headers['content-disposition']

  if (disposition) {
    const match = disposition.match(/filename\*=UTF-8''([^;]+)/)
    if (match && match[1]) {
      fileName = decodeURIComponent(match[1])
    }
  }

  return {
    blob: response.data,
    fileName
  }
}