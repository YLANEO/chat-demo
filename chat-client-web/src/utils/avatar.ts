// 工具函数：把数据库里的头像URL转成浏览器能访问的URL,因为数据库里的头像URL是本地磁盘路径（与chat-client-desktop共用的），
// 而浏览器不能直接访问本地磁盘路径，所以需要转换成浏览器能访问的URL

// 默认头像
const DEFAULT_AVATAR = '/default-avatar.png'

/*
把数据库里查出来的 avatar_url 转成浏览器能加载的 URL
支持三种情况：
1. 本地磁盘路径 D:/files_D/avatar_chat_demo/user_1.png  → /uploads/user_1.png
2. 已经是 HTTP URL  http://xxx/user_1.png                  → 原样返回
3. 空字符串 / null / undefined                            → 默认头像
 */
export function resolveAvatarUrl(rawUrl?: string | null): string {
  if (!rawUrl) return DEFAULT_AVATAR

  // 情况 1：磁盘绝对路径（Windows D:/xxx、C:/xxx 或 Linux /xxx）
  if (/^[A-Za-z]:[\\/]/.test(rawUrl) || rawUrl.startsWith('/') && !rawUrl.startsWith('//')) {
    // 抽出来文件名
    const fileName = rawUrl.split(/[\\/]/).pop() ?? ''
    return `/uploads/${fileName}`// 返回相对路径，Vite代理会转发到后端
  }

  // 情况 2：完整 HTTP(S) URL，直接返回
  if (/^https?:\/\//.test(rawUrl)) {
    return rawUrl
  }

  // 情况 3：其他奇怪情况（比如数据库里就是 /uploads/xxx 这种）
  // 如果看着已经是 URL 路径就直接用
  if (rawUrl.startsWith('/uploads/')) {
    return rawUrl
  }

  // 兜底
  return DEFAULT_AVATAR
}
