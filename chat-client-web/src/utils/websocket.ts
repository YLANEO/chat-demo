import { useUserStore } from '@/stores/userStore'

type Handler = (data: any) => void

class ChatSocket {
  private ws: WebSocket | null = null
  private handlers: Record<string, Handler[]> = {}
  private reconnectTimer: number | null = null
  private heartbeatTimer: number | null = null

  connect() {
    if (this.ws?.readyState === WebSocket.OPEN) return
    const userStore = useUserStore()
    const userId = userStore.userId
    if (!userId) return

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const url = `${protocol}://${location.host}/ws/chat?userId=${userId}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      console.log('[WS] connected', url)
      this.startHeartbeat()
    }
    this.ws.onmessage = ev => {
      try {
        const msg = JSON.parse(ev.data)
        const list = this.handlers[msg.type] ?? []
        list.forEach(h => h(msg))
      } catch (e) { console.error('[WS] parse error', e) }
    }
    this.ws.onclose = () => {
      console.log('[WS] closed, retry in 3s')
      this.stopHeartbeat()
      this.reconnectTimer = window.setTimeout(() => this.connect(), 3000)
    }
    this.ws.onerror = err => console.error('[WS] error', err)
  }

  disconnect() {
    if (this.reconnectTimer) { clearTimeout(this.reconnectTimer); this.reconnectTimer = null }
    this.stopHeartbeat()
    this.ws?.close()
    this.ws = null
  }

  send(payload: object) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload))
    }
  }

  on(type: string, fn: Handler) {
    ; (this.handlers[type] ||= []).push(fn)
  }

  off(type: string, fn?: Handler) {
    if (!fn) { delete this.handlers[type]; return }
    this.handlers[type] = (this.handlers[type] || []).filter(h => h !== fn)
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => this.send({ type: 'ping' }), 25000)
  }
  private stopHeartbeat() {
    if (this.heartbeatTimer) { clearInterval(this.heartbeatTimer); this.heartbeatTimer = null }
  }
}

export const chatSocket = new ChatSocket()
