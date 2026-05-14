import request from '@/utils/request.js'

//=============课程推送学习相关接口============
export const courseCategoriesService = () => {
    return request.get('/study/course/categories')
}

export const courseHomeService = (params) => {
    return request.get('/study/course/home', { params })
}

export const courseDetailService = (courseId) => {
    return request.get('/study/course/detail', { params: { courseId } })
}

export const courseStartService = (courseId) => {
    return request.post('/study/course/start', null, { params: { courseId } })
}

export const courseReviewService = (courseId) => {
    return request.post('/study/course/review', null, { params: { courseId } })
}

export const courseProgressService = (data) => {
    return request.post('/study/course/progress', data)
}

export const courseToggleFavoriteService = (courseId) => {
    return request.post('/study/course/favorite/toggle', null, { params: { courseId } })
}

export const courseRemoveMyCourseService = (courseId) => {
    return request.post('/study/course/remove', null, { params: { courseId } })
}

export const courseNoteListService = (courseId) => {
    return request.get('/study/course/note/list', { params: { courseId } })
}

export const courseNoteAddService = (data) => {
    return request.post('/study/course/note', data)
}

export const courseNoteUpdateService = (data) => {
    return request.put('/study/course/note', data)
}

export const courseNoteDeleteService = (id) => {
    return request.delete('/study/course/note', { params: { id } })
}

//=============思政检索相关接口============
// 课程思政检索首页数据
export const ideologyHomeService = () => {
    return request.get('/study/element/home')
}
// 思政元素分页检索
export const ideologyPageService = (params) => {
    return request.get('/study/element/page', { params })
}
// 思政元素详情
export const ideologyDetailService = (id) => {
    return request.get(`/study/element/${id}`)
}

// 收藏列表
export const ideologyFavoritesService = () => {
    return request.get('/study/element/favorites')
}

// 教学课程下拉
export const ideologyTeachingCoursesService = () => {
    return request.get('/study/element/teaching-courses')
}

// 收藏/取消收藏
export const ideologyToggleFavoriteService = (id) => {
    return request.post(`/study/element/${id}/favorite`)
}