//定制请求的实例
import axios from 'axios';
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token.js'
import router from '@/router'

//定义一个变量,记录公共的前缀,baseURL
const instance = axios.create({
    baseURL: '/api',
    timeout: 50000
})

//添加请求拦截器
instance.interceptors.request.use(
    (config)=>{
        //请求前的回调
        //添加token
        const tokenStore = useTokenStore();
        //判断有没有token
        if(tokenStore.token){
            config.headers.Authorization = tokenStore.token
        }
        return config;
    },
    (err)=>{
        //请求错误的回调
        Promise.reject(err)
    }
)

//添加响应拦截器
instance.interceptors.response.use(
    (response) => {
        const result = response.data
        if (result.code === 0) {
          return result
        }
        //操作失败
        ElMessage.error(result.message || '服务异常')
        return Promise.reject(result)
    },
    (err) => {
        //判断响应状态码,如果为401,则证明未登录,提示请登录,并跳转到登录页面
        const tokenStore = useTokenStore()
        if(err.response && err.response.status===401){
            tokenStore.removeToken()
            ElMessage.error('登录状态已失效，请重新登录')
            router.push('/login')
        }else{
            ElMessage.error(err.response?.data?.message || '服务异常')
        }
        return Promise.reject(err);//异步的状态转化成失败的状态
    }
)

export default instance;