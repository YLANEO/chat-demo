package com.ylaneo.chat_client_android.util;

/**
 * 全局常量
 */
public class Constants {

    private Constants() {
    }

    // 网络相关（由 TcpClient 引用）
    public static final String SERVER_HOST = "10.20.14.123"; // 后端服务器 IP（与 TcpClient 一致）
//    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 20001;

    // HTTP 基础地址（用于加载头像等静态资源，与 TCP 端口不同）
    public static final String HTTP_BASE_URL = "http://10.20.14.123:8080";

    // 消息类型
    public static final int MESSAGE_TYPE_TEXT = 0;

    // 消息状态
    public static final int MESSAGE_STATUS_UNREAD = 0;
    public static final int MESSAGE_STATUS_READ = 1;

    // 好友申请状态
    public static final int FRIEND_REQUEST_PENDING = 0;
    public static final int FRIEND_REQUEST_ACCEPTED = 1;
    public static final int FRIEND_REQUEST_REJECTED = 2;

    // 用户在线状态
    public static final int USER_STATUS_OFFLINE = 0;
    public static final int USER_STATUS_ONLINE = 1;
}
