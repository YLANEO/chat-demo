<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ChatDotRound, Picture, Microphone, Promotion, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import type { User, ChatMessage } from '@/types/api'
import { getFriendListService } from '@/api/contact'
import { getMessageHistoryService, sendMessageService, markReadService } from '@/api/message'
import { chatSocket } from '@/utils/websocket'
import { useUserStore } from '@/stores/userStore'
import { resolveAvatarUrl } from '@/utils/avatar'

/* =============================================================
 * 全局对象：好友列表 / 当前选中 / 历史消息 / 最近一条消息
 * 全在这一个组件内维护，方便追踪所有状态变化
 * ============================================================= */
const userStore = useUserStore()

const friends = ref<User[]>([])                       // 左侧好友列表
const activeFriendId = ref<number | null>(null)              // 当前选中的好友
const messages = ref<ChatMessage[]>([])                // 当前会话消息
const lastMsgMap = ref<Record<number, ChatMessage>>({})  // 好友id -> 最新一条
const unreadMap = ref<Record<number, number>>({})       // 好友id -> 未读数
const loading = ref(false)
const inputText = ref('')
const messageListRef = ref<HTMLElement | null>(null)

const searchKeyword = ref('')

const activeFriend = computed<User | undefined>(() =>
  friends.value.find(f => f.userid === activeFriendId.value)
)

/* =============================================================
 * 1. 进入页面：拉好友列表、连 WS、默认选第一个好友
 * ============================================================= */
onMounted(async () => {
  try {
    friends.value = await getFriendListService()
  } catch (e) {
    ElMessage.error('获取好友列表失败' + e)
  }

  // 全局唯一 WS 连接 + 注册本页要监听的 type
  chatSocket.on('chat', onWsChat)
  chatSocket.on('read', onWsRead)
  chatSocket.connect()

  // 默认打开第一个好友
  if (friends.value.length > 0 && activeFriendId.value == null) {
    await selectFriend(friends.value[0].userid)
  }
})

onBeforeUnmount(() => {
  // 离开页面只解绑监听，不关 WS（WS 是 app 级单例）
  chatSocket.off('chat', onWsChat)
  chatSocket.off('read', onWsRead)
})

/* =============================================================
 * 2. 切换好友：拉历史消息 -> 滚到底 -> 自动标记已读
 * ============================================================= */
async function selectFriend(fid: number) {
  if (activeFriendId.value === fid) {
    scrollToBottom()
    return
  }
  activeFriendId.value = fid
  loading.value = true
  try {
    messages.value = await getMessageHistoryService(fid)
    await nextTick()
    scrollToBottom()

    // 如果该好友有未读，自动标已读
    if ((unreadMap.value[fid] || 0) > 0) {
      await markReadService(fid).catch(() => { })
      unreadMap.value[fid] = 0
    }
  } catch (e) {
    ElMessage.error('加载历史消息失败')
  } finally {
    loading.value = false
  }
}

/* =============================================================
 * 3. 发送消息：乐观追加 -> HTTP 落库 -> 替换临时消息为后端真实记录
 * ============================================================= */
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || activeFriendId.value == null) return

  const fid = activeFriendId.value
  const tempId = -Date.now() // 临时 id，负数不会跟数据库冲突

  // 1) 立刻把消息画到界面上（乐观更新）
  const optimistic: ChatMessage = {
    chatMessageId: tempId,
    senderId: userStore.userId!,
    receiverId: fid,
    type: 0,
    content: text,
    readAt: null,
    status: 0,
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
  }
  messages.value.push(optimistic)
  lastMsgMap.value[fid] = optimistic
  inputText.value = ''
  await nextTick()
  scrollToBottom()

  // 2) 调接口发出去
  try {
    const saved = await sendMessageService(fid, 0, text)
    // 用后端真实记录把临时消息换掉
    const idx = messages.value.findIndex(m => m.chatMessageId === tempId)
    if (idx >= 0) messages.value[idx] = saved
    lastMsgMap.value[fid] = saved
  } catch {
    const idx = messages.value.findIndex(m => m.chatMessageId === tempId)
    if (idx >= 0) messages.value[idx].status = 1   // 标失败
    ElMessage.error('发送失败')
  }
}

/* =============================================================
 * 4. WebSocket 回调
 *    - onWsChat : 收到聊天消息（对方发的或自己同步来的）
 *    - onWsRead : 收到已读回执
 * ============================================================= */
function onWsChat(payload: any) {
  // 服务端约定：payload.content 才是真正的 ChatMessage
  const m: ChatMessage = payload.content ?? payload
  const me = userStore.userId
  if (!me || !m) return

  // 判断当前是和谁在说话
  const fid = m.senderId === me ? m.receiverId : m.senderId

  // 更新左侧好友最新消息预览
  lastMsgMap.value[fid] = m

  // 对方发来的：加未读 + 更新好友为在线
  if (m.senderId !== me) {
    unreadMap.value[fid] = (unreadMap.value[fid] ?? 0) + 1
    const f = friends.value.find(x => x.userid === fid)
    if (f && f.status === 0) f.status = 1
  }

  // 当前正在和 fid 聊天 -> 直接追加；并自动标已读
  if (activeFriendId.value === fid) {
    // 避免重复：自己发的不在这里再 add（HTTP 已经 add 了）
    if (m.senderId !== me && !messages.value.some(x => x.chatMessageId === m.chatMessageId)) {
      messages.value.push(m)
      markReadService(fid).catch(() => { })
      nextTick(() => scrollToBottom())
    }
  }
}

function onWsRead(_payload: any) {
  // 把"我发给对方的"消息全部标已读
  const me = userStore.userId
  if (!me) return
  messages.value
    .filter(x => x.senderId === me && !x.readAt)
    .forEach(x => x.readAt = new Date().toISOString())
}

/* =============================================================
 * 5. 工具函数
 * ============================================================= */
function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function formatTime(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  const isToday = new Date().toDateString() === d.toDateString()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return isToday ? `${hh}:${mm}` : `${d.getMonth() + 1}-${d.getDate()} ${hh}:${mm}`
}

function previewText(fid: number): string {
  const m = lastMsgMap.value[fid]
  if (!m) return ' '
  if (m.type === 1) return '[图片]'
  if (m.type === 2) return '[文件]'
  if (m.type === 4) return '[系统]'
  return m.content
}

function resolveAvatar(url: string) {
  return resolveAvatarUrl(url)
}

function handleEnterKey(e: KeyboardEvent) {
  // 配合 el-input 的 @keydown.enter.exact.prevent
  e.preventDefault()
  sendMessage()
}

/* =============================================================
 * 6. 搜索（左侧过滤）
 * ============================================================= */
const filteredFriends = computed<User[]>(() => {
  const k = searchKeyword.value.trim().toLowerCase()
  if (!k) return friends.value
  return friends.value.filter(
    f => f.username.toLowerCase().includes(k) ||
      f.nickname.toLowerCase().includes(k)
  )
})
</script>
<template>
<div class="chat-container">
  <!-- ===========================================
          左侧好友列表
          =========================================== -->
  <aside class="friend-list">
    <div class="friend-search">
      <el-input v-model="searchKeyword" placeholder="搜索好友" :prefix-icon="Search" clearable />
    </div>
    <div class="friend-scroll">
      <div v-for="f in filteredFriends" :key="f.userid" class="friend-item" :class="{ active: f.userid === activeFriendId }" @click="selectFriend(f.userid)">
        <div class="friend-avatar">
          <img :src="resolveAvatar(f.avatar_url)" :alt="f.nickname" />
          <span class="online-dot" v-if="f.status === 1"></span>
        </div>
        <div class="friend-info">
          <div class="friend-top">
            <span class="friend-name">{{ f.nickname || f.username }}</span>
            <span class="friend-time"> {{ formatTime(lastMsgMap[f.userid]?.createTime) }} </span>
          </div>
          <div class="friend-bottom">
            <span class="friend-msg">{{ previewText(f.userid) }}</span>
            <span class="friend-badge" v-if="(unreadMap[f.userid] || 0) > 0"> {{ unreadMap[f.userid] }} </span>
          </div>
        </div>
      </div>
      <div v-if="friends.length === 0" class="empty-friend">暂无好友</div>
    </div>
  </aside>
  <!-- ===========================================
          右侧聊天区
          =========================================== -->
  <section class="chat-area">
    <!-- 空状态 -->
    <div v-if="!activeFriend" class="empty-tip">
      <el-icon :size="48">
        <ChatDotRound />
      </el-icon>
      <p>请选择一位好友开始聊天</p>
    </div>
    <template v-else>
      <!-- 顶部 -->
      <header class="chat-header">
        <img :src="resolveAvatar(activeFriend.avatar_url)" class="chat-header-avatar" :alt="activeFriend.nickname" />
        <div>
          <div class="chat-header-name"> {{ activeFriend.nickname || activeFriend.username }} </div>
          <div class="chat-header-status"> {{ activeFriend.status === 1 ? '在线' : '离线' }} </div>
        </div>
      </header>
      <!-- 中间消息列表 -->
      <main class="message-list" ref="messageListRef" v-loading="loading">
        <div v-for="m in messages" :key="m.chatMessageId" class="message-row" :class="{ mine: m.senderId === userStore.userId }">
          <div class="message-bubble">
            <template v-if="m.type === 4">[系统消息] {{ m.content }}</template>
            <template v-else>{{ m.content }}</template>
          </div>
          <div class="message-time"> {{ formatTime(m.createTime) }} <span v-if="m.status === 1" class="send-fail">发送失败</span>
          </div>
        </div>
        <div v-if="!loading && messages.length === 0" class="empty-msg"> 暂无消息，开始聊天吧 👋 </div>
      </main>
      <!-- 底部输入区 -->
      <footer class="message-input">
        <div class="input-toolbar">
          <el-button text :icon="Picture">表情</el-button>
          <el-button text :icon="Microphone">图片</el-button>
        </div>
        <el-input v-model="inputText" type="textarea" :rows="3" placeholder="输入消息..." resize="none" @keydown.enter.exact.prevent="sendMessage" />
        <div class="input-footer">
          <el-button type="primary" :icon="Promotion" :disabled="!inputText.trim()" @click="sendMessage"> 发送 </el-button>
        </div>
      </footer>
    </template>
  </section>
</div>
</template>
<style lang="scss" scoped>
/* =============================================================
 *  整个聊天页 —— 占满父容器
 * ============================================================= */
.chat-container {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* =============================================================
 *  左侧好友列表
 * ============================================================= */
.friend-list {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--theme-border);
  background: rgba(0, 0, 0, 0.08);
  min-height: 0;
}

.friend-search {
  flex-shrink: 0;
  padding: 12px;
  border-bottom: 1px solid var(--theme-border);
}

.friend-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 6px 0;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--theme-border);
    border-radius: 2px;
  }
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin: 2px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: var(--theme-menu-hover);
  }

  &.active {
    background: var(--theme-menu-active);
  }

  .friend-avatar {
    position: relative;
    flex-shrink: 0;
    width: 40px;
    height: 40px;

    img {
      width: 100%;
      height: 100%;
      border-radius: 5px;
      object-fit: cover;
    }

    .online-dot {
      position: absolute;
      bottom: 0;
      right: 0;
      width: 10px;
      height: 10px;
      border-radius: 5px;
      background: #2ecc71;
      border: 2px solid var(--theme-sidebar-bg);
    }
  }

  .friend-info {
    flex: 1;
    min-width: 0;
  }

  .friend-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .friend-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--theme-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .friend-time {
    font-size: 11px;
    color: var(--theme-text-muted);
    flex-shrink: 0;
    margin-left: 6px;
  }

  .friend-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 4px;
  }

  .friend-msg {
    font-size: 12px;
    color: var(--theme-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }

  .friend-badge {
    flex-shrink: 0;
    margin-left: 6px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 9px;
    background: #f56c6c;
    color: #fff;
    font-size: 11px;
    line-height: 18px;
    text-align: center;
  }
}

.empty-friend {
  padding: 20px;
  text-align: center;
  color: var(--theme-text-muted);
  font-size: 13px;
}

/* =============================================================
 *  右侧聊天区
 * ============================================================= */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.chat-header {
  flex-shrink: 0;
  height: 56px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--theme-border);
  background: rgba(0, 0, 0, 0.05);
}

.chat-header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 5px;
  object-fit: cover;
}

.chat-header-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--theme-text);
}

.chat-header-status {
  font-size: 11px;
  color: var(--theme-text-muted);
}

/* =============================================================
 *  消息列表（核心滚动区）
 * ============================================================= */
.message-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--theme-border);
    border-radius: 3px;
  }
}

.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;

  &.mine {
    align-items: flex-end;

    .message-bubble {
      background: var(--theme-btn-bg);
      color: #fff;
      border-bottom-left-radius: 14px;
      border-bottom-right-radius: 4px;
    }

    .message-time {
      text-align: right;
    }
  }
}

.message-bubble {
  max-width: 65%;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--theme-text);
  border-radius: 14px;
  border-bottom-left-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--theme-text-muted);
}

.send-fail {
  margin-left: 6px;
  color: #f56c6c;
}

.empty-msg {
  margin: auto;
  color: var(--theme-text-muted);
  font-size: 13px;
}

/* =============================================================
 *  底部输入区
 * ============================================================= */
.message-input {
  flex-shrink: 0;
  padding: 12px 20px 16px;
  border-top: 1px solid var(--theme-border);
  background: rgba(0, 0, 0, 0.05);
}

.input-toolbar {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}

.input-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* =============================================================
 *  空状态
 * ============================================================= */
.empty-tip {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--theme-text-muted);

  p {
    margin: 0;
    font-size: 14px;
  }
}
</style>
