//定义store
import {defineStore} from 'pinia'
import {ref} from 'vue'
/* 
    第一个参数:名字,唯一性
    第二个参数:函数,函数的内部可以定义状态的所有内容

    返回值: 函数
*/
export const useTokenStore = defineStore('token',()=>{
    //定义状态的内容

    //1.响应式变量,保存token的值,初始值从localStorage中获取
    const token = ref(localStorage.getItem('token') || '')
    const role = ref(localStorage.getItem('role') || '')
    const username = ref(localStorage.getItem('username') || '')
    const realName = ref(localStorage.getItem('realName') || '')

    const setAuth = (auth = {}) => {
        token.value = auth.token || ''
        role.value = auth.role || ''
        username.value = auth.username || ''
        realName.value = auth.realName || ''
    
        localStorage.setItem('token', token.value)
        localStorage.setItem('role', role.value)
        localStorage.setItem('username', username.value)
        localStorage.setItem('realName', realName.value)
    }

    //2.定义一个函数,修改token的值
    const setToken = (newToken)=>{
        token.value = newToken
        localStorage.setItem('token', token.value)
    }

    //3.函数,移除token的值
    const removeToken = ()=>{
        token.value = ''
        role.value = ''
        username.value = ''
        realName.value = ''
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        localStorage.removeItem('username')
        localStorage.removeItem('realName')
    }

    return { token, role, username, realName, 
        setAuth, setToken, removeToken 
    }
},{
    persist:true//持久化存储
});