package com.ylaneo.chatclientdesktop.view;

import com.sun.tools.javac.Main;
import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.net.HeartbeatSender;
import com.ylaneo.chatclientdesktop.session.UserSession;
import com.ylaneo.chatclientdesktop.view.pages.ContactView;
import com.ylaneo.chatclientdesktop.view.pages.MessageView;
import com.ylaneo.chatclientdesktop.view.pages.UserInfoView;
import com.ylaneo.chatclientdesktop.view.pages.UserInfoView_1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;

public class MainView extends JFrame {
    // ======================================== 组件 ========================================
    public CardLayout cardLayout=new CardLayout();
    public JPanel contentPanel=new JPanel(cardLayout);//内容显示部分

    //全局按钮统一定义在这里，方便后面按钮功能统一管理
    JButton navMessage,navContact,navUserInfo,jButtonLogout;

    // ======================================== 整体布局 ========================================
    public MainView(){
        setTitle("主页面");
        setSize(1600,1000);
        setLocationRelativeTo(null);
        setVisible(true);
        //监听窗口关闭事件，并将用户状态修改为离线
//        addWindowListener(new WindowAdapter() {
//            //
//            @Override
//            public void windowClosing(WindowEvent e) {
//                //获取当前用户信息，并根据该信息修改数据库中的在线状态
//                UserFacade userFacade=new UserFacadeImpl();
//                userFacade.updateUserStatus(UserSession.getInstance().getCurrentUser().getUserid(),0);
//            }
//        });
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //修改窗口关闭事件：手动结束线程（Pushlistener和HeartbeatSender）。而非直接结束整个进程，导致线程可能没有正确结束
//        addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent e) {
//                // 1. 清理 socket
//                Socket pushSocket = UserSession.getInstance().getPushSocket();
//                if (pushSocket != null && !pushSocket.isClosed()) {
//                    try {
//                        pushSocket.close();
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//                }
//                // 2. 改数据库状态
//                if (UserSession.getInstance().isLoggedIn()) {
//                    new UserFacadeImpl().updateUserStatus(
//                            UserSession.getInstance().getCurrentUser().getUserid(),
//                            0
//                    );
//                }
//                // 3. 退出
//                System.exit(0);
//            }
//        });
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);  // 禁用默认退出，自己控制
        // TODO: 2026/10/1 自定义这个关闭按钮添加停止心跳线程和监听线程的功能
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                doLogout();
            }

//            @Override
//            public void windowClosing(WindowEvent e) {
//                //弹出提示框，确认后再登出
//                int confirmDelete=JOptionPane.showConfirmDialog(MainView.this,"确认退出登录吗","确认退出登录窗口",JOptionPane.YES_NO_OPTION);//
//                if(confirmDelete!=JOptionPane.YES_OPTION){//没有选择确认按钮
//                    return;
//                }
//                UserFacade userFacade=new UserFacadeImpl();
//                userFacade.updateUserStatus(UserSession.getInstance().getCurrentUser().getUserid(),0);
//                UserSession.getInstance().logout();
//                //新增：关键！！！！！关闭socket连接，间接结束了推送PushListener线程和心跳线程HeartbeatSender,其实没有结束HeartBeatSender线程
//                Socket pushSocket = UserSession.getInstance().getPushSocket();
//                if (pushSocket != null && !pushSocket.isClosed()) {
//                    try {
//                        //
//                        //结束HeartBeatSender线程,使用UserSession拿到这个单例线程？
//                        UserSession.getInstance().getHeartbeatSender().closeNow();
//                        //
//                        pushSocket.close();//直接进行一个socket的关闭
//                        System.out.println("关闭socket长连接");
//
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//                }
//
//                //
//                dispose();//关闭当前窗口
//            }


            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });

        //修改Jframe的默认内容容器为一个Jpanel，方便后面控制，相当于背景板
        JPanel backPanel=new JPanel();
        backPanel.setLayout(new BorderLayout());
        setContentPane(backPanel);//使用root对Jframe进行设置
        //定义出主体内容部分，并将导航侧栏和中间部分添加到主体内容部分
        JPanel centerPanel=new JPanel(new BorderLayout());
        centerPanel.add(createNavSideBar(),BorderLayout.WEST);//将导航栏添加到主页面的左侧
        centerPanel.add(contentArea(),BorderLayout.CENTER);//
        //
        backPanel.add(centerPanel,BorderLayout.CENTER);
        setVisible(true);
        //为按钮绑定功能
        bindButtonActions();
    }
    // ======================================== 导航栏统一管理 ========================================
    JPanel createNavSideBar(){
        //
        JPanel nav=new JPanel();
        nav.setLayout(new BoxLayout(nav,BoxLayout.Y_AXIS));//导航按钮纵向排列
        nav.setPreferredSize(new Dimension(98,40));//设置尺寸
//        nav.setBackground(new Color(220,220,220));//导航栏背景颜色设置
        nav.setBackground(new Color(175, 159, 248));//导航栏背景颜色设置
//        nav.setBorder(BorderFactory.createLineBorder(new Color(138, 138, 138), 1));
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, new Color(153, 133, 255)));//只创建右边的边框
        //创建并添加按钮
        navMessage=createNavButton("消息");
        navContact=createNavButton("好友");
        navUserInfo=createNavButton("个人信息");
        //退出登录按钮
        jButtonLogout=createNavButton("退出登录");
        //
        nav.add(navMessage);
        nav.add(navContact);
        nav.add(navUserInfo);
        //
        nav.add(Box.createVerticalGlue());//添加弹性空间，将下方按钮推到最底部
        //
        nav.add(jButtonLogout);
        //切换卡片功能
        navMessage.addActionListener(e->{
            cardLayout.show(contentPanel,"message");
            updateNavButtonStyles(navMessage);
        });
        navContact.addActionListener(e->{
            cardLayout.show(contentPanel,"contact");
            updateNavButtonStyles(navContact);
        });
        navUserInfo.addActionListener(e->{
            cardLayout.show(contentPanel,"userinfo");
            updateNavButtonStyles(navUserInfo);
        });
        //
        return nav;
    }
    // ======================================== 统一添加按钮的方法 ========================================
    //统一创建按钮的方法
    JButton createNavButton(String buttonText){
        JButton jButton=new JButton(buttonText);
        jButton.setHorizontalAlignment(SwingConstants.LEFT);//对齐方式
        jButton.setPreferredSize(new Dimension(80,40));//
        jButton.setFont(new Font("微软雅黑",Font.BOLD,15));
        jButton.setMaximumSize(new Dimension(200,40));//横向占满该栏
        jButton.setFocusPainted(false);
        jButton.setBorderPainted(false);//边框
        jButton.setContentAreaFilled(false);//填充
        jButton.setCursor(new Cursor(Cursor.HAND_CURSOR));//鼠标悬停

        //保存默认背景色
        Color defaultBg = new Color(175, 159, 248);
        jButton.setOpaque(true);
        jButton.setBackground(defaultBg);
        //标记为非选中状态
        jButton.putClientProperty("selected", false);
        //
        jButton.addMouseListener(new MouseAdapter() {
            //悬停
            @Override
            public void mouseEntered(MouseEvent e) {
//                super.mouseEntered(e);
                jButton.setBackground(new Color(255, 255, 255));
                jButton.setOpaque(true);//设置为透明
            }
            //移开
            @Override
            public void mouseExited(MouseEvent e) {
//                super.mouseExited(e);
                jButton.setOpaque(true);//设置为不透明
                jButton.setBackground(defaultBg);//恢复成默认背景颜色
                // 判断是否是选中状态
                Boolean isSelected = (Boolean) jButton.getClientProperty("selected");
                if (isSelected != null && isSelected) {
                    // 是选中按钮，恢复选中颜色
                    jButton.setBackground(new Color(218, 132, 239));
                } else {
                    // 非选中按钮，恢复默认颜色
                    jButton.setBackground(new Color(175, 159, 248));
                }
            }
        });

        return jButton;
    }
    //统一修改按钮样式的方法(点击按钮改变其背景颜色，方便区别当前页面)
    private void updateNavButtonStyles(JButton selectedButton) {
        // 默认样式
        Color defaultBg = new Color(175, 159, 248);  // 与导航栏背景色一致
        Color selectedBg = new Color(218, 132, 239);  // 更深的颜色表示选中

        // 重置所有按钮
        JButton[] allButtons = {navMessage, navContact, navUserInfo};
        //
        for (JButton btn : allButtons) {
            btn.putClientProperty("selected", false);// 标记其他按钮为为非选中
            btn.setBackground(defaultBg);
        }
        // 标记当前按钮为选中状态
        selectedButton.putClientProperty("selected", true);
        selectedButton.setBackground(selectedBg);
    }
    // ======================================== 创建内容部分并返回 ========================================
    JPanel contentArea(){
        //
        contentPanel.add(new MessageView(),    "message");
        contentPanel.add(new ContactView(), "contact");
        contentPanel.add(new UserInfoView(),"userinfo");
        contentPanel.add(new UserInfoView_1(),"userinfo1");
        return contentPanel;
    }
    // ======================================== 统一进行按钮功能绑定 ========================================
    //退出登录
//    public void bindButtonActions(){
//        jButtonLogout.addActionListener(e->{
//            //弹出提示框，确认后再登出
//            int confirmDelete=JOptionPane.showConfirmDialog(this,"确认退出登录吗","确认退出登录窗口",JOptionPane.YES_NO_OPTION);//
//            if(confirmDelete!=JOptionPane.YES_OPTION){//没有选择确认按钮
//                return;
//            }
//            UserFacade userFacade=new UserFacadeImpl();
//            userFacade.updateUserStatus(UserSession.getInstance().getCurrentUser().getUserid(),0);
//            UserSession.getInstance().logout();
//            //新增：关键！！！！！关闭socket连接，间接结束了推送PushListener线程和心跳线程HeartbeatSender,其实没有结束HeartBeatSender线程
//            Socket pushSocket = UserSession.getInstance().getPushSocket();
//            if (pushSocket != null && !pushSocket.isClosed()) {
//                try {
//                    //
//                    //结束HeartBeatSender线程,使用UserSession拿到这个单例线程？
//                    UserSession.getInstance().getHeartbeatSender().closeNow();
//                    try {
//                        UserSession.getInstance().getHeartbeatSender().join(500);   // 最多等 500ms，**确保线程结束 + 日志已打印**
//                    } catch (InterruptedException ignored) {
//                        System.out.println("[InterruptedException-Mainview]");
//                    }
//                    //
//                    pushSocket.close();//直接进行一个socket的关闭
//                    System.out.println("关闭socket长连接");
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//
//            //
//            dispose();//关闭当前窗口
//            new LoginView();
//        });
//    }
    public void bindButtonActions(){
        jButtonLogout.addActionListener(e->{
            doLogout();
            new LoginView();
        });
    }
    // ======================================== 全局的函数 ========================================
    // MainView 类内
    private void doLogout() {
        //提示框
        int confirmDelete = JOptionPane.showConfirmDialog(this, "确认退出登录吗", "确认退出登录窗口", JOptionPane.YES_NO_OPTION);
        if (confirmDelete != JOptionPane.YES_OPTION) return;
        //
        new UserFacadeImpl().updateUserStatus(UserSession.getInstance().getCurrentUser().getUserid(), 0);
        UserSession.getInstance().logout();
        //拿到心跳线程
        HeartbeatSender heartbeatSender = UserSession.getInstance().getHeartbeatSender();

        // 2. 关 socket
        Socket pushSocket = UserSession.getInstance().getPushSocket();
        if (pushSocket != null && !pushSocket.isClosed()) {
            try {
                pushSocket.close();
                System.out.println("关闭socket长连接");
                System.out.println("[Mainview]"+(heartbeatSender.isAlive()?"heartbeatSender还是活的":"heartbeatSender死了")+ LocalDateTime.now());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // 1. 停心跳线程

        System.out.println("[Mainview]"+(heartbeatSender.isAlive()?"heartbeatSender还是活的":"heartbeatSender死了")+ LocalDateTime.now());
        if (heartbeatSender != null && heartbeatSender.isAlive()) {
            heartbeatSender.closeNow();
            try {
                heartbeatSender.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[Mainview]"+(heartbeatSender.isAlive()?"heartbeatSender还是活的":"heartbeatSender死了")+ LocalDateTime.now());


        // 3. 关闭窗口 + 开登录页
        dispose();

    }
}

