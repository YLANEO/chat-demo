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

    //使用volatile的原因是：需要线程间可见，并且不能
    private static volatile UserManager instance;

    private UserManager() {
    }

    //双重检查锁，懒汉式单例
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
        //手动拼接要发送的请求
        String request = "<login>" + "<" + username + ">" + "<" + password + ">";
        try {
            //通过单例模式的TcpClient进行请求的发送和响应信息的获取
            String response = TcpClient.getInstance().sendRequest(request);
            //解析返回的数据，封装成ParsedResponse类对象
            ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
            if (!pr.isSuccess()) {
                throw new RuntimeException(pr.message);
            }
            if (pr.data.isEmpty()) {
                throw new RuntimeException("登录响应没有携带用户信息");
            }
            User user = new User();
            try {
                user.setUserid(Integer.parseInt(pr.data.get(0)));
            } catch (NumberFormatException e) {
                throw new RuntimeException("登录响应中的userId格式错误");
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
            throw new RuntimeException("状态更新异常: " + e.getMessage(), e);
        }
    }

    /**
     * 获取单个用户的完整资料
     * 服务端 <getUserData> 接口返回格式：<0><msg><username><nickname><avatar_url><status>
     *
     * 用于聊天页面进入时刷新当前用户自己的头像等场景。
     * 失败/解析异常返回 null，调用方按需处理。
     */
    public User getUserData(int userId) {
        String request = "<getUserData>" + "<" + userId + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
            if (!pr.isSuccess() || pr.data.size() < 4) {
                return null;
            }
            User u = new User();
            u.setUserid(userId);
            u.setUsername(pr.data.get(0));
            u.setNickname(pr.data.get(1));
            u.setAvatarUrl(pr.data.get(2));
            try {
                u.setStatus(Integer.parseInt(pr.data.get(3)));
            } catch (NumberFormatException ignore) {
            }
            return u;
        } catch (IOException e) {
            return null;
        }
    }
}
