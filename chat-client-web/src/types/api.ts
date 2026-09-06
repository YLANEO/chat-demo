//与后端Result<T>对齐
export interface Result<T = unknown> {
  code: number       // 0-成功 1-业务错误  2-系统错误
  message: string
  data: T
}

//与后端 entity/User 对齐
export interface User {
  userid: number
  username: string
  password?: string// 前端一般不需要
  nickname: string
  avatar_url: string
  status: number// 在线状态
}

//登录表单
export interface LoginForm {
  username: string
  password: string
  // remember: boolean
}

//注册表单
export interface RegisterForm {
  username: string
  password: string
  rePassword: string
}

// ==========================
// ★ 新增：聊天消息
// ==========================
export interface ChatMessage {
  chatMessageId: number
  senderId: number
  receiverId: number
  type: number   // 0=文本 1=图片 2=文件 3=语音 4=系统
  content: string
  readAt: string | null   // ISO 字符串，未读为 null
  status: number          // 0=成功 1=失败 2=已撤回
  createTime: string
  updateTime: string
}
// ==========================
// ★ 新增：好友申请记录
// ==========================
export interface FriendRequest {
  requestId: number
  senderId: number
  senderUsername: string
  senderNickname: string
  senderAvatar: string
  message: string
  status: number
  createTime: string
}