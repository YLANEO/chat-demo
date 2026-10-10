package com.ylaneo.chatclientdesktop;

import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.session.UserSession;
import com.ylaneo.chatclientdesktop.view.LoginView;

public class RunClient {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        //设置外观
//        try {
//            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //创建窗口
        new LoginView();

        //添加直接关闭这个进程动作的监听与相应处理
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try{
                if(UserSession.getInstance().getCurrentUser()!=null){
                    //更新用户在线状态
                    new UserFacadeImpl().updateUserStatus(UserSession.getInstance().getCurrentUser().getUserid(),0);
                }
            }catch (Exception ignored){
                //shutdown 钩子里面不要抛异常，也不能阻塞太久，JVM只给几秒的时间，超时会直接被杀？
            }
        }));
    }
}