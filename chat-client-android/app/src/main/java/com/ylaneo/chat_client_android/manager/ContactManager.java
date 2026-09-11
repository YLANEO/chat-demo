package com.ylaneo.chat_client_android.manager;

import com.ylaneo.chat_client_android.model.FriendRequestDto;
import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.network.ProtocolUtil;
import com.ylaneo.chat_client_android.network.TcpClient;
import com.ylaneo.chat_client_android.util.EntityParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 联系人/好友相关业务封装（对应桌面端 ContactFacadeImpl）
 */
public class ContactManager {

    private static volatile ContactManager instance;

    private ContactManager() {
    }

    public static ContactManager getInstance() {
        if (instance == null) {
            synchronized (ContactManager.class) {
                if (instance == null) {
                    instance = new ContactManager();
                }
            }
        }
        return instance;
    }

    /**
     * 获取好友列表
     */
    public List<User> getFriendList(int userId) {
        String request = "<getFriendList>" + "<" + userId + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            EntityParser.WrapperResult wr = EntityParser.parseWrapper(response);
            if (!wr.isSuccess()) {
                throw new RuntimeException(wr.message);
            }
            if (wr.dataFields.isEmpty()) {
                return new ArrayList<>();
            }
            List<String> dataFields = wr.dataFields.subList(1, wr.dataFields.size());
            return EntityParser.parseUsers(dataFields);
        } catch (IOException e) {
            throw new RuntimeException("网络异常: " + e.getMessage(), e);
        }
    }

    /**
     * 按关键字搜索用户
     */
    public List<User> searchUserByKeyword(String keyword, int currentUserId) {
        String request = "<searchUserByKeyword>" + "<" + keyword + ">" + "<" + currentUserId + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            EntityParser.WrapperResult wr = EntityParser.parseWrapper(response);
            if (!wr.isSuccess()) {
                throw new RuntimeException(wr.message);
            }
            if (wr.dataFields.isEmpty()) {
                return new ArrayList<>();
            }
            List<String> dataFields = wr.dataFields.subList(1, wr.dataFields.size());
            return EntityParser.parseUsers(dataFields);
        } catch (IOException e) {
            throw new RuntimeException("网络异常: " + e.getMessage(), e);
        }
    }

    /**
     * 发送好友申请
     */
    public void sendFriendRequest(int senderId, int receiverId, String message) {
        String request = "<sendFriendRequest>" + "<" + senderId + ">" + "<" + receiverId + ">" + "<" + message + ">";
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
     * 收到的好友申请列表
     */
    public List<FriendRequestDto> getFriendRequests(int receiverId) {
        String request = "<getFriendRequests>" + "<" + receiverId + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            EntityParser.WrapperResult wr = EntityParser.parseWrapper(response);
            if (!wr.isSuccess()) {
                throw new RuntimeException(wr.message);
            }
            if (wr.dataFields.isEmpty()) {
                return new ArrayList<>();
            }
            List<String> dataFields = wr.dataFields.subList(1, wr.dataFields.size());
            return EntityParser.parseFriendRequests(dataFields);
        } catch (IOException e) {
            throw new RuntimeException("网络异常: " + e.getMessage(), e);
        }
    }

    /**
     * 处理好友申请（同意/拒绝）
     *
     * @param status 1=同意 2=拒绝
     */
    public void handleRequest(int requestId, int senderId, int receiverId, int status) {
        String request = "<handleRequest>" + "<" + requestId + ">" + "<" + senderId + ">" + "<" + receiverId + ">" + "<" + status + ">";
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
     * 判断两个用户是否是好友
     */
    public boolean isFriend(int userId, int friendId) {
        String request = "<isFriend>" + "<" + userId + ">" + "<" + friendId + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
            if (!pr.isSuccess() || pr.data.isEmpty()) {
                return false;
            }
            return "1".equals(pr.data.get(0));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 是否有待处理的好友申请
     */
    public boolean hasPendingRequest(int senderId, int receiverId) {
        String request = "<hasPendingRequest>" + "<" + senderId + ">" + "<" + receiverId + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
            if (!pr.isSuccess() || pr.data.isEmpty()) {
                return false;
            }
            return Boolean.parseBoolean(pr.data.get(0));
        } catch (IOException e) {
            return false;
        }
    }
}
