<!-- 将ws handler提升到全局而不是只在chatview中。 -->
<script setup lang="ts">
import { onMounted, onBeforeUnmount, watch } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { chatSocket } from '@/utils/websocket'
import { useUserStore } from '@/stores/userStore'
import { useMessageStore } from '@/stores/messageStore'
import { useFriendStore } from '@/stores/friendStore'

const userStore = useUserStore()
const messageStore = useMessageStore()
const friendStore = useFriendStore()

function onWsChat(msg: any) {
  // msg 是 WebSocketMessage, content 字段是 ChatMessage
  const m = msg.content
  if (!m) return
  const me = userStore.userId
  if (!me) return
  // 找出"对方"的 id（不是我）
  const friendId = m.senderId === me ? m.receiverId : m.senderId
  messageStore.upsert(m)
  // 当前没在看这个好友，就增加未读
  if (messageStore.activeFriendId !== friendId) {
    messageStore.setUnread(friendId, (messageStore.unreadMap[friendId] || 0) + 1)
  }
}

function onWsRead(msg: any) {
  // 已读回执：把对应好友的消息里"对方发的"都设为已读
  const me = userStore.userId
  if (!me) return
  const friendId = msg.senderId === me ? msg.receiverId : msg.senderId
  const list = messageStore.messageMap[friendId] || []
  list.forEach((x: any) => {
    if (x.senderId === friendId) x.readAt = new Date().toISOString()
  })
  messageStore.setUnread(friendId, 0)
}

function onWsStatusUpdate(msg: any) {
  // 好友上线/下线事件：重新拉取好友列表，保持状态同步
  console.log('[WS] 收到状态更新:', msg)
  friendStore.loadFriends()
}

function startWs() {
  chatSocket.on('chat', onWsChat)
  chatSocket.on('read', onWsRead)
  chatSocket.on('statusUpdate', onWsStatusUpdate)
  chatSocket.connect()
}

function stopWs() {
  chatSocket.off('chat', onWsChat)
  chatSocket.off('read', onWsRead)
  chatSocket.off('statusUpdate', onWsStatusUpdate)
  chatSocket.disconnect()
}

onMounted(() => {
  // 登录后 userStore.userId 才有值，监听它来 connect / disconnect
  watch(
    () => userStore.userId,
    async (uid, oldUid) => {
      if (uid && !oldUid) {
        // 刚登录（或刷新后 userId 已从 sessionStorage 恢复）：先拉好友列表，
        // 再连 WS。这样所有页面（包括 FriendsView）的 store 数据已就绪，
        // 不会再出现"刷新后好友列表为空"的问题。
        await friendStore.loadFriends()
        startWs()
      }
      if (!uid && oldUid) stopWs()        // 退出登录
    },
    { immediate: true }
  )
})

onBeforeUnmount(() => {
  stopWs()
})
</script>
<template>
<RouterView />
</template>
<style>
/* 全局样式 */
</style>
