import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Result } from '@/types/api'
import { useUserStore } from '@/stores/userStore'//useUserStore是自定义的名称？不是userStore中有的
import { useRouter, useRoute } from 'vue-router'

// 创建一个路由器实例
// const router = useRouter();



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

// 幂等标记：token 过期时弹窗只弹一次，防止多个并发请求同时 401 弹出多个弹窗
let isRedirecting = false

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
    const status = error.response?.status
    const data = error.response?.data as { code?: number; message?: string } | undefined

    // 后端信号：HTTP 401或业务code=1都表示token无效/已过期
    // （参考LoginInterceptor异常分支：response.setStatus(401)+body{code:1,message:"token无效或已过期"}）
    if (status === 401 || data?.code === 1) {

      const userStore = useUserStore()
      // 第一种方式：直接硬刷
      //   userStore.clearUser()//清store+sessionStorage；App.vue的watch会自动stopWs()
      //   ElMessage.error(data?.message || '登录已过期，请重新登录')
      //   // 用硬刷而不是router.push：避免axios拦截器里使用路由实例的潜在时序问题，
      //   // 同时保证受保护页面彻底卸载（不留幽灵WS/缓存状态）
      //   setTimeout(() => {
      //     window.location.href = '/login'
      //   }, 1500)   // 等 1.5 秒，提示框有足够时间显示
      //   // window.location.href = '/login'
      //   //
      //   // router.push('/login')

      // 第二种方式：弹窗确认
      userStore.clearUser()

      if (!isRedirecting) {
        isRedirecting = true
        ElMessageBox.confirm('您的登录已过期，请重新登录', '登录已过期', {
          confirmButtonText: '确定',
          showCancelButton: false,
          closeOnClickModal: false,
          closeOnPressEscape: false,
          type: 'warning',
        }).then(() => {
          window.location.href = '/login'
        }).catch(() => {
          isRedirecting = false
        })
      }


      return Promise.reject(error)
    }

    // 其他错误（网络断、500、502...）走兜底
    ElMessage.error('前端响应拦截器报错：系统繁忙，请稍后再试')
    return Promise.reject(error)
  }
)

export default axiosInstance
