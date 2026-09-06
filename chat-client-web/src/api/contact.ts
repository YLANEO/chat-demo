import request from './request'
import type { User, FriendRequest } from '@/types/api'  // 见下面类型补充

// 获取所有好友
export const getFriendListService = () =>
  request.get<User[]>('/contact/friends')
// 根据关键词搜索好友
export const searchUserService = (keyword: string) =>
  request.get<User[]>('/contact/search', { params: { keyword } })
// 发送好友申请
export const sendFriendRequestService = (receiverId: number, message: string) =>
  request.post<void>('/contact/request/send', { receiverId, message })
// 获取所有好友申请
export const getFriendRequestListService = () =>
  request.get<FriendRequest[]>('/contact/requests')
// 处理好友申请
export const handleFriendRequestService = (
  requestId: number,
  senderId: number,
  status: 1 | 2,    // 1=同意 2=拒绝
) => request.post<void>('/contact/request/handle', { requestId, senderId, status })
//检测两人是否为好友，返回 0 或 1
export const isFriendService = (userId: number, friendId: number): Promise<number> =>
  request.get<number>('/contact/is-friend', { params: { userId, friendId } })
//检测是否已发送过待处理的好友申请
export const hasPendingRequestService = (senderId: number, receiverId: number): Promise<boolean> =>
  request.get<boolean>('/contact/has-pending-request', { params: { senderId, receiverId } })
