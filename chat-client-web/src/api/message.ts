import request from './request'
import type { ChatMessage } from '@/types/api'   // 见 3.2 节的类型定义

// 获取历史消息，与后端ChatMessage字段对齐
export const getMessageHistoryService = (friendId: number) =>
  request.get<ChatMessage[]>(`/message/history`, { params: { friendId } })
// 分页获取历史消息
export const getMessagePageService = (friendId: number, lastMessageId?: number, limit = 20) =>
  request.get<ChatMessage[]>(`/message/page`, { params: { friendId, lastMessageId, limit } })
// 发送消息
export const sendMessageService = (friendId: number, type: number, content: string) =>
  request.post<ChatMessage>(`/message/send`, { friendId, type, content })
// 标记消息为已读
export const markReadService = (friendId: number) =>
  request.post<number>(`/message/read`, { friendId })
// 获取未读消息数
export const getUnreadCountService = (friendId: number) =>
  request.get<number>(`/message/unread-count`, { params: { friendId } })
// 获得未读消息数
export const getTotalUnreadService = () =>
  request.get<number>(`/message/total-unread`)
