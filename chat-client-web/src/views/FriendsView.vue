<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { Search, Check, Close, User, ChatDotRound, Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import type { User as UserType, FriendRequest } from '@/types/api'
import {
  getFriendListService,
  searchUserService,
  sendFriendRequestService,
  getFriendRequestListService,
  handleFriendRequestService,
  isFriendService,
  hasPendingRequestService,
} from '@/api/contact'
import { chatSocket } from '@/utils/websocket'
import { useUserStore } from '@/stores/userStore'
import { resolveAvatarUrl } from '@/utils/avatar'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

// ============================ Tab 状态 ============================
const activeTab = ref<'friends' | 'requests'>('friends')

// ============================ 好友列表 ============================
const friendList = ref<UserType[]>([])
const friendLoading = ref(false)

// ============================ 申请列表 ============================
const requestList = ref<FriendRequest[]>([])
const requestLoading = ref(false)

// ============================ 搜索 ============================
const searchKeyword = ref('')
const searchResults = ref<UserType[]>([])
const searchLoading = ref(false)
const searching = ref(false)

// 记录每个搜索结果用户的状态：none | pending | already_friend
type UserStatus = 'none' | 'pending' | 'already_friend'
const searchUserStatusMap = ref<Record<number, UserStatus>>({})

// ============================ 初始化 ============================
onMounted(async () => {
  await Promise.all([loadFriends(), loadRequests()])

  chatSocket.on('friendRequest', loadRequests)
  chatSocket.on('friendListUpdate', loadFriends)
  chatSocket.connect()
})

onBeforeUnmount(() => {
  chatSocket.off('friendRequest', loadRequests)
  chatSocket.off('friendListUpdate', loadFriends)
})

// ============================ 加载数据 ============================
async function loadFriends() {
  friendLoading.value = true
  try {
    friendList.value = await getFriendListService()
  } catch (e) {
    ElMessage.error('获取好友列表失败')
  } finally {
    friendLoading.value = false
  }
}

async function loadRequests() {
  requestLoading.value = true
  try {
    requestList.value = await getFriendRequestListService()
  } catch (e) {
    ElMessage.error('获取好友申请列表失败')
  } finally {
    requestLoading.value = false
  }
}

// ============================ 搜索用户 ============================
async function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) {
    searchResults.value = []
    searchUserStatusMap.value = {}
    return
  }
  searching.value = true
  searchLoading.value = true
  try {
    // 搜索结果里可能包含已经是好友的人，需要查每个用户的状态
    const results = await searchUserService(kw)
    searchResults.value = results

    // 并行查询每个用户的状态
    const me = userStore.userId!
    const statusMap: Record<number, UserStatus> = {}
    await Promise.all(
      results.map(async (u) => {
        try {
          const [isFriend, hasPending] = await Promise.all([
            isFriendService(me, u.userid),
            hasPendingRequestService(me, u.userid),
          ])
          if (isFriend === 1) {
            statusMap[u.userid] = 'already_friend'
          } else if (hasPending) {
            statusMap[u.userid] = 'pending'
          } else {
            statusMap[u.userid] = 'none'
          }
        } catch {
          statusMap[u.userid] = 'none'
        }
      })
    )
    searchUserStatusMap.value = statusMap
  } catch (e) {
    ElMessage.error('搜索用户失败')
  } finally {
    searchLoading.value = false
    searching.value = false
  }
}

function clearSearch() {
  searchKeyword.value = ''
  searchResults.value = []
  searchUserStatusMap.value = {}
}

// ============================ 发送好友申请 ============================
async function handleAddFriend(user: UserType) {
  const status = searchUserStatusMap.value[user.userid]
  if (status === 'pending') {
    ElMessage.warning('已发送过好友申请，等待对方处理')
    return
  }
  if (status === 'already_friend') {
    ElMessage.info('你们已经是好友了')
    return
  }
  try {
    await sendFriendRequestService(user.userid, '你好，我想加你为好友')
    searchUserStatusMap.value[user.userid] = 'pending'
    ElMessage.success('好友申请已发送')
  } catch {
    ElMessage.error('发送好友申请失败')
  }
}

// ============================ 处理申请（同意/拒绝） ============================
async function handleRequest(friendRequest: FriendRequest, status: 1 | 2) {
  const action = status === 1 ? '同意' : '拒绝'
  try {
    await handleFriendRequestService(
      friendRequest.requestId,
      friendRequest.senderId,
      status
    )
    // 从列表中移除已处理的
    requestList.value = requestList.value.filter(
      r => r.requestId !== friendRequest.requestId
    )
    ElMessage.success(`已${action}`)
    // 如果是同意，刷新好友列表
    if (status === 1) {
      await loadFriends()
    }
  } catch (e) {
    ElMessage.error(`${action}失败`)
  }
}

// ============================ 跳转聊天 ============================
function startChat(user: UserType) {
  router.push('/main/chat')
  // 注意：实际选好友是聊天页面内部处理的，这里只需要跳过去
  // 如果需要预选，可以在 router.push 时传 query 参数，然后在 ChatView 中读取
}

// ============================ 工具函数 ============================
function formatTime(isoStr: string): string {
  if (!isoStr) return ''
  // 后端 LocalDateTime 格式: "2026-09-05T16:25:00"
  const d = new Date(isoStr.replace(' ', 'T'))
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  const isToday = now.toDateString() === d.toDateString()
  const mm = String(d.getMinutes()).padStart(2, '0')
  if (isToday) {
    return `${d.getHours()}:${mm}`
  }
  return `${d.getMonth() + 1}-${d.getDate()} ${d.getHours()}:${mm}`
}

function resolveAvatar(url: string) {
  return resolveAvatarUrl(url)
}

const pendingCount = computed(() => requestList.value.length)

const filteredFriends = computed(() => friendList.value)

const showSearchResults = computed(() => searchKeyword.value.trim().length > 0)
</script>
<template>
<div class="friends-page">
  <!-- ====================== 左侧：Tab + 好友列表 ====================== -->
  <aside class="friends-sidebar">
    <!-- Tab 切换 -->
    <div class="tab-bar">
      <button class="tab-btn" :class="{ active: activeTab === 'friends' }" @click="activeTab = 'friends'">
        <User />
        <span>好友</span>
        <span class="tab-count">{{ friendList.length }}</span>
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'requests' }" @click="activeTab = 'requests'">
        <Bell />
        <span>申请</span>
        <span class="tab-count badge" v-if="pendingCount > 0">{{ pendingCount }}</span>
      </button>
    </div>
    <!-- 好友列表 -->
    <div class="sidebar-list" v-show="activeTab === 'friends'">
      <div v-for="f in filteredFriends" :key="f.userid" class="friend-item" @click="startChat(f)">
        <div class="friend-avatar">
          <img :src="resolveAvatar(f.avatar_url)" :alt="f.nickname" />
          <span class="online-dot" v-if="f.status === 1"></span>
        </div>
        <div class="friend-info">
          <div class="friend-name"> {{ f.nickname || f.username }} </div>
          <div class="friend-username">@{{ f.username }}</div>
        </div>
        <el-button text :icon="ChatDotRound" @click.stop="startChat(f)" title="发起聊天" />
      </div>
      <div v-if="friendLoading" class="list-empty">
        <span>加载中...</span>
      </div>
      <div v-else-if="filteredFriends.length === 0" class="list-empty">
        <span>暂无好友</span>
      </div>
    </div>
    <!-- 申请列表 -->
    <div class="sidebar-list" v-show="activeTab === 'requests'">
      <div v-for="req in requestList" :key="req.requestId" class="request-item">
        <img :src="resolveAvatar(req.senderAvatar)" :alt="req.senderNickname" class="request-avatar" />
        <div class="request-info">
          <div class="request-name"> {{ req.senderNickname || req.senderUsername }} </div>
          <div class="request-msg">{{ req.message }}</div>
          <div class="request-time">{{ formatTime(req.createTime) }}</div>
        </div>
        <div class="request-actions">
          <el-button type="primary" text circle :icon="Check" @click="handleRequest(req, 1)" title="同意" />
          <el-button type="danger" text circle :icon="Close" @click="handleRequest(req, 2)" title="拒绝" />
        </div>
      </div>
      <div v-if="requestLoading" class="list-empty">
        <span>加载中...</span>
      </div>
      <div v-else-if="requestList.length === 0" class="list-empty">
        <span>暂无待处理的好友申请</span>
      </div>
    </div>
  </aside>
  <!-- ====================== 右侧：搜索和结果 ====================== -->
  <section class="friends-main">
    <div class="search-area">
      <div class="search-header">
        <h3>添加好友</h3>
      </div>
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索用户名或昵称..." :prefix-icon="Search" clearable @keyup.enter="handleSearch" @clear="clearSearch" />
        <el-button type="primary" :loading="searchLoading" @click="handleSearch" style="margin-left: 8px"> 搜索 </el-button>
      </div>
      <!-- 搜索结果 -->
      <div class="search-results" v-if="showSearchResults">
        <div v-if="searchLoading" class="result-loading">
          <span>搜索中...</span>
        </div>
        <div v-else-if="searchResults.length === 0" class="result-empty">
          <span>未找到相关用户</span>
        </div>
        <div v-else v-for="user in searchResults" :key="user.userid" class="search-result-item">
          <img :src="resolveAvatar(user.avatar_url)" :alt="user.nickname" class="result-avatar" />
          <div class="result-info">
            <div class="result-name"> {{ user.nickname || user.username }} </div>
            <div class="result-username">@{{ user.username }}</div>
          </div>
          <div class="result-action">
            <!-- 已经是好友 -->
            <el-tag type="success" v-if="searchUserStatusMap[user.userid] === 'already_friend'"> 已添加 </el-tag>
            <!-- 已发送申请 -->
            <el-tag v-else-if="searchUserStatusMap[user.userid] === 'pending'"> 已发送 </el-tag>
            <!-- 可以添加 -->
            <el-button v-else type="primary" size="small" @click="handleAddFriend(user)"> 添加 </el-button>
          </div>
        </div>
      </div>
      <!-- 未搜索时的空状态 -->
      <div class="main-empty" v-else>
        <el-icon :size="48">
          <Search />
        </el-icon>
        <p>输入关键词搜索用户并发送好友申请</p>
      </div>
    </div>
  </section>
</div>
</template>
<style lang="scss" scoped>
.friends-page {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* ====================== 左侧边栏 ====================== */
.friends-sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--theme-border);
  background: rgba(0, 0, 0, 0.08);
}

/* Tab 栏 */
.tab-bar {
  display: flex;
  border-bottom: 1px solid var(--theme-border);
  flex-shrink: 0;
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 0;
  background: transparent;
  border: none;
  color: var(--theme-text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    color: var(--theme-text);
    background: var(--theme-menu-hover);
  }

  &.active {
    color: var(--theme-accent);
    background: var(--theme-menu-active);

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 2px;
      background: var(--theme-accent);
    }
  }
}

.tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 11px;
  line-height: 18px;
  text-align: center;

  &.badge {
    background: #f56c6c;
    color: #fff;
  }
}

/* 列表 */
.sidebar-list {
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

/* 好友项 */
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
      border: 2px solid rgba(0, 0, 0, 0.08);
    }
  }

  .friend-info {
    flex: 1;
    min-width: 0;
  }

  .friend-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--theme-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .friend-username {
    font-size: 11px;
    color: var(--theme-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

/* 申请项 */
.request-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin: 2px 8px;
  border-radius: 10px;
  transition: background 0.2s ease;

  &:hover {
    background: var(--theme-menu-hover);
  }

  .request-avatar {
    width: 40px;
    height: 40px;
    border-radius: 5px;
    object-fit: cover;
    flex-shrink: 0;
  }

  .request-info {
    flex: 1;
    min-width: 0;
  }

  .request-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--theme-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .request-msg {
    font-size: 12px;
    color: var(--theme-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .request-time {
    font-size: 11px;
    color: var(--theme-text-muted);
    margin-top: 2px;
  }

  .request-actions {
    display: flex;
    gap: 4px;
    flex-shrink: 0;
  }
}

.list-empty {
  padding: 20px;
  text-align: center;
  color: var(--theme-text-muted);
  font-size: 13px;
}

/* ====================== 右侧主区 ====================== */
.friends-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.search-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.search-header {
  flex-shrink: 0;
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--theme-border);

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--theme-text);
  }
}

.search-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--theme-border);
}

/* 搜索结果 */
.search-results {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 8px 0;

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

.result-loading,
.result-empty {
  padding: 24px;
  text-align: center;
  color: var(--theme-text-muted);
  font-size: 13px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  transition: background 0.2s ease;
  cursor: default;

  &:hover {
    background: var(--theme-menu-hover);
  }

  .result-avatar {
    width: 44px;
    height: 44px;
    border-radius: 5px;
    object-fit: cover;
    flex-shrink: 0;
  }

  .result-info {
    flex: 1;
    min-width: 0;
  }

  .result-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--theme-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .result-username {
    font-size: 12px;
    color: var(--theme-text-muted);
  }

  .result-action {
    flex-shrink: 0;
  }
}

/* 未搜索时的空状态 */
.main-empty {
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
