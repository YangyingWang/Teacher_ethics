import request from '@/utils/request.js'

// 管理首页总览数据
export const getAdminOverviewService = () => {
  return request.get('/admin/overview')
}

export const getDepartmentOptionsService = () => {
  return request.get('/admin/teachers/departments')
}

export const getTeacherPageService = (params) => {
  return request.get('/admin/teachers/page', { params })
}

export const getTeacherDetailService = (id) => {
  return request.get(`/admin/teachers/${id}`)
}

export const getStatisticsService = (params) => {
  return request.get('/admin/statistics', { params })
}