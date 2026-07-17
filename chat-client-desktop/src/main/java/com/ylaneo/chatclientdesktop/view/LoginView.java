package com.ylaneo.chatclientdesktop.view;



import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.session.UserSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginView extends JFrame {
// ======================================== 组件定义 ========================================
    //页面组件
    JPanel loginWindow;
    JPanel registerWindow;
    //标签、表格文本框和提交按钮
    private JLabel jLabelusername,jLabelpassword,jLabelrePassword;
    private JButton loginButton,registerButton,returnloginButton,gotoRegisterButton;
    //登录注册页面不能共用的组件分别定义
    //登录页面组件定义
    private JTextField loginUserName;
    private JPasswordField loginPassword;
    //注册页面组件定义
    private JTextField registerUserName;
    private JPasswordField registerPassword,registerRePassword;

// ======================================== 登录页面 ========================================
    //初始化组件
    public void initLoginWindow(){
        //将页面变量初始化，方便在类中使用
        loginWindow=new JPanel();
        //整体使用BoxLayout布局，然后分行进行填充，每行都使用FlowLayout布局
        //整体布局设置，注意BoxLayout的构造方式,具体参数见参考文档
        loginWindow.setLayout(new BoxLayout(loginWindow ,BoxLayout.Y_AXIS));
//        loginWindow.setLayout(new GridLayout(4,1,10,10));
        //所有组件定义和组件的初始化
        jLabelusername=new JLabel("用户名");
        jLabelusername.setPreferredSize(new Dimension(70, 25));//设置统一的宽度进行对齐
        jLabelpassword=new JLabel("密码");
        jLabelpassword.setPreferredSize(new Dimension(70, 25));//设置统一的宽度进行对齐
        loginUserName=new JTextField(30);
        loginPassword=new JPasswordField(30);
        loginButton=new JButton("登录");
        gotoRegisterButton=new JButton("去注册新用户");

        JPanel row0=new JPanel(new FlowLayout(FlowLayout.CENTER));
        row0.add(Box.createVerticalStrut(4));//
        //
        JPanel row1=new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel text1=new JLabel("登录页面");
        text1.setFont(new Font("宋体",Font.BOLD,20));
        row1.add(text1 );//
        //第二行
        JPanel row2=new JPanel(new FlowLayout());
        row2.setBounds(0,0,0,0);
        row2.add(jLabelusername);
        row2.add(loginUserName);
        //
        JPanel row3=new JPanel(new FlowLayout());
        row3.add(jLabelpassword);
        row3.add(loginPassword);
        //
        JPanel row5=new JPanel(new FlowLayout());
        row5.add(loginButton);
        row5.add(gotoRegisterButton);
        //添加组件到Jframe中，顺序添加即可，添加那两个Jpanel
        loginWindow.add(row0);
        loginWindow.add(row1);
        loginWindow.add(row2);
        loginWindow.add(row3);
        loginWindow.add(row5);

        // 为按钮绑定监听事件
        /*gotoRegisterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CardLayout cardLayout=(CardLayout) getContentPane().getLayout();//获得当前Jframe的布局对象
                cardLayout.show(getContentPane(),"REGISTER");
                setTitle("注册页面");
            }
        });*/

        //写法二：lambda表达式
        gotoRegisterButton.addActionListener(e->{
            CardLayout cardLayout=(CardLayout) getContentPane().getLayout();//获得当前Jframe的布局对象
            cardLayout.show(getContentPane(),"REGISTER");
            setTitle("注册页面");
        });

        /* 修改 登录逻辑：登录按钮绑定监听事件
        把直接调用dao获得封装好的result修改为借助封装好的类获得需要的信息   */
        loginButton.addActionListener(e->{
            String username=loginUserName.getText();
            String password=new String(loginPassword.getPassword());
            //直接调用client中service层的userservice中的接口，还是获得Result类型的数据
            UserFacade userFacade =new UserFacadeImpl();
            Result<User> r= userFacade.login(username,password);
            User u=r.getData();
            //登录成功,登陆成功才去统一取r里面的数据，不然有可能是空的导致报错
            if(r.getCode()==0){
                //
                JOptionPane.showMessageDialog(this,r.getMessage());//显示提示信息
                //创建一个User实例，并为他的属性赋值，便于实现用户信息状态管理
                User user=new User();
                user.setUserid(u.getUserid());
                user.setUsername(username);
                user.setStatus(1);
                //单例模式
                UserSession.getInstance().login(user);
                //切换用户在线状态
                userFacade.updateUserStatus(user.getUserid(), 1);
                //关闭当前页面并打开主页面
                this.dispose();
                new MainView();
            }
            //失败
            if(r.getCode()==1){
                JOptionPane.showMessageDialog(this,r.getMessage());
            }
        });

    }

// ======================================== 注册页面 ========================================
    //初始化注册页面
    public void initRegisterWindow(){
        //
        registerWindow=new JPanel();
        registerWindow.setLayout(new BoxLayout(registerWindow ,BoxLayout.Y_AXIS));
        //所有组件定义和组件的初始化
        jLabelusername=new JLabel("用户名");
        jLabelusername.setPreferredSize(new Dimension(70, 25));//设置统一的宽度进行对齐
        jLabelpassword=new JLabel("密码");
        jLabelpassword.setPreferredSize(new Dimension(70, 25));//设置统一的宽度进行对齐
        jLabelrePassword=new JLabel("确认密码");
        jLabelrePassword.setPreferredSize(new Dimension(70, 25));//设置统一的宽度进行对齐
        registerUserName=new JTextField(30);
        registerPassword=new JPasswordField(30);
        registerRePassword=new JPasswordField(30);
        returnloginButton=new JButton("返回登录页面");
        registerButton=new JButton("注册");
        //整体使用BoxLayout布局，然后分行进行填充，每行都使用FlowLayout布局

        //整体布局设置，注意BoxLayout的构造方式
        registerWindow.setLayout(new BoxLayout(registerWindow,BoxLayout.Y_AXIS));

        //第一行不需要了,还是要的,吗？
        JPanel row0=new JPanel(new FlowLayout(FlowLayout.CENTER));
        row0.add(Box.createVerticalStrut(4));//让标题和其下的表单保持合理的间隔
        //
        JPanel row0_1=new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel text1=new JLabel("注册页面");
        text1.setFont(new Font("宋体",Font.BOLD,20));
        row0_1.add(text1 );//
        //第二行
        JPanel row2=new JPanel(new FlowLayout());
        row2.add(jLabelusername);
        row2.add(registerUserName);
        //
        JPanel row3=new JPanel(new FlowLayout());
        row3.add(jLabelpassword);
        row3.add(registerPassword);
        //
        JPanel row4=new JPanel(new FlowLayout());
        row4.add(jLabelrePassword);
        row4.add(registerRePassword);
        //
        JPanel row5=new JPanel(new FlowLayout());
        row5.add(registerButton);
        row5.add(returnloginButton);
        //添加组件到Jframe中，顺序添加即可，添加那两个Jpanel
        registerWindow.add(row0);
        registerWindow.add(row0_1);
        registerWindow.add(row2);
        registerWindow.add(row3);
        registerWindow.add(row4);
        registerWindow.add(row5);

        // 为按钮绑定监听事件
        //为页面切换按钮绑定监听事件
        returnloginButton.addActionListener(e->{
            CardLayout cardLayout=(CardLayout) getContentPane().getLayout();//获得当前Jframe的布局对象
            cardLayout.show(getContentPane(),"LOGIN");
            setTitle("登录页面");
        });


        //修改注册监听事件，取代直接使用dao中的接口的方式
        registerButton.addActionListener(e->{
            String username=registerUserName.getText();
            String password=new String(registerPassword.getPassword());
            String rePassword=new String(registerRePassword.getPassword());
            //使用userdao的register接口进行注册
            UserFacade userFacade =new UserFacadeImpl();
            Result r= userFacade.register(username,password,rePassword);
            //注册成功
            if(r.getCode()==0){
                JOptionPane.showMessageDialog(this,r.getMessage());//显示提示信息
                //并切换页面
                CardLayout cardLayout=(CardLayout) getContentPane().getLayout();//获得当前Jframe的布局对象
                cardLayout.show(getContentPane(),"LOGIN");
                setTitle("登录页面");
            }
            //失败
            if(r.getCode()==1){
                JOptionPane.showMessageDialog(this,r.getMessage());
            }
        });


    }

// ======================================== 主体 ========================================
    //主体JFrame属性设置
    public LoginView() {
        //窗口主体的基本属性
        this.setTitle("登录页面");
        this.setSize(500, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//文档：退出应用程序使用System exit方法。仅在应用程序中使用。
        this.setLocationRelativeTo(null);//让窗口居于屏幕中央
        this.setVisible(true);//将窗口设为可见，不然不会显示窗口
        this.setLayout(new CardLayout());//整体使用CardLayout布局方便进行登录注册页面的切换

        //对登录注册页面进行初始化
        initLoginWindow();
        initRegisterWindow();
        //将两个页面添加到主体Jframe上，以便进行页面的切换
        this.add(loginWindow, "LOGIN");    // 添加到 CardLayout
        this.add(registerWindow, "REGISTER");
    }
}
