package com.ylaneo.chatclientdesktop.view.pages;

import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.ContactFacade;
import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.facade.impl.ContactFacadeImpl;
import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.session.UserSession;
import com.ylaneo.chatclientdesktop.view.MainView;
import com.ylaneo.chatclientdesktop.view.dialogs.SearchUserDialogView;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ContactView extends JPanel {
    // ======================================== 组件 ========================================
    //
    UserFacade userFacade=new UserFacadeImpl();
    ContactFacade contactFacade=new ContactFacadeImpl();
    //左侧
    private JTextField searchFriendField;   // 搜索已添加好友
    private JTextField searchUserField;     // 搜索用户用于添加
    private JPanel friendListPanel;         // 好友列表容器
    private JLabel leftTitle;
    //右侧
    private JLabel detailAvatar;
    private JLabel detailNickname;
    private JLabel detailUsername;
    private JLabel detailStatus;
    private JLabel detailRemark;
    private JButton sendMessageBtn;
    private JPanel rightPlaceholder;        // 右侧默认占位
    private JPanel rightDetailPanel;        // 右侧详情容器
    // 当前选中的好友
    private User currentSelectedFriend;
    // ======================================== 构造函数 ========================================
    public ContactView(){
        //
        initcomponents();
        loadContacts();
    }
    // ======================================== 初始化组件 ========================================
    private void initcomponents(){
        //页面整体的布局管理器
        setLayout(new BorderLayout());
        setBackground(new Color(225, 225, 225));

        add(buildLeftPanel(), BorderLayout.WEST);
        add(buildRightPanel(), BorderLayout.CENTER);
    }
    // ======================================== 左侧栏 ========================================
    private JPanel buildLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBackground(new Color(240, 242, 248));
//        left.setBorder(new EmptyBorder(12, 12, 12, 12));
//        leftPanel.setBorder(BorderFactory.createLineBorder(new Color(138, 138, 138), 1));
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(168, 168, 168)));//只创建右边的边框
        leftPanel.setPreferredSize(new Dimension(300, 0));
        // 标题
        leftTitle = new JLabel("好友列表");
        leftTitle.setFont(new Font("微软雅黑", Font.BOLD, 16));
        leftTitle.setForeground(new Color(60, 60, 70));
        //两行搜索框
        searchFriendField = createSearchField("搜索已添加好友");
        searchUserField = createSearchField("搜索用户昵称/用户名");
        // 搜索按钮：添加用户
        JButton searchUserBtn = new JButton("搜索用户");
        styleButton(searchUserBtn, new Color(66, 133, 244));
        searchUserBtn.setPreferredSize(new Dimension(80, 35));
        searchUserBtn.addActionListener(e -> handleAddUserSearch());
        //将第二个搜索框和搜索按钮组成一行
        JPanel row2 = new JPanel(new BorderLayout(6, 0));//6px横向间距
        row2.add(searchUserField, BorderLayout.CENTER);//CENTER会填满剩余空间
        row2.add(searchUserBtn, BorderLayout.EAST);//按钮固定在右侧
        // 顶部区域（标题 + 两个搜索框）
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        //为每个组件分别设置对齐方式
        leftTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchFriendField.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchUserField.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchUserBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
//        topPanel.add(leftTitle);
        topPanel.add(Box.createVerticalStrut(12));
        topPanel.add(searchFriendField);
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(row2);
        topPanel.add(Box.createVerticalStrut(6));

        topPanel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));//
        // 好友列表（放入 JScrollPane）
        friendListPanel = new JPanel();
        friendListPanel.setLayout(new BoxLayout(friendListPanel, BoxLayout.Y_AXIS));
        friendListPanel.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(friendListPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 230)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(scroll, BorderLayout.CENTER);
        // 监听本地好友搜索：过滤已显示的列表
        searchFriendField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterFriendList(searchFriendField.getText().trim());
            }
        });
        return leftPanel;
    }
    // ======================================== 右侧栏 ========================================
    private JPanel buildRightPanel() {
        JPanel rightPanel = new JPanel(new CardLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
//        rightPanel.setBorder(BorderFactory.createLineBorder(new Color(138, 138, 138), 1));
        // 默认占位
        rightPlaceholder = new JPanel(new GridBagLayout());
        rightPlaceholder.setBackground(Color.WHITE);
        JLabel tip = new JLabel("请在左侧选择一位好友查看详情");
        tip.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        tip.setForeground(new Color(150, 150, 160));
        rightPlaceholder.add(tip);
        // 详情面板
        rightDetailPanel = buildDetailPanel();
        rightPanel.add(rightPlaceholder, "placeholder");
        rightPanel.add(rightDetailPanel, "detail");
        return rightPanel;
    }
    private JPanel buildDetailPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(30, 40, 30, 40));
        root.setAlignmentX(Component.CENTER_ALIGNMENT);
        //头像板块（用容器包裹，让尺寸固定）
//        detailAvatar = new JLabel();
//        detailAvatar.setPreferredSize(new Dimension(160, 160));
//        detailAvatar.setHorizontalAlignment(SwingConstants.CENTER);
//        detailAvatar.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 230), 1));
//        detailAvatar.setOpaque(true);
//        detailAvatar.setBackground(new Color(245, 245, 250));

        JPanel avatarContainer = new JPanel(new BorderLayout());
        avatarContainer.setPreferredSize(new Dimension(160, 160));
        avatarContainer.setMaximumSize(new Dimension(160, 160));
        avatarContainer.setOpaque(true);
        avatarContainer.setBackground(new Color(245, 245, 250));
        avatarContainer.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 230), 1));
        detailAvatar = new JLabel();
        detailAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        detailAvatar.setVerticalAlignment(SwingConstants.CENTER);
        avatarContainer.add(detailAvatar, BorderLayout.CENTER);

        //好友信息板块
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
//        info.setLayout(new BorderLayout(10,10));//
        info.setOpaque(false);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        info.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        /*// 信息行
        detailNickname = createInfoLabel(20, true);
        detailUsername = createInfoLabel(14, false);
        detailStatus = createInfoLabel(14, false);
        detailRemark = createInfoLabel(14, false);
        //居中
        detailNickname.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailRemark.setAlignmentX(Component.LEFT_ALIGNMENT);*/
        //另一种方法创建信息行
        //
        JPanel jPanelNickname=createInfoRow("昵称",null,false);
        detailNickname = (JLabel) jPanelNickname.getClientProperty("valueLabel");
        jPanelNickname.setAlignmentX(Component.CENTER_ALIGNMENT);//

        JPanel jPanelUsername=createInfoRow("用户名",null,false);
        detailUsername = (JLabel) jPanelUsername.getClientProperty("valueLabel");
        jPanelUsername.setAlignmentX(Component.CENTER_ALIGNMENT);//

        JPanel jPanelStatus=createInfoRow("在线状态",null,false);
        detailStatus = (JLabel) jPanelStatus.getClientProperty("valueLabel");
        jPanelStatus.setAlignmentX(Component.CENTER_ALIGNMENT);//

        JPanel jPanelRemark=createInfoRow("备注",null,false);
        detailRemark = (JLabel) jPanelRemark.getClientProperty("valueLabel");
        jPanelRemark.setAlignmentX(Component.CENTER_ALIGNMENT);//
        /*//第一种方法的添加
        info.add(detailNickname);
        info.add(Box.createVerticalStrut(8));
        info.add(detailUsername);
        info.add(Box.createVerticalStrut(8));
        info.add(detailStatus);
        info.add(Box.createVerticalStrut(8));
        info.add(detailRemark);*/
        //第二种方法的添加
        info.add(jPanelNickname);
        info.add(Box.createVerticalStrut(8));
        info.add(jPanelUsername);
        info.add(Box.createVerticalStrut(8));
        info.add(jPanelStatus);
        info.add(Box.createVerticalStrut(8));
        info.add(jPanelRemark);
        // 发送消息按钮
        sendMessageBtn = new JButton("发送消息");
        styleButton(sendMessageBtn, new Color(66, 133, 244));
//        sendMessageBtn.setPreferredSize(new Dimension(70, 35));
        sendMessageBtn.addActionListener(e -> handleSendMessage());
        sendMessageBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendMessageBtn.setPreferredSize(new Dimension(80,40));
        sendMessageBtn.setMaximumSize(new Dimension(80, 40));//关键：锁死宽度
        //容纳头像，信息，按钮的复合板块
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 230), 0),//
                BorderFactory.createEmptyBorder(20, 20, 20, 20)//内边距
        ));
        //
        detailAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(avatarContainer);
        center.add(Box.createVerticalStrut(24));
        center.add(info);
        center.add(Box.createVerticalStrut(30));
        center.add(sendMessageBtn);
        //
        root.add(center, BorderLayout.CENTER);
        return root;
    }
    // ======================================== 通用工具方法 ========================================
    //统一按钮样式的工具方法
    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(80, 40));
        button.setMaximumSize(new Dimension(80, 40));//关键：锁死宽度
        //
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210)),
                BorderFactory.createEmptyBorder(2, 3, 2, 3)));

        // 鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
    }
    //创建搜索框的方法
    private JTextField createSearchField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(0, 32));// 0 = 由布局管理器决定
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        f.setToolTipText(placeholder);
        return f;
    }
    //创建信息行的方法
    private JPanel createInfoRow(String title, String value, boolean editable) {
        JPanel row = new JPanel();
        //
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));//对齐方式
        row.setMaximumSize(new Dimension(600, 40));//最大宽度限制
        row.setAlignmentX(Component.CENTER_ALIGNMENT);//置于父组件中间
        //
        row.putClientProperty("titleLabel", new JLabel(title));//使用客户端属性存储一个临时的 JLabel 引用到面板上，方便后续通过键名 "titleLabel" 检索（这是一个缓存/查找机制）
        //
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        titleLabel.setPreferredSize(new Dimension(90, 30));//每一行前面的文字的宽度
        titleLabel.setForeground(new Color(100, 100, 110));
        JLabel sign=new JLabel(":  ");
        sign.setFont(new Font("微软雅黑", Font.BOLD, 16));
        row.add(titleLabel);
        row.add(sign);
        //可以编辑的信息行
        if (editable) {
            JTextField textField = new JTextField();
            textField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            textField.setPreferredSize(new Dimension(200, 32));
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 210)),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            if (value != null) {
                textField.setText(value);
            }
            row.add(textField);
            row.putClientProperty("valueField", textField);//存储文本框引用到客户端属性，键名为 "valueField"
            //为可编辑的信息行添加修改按钮
            JButton btn = new JButton("保存");
            btn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            btn.setPreferredSize(new Dimension(70, 32));
            row.add(Box.createHorizontalStrut(10));
            row.add(btn);
            row.putClientProperty("actionBtn", btn);//存储按钮引用到客户端属性，键名为 "actionBtn"
        } else {//不能编辑的信息行
            JLabel valueLabel = new JLabel(value != null ? value : "");
            valueLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            valueLabel.setForeground(new Color(50, 50, 50));
            row.add(valueLabel);
            row.putClientProperty("valueLabel", valueLabel);//存储文本框引用到客户端属性，键名为 "valueField"
        }

        return row;
    }
    // ======================================== 加载好友列表 ========================================
    //将用户头像加载到用户信息行的方法
    private void loadAvatarInto(JLabel label, String path, int size) {
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            ImageIcon icon = new ImageIcon(path);
            Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(img));
            label.setText("");
        } else {
            label.setIcon(null);
            label.setText("暂无头像");
            label.setFont(new Font("微软雅黑", Font.PLAIN, 8));
            label.setForeground(new Color(180, 180, 190));
            label.setHorizontalAlignment(SwingConstants.CENTER);
        }
    }
    //用户要展示的名称（备注优先，没有则展示昵称，还没有则用户名）
    private String getDisplayName(User friend, String remark) {
        if (remark != null && !remark.isEmpty()) return remark;
        if (friend.getNickname() != null && !friend.getNickname().isEmpty()) return friend.getNickname();
        return friend.getUsername();
    }
    //渲染好友列表的方法
    private void loadContacts() {
        //根据userid获取所有的好友列表
        Integer userId=UserSession.getInstance().getCurrentUser().getUserid();
        List<User> friendList = contactFacade.getFriendList(userId).getData();
        // 暂用本地假数据演示
        User u1 = new User();
        u1.setUserid(101);
        u1.setUsername("zhangsan");
        u1.setNickname("张三");
        u1.setAvatar_url("D:/files_D/avatar_chat_demo/user_2.png");
        u1.setStatus(1);
        friendList.add(u1);
        User u2 = new User();
        u2.setUserid(102);
        u2.setUsername("lisi");
        u2.setNickname(null);
        u2.setAvatar_url(null);
        u2.setStatus(0);
        friendList.add(u2);
        renderFriendList(friendList);//渲染好友列表
    }
    //根据传入的好友用户列表信息渲染好友列表
    private void renderFriendList(List<User> friends) {
        friendListPanel.removeAll();
        if (friends == null || friends.isEmpty()) {
            JLabel empty = new JLabel("  暂无好友");
            empty.setForeground(new Color(150, 150, 160));
            empty.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            empty.setBorder(new EmptyBorder(12, 8, 12, 8));
            friendListPanel.add(empty);
        } else {
            for (User u : friends) {
                friendListPanel.add(createFriendRow(u));
//                friendListPanel.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(196, 196, 196)));//只有下边添加边框
//                friendListPanel.add(new JSeparator());
            }
        }
        friendListPanel.revalidate();
        friendListPanel.repaint();
    }
    //创建一行好友行，用于上面的渲染好友列表
    private JPanel createFriendRow(User friend) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(196, 196, 196)));//只有下边添加边框
        row.setBorder(new EmptyBorder(10, 10, 10, 10));//
        // 头像
        JLabel avatar = new JLabel();
        avatar.setPreferredSize(new Dimension(44, 44));
        loadAvatarInto(avatar, friend.getAvatar_url(), 44);
        // 中间：昵称/用户名（备注优先 > 昵称 > 用户名）
        String display = getDisplayName(friend, null);
        JLabel nameLabel = new JLabel(display);
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        // 右侧：在线状态
        JLabel statusLabel = new JLabel();
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        if (friend.getStatus() != null && friend.getStatus() == 1) {
            statusLabel.setText("● 在线");
            statusLabel.setForeground(new Color(76, 175, 80));
        } else {
            statusLabel.setText("○ 离线");
            statusLabel.setForeground(new Color(150, 150, 150));
        }
        row.add(avatar, BorderLayout.WEST);
        row.add(nameLabel, BorderLayout.CENTER);
        row.add(statusLabel, BorderLayout.EAST);
        // 点击/悬停效果
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(new Color(230, 238, 255)); }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(Color.WHITE); }
            @Override public void mouseClicked(MouseEvent e) {
                currentSelectedFriend = friend;
                showFriendDetail(friend, null);
            }
        });
        return row;
    }
    //显示好友详情
    private void showFriendDetail(User friend, String remark) {
        loadAvatarInto(detailAvatar, friend.getAvatar_url(), 160);
        detailNickname.setText( (friend.getNickname() == null ? "-" : friend.getNickname()));
        detailUsername.setText( friend.getUsername());
        if (friend.getStatus() != null && friend.getStatus() == 1) {
            detailStatus.setText("● 在线");
            detailStatus.setForeground(new Color(76, 175, 80));
        } else {
            detailStatus.setText("○ 离线");
            detailStatus.setForeground(new Color(150, 150, 150));
        }
        detailRemark.setText( (remark == null ? "(未设置)" : remark));
        CardLayout cl = (CardLayout) ((JPanel) rightDetailPanel.getParent()).getLayout();
        cl.show((JPanel) rightDetailPanel.getParent(), "detail");
    }
    //过滤好友列表
    private void filterFriendList(String keyword) {
        // 简化：从已有列表重新筛选；真实场景建议后端过滤
        List<User> all = getCurrentFriendList();
        if (keyword.isEmpty()) {
            renderFriendList(all);
            return;
        }
        List<User> filtered = new ArrayList<>();
        for (User u : all) {
            if ((u.getNickname() != null && u.getNickname().contains(keyword))
                    || u.getUsername().contains(keyword)) {
                filtered.add(u);
            }
        }
        renderFriendList(filtered);
    }
    //获得当前用户的好友列表信息
    private List<User> getCurrentFriendList() {
        //与loadContacts中数据保持一致，可改为全局缓存列表
        List<User> list = new ArrayList<>();
        User u1 = new User();
        u1.setUserid(101); u1.setUsername("zhangsan"); u1.setNickname("张三");
        u1.setAvatar_url("D:/files_D/avatar_chat_demo/user_2.png"); u1.setStatus(1);
        list.add(u1);
        User u2 = new User();
        u2.setUserid(102); u2.setUsername("lisi"); u2.setNickname(null);
        u2.setAvatar_url(null); u2.setStatus(0);
        list.add(u2);
        return list;
    }
    // ======================================== 按钮功能绑定 ========================================
    //搜索用户按钮功能绑定
    private void handleAddUserSearch() {
        String keyword = searchUserField.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入要搜索的用户昵称或用户名");
            return;
        }

        // 调用后端搜索接口
        Integer currentUserId = UserSession.getInstance().getCurrentUser().getUserid();
        List<User> results = contactFacade.searchUserByKeyword(keyword, currentUserId).getData();

        // 弹出搜索结果弹窗
        SearchUserDialogView dialog = new SearchUserDialogView((Frame) SwingUtilities.getWindowAncestor(this));//这里是调用搜索弹窗的构造函数，传入当前页面的父元素
        dialog.setUserList(results);
        dialog.setVisible(true);

//        User selected = dialog.getSelectedUser();
//        if (selected != null) {
//            System.out.println("选中了用户：" + selected.getUsername());
//        }
    }
    //发送消息按钮功能绑定
    private void handleSendMessage() {
        if (currentSelectedFriend == null) {
            JOptionPane.showMessageDialog(this, "请先选择一位好友");
            return;
        }
        // 把目标用户 id 暂存到会话对象中
        UserSession.getInstance().getCurrentUser(); // 当前用户上下文
        // 跳到消息卡片页（注意：消息卡片名要与 MainView 中 CardLayout 添加的 key 一致）
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView) {
            ((MainView) window).cardLayout.show(((MainView) window).contentPanel, "message");
            // TODO: 将 currentSelectedFriend.getUserid() 传给 MessageView，定位会话窗口
        }
    }

}
