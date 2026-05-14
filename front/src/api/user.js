import request from '@/utils/request.js'

export const userRegisterService = (data)=>{
    //借助于UrlSearchParams完成传递
    const params = new URLSearchParams()
    Object.keys(data).forEach((key) => {
        if (data[key] !== undefined && data[key] !== null) {
          params.append(key, data[key])
        }
    })
    return request.post('/user/register',params);
}

export const userLoginService = (data)=>{
    const params = new URLSearchParams();
    Object.keys(data).forEach((key) => {
        if (data[key] !== undefined && data[key] !== null) {
          params.append(key, data[key])
        }
    })
    return request.post('/user/login', params)
}

export const userInfoService = ()=>{
    return request.get('/user/userInfo')
}

export const userInfoUpdateService = (userInfoData)=>{
    return request.put('/user/userUpdate',userInfoData)
}

export const userPwdUpdateService = (userPwdData)=>{
    return request.patch('/user/updatePwd', userPwdData, {
        headers: { 'Content-Type': 'application/json' }  // 确保请求头是 JSON
    })
}