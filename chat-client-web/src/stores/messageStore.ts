import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage } from '@/types/api'
import { sendMessageService, markReadService } from '@/api/message'
import { useUserStore } from '@/stores/userStore'

export const useMessageStore = defineStore('message', () => {
  // 按好友 id 分组缓存消息
  const messageMap = ref<Record<number, ChatMessage[]>>({})

  // 当前激活的聊天对象 id
  const activeFriendId = ref<number | null>(null)

  // 各好友未读数
  const unreadMap = ref<Record<number, number>>({})

  // 拿到或初始化某个好友的消息列表
  function ensure(id: number): ChatMessage[] {
    if (!messageMap.value[id]) messageMap.value[id] = []
    return messageMap.value[id]
  }

  // 写入或替换一条消息
  function upsert(m: ChatMessage) {
    const list = ensure(m.receiverId === currentUid() ? m.senderId : m.receiverId)
    const idx = list.findIndex(x => x.chatMessageId === m.chatMessageId)
    if (idx >= 0) list[idx] = m
    else list.push(m)
  }

  function setMessages(friendId: number, list: ChatMessage[]) {
    messageMap.value[friendId] = list
  }

  function prependMessages(friendId: number, list: ChatMessage[]) {
    ensure(friendId).unshift(...list)
  }

  function setUnread(friendId: number, count: number) {
    unreadMap.value[friendId] = count
  }

  function active(friendId: number | null) {
    activeFriendId.value = friendId
    if (friendId != null) unreadMap.value[friendId] = 0
  }

  // —— 下面封装 service 调用，统一走 store ——
  async function sendMessage(friendId: number, content: string) {
    const res = await sendMessageService(friendId, 0, content)
    upsert(res)
  }

  async function markRead(friendId: number) {
    await markReadService(friendId)
    const list = messageMap.value[friendId] || []
    list.forEach(m => { if (m.senderId === friendId) m.readAt = new Date().toISOString() })
    unreadMap.value[friendId] = 0
  }

  // 工具：拿到当前登录用户的 id
  function currentUid(): number {
    const userStore = useUserStore()
    return userStore.userId ?? 0
  }

  return {
    messageMap, activeFriendId, unreadMap,
    ensure, upsert, setMessages, prependMessages, setUnread, active,
    sendMessage, markRead,
  }
})