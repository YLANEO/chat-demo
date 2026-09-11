package com.ylaneo.chat_client_android.manager;

import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.network.ProtocolUtil;
import com.ylaneo.chat_client_android.network.TcpClient;

import java.io.IOException;
import java.util.List;

/**
 * 用户相关业务封装（对应桌面端 UserFacadeImpl）
 *
 * 所有方法都在子线程调用，返回值是封装好的业务对象
 * 异常通过抛出 RuntimeException 传递给调用者
 */
public class UserManager {

    private static volatile UserManager instance;

    private UserManager() {
    }

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;
    }

    /**
     * 登录
     *
     * @return 完整的 User 信息（包含 userid）
     */
    public User login(String username, String password) {
        String request = "<login>" + "<" + username + ">" + "<" + password + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
            if (!pr.isSuccess()) {
                throw new RuntimeException(pr.message);
            }
            if (pr.data.isEmpty()) {
                throw new RuntimeException("登录响应缺少用户 ID");
            }
            User user = new User();
            try {
                user.setUserid(Integer.parseInt(pr.data.get(0)));
            } catch (NumberFormatException e) {
                throw new RuntimeException("登录响应 userId 格式错误");
            }
            user.setUsername(username);
            user.setStatus(1); // 登录成功默认在线
            return user;
        } catch (IOException e) {
            throw new RuntimeException("网络异常: " + e.getMessage(), e);
        }
    }

    /**
     * 注册
     */
    public void register(String username, String password, String repassword) {
        String request = "<register>" + "<" + username + ">" + "<" + password + ">" + "<" + repassword + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
            if (!pr.isSuccess()) {
                throw new RuntimeException(pr.message);
            }
        } catch (IOException e) {
            throw new RuntimeException("网络异常: " + e.getMessage(), e);
        }
    }

    /**
     * 更新在线状态
     */
    public void updateUserStatus(int userId, int status) {
        String request = "<updateUserStatus>" + "<" + userId + ">" + "<" + status + ">";
        try {
            TcpClient.getInstance().sendRequest(request);
        } catch (IOException e) {
            // 状态更新失败不抛异常
        }
    }
}
