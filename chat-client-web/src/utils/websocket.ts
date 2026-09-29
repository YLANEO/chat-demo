import { useUserStore } from '@/stores/userStore'

// type定义一个类型的别名，表示一个处理函数，接受一个参数data，类型为any
type Handler = (data: any) => void

const handleInfo = (mes:string)=>{

}

class ChatSocket {
  private ws: WebSocket | null = null
  // Record就相当于后端的表，键是一个字符串，但是这里的值是一个处理函数的数组
  private handlers: Record<string, Handler[]> = {}
  private reconnectTimer: number | null = null

  connect() {
    // 防止重复连接
    // ?.是可选链运算符：如果this.ws是null或undefined，表达式会返回undefined，不会报错
    if (this.ws?.readyState === WebSocket.OPEN) return
    const userStore = useUserStore()
    const userId = userStore.userId
    if (!userId) return

    // 创建新的连接
    // const protocol = (location.protocol == 'https:') ? 'wss' : 'ws'
    const protocol='ws'
    const url = `${protocol}://${location.host}/ws/chat?userId=${userId}`
    this.ws = new WebSocket(url)//真正创建websocket连接的位置

    this.ws.onopen = () => {
      // 打印成功连接的信息
      console.log('[WS] connected', url)
    }
    // 处理接收到的消息,主要是一个分发消息的功能
    this.ws.onmessage = (event) => {
      console.log(event);

      try {
        const msg = JSON.parse(event.data)
        console.log(msg);

        const list = this.handlers[msg.type] ?? []
        list.forEach(h => h(msg))
      } catch (e) { console.error('[WS] parse error', e) }
    }
    this.ws.onclose = () => {
      console.log('[WS] closed, retry in 3s')
      this.reconnectTimer = window.setTimeout(() => this.connect(), 3000)
    }
    this.ws.onerror = err => console.error('[WS] error', err)
  }

  disconnect() {
    if (this.reconnectTimer) { clearTimeout(this.reconnectTimer); this.reconnectTimer = null }
    this.ws?.close()
    this.ws = null
  }

  send(payload: object) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload))
    }
  }

  // 注册处理器，也就是订阅
  on(type: string, fn: Handler) {
    (this.handlers[type] ||= []).push(fn)
  }

  // 移除处理器，也就是取消订阅
  off(type: string, fn?: Handler) {
    if (!fn) { delete this.handlers[type]; return }
    this.handlers[type] = (this.handlers[type] || []).filter(h => h !== fn)
  }

}

export const chatSocket = new ChatSocket()
