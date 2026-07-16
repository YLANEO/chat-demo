package com.ylaneo.chatclientdesktop.view;

import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.session.UserSession;
import com.ylaneo.chatclientdesktop.view.pages.UserInfoView;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class MainView extends JFrame {
    // ======================================== 组件 ========================================
    CardLayout cardLayout=new CardLayout();
    JPanel contentPanel=new JPanel(cardLayout);//内容显示部分

    //全局按钮统一定义在这里，方便后面按钮功能统一管理
    JButton navMessage,navContact,navUserInfo,jButtonLogout;

    // ======================================== 整体布局 ========================================
    public MainView(){
        setTitle("主页面");
        setSize(1600,1000);
        setLocationRelativeTo(null);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        nav.setPreferredSize(new Dimension(120,20));//设置尺寸
        nav.setBackground(new Color(220,220,220));//导航栏背景颜色设置
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
        navMessage.addActionListener(e->cardLayout.show(contentPanel,"message"));
        navContact.addActionListener(e->cardLayout.show(contentPanel,"contact"));
        navUserInfo.addActionListener(e->cardLayout.show(contentPanel,"userinfo"));
        //
        return nav;
    }
    // ======================================== 统一添加按钮的方法 ========================================
    JButton createNavButton(String buttonText){
        JButton jButton=new JButton(buttonText);
        jButton.setHorizontalAlignment(SwingConstants.LEFT);//对齐方式
        jButton.setPreferredSize(new Dimension(180,40));//
        jButton.setFont(new Font("黑体",Font.BOLD,15));
        jButton.setMaximumSize(new Dimension(9999,40));//横向占满该栏
        jButton.setFocusPainted(false);
        jButton.setBorderPainted(true);//边框
        jButton.setContentAreaFilled(false);//填充
        jButton.setCursor(new Cursor(Cursor.HAND_CURSOR));//鼠标悬停
        //
        jButton.addMouseListener(new MouseAdapter() {
            //悬停
            @Override
            public void mouseEntered(MouseEvent e) {
//                super.mouseEntered(e);
                jButton.setBackground(new Color(255, 255, 255));
                jButton.setOpaque(true);
            }
            //移开
            @Override
            public void mouseExited(MouseEvent e) {
//                super.mouseExited(e);
                jButton.setOpaque(false);
            }
        });

        return jButton;
    }

    // ======================================== 创建内容部分并返回 ========================================
    JPanel contentArea(){
        //
        // contentPanel.add(new ChatView(),    "MESSAGE");
        // contentPanel.add(new ContactView(), "CONTACT");
        contentPanel.add(new UserInfoView(),"userinfo");
        return contentPanel;
    }
    // ======================================== 统一进行按钮功能绑定 ========================================
    //退出登录
    public void bindButtonActions(){
        jButtonLogout.addActionListener(e->{
            //
            UserFacade userFacade=new UserFacadeImpl();
            userFacade.updateUserStatus(UserSession.getInstance().getCurrentUser().getUserid(),0);
            UserSession.getInstance().logout();
            dispose();//关闭当前窗口
            new LoginView();
        });
    }
}

