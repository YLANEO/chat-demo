package com.ylaneo.chat_client_android.network;

import android.util.Log;

import com.ylaneo.chat_client_android.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * TCP 短连接客户端（与桌面端 RequestSender 对应）
 *
 * 用法：
 *   String resp = TcpClient.getInstance().sendRequest("<login><user><pwd>");
 *
 * 设计说明：
 * - 每次请求建立新连接，响应回来后立即关闭（与桌面端 RequestSender 行为一致）
 * - 网络操作必须在子线程调用
 * - 单例：服务器地址和端口是全局共享的
 */
public class TcpClient {

    private static final String TAG = "TcpClient";

    private static volatile TcpClient instance;

    private final String host;
    private final int port;

    private TcpClient() {
        // 真机/模拟器通过局域网 IP 访问本机后端
        // this.host = "10.0.2.2"; // 仅 Android 模拟器访问宿主机
        this.host = Constants.SERVER_HOST;//真实的ip地址
        this.port = Constants.SERVER_PORT;//请求的后端端口：20001，
    }

    public static TcpClient getInstance() {
        if (instance == null) {
            //同步？
            synchronized (TcpClient.class) {
                if (instance == null) {
                    instance = new TcpClient();
                }
            }
        }
        return instance;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * 发送请求并读取响应（短连接）
     *
     * @param request 协议格式字符串，如 "<login><user><pwd>"
     * @return 服务器原始响应字符串
     * @throws IOException 网络错误
     */
    public synchronized String sendRequest(String request) throws IOException {
        Socket socket = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            Log.d(TAG, "连接服务器 " + host + ":" + port);
            //创建socket对象用于和服务端通信，tcp协议
            socket = new Socket(host, port);
            socket.setSoTimeout(10000); // 10 秒读超时，避免卡死
            //将输入输出流对象与socket对象绑定
            is = socket.getInputStream();
            os = socket.getOutputStream();
            //使用输出流对象进行数据的写入，也就是向服务端发送请求
            os.write(request.getBytes());
            os.flush();
            Log.d(TAG, "发送请求: " + request);
            //创建用于接受服务端数据的
            byte[] buffer = new byte[65535];
            //接收响应的信息
            int len = is.read(buffer);
            if (len <= 0) {
                throw new IOException("服务器无响应");
            }
            //将接收到的响应信息封装到String变量中，并将这个String变量返回给
            String response = new String(buffer, 0, len);
            Log.d(TAG, "收到响应: " + response);
            return response;
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
