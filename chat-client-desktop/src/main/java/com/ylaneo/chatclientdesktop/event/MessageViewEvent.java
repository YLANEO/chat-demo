package com.ylaneo.chatclientdesktop.event;

import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.util.ResponseHandler;
import com.ylaneo.chatclientdesktop.view.MainView;
import com.ylaneo.chatclientdesktop.view.pages.ContactView;
import com.ylaneo.chatclientdesktop.view.pages.MessageView;

import javax.swing.*;
import java.awt.*;
import java.util.List;

//为了分离网络层操作与ui界面操作，从PushListener中剥离出来的ui更新内容
public class MessageViewEvent {
    //
    ResponseHandler responseHandler = new ResponseHandler();
    UserFacade userFacade=new UserFacadeImpl();

    //
    public void handleRefreshMessageViewFriendStatus(String msg){
        List<String> params = responseHandler.handleParams(msg);

        if (params.size() < 4) {
            return;
        }

        Integer userId;
        Integer status;

        try {
            userId = Integer.valueOf(params.get(2));
            status = Integer.valueOf(params.get(3));
        } catch (NumberFormatException e) {
            return;
        }

        Integer finalUserId = userId;
        Integer finalStatus = status;

        SwingUtilities.invokeLater(() -> refreshMessageViewFriendStatus(finalUserId, finalStatus));//更新：根据要修改的好友的id和status，
    }

    //更新mainview-contactview窗口中的显示信息
    private void refreshMessageViewFriendStatus(Integer userId, Integer status) {//更新好友在线状态
        Window[] windows = Window.getWindows();//获得当前的所有窗口
        //
        for (Window window : windows) {
            if (!(window instanceof MainView)) {//如果找到的窗口不是MainView的一个实例
                continue;//直接跳过这次循环之后的内容，进入下一次循环
            }
            if (!window.isDisplayable()) continue;//  关键这一行，不然会找到已经被dispose，但是还残留在内存里的窗口。

            MainView mainView = (MainView) window;//找到了主页面mainview

            for (Component component : mainView.contentPanel.getComponents()) {
                if (component instanceof MessageView) {
                    MessageView messageView = (MessageView) component;//找到主页面中的好友页面contactview
                    messageView.updateFriendStatus(userId, status);//更新好友页面：根据好友的id和status字段，更新该好友的在线状态
                    return;
                }
            }
        }
    }

    //更新mainview-contactview窗口中的好友列表信息
    public void handleRefreshMessageViewFriendList(String msg){
        List<String> params = responseHandler.handleParams(msg);

        Integer friendId;//新添加的好友的id，根据id获取好友的信息并填入好友列表中
        try {
            friendId = Integer.valueOf(params.get(2));
        } catch (NumberFormatException e) {
            return;
        }

        //把网络调用挪进 EDT，避免阻塞 PushListener
        SwingUtilities.invokeLater(() -> {
            User friend = userFacade.getUserData(friendId).getData();
            if (friend == null) {
                return;
            }
            refreshMessageViewFriendList(friend);//更新：根据要修改的好友的id和status，
        });
    }
    //更新mainview-contactview窗口中的好友列表信息
    private void refreshMessageViewFriendList(User friend) {//更新好友在线状态
        //
        //
        Window[] windows = Window.getWindows();//获得当前的所有窗口
        for (Window window : windows) {
            if (!(window instanceof MainView)) {//如果找到的窗口不是MainView的一个实例
                continue;//直接跳过这次循环之后的内容，进入下一次循环
            }
            if (!window.isDisplayable()) continue;//  关键这一行，不然会找到已经被dispose，但是还残留在内存里的窗口。

            MainView mainView = (MainView) window;//找到了主页面mainview

            for (Component component : mainView.contentPanel.getComponents()) {
                if (component instanceof MessageView) {
                    MessageView messageView = (MessageView) component;//找到主页面中的好友页面contactview
                    messageView.addFriend(friend);//更新好友页面：根据好友的id和status字段，更新该好友的在线状态
                    return;
                }
            }
        }
    }

}
