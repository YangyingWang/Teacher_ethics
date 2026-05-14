import request from '@/utils/request.js'

//=============情景相关接口============
export const sceneListService = () => {
    return request.get('/test/scene/list')
}

export const sceneInfoService = (id) => {
    return request.get('/test/scene/info', { params: { id } })
}

export const sceneCategoriesService = () => {
    return request.get('/test/scene/categories')
}


//=============情景测试相关接口============
export const createQuizService = (sceneId) => {
    return request.post('/test/quiz/start', null, { params: { sceneId }})
}
  
export const getQuestionsService = (qnId) => {
    return request.get('/test/quiz/questions', { params: { qnId } })
}
  
export const submitService = (qnId, payload) => {
    // payload: { timeSpent, answers:[{questionId,userAnswer,isMarked}] }
    return request.post('/test/quiz/submit', payload, { params: { qnId } })
}
  
export const getResultService = (qnId) => {
    return request.get('/test/record',{ params: { qnId } })
}

export const abandonService = (qnId) => {
    return request.delete('/test/quiz', { params: { qnId } })
}


//=============答卷记录相关接口============
export const recentRecordsService = () => {
    return request.get('/test/record/recent')
}

export const recordsService = (params) => {
    return request.get('/test/record/list', { params: params})
}

export const deleteRecordService = (qnId) => {
    return request.delete('/test/record', { params: { qnId } })
}


//=============诊断报告相关接口============
export const createFullReportService = (qnId) => {
    return request.post('/test/report', null, { params: { qnId }})
}

export const getReportDetailService = (rId) => {
    return request.get('/test/report',{ params: { rId } })
}
export const getReportDimensionsService = () => {
    return request.get('/test/report/dimensions')
}
  
export const recentReportsService = () => {
    return request.get('/test/report/recent')
}

export const reportsService = (params) => {
  return request.get('/test/report/list', { params: params})
}