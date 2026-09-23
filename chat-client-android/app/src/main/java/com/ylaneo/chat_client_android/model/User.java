package com.ylaneo.chat_client_android.model;

/**
 * 用户实体（与桌面端 User 对应）
 *
 * 字段：
 *   userid      用户 ID
 *   username    用户名
 *   password    密码（登录后不再需要保存）
 *   nickname    昵称
 *   avatarUrl   头像 URL
 *   status      状态：0=离线 1=在线
 */
public class User {
    private Integer userid;
    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;
    private Integer status;

    public User() {
    }

    public User(Integer userid, String username, String password, String nickname, String avatarUrl, Integer status) {
        this.userid = userid;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.status = status;
    }

    public Integer getUserid() {
        return userid;
    }

    public void setUserid(Integer userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 列表显示用：优先昵称，没有则用户名
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        }
        return username != null ? username : "未知用户";
    }

    /**
     * 列表显示用：displayName + @username（同名时也照常拼接）
     */
    public String getDisplayNameWithAt() {
        String name = getDisplayName();
        if (username != null && !username.isEmpty()) {
            return name + "   @" + username;
        }
        return name;
    }
}
