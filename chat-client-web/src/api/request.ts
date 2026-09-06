import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'
import { useUserStore } from '@/stores/userStore'//useUserStore是自定义的名称？不是userStore中有的

// 创建一个axios实例，把 axios 封装成实例，而不是直接用全局 axios。并设置基础URL和超时时间
const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：在请求发送之前，拦截请求并添加token
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    //包一层try/catch，多一层保险？
    try {
      //借助userUserstore获取用户信息
      const userStore = useUserStore()
      if (userStore.userInfoInstance.token) {
        // config.headers['Authorization'] = userStore.userInfoInstance.token
        config.headers.Authorization = userStore.userInfoInstance.token
      }
    } catch (err) {
      console.log("拦截器处理请求响应时出错" + err)
    }
    //将处理后的config返回
    return config
  },
  //上面已经进行了异常处理
  // (error: unknown) => {
  //   ElMessage.error("拦截器处理请求响应时出错" + error)
  //   //将错误信息返回
  //   return Promise.reject(error)
  // }
)

// 响应拦截器：剥出 data
axiosInstance.interceptors.response.use(
  //这个AxiosResponse是从axios中引入的类型
  (response: AxiosResponse<Result>) => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.message ? '前端响应拦截器报错：' + res.message : '前端响应拦截器报错：操作失败')
      return Promise.reject(res)
    }
    return res.data// 业务层直接拿到 T
  },
  (error) => {
    ElMessage.error('前端响应拦截器报错：系统繁忙，请稍后再试')
    return Promise.reject(error)
  }
)

export default axiosInstance
