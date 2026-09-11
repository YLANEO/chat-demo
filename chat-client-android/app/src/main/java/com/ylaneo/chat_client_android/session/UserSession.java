package com.ylaneo.chat_client_android.session;

import com.ylaneo.chat_client_android.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户会话（与桌面端 UserSession 对应）
 *
 * 单例，登录后保存当前用户，退出登录时清空。
 * 注意：不要在这里保存密码等敏感信息。
 */
public class UserSession {

    private static volatile UserSession instance;

    private User currentUser;
    private final List<Runnable> onLogoutListeners = new ArrayList<>();

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Integer getCurrentUserId() {
        return currentUser != null ? currentUser.getUserid() : null;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
        for (Runnable r : onLogoutListeners) {
            try {
                r.run();
            } catch (Exception ignore) {
            }
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null && currentUser.getUserid() != null;
    }

    /**
     * 注册退出登录监听器（用于关闭推送连接等）
     */
    public void addOnLogoutListener(Runnable listener) {
        onLogoutListeners.add(listener);
    }

    public void removeOnLogoutListener(Runnable listener) {
        onLogoutListeners.remove(listener);
    }
}
