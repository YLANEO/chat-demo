package com.ylaneo.chat_client_android.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 推送长连接监听（与桌面端 PushListener 对应）
 *
 * 登录成功后启动：
 *   1. 与服务器建立长连接
 *   2. 发送 <registerPushSocket><userId> 注册自己
 *   3. 在循环里持续监听服务器推送
 *   4. 收到 ping 时回复 pong
 *   5. 收到业务推送时回调给 PushListener 的监听者（主界面、聊天界面等）
 *
 * 关键点（与桌面端的差异）：
 * - Android UI 操作必须在主线程，所以用 Handler(Looper.getMainLooper()) 切回主线程
 * - 用接口回调让外部页面订阅推送，避免 PushListener 直接持有 Activity 引用导致内存泄漏
 */
public class PushListener implements Runnable {

    private static final String TAG = "PushListener";

    private final Socket socket;
    private volatile boolean running = true;

    // 主线程 Handler：把回调切到主线程，避免在子线程修改 UI
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 监听者列表（外部页面订阅推送）
    private static final List<PushCallback> callbacks = new CopyOnWriteArrayList<>();

    public interface PushCallback {
        /**
         * @param pushType 推送类型：statusUpdate / friendListUpdate / messageAdd
         * @param params   解析后的字段列表（不含 <push> 前缀）
         */
        void onPush(String pushType, List<String> params);
    }

    public PushListener(Socket socket) {
        this.socket = socket;
    }

    /**
     * 注册推送回调（任意线程可调用）
     */
    public static void registerCallback(PushCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    /**
     * 注销推送回调
     */
    public static void unregisterCallback(PushCallback callback) {
        callbacks.remove(callback);
    }

    public void stop() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignore) {
        }
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[65535];

            while (running) {
                int len = is.read(buffer);
                if (len <= 0) {
                    Log.d(TAG, "推送连接已断开，退出监听循环");
                    break;
                }
                String msg = new String(buffer, 0, len).trim();
                Log.d(TAG, "收到推送: " + msg);

                // 1. 心跳：收到 ping 回复 pong
                if ("ping".equals(msg)) {
                    os.write("pong".getBytes());
                    os.flush();
                    Log.d(TAG, "收到 ping，已回复 pong");
                    continue;
                }

                // 2. 业务推送：<push><type><...data>
                if (msg.startsWith("<push>")) {
                    handlePush(msg);
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "推送监听异常: " + e.getMessage());
        } finally {
            Log.d(TAG, "PushListener 线程结束");
        }
    }

    private void handlePush(String msg) {
        List<String> params = ProtocolUtil.parse(msg);
        // params[0] = "push"
        // params[1] = 推送类型 (statusUpdate / friendListUpdate / messageAdd)
        // params[2..] = 数据
        if (params.size() < 2) {
            return;
        }
        String pushType = params.get(1);
        List<String> data = params.subList(2, params.size());

        // 切回主线程分发
        mainHandler.post(() -> {
            for (PushCallback cb : callbacks) {
                try {
                    cb.onPush(pushType, data);
                } catch (Exception e) {
                    Log.e(TAG, "推送回调异常: " + e.getMessage());
                }
            }
        });
    }
}
