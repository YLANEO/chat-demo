package com.ylaneo.chatclientdesktop.view.pages;

import com.ylaneo.chatclientdesktop.dto.FriendRequestDto;
import com.ylaneo.chatclientdesktop.entity.Result;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private JTextField searchFriendField;//搜索已添加好友
    private JTextField searchUserField;//搜索用户用于添加
    private JPanel friendListPanel;//好友列表容器
    private JLabel leftTitle;
    //右侧
    private JLabel detailAvatar;
    private JLabel detailNickname;
    private JLabel detailUsername;
    private JLabel detailStatus;
    private JLabel detailRemark;
    private JButton sendMessageBtn;
    private JPanel rightPlaceholder;//右侧默认占位
    private JPanel rightDetailPanel;//右侧详情容器
    private JPanel rightRequestsPanel;//好友申请页面
    // 当前选中的好友
    private User currentSelectedFriend;
    //当前好友列表
    private List<User> currentFriends = new ArrayList<>();
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
        //搜索用户的按钮
        JButton searchUserBtn = new JButton("搜索用户");
        styleButton(searchUserBtn, new Color(66, 133, 244));
        searchUserBtn.setPreferredSize(new Dimension(80, 35));
        searchUserBtn.addActionListener(e -> handleAddUserSearch());
        //将第二个搜索框和搜索按钮组成一行
        JPanel row2 = new JPanel(new BorderLayout(6, 0));//6px横向间距
        row2.add(searchUserField, BorderLayout.CENTER);//CENTER会填满剩余空间
        row2.add(searchUserBtn, BorderLayout.EAST);//按钮固定在右侧
        //查看收到的好友申请按钮
        JButton viewRequestsBtn = new JButton("查看收到的好友申请");
        styleButton(viewRequestsBtn, new Color(124, 146, 220));  // 橙色提示
        viewRequestsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewRequestsBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        viewRequestsBtn.addActionListener(e -> showFriendRequestsView());
        //顶部：两个搜索功能和一个好友申请查看按钮
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        //为每个组件分别设置对齐方式
        leftTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchFriendField.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchUserField.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchUserBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        //将所有组件加入到顶部区域
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(searchFriendField);
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(row2);
        topPanel.add(Box.createVerticalStrut(6));
        topPanel.add(viewRequestsBtn);//
        topPanel.add(Box.createVerticalStrut(6));
        topPanel.add(leftTitle);

        topPanel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));//
        //好友列表区域
        friendListPanel = new JPanel();
        friendListPanel.setLayout(new BoxLayout(friendListPanel, BoxLayout.Y_AXIS));
        friendListPanel.setBackground(Color.WHITE);
        //用于存放好友列表组件的可滚动页面
        JScrollPane scroll = new JScrollPane(friendListPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 230)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(scroll, BorderLayout.CENTER);
        //监听本地好友搜索：过滤已显示的列表
        searchFriendField.addKeyListener(new KeyAdapter() {
            //
            @Override
            public void keyReleased(KeyEvent e) {
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
        //未选中好友时显示的空页面
        rightPlaceholder = new JPanel(new GridBagLayout());
        rightPlaceholder.setBackground(Color.WHITE);
        JLabel tip = new JLabel("请在左侧选择一位好友查看详情");
        tip.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        tip.setForeground(new Color(150, 150, 160));
        rightPlaceholder.add(tip);
        //好友信息页面和好友申请页面
        rightDetailPanel = buildDetailPanel();
        rightRequestsPanel = new JPanel();  //占位，会动态替换内容
        rightRequestsPanel.setLayout(new BorderLayout());
        //添加到卡片中
        rightPanel.add(rightPlaceholder, "placeholder");
        rightPanel.add(rightDetailPanel, "detail");
        rightPanel.add(rightRequestsPanel, "requests");
        return rightPanel;
    }
    //构建右侧好友信息显示页面
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
        avatarContainer.setPreferredSize(new Dimension(200, 200));
        avatarContainer.setMaximumSize(new Dimension(200, 200));
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
    //好友申请查看页面
    private void showFriendRequestsView() {
        //首先清除之前的列表页面，防止重叠放置，注意需要进行三步操作。后面两步不能漏，
        rightRequestsPanel.removeAll();
        rightRequestsPanel.revalidate();
        rightRequestsPanel.repaint();
        //
        Integer currentUserId = UserSession.getInstance().getCurrentUser().getUserid();
        Result<List<FriendRequestDto>> result = contactFacade.getFriendRequests(currentUserId);

        if (result.getCode() != 0) {
            JOptionPane.showMessageDialog(this, result.getMessage());
            return;
        }

        List<FriendRequestDto> requests = result.getData();

        // 创建好友申请列表面板
        rightRequestsPanel.setBackground(Color.WHITE);
        rightRequestsPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        // 标题
        JLabel title = new JLabel("收到的好友申请（共 " + requests.size() + " 条）");
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        rightRequestsPanel.add(title, BorderLayout.NORTH);

        // 列表容器
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        if (requests.isEmpty()) {
            JLabel empty = new JLabel("暂无好友申请");
            empty.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            empty.setForeground(new Color(150, 150, 160));
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            listPanel.add(empty);
        } else {
            for (FriendRequestDto item : requests) {
                listPanel.add(createFriendRequestRow(item));
                listPanel.add(Box.createVerticalStrut(8));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        rightRequestsPanel.add(scroll, BorderLayout.CENTER);

        // 用一个新的 rightPanel（CardLayout）显示
        // 或者用一个全局的 rightRequestsPanel 字段
        CardLayout cl = (CardLayout) ((JPanel) rightRequestsPanel.getParent()).getLayout();
        cl.show((JPanel) rightRequestsPanel.getParent(), "requests");
    }
    //点击一条好友申请行弹窗显示备注详细信息
    private void showFriendRequestDetailDialog(FriendRequestDto item) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "好友申请详情",
                true  // modal
        );
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 15));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 25, 20, 25));

        // 顶部：头像 + 昵称 + 用户名
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel avatar = new JLabel();
        avatar.setPreferredSize(new Dimension(80, 80));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel avatarContainer = new JPanel(new BorderLayout());
        avatarContainer.setOpaque(false);
        avatarContainer.setPreferredSize(new Dimension(80, 80));
        avatarContainer.add(avatar, BorderLayout.CENTER);
        loadAvatarInto(avatar, item.getSenderAvatar(), 80);

        JLabel nicknameLabel = new JLabel(
                item.getSenderNickname() != null ? item.getSenderNickname() : "-");
        nicknameLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        nicknameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel usernameLabel = new JLabel("@" + item.getSenderUsername());
        usernameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        usernameLabel.setForeground(new Color(130, 130, 140));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(avatarContainer);
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(nicknameLabel);
        topPanel.add(Box.createVerticalStrut(2));
        topPanel.add(usernameLabel);

        // 中间：备注信息
        JPanel msgPanel = new JPanel(new BorderLayout());
        msgPanel.setBackground(new Color(245, 245, 250));
        msgPanel.setBorder(new EmptyBorder(12, 15, 12, 15));

        JTextArea msgArea = new JTextArea(item.getMessage() != null ? item.getMessage() : "(无备注)");
        msgArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgArea.setEditable(false);
        msgArea.setOpaque(false);
        msgPanel.add(msgArea, BorderLayout.CENTER);

        // 底部：同意 / 拒绝 按钮
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
        btnPanel.setOpaque(false);

        JButton acceptBtn = new JButton("同意");
        styleButton(acceptBtn, new Color(76, 175, 80));
        JButton rejectBtn = new JButton("拒绝");
        styleButton(rejectBtn, new Color(244, 67, 54));

        acceptBtn.setPreferredSize(new Dimension(100, 38));
        rejectBtn.setPreferredSize(new Dimension(100, 38));

        btnPanel.add(Box.createHorizontalGlue());
        btnPanel.add(rejectBtn);
        btnPanel.add(Box.createHorizontalStrut(12));
        btnPanel.add(acceptBtn);
        btnPanel.add(Box.createHorizontalGlue());

        root.add(topPanel, BorderLayout.NORTH);
        root.add(msgPanel, BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);

        // 绑定事件
        Integer senderId=item.getSenderId();
        Integer receiverId=UserSession.getInstance().getCurrentUser().getUserid();
        acceptBtn.addActionListener(e -> {
            handleRespondRequest(item.getRequestId(),senderId,receiverId,1);
            dialog.dispose();
        });
        rejectBtn.addActionListener(e -> {
            handleRespondRequest(item.getRequestId(),senderId,receiverId,2);
            dialog.dispose();
        });

        dialog.setVisible(true);
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
    //创建好友信息页面信息行的方法
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

    //查看好友申请行创建方法
    private JPanel createFriendRequestRow(FriendRequestDto item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 230)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // 左侧：头像
        JLabel avatar = new JLabel();
        avatar.setPreferredSize(new Dimension(50, 50));
        loadAvatarInto(avatar, item.getSenderAvatar(), 50);

        // 中间：昵称 + 用户名
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel nicknameLabel = new JLabel(
                item.getSenderNickname() != null ? item.getSenderNickname() : "-");
        nicknameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        JLabel usernameLabel = new JLabel("@" + item.getSenderUsername());
        usernameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        usernameLabel.setForeground(new Color(130, 130, 140));
        nicknameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(nicknameLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(usernameLabel);

        // 中间偏右：备注信息（截断显示）
        JLabel messageLabel = new JLabel();
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageLabel.setForeground(new Color(100, 100, 110));
        String msg = item.getMessage() != null ? item.getMessage() : "";
        if (msg.length() > 30) msg = msg.substring(0, 30) + "...";
        messageLabel.setText("备注: " + msg);

        // 右侧：同意 / 拒绝按钮
        JButton acceptBtn = new JButton("同意");
        styleButton(acceptBtn, new Color(76, 175, 80));  // 绿色
        JButton rejectBtn = new JButton("拒绝");
        styleButton(rejectBtn, new Color(244, 67, 54));  // 红色

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
        btnPanel.setOpaque(false);
        btnPanel.add(acceptBtn);
        btnPanel.add(Box.createHorizontalStrut(8));
        btnPanel.add(rejectBtn);

        JPanel eastBox = new JPanel(new BorderLayout());
        eastBox.setOpaque(false);
        eastBox.add(btnPanel, BorderLayout.CENTER);

        // 中间区域：信息 + 备注
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(info);
        centerPanel.add(Box.createVerticalStrut(4));
        centerPanel.add(messageLabel);

        row.add(avatar, BorderLayout.WEST);
        row.add(centerPanel, BorderLayout.CENTER);
        row.add(eastBox, BorderLayout.EAST);

        // 整行点击 → 弹窗显示详情
        final FriendRequestDto finalItem = item;
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                //
                showFriendRequestDetailDialog(finalItem);//展示选中行的备注信息弹窗
            }
            @Override public void mouseEntered(MouseEvent e) {
                //
                row.setBackground(new Color(245, 245, 250));
            }
            @Override public void mouseExited(MouseEvent e) {
                //
                row.setBackground(Color.WHITE);
            }
        });

        //按钮事件
        Integer senderId=item.getSenderId();
        Integer receiverId=UserSession.getInstance().getCurrentUser().getUserid();
        acceptBtn.addActionListener(e -> handleRespondRequest(item.getRequestId(),senderId,receiverId,1));
        rejectBtn.addActionListener(e -> handleRespondRequest(item.getRequestId(),senderId,receiverId,2));

        return row;
    }
    // ======================================== 加载好友列表 ========================================
    //将用户头像加载到用户信息行的方法,还会指定尺寸
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
//    public void loadContacts() {
//        //根据userid获取所有的好友列表
//        Integer userId=UserSession.getInstance().getCurrentUser().getUserid();
//        List<User> friendList = contactFacade.getFriendList(userId).getData();
//        renderFriendList(friendList);//渲染好友列表
//    }
    public void loadContacts() {
        Integer userId = UserSession.getInstance().getCurrentUser().getUserid();
        Result<List<User>> result = contactFacade.getFriendList(userId);

        if (result.getCode() != 0 || result.getData() == null) {
            currentFriends = new ArrayList<>();
            renderFriendList(currentFriends);
            return;
        }

        currentFriends = new ArrayList<>(result.getData());
        renderFriendList(currentFriends);
    }
    //刷新好友在线状态
    public void updateFriendStatus(Integer userId, Integer status) {
        if (userId == null || status == null) {
            return;
        }

        for (User friend : currentFriends) {
            if (userId.equals(friend.getUserid())) {
                friend.setStatus(status);
                renderFriendList(currentFriends);

                if (currentSelectedFriend != null &&
                        userId.equals(currentSelectedFriend.getUserid())) {
                    currentSelectedFriend.setStatus(status);
                    showFriendDetail(currentSelectedFriend, null);
                }
                return;
            }
        }
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
        friendListPanel.revalidate();//
        friendListPanel.repaint();//
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
        loadAvatarInto(avatar, friend.getAvatar_url(), 44);//
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
            @Override public void mouseEntered(MouseEvent e) {
                //
                row.setBackground(new Color(230, 238, 255));
            }
            @Override public void mouseExited(MouseEvent e)  {
                //
                row.setBackground(Color.WHITE);
            }
            @Override public void mouseClicked(MouseEvent e) {
                //
                currentSelectedFriend = friend;//
                showFriendDetail(friend, null);//显示选中用户的信息到右侧页面
            }
        });
        return row;
    }
    //在右侧页面显示被选中的好友信息
    private void showFriendDetail(User friend, String remark) {
        loadAvatarInto(detailAvatar, friend.getAvatar_url(), 200);
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
        cl.show((JPanel) rightDetailPanel.getParent(), "detail");//将右侧页面切换为被选中的好友信息页面
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
        Integer currentUserId=UserSession.getInstance().getCurrentUser().getUserid();
        List<User> list = contactFacade.getFriendList(currentUserId).getData();
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
    //处理好友申请按钮
    // ContactView 中调用
    private void handleRespondRequest(Integer requestId,Integer senderId, Integer receiverId, Integer action) {
        Result result = contactFacade.handleRequest(requestId,senderId,receiverId,action);
        if (result.getCode() == 0) {
            JOptionPane.showMessageDialog(this, action == 1 ? "已同意" : "已拒绝");
            // 刷新好友申请列表
            showFriendRequestsView();
            // 如果是同意，可能还需要刷新好友列表
            if (action == 1) {
                loadContacts();//同意好友申请后需要重新渲染好友列表
            }
        } else {
            JOptionPane.showMessageDialog(this, "操作失败：" + result.getMessage());
        }
    }

}
