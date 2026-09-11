package com.ylaneo.chat_client_android.network;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 推送连接管理器（管理长连接、监听线程、心跳线程）
 *
 * 登录成功后调用 connect()，退出登录时调用 disconnect()
 */
public class PushManager {

    private static final String TAG = "PushManager";

    private static volatile PushManager instance;

    private Socket pushSocket;
    private PushListener pushListener;
    private Thread listenerThread;
    private HeartbeatThread heartbeatThread;
    private volatile boolean heartbeatRunning = false;

    private PushManager() {
    }

    public static PushManager getInstance() {
        if (instance == null) {
            synchronized (PushManager.class) {
                if (instance == null) {
                    instance = new PushManager();
                }
            }
        }
        return instance;
    }

    public boolean isConnected() {
        return pushSocket != null && !pushSocket.isClosed() && pushSocket.isConnected();
    }

    /**
     * 与服务器建立推送长连接，并注册用户身份
     *
     * @param userId 当前登录用户的 ID
     */
    public synchronized void connect(int userId) {
        if (isConnected()) {
            Log.w(TAG, "推送连接已存在，无需重复连接");
            return;
        }
        try {
            TcpClient tcp = TcpClient.getInstance();
            pushSocket = new Socket(tcp.getHost(), tcp.getPort());
            pushSocket.setKeepAlive(true);

            // 注册推送身份
            String registerMsg = "<registerPushSocket><" + userId + ">";
            OutputStream os = pushSocket.getOutputStream();
            os.write(registerMsg.getBytes());
            os.flush();
            Log.d(TAG, "推送连接建立成功，已注册 userId=" + userId);

            // 启动监听线程
            pushListener = new PushListener(pushSocket);
            listenerThread = new Thread(pushListener, "PushListener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            // 启动心跳线程
            heartbeatRunning = true;
            heartbeatThread = new HeartbeatThread();
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();
        } catch (IOException e) {
            Log.e(TAG, "推送连接建立失败: " + e.getMessage());
            disconnect();
        }
    }

    /**
     * 断开推送连接
     */
    public synchronized void disconnect() {
        heartbeatRunning = false;

        if (pushListener != null) {
            pushListener.stop();
            pushListener = null;
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        if (pushSocket != null && !pushSocket.isClosed()) {
            try {
                pushSocket.close();
            } catch (IOException ignore) {
            }
            pushSocket = null;
        }
        Log.d(TAG, "推送连接已断开");
    }

    /**
     * 心跳线程：每 30 秒发送一次 ping
     */
    private class HeartbeatThread extends Thread {
        @Override
        public void run() {
            while (heartbeatRunning) {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    break;
                }
                if (pushSocket == null || pushSocket.isClosed()) {
                    Log.d(TAG, "心跳线程检测到连接已关闭，退出");
                    break;
                }
                try {
                    OutputStream os = pushSocket.getOutputStream();
                    os.write("ping".getBytes());
                    os.flush();
                    Log.d(TAG, "已发送心跳 ping");
                } catch (IOException e) {
                    Log.w(TAG, "心跳发送失败: " + e.getMessage());
                    break;
                }
            }
        }
    }
}
