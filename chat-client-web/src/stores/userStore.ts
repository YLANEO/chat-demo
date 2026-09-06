import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/api'
import { resolveAvatarUrl } from '@/utils/avatar'

// 定义用户信息接口（相当于后端的类），继承自导入的User接口，并且token属性是可选的
interface UserInfo extends Partial<User> {
  token?: string
}

// 定义localStorage的key,作为localstorage的键名
const STORAGE_KEY = 'userInfo_localstorage_key'

// 
export const useUserStore = defineStore('userInfo_pinia_store', () => {
  // 从localStorage初始化，
  const userInfoInstance = ref<UserInfo>(loadFromStorage())
  //为什么要用computed？因为computed是惰性计算的，只有当依赖的值发生变化时，才会重新计算，而ref是直接计算的，每次都会重新计算
  const isLoggedIn = computed<boolean>(() => !!userInfoInstance.value.userid)
  const userId = computed<number | undefined>(() => userInfoInstance.value.userid)
  const username = computed<string>(() => userInfoInstance.value.username ?? '')
  const nickname = computed<string>(() => userInfoInstance.value.nickname ?? '')
  //
  const avatarUrl = computed<string>(() => resolveAvatarUrl(userInfoInstance.value.avatar_url))
  // const avatarUrl = computed<string>(() => userInfoInstance.value.avatar_url ?? '')
  //
  function setUser(data: UserInfo): void {
    userInfoInstance.value = data
    // 调用这里的方法，将用户信息保存到localStorage中
    saveToStorage(data)
  }
  //
  function clearUser(): void {
    userInfoInstance.value = {}
    sessionStorage.removeItem(STORAGE_KEY)
  }

  //这个状态管理最后要返回的属性和方法
  return {
    userInfoInstance,
    isLoggedIn,
    userId,
    username,
    nickname,
    avatarUrl,
    setUser,
    clearUser,
  }
})

// 修改前（localStorage）
function loadFromStorage(): UserInfo {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as UserInfo) : {}
  } catch {
    return {}
  }
}



function saveToStorage(data: UserInfo): void {
  // 将用户信息保存到localStorage中  
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}
