import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/api'
import { getFriendListService } from '@/api/contact'

/**
 * friendStore：统一管理好友列表（包含在线状态）
 * 所有需要展示好友状态的地方都从这里读取，保证数据一致性。
 *
 * 当收到 WebSocket statusUpdate 事件时，调用 loadFriends() 重新拉取，
 * 视图自动响应式更新。
 */
export const useFriendStore = defineStore('friend', () => {
  // 原始好友列表
  const friends = ref<User[]>([])

  // 按 userId 索引，方便 O(1) 查找和更新单条状态
  const friendMap = computed(() => {
    const map = new Map<number, User>()
    for (const f of friends.value) {
      map.set(f.userid, f)
    }
    return map
  })

  // 加载/刷新好友列表
  async function loadFriends(): Promise<void> {
    friends.value = await getFriendListService()
  }

  // 根据 userId 获取好友（不存在返回 undefined）
  function getFriend(userId: number): User | undefined {
    return friendMap.value.get(userId)
  }

  return {
    friends,
    friendMap,
    loadFriends,
    getFriend,
  }
})
