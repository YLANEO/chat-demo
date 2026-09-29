import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage } from '@/types/api'
import { sendMessageService, markReadService } from '@/api/message'
import { useUserStore } from '@/stores/userStore'

export const useMessageStore = defineStore('message', () => {
  const lastMsgMap = ref<Record<number, ChatMessage>>({})
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

  /**
   * 写入或替换一条消息。
   * 去重策略：只按 chatMessageId 命中。真实消息 id 唯一，足够。
   */
  function upsert(m: ChatMessage) {
    const friendId = m.receiverId === currentUid() ? m.senderId : m.receiverId
    const list = ensure(friendId)

    let idx = list.findIndex(x => x.chatMessageId === m.chatMessageId)
    if (idx >= 0) list[idx] = m
    else list.push(m)

    // 同步更新每个好友的最新一条消息（用于好友列表预览）
    lastMsgMap.value[friendId] = m
  }

  // 按 chatMessageId 移除一条消息（兼容老调用）
  function remove(friendId: number, chatMessageId: number) {
    const list = messageMap.value[friendId]
    if (!list) return
    const idx = list.findIndex(m => m.chatMessageId === chatMessageId)
    if (idx >= 0) list.splice(idx, 1)
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

  //发送消息：等HTTP请求回来拿到真实消息后入列表，简单可靠
  async function sendMessage(friendId: number, content: string) {
    const saved = await sendMessageService(friendId, 0, content)
    upsert(saved)
    return saved
  }

  async function markRead(friendId: number) {
    await markReadService(friendId)
    const list = messageMap.value[friendId] || []
    list.forEach(m => { if (m.senderId === friendId) m.readAt = new Date().toISOString() })
    unreadMap.value[friendId] = 0
  }

  // 工具：拿到当前登录用户的id
  function currentUid(): number {
    const userStore = useUserStore()
    return userStore.userId ?? 0
  }

  return {
    messageMap, activeFriendId, unreadMap, lastMsgMap,
    ensure, upsert, remove, setMessages, prependMessages, setUnread, active,
    sendMessage, markRead,
  }
})
