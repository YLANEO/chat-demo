package com.ylaneo.chatclientdesktop;

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
//        new MainView();
    }
}