package com.ylaneo.chatclientdesktop.view.pages;

import com.ylaneo.chatclientdesktop.entity.ChatMessage;
import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.ContactFacade;
import com.ylaneo.chatclientdesktop.facade.MessageFacade;
import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.facade.impl.ContactFacadeImpl;
import com.ylaneo.chatclientdesktop.facade.impl.MessageFacadeImpl;
import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.session.UserSession;
import com.ylaneo.chatclientdesktop.view.MainView;
import com.ylaneo.chatclientdesktop.view.dialogs.SearchUserDialogView;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageView extends JPanel {
    // ======================================== 组件 ========================================
    //
    UserFacade userFacade=new UserFacadeImpl();
    ContactFacade contactFacade=new ContactFacadeImpl();
    MessageFacade messageFacade=new MessageFacadeImpl();
    //分页加载消息的相关参数
    private static final int PAGE_SIZE = 50;
    private List<ChatMessage> allMessages = new ArrayList<>();
    private int currentPage = 0;
    private boolean hasMore = true;
    private boolean isLoading = false;
    //左侧
    private JTextField searchFriendField;//搜索已添加好友
    private JPanel friendListPanel;//好友列表容器
    private JLabel leftTitle;
    //右侧
    private JPanel top;//聊天页面顶栏
    private JLabel friendName;//顶部好友的昵称或者用户名
    private JPanel friendNameContainer;//放置friendName的容器
    private JLabel friendStatus;
    private JPanel friendStatusContainer;//放置friendStatus的容器
    private JPanel center;//聊天信息区域
    private JScrollPane chatScroll;//放置center的可滚动容器
    private JPanel buttom;//发送消息区域
    private JTextArea messageArea;
    private JButton sendMessageBtn;
    private JPanel rightPlaceholder;//右侧默认占位
    private JPanel rightChatPanel;//右侧聊天页面容器（根据点击的用户切换）
    // 当前选中的好友
    private User currentSelectedFriend;
    //当前好友列表
    private List<User> currentFriends = new ArrayList<>();

    // ======================================== 主页面 ========================================
    public MessageView(){
        initcomponents();//初始化页面整体框架
        loadContacts();
    }

    // ======================================== 初始化左右面板和其中的组件 ========================================
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
        //一行搜索框
        searchFriendField = createSearchField("搜索已添加好友");

        //顶部：两个搜索功能和一个好友申请查看按钮
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        //为每个组件分别设置对齐方式
        leftTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchFriendField.setAlignmentX(Component.CENTER_ALIGNMENT);
        //将所有组件加入到顶部区域
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(searchFriendField);
        topPanel.add(Box.createVerticalStrut(8));
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
                //
                filterFriendList(searchFriendField.getText().trim());
            }
        });
        return leftPanel;
    }
    // ======================================== 右侧栏 ========================================
    //右侧栏整体
    private JPanel buildRightPanel() {
        JPanel rightPanel = new JPanel(new CardLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
//        rightPanel.setBorder(BorderFactory.createLineBorder(new Color(138, 138, 138), 1));
        //未选中好友时显示的空页面
        rightPlaceholder = new JPanel(new GridBagLayout());
        rightPlaceholder.setBackground(Color.WHITE);
        JLabel tip = new JLabel("请在左侧选择一位好友");
        tip.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        tip.setForeground(new Color(150, 150, 160));
        rightPlaceholder.add(tip);
        //
        rightChatPanel=builChatPanel();
        //添加到卡片中
        rightPanel.add(rightPlaceholder, "placeholder");
        rightPanel.add(rightChatPanel,"chat");
        return rightPanel;
    }
    //构建右侧好友信息显示页面
    private JPanel builChatPanel() {
        //总支：上中下三个部分都要添加到这里
        JPanel root = new JPanel();
        root.setLayout(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(1, 0, 0, 0));
        root.setAlignmentX(Component.CENTER_ALIGNMENT);
        //顶部：一行放置昵称或者用户名以及在线状态
        top=new JPanel();
        top.setLayout(new BorderLayout());
        top.setPreferredSize(new Dimension(0,40));
        friendName=new JLabel();
        friendName.setFont(new Font("黑体",Font.BOLD,20));
        friendNameContainer=new JPanel(new BorderLayout());
        friendNameContainer.setPreferredSize(new Dimension(150, 30));
        friendNameContainer.setMaximumSize(new Dimension(999, 30));
        friendNameContainer.setOpaque(true);
        friendNameContainer.setBackground(new Color(213, 213, 213));
//        friendNameContainer.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 230), 1));
        friendNameContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(174, 174, 178), 1),
                BorderFactory.createEmptyBorder(0, 15, 0, 0)
        ));//修改为同时设置空白和边框
        friendNameContainer.add(friendName);

        friendStatus=new JLabel();
        friendStatus.setFont(new Font("宋体",Font.BOLD,15));
        friendStatusContainer=new JPanel(new BorderLayout());
        friendStatusContainer.setPreferredSize(new Dimension(200, 30));
        friendStatusContainer.setMaximumSize(new Dimension(200, 30));
        friendStatusContainer.setOpaque(true);
        friendStatusContainer.setBackground(new Color(255, 255, 255));
//        friendStatusContainer.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 230), 1));
        friendStatusContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 230), 1),
                BorderFactory.createEmptyBorder(0, 15, 0, 0)
        ));//修改为同时设置空白和边框
        friendStatusContainer.add(friendStatus);

        top.add(friendNameContainer,BorderLayout.WEST);
        top.add(friendStatusContainer,BorderLayout.CENTER);
        //中间：聊天信息展示区
        //
        center=new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(new Color(240, 240, 245,255));
        chatScroll = new JScrollPane(center);//创建可滚动容器，将聊天区域放进去
        chatScroll.setBorder(null);
        chatScroll.getVerticalScrollBar().setUnitIncrement(30);//每次滚动的像素数量
        root.add(chatScroll, BorderLayout.CENTER);
        //底部：消息输入区
        //
        JPanel buttom=new JPanel();
        buttom.setPreferredSize(new Dimension(9999,300));
        buttom.setLayout(new BoxLayout(buttom,BoxLayout.Y_AXIS));
        buttom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210),1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        messageArea=new JTextArea();//消息输入区
        messageArea.setPreferredSize(new Dimension(9999,250));
        messageArea.setMaximumSize(new Dimension(9999, 250));
        messageArea.setFont(new Font("微软雅黑",Font.PLAIN,15));
        messageArea.setBackground(new Color(255,255,255));
        sendMessageBtn = new JButton("发送消息");// 发送消息按钮
        styleButton(sendMessageBtn, new Color(66, 133, 244));
        sendMessageBtn.addActionListener(e -> handleSendMessage());
        sendMessageBtn.setAlignmentX(RIGHT_ALIGNMENT);
        sendMessageBtn.setPreferredSize(new Dimension(80,40));
        sendMessageBtn.setMaximumSize(new Dimension(80, 40));//关键：锁死宽度
        JPanel sendMessageBtnContainer = new JPanel();
        sendMessageBtnContainer.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        sendMessageBtnContainer.setBackground(new Color(255,255,255));
//        sendMessageBtnContainer.setOpaque(true);
        sendMessageBtnContainer.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(138, 138, 138)),
                BorderFactory.createLineBorder(new Color(200, 200, 210),1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        sendMessageBtnContainer.add(sendMessageBtn);
        buttom.add(messageArea);
//        buttom.add(Box.createVerticalStrut(5));
        buttom.add(sendMessageBtnContainer);
        root.add(buttom,BorderLayout.SOUTH);




        //另一种方法创建信息行
        //
        /*JPanel jPanelNickname=createInfoRow("昵称",null,false);
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
        jPanelRemark.setAlignmentX(Component.CENTER_ALIGNMENT);//*/


        //将三个部分添加到root页面中
        root.add(top, BorderLayout.NORTH);
//        root.add(center,BorderLayout.CENTER);
        root.add(chatScroll, BorderLayout.CENTER);
        root.add(buttom,BorderLayout.SOUTH);

        //给centerPanel的滚动条加监听(分页方案的)
        /*chatScroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            private int previousValue = 0;

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                JScrollBar sb = (JScrollBar) e.getSource();
                if (!e.getValueIsAdjusting()) {
                    // 滚动到最顶部（往上滑）
                    if (e.getValue() == 0 && previousValue > 0) {
                        loadMessagesPage(currentSelectedFriend.getUserid(), currentPage + 1);
                    }
                    previousValue = e.getValue();
                }
            }
        });*/

        //
        return root;
    }

    // ======================================== 提取出来的可复用的方法 ========================================
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
                BorderFactory.createEmptyBorder(2, 3, 2, 3)
        ));

        // 鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                //
                button.setBackground(bgColor.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                //
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
    //将用户头像加载到信息行（Jlabel）的方法,需要指定尺寸
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
    //创建聊天消息的气泡框，根据收发方设置不同的颜色
//    private JPanel createBubble(String text, boolean isMine, int maxTextWidth) {
//        Color bgColor   = isMine ? new Color(66, 133, 244) : new Color(245, 247, 252);
//        Color textColor = isMine ? Color.WHITE               : new Color(40, 40, 40);
//
//        JTextArea textArea = new JTextArea(text);
//        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
//        textArea.setForeground(textColor);
//        textArea.setLineWrap(true);
//        textArea.setWrapStyleWord(true);
//        textArea.setEditable(false);
//        textArea.setOpaque(false);
//        textArea.setBorder(null);
//
//        int borderWidth = 40;
//        if (maxTextWidth <= 0) {
//            maxTextWidth = 400;
//        }
//
//        textArea.setSize(new Dimension(maxTextWidth, Short.MAX_VALUE));
//        Dimension textPrefSize = textArea.getPreferredSize();
//
//        int bubbleWidth = textPrefSize.width + borderWidth;
//        int bubbleHeight = textPrefSize.height + borderWidth;
//
//        // 关键改动：改用 FlowLayout，让 bubble 宽度由内容决定
//        JPanel bubble = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        bubble.setOpaque(true);
//        bubble.setBackground(bgColor);
//        bubble.setBorder(BorderFactory.createCompoundBorder(
//                new LineBorder(bgColor, 10, true),
//                new EmptyBorder(4, 10, 4, 10)
//        ));
//        bubble.add(textArea);
//
//        bubble.setPreferredSize(new Dimension(bubbleWidth, bubbleHeight));
//        bubble.setMaximumSize(new Dimension(bubbleWidth, Integer.MAX_VALUE));
//
//        return bubble;
//    }

    //方案A：创建聊天消息的气泡框，根据收发方设置不同的颜色。高度计算有点问题
    private JPanel createBubble(String text, boolean isMine, int maxTextWidth) {
        Color bgColor   = isMine ? new Color(66, 133, 244) : new Color(245, 247, 252);
        Color textColor = isMine ? Color.WHITE               : new Color(40, 40, 40);

        JTextArea textArea = new JTextArea(text);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        textArea.setForeground(textColor);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setBorder(null);
        textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));//设置鼠标指针指向时的样式改变

        int borderWidth = 40;
        if (maxTextWidth <= 0) {
            maxTextWidth = 400;
        }

        textArea.setSize(maxTextWidth, Short.MAX_VALUE);
        textArea.validate();

        // 手动计算文本换行后的实际高度
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int lineHeight = fm.getHeight();

        // 计算每行能容纳的字符数（估算）
        int charsPerLine = maxTextWidth / fm.stringWidth("中");
        if (charsPerLine < 1) charsPerLine = 1;

        // 计算文本换行后的实际行数
        int totalLines = 0;
        if (text.contains("\n")) {
            String[] lines = text.split("\n");
            for (String line : lines) {
                int lineLen = line.length();
                totalLines += (lineLen == 0) ? 1 : ((lineLen - 1) / charsPerLine + 1);
            }
        } else {
            int lineLen = text.length();
            totalLines = (lineLen == 0) ? 1 : ((lineLen - 1) / charsPerLine + 1);
        }

        // 计算文本实际需要的像素高度
        int textHeight = totalLines * lineHeight-totalLines*4;

//        Dimension textPrefSize = textArea.getPreferredSize();
//        int bubbleHeight0 = textPrefSize.height + borderWidth;
//
//        int textHeight=bubbleHeight0;

        // 用计算出的高度覆盖 textArea 的 preferredSize，防止高度不足,增加这里的高度和宽度可以解决文本显示不完全的问题
        textArea.setPreferredSize(new Dimension(maxTextWidth, textHeight+50));//
        textArea.setMaximumSize(new Dimension(maxTextWidth, textHeight+50));

        // 气泡宽度 = 文本宽度 + hgap，高度由内容决定
        int bubbleWidth = maxTextWidth + 20;
        int bubbleHeight = textHeight + 30;

        // 关键改动：改用 FlowLayout，让 bubble 宽度由内容决定
        JPanel bubble = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bubble.setOpaque(true);
        bubble.setBackground(bgColor);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bgColor, 10, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        bubble.add(textArea);

        bubble.setPreferredSize(new Dimension(bubbleWidth, bubbleHeight));
        bubble.setMaximumSize(new Dimension(bubbleWidth, Integer.MAX_VALUE));

        return bubble;
    }


    //计算气泡框的最大宽度
    private int maxBubbleWidth() {
        int viewport = chatScroll == null ? 800 : chatScroll.getViewport().getWidth();
        // 60% 视口,但要给 LineBorder (10*2=20) + EmptyBorder (10*2=20) 留 40px
        int maxBubble = Math.max(180, Math.min((int)(viewport * 0.6), 520));
        // 文本区可用宽度 = 气泡宽度 - 边框内外边距(约 40)
        return Math.max(40, maxBubble - 40);
    }


    private JPanel createMessageRow(ChatMessage msg, Integer myUserId) {
        boolean isMine = msg.getSenderId().equals(myUserId);

        //计算出气泡框的最大宽度，作为参数用于创建气泡框组件
        int maxTextWidth = maxBubbleWidth();
        JPanel bubble = createBubble(msg.getContent(), isMine, maxTextWidth);


        // 此时 bubble 已经是"刚性组件":preferredSize.width == maximumSize.width
        // BoxLayout 不会再拉伸它

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 12, 6, 12));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setAlignmentY(Component.TOP_ALIGNMENT);

        if (isMine) {
            row.add(Box.createHorizontalGlue()); // 撑杆在左 → 气泡贴右
            row.add(bubble);
        } else {
            row.add(bubble);                      // 气泡在左
            row.add(Box.createHorizontalGlue());  // 撑杆在右(视觉对称)
        }

        Dimension rowPref = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowPref.height));
        return row;
    }

    // ======================================== 加载好友列表 ========================================


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
                renderFriendList(currentFriends);//刷新好友列表中的在线状态

                if (currentSelectedFriend != null &&
                        userId.equals(currentSelectedFriend.getUserid())) {
                    currentSelectedFriend.setStatus(status);
                    showChatPanel(currentSelectedFriend,null);//刷新消息页面中的在线状态
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
//                showFriendDetail(friend, null);//显示选中用户的信息到右侧页面
                showChatPanel(friend,null);
            }
        });
        return row;
    }

    //选中好友，打开对应的聊天窗口
    private void showChatPanel(User friend,String remark){
        //顶部渲染选中好友的备注或者昵称（用户名）
        if(remark!=null ){
            friendName.setText(getDisplayName(currentSelectedFriend,remark));
        }else {
//            friendName.setText(currentSelectedFriend.getNickname()!=null?currentSelectedFriend.getNickname():currentSelectedFriend.getUsername());
            friendName.setText(getDisplayName(currentSelectedFriend,null));
        }
        //渲染在线状态
        if (friend.getStatus() != null && friend.getStatus() == 1) {
            friendStatus.setText("● 在线");
            friendStatus.setForeground(new Color(76, 175, 80));
        } else {
            friendStatus.setText("○ 离线");
            friendStatus.setForeground(new Color(150, 150, 150));
        }
        //调用封装好的方法展示与该好友的聊天消息页面
        loadAllMessages(UserSession.getInstance().getCurrentUser().getUserid(),friend.getUserid());
        //
        CardLayout cl = (CardLayout) ((JPanel) rightChatPanel.getParent()).getLayout();
        cl.show((JPanel) rightChatPanel.getParent(), "chat");//将右侧页面切换为被选中的好友信息页面
    }
    //过滤好友列表，根据关键词只显示符合关键词的好友
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

    // ======================================== 加载聊天消息 ========================================

    //将传入的消息列表渲染到页面上
    private void renderMessages(List<ChatMessage> messages) {
        center.removeAll();

        Integer myUserId = UserSession.getInstance().getCurrentUser().getUserid();
        for (ChatMessage msg : messages) {
            center.add(createMessageRow(msg, myUserId));
        }
        //
        center.revalidate();
        center.repaint();

        //自动滚动到底部（显示最新消息）
        JScrollBar vertical = chatScroll.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> {
            vertical.setValue(vertical.getMaximum());
        });
    }



    /*
    方案一：一次性加载所有的消息
    */
    //加载所有的聊天消息
    private void loadAllMessages(Integer userId,Integer friendId) {
//        List<ChatMessage> messages = messageFacade.getAllMessage(userId,friendId).getData();
        allMessages = messageFacade.getAllMessage(userId,friendId).getData();
        //
//        messages.add(msg(1, friendId, userId, "你好，请问是张三吗？", baseTime().minusMinutes(30)));
//        messages.add(msg(2, userId,  friendId, "是的，我是。",            baseTime().minusMinutes(28)));
//        messages.add(msg(3, friendId, userId, "我是从 GitHub 上关注到你",
//                baseTime().minusMinutes(20)));
//        messages.add(msg(4, userId,  friendId, "感谢关注！请问有什么事？",
//                baseTime().minusMinutes(15)));
//        messages.add(msg(5, friendId, userId, "想请教一下你这个项目用的是什么架构？",
//                baseTime().minusMinutes(10)));
//        messages.add(msg(6, userId,  friendId, "前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端前后端分离 + Socket 自定义协议 + Swing 客户端",
//                baseTime().minusMinutes(5)));
//        messages.add(msg(7, friendId, userId, "明白了，谢谢！",
//                baseTime().minusMinutes(1)));
//        messages.add(msg(8, friendId, userId, "你好，请问是张三吗？", baseTime().minusMinutes(30)));
//        messages.add(msg(9, userId,  friendId, "是的，我是。",            baseTime().minusMinutes(28)));
//        messages.add(msg(10, friendId, userId, "我是从 GitHub 上关注到你",
//                baseTime().minusMinutes(20)));
//        messages.add(msg(11, userId,  friendId, "感谢关注！请问有什么事？",
//                baseTime().minusMinutes(15)));
//        messages.add(msg(12, friendId, userId, "想请教一下你这个项目用的是什么架构？",
//                baseTime().minusMinutes(10)));
//        messages.add(msg(6, userId,  friendId, "前后端分离 + Socket 自定义协议 + Swing 客户端",
//                baseTime().minusMinutes(5)));
//        messages.add(msg(7, friendId, userId, "明白了，谢谢！",
//                baseTime().minusMinutes(1)));
//        messages.add(msg(1, friendId, userId, "你好，请问是张三吗？", baseTime().minusMinutes(30)));
//        messages.add(msg(2, userId,  friendId, "是的，我是。",            baseTime().minusMinutes(28)));
//        messages.add(msg(3, friendId, userId, "我是从 GitHub 上关注到你",
//                baseTime().minusMinutes(20)));
//        messages.add(msg(4, userId,  friendId, "感谢关注！请问有什么事？",
//                baseTime().minusMinutes(15)));
//        messages.add(msg(5, friendId, userId, "想请教一下你这个项目用的是什么架构？",
//                baseTime().minusMinutes(10)));
//        messages.add(msg(6, userId,  friendId, "前后端分离 + Socket 自定义协议 + Swing 客户端",
//                baseTime().minusMinutes(5)));
//        messages.add(msg(7, friendId, userId, "明白了，谢谢！",
//                baseTime().minusMinutes(1)));
//        messages.add(msg(8, friendId, userId, "你好，请问是张三吗？", baseTime().minusMinutes(30)));
//        messages.add(msg(9, userId,  friendId, "是的，我是。",            baseTime().minusMinutes(28)));
//        messages.add(msg(10, friendId, userId, "我是从 GitHub 上关注到你",
//                baseTime().minusMinutes(20)));
//        messages.add(msg(11, userId,  friendId, "感谢关注！请问有什么事？",
//                baseTime().minusMinutes(15)));
//        messages.add(msg(12, friendId, userId, "想请教一下你这个项目用的是什么架构？",
//                baseTime().minusMinutes(10)));
//        messages.add(msg(6, userId,  friendId, "前后端分离 + Socket 自定义协议 + Swing 客户端",
//                baseTime().minusMinutes(5)));
//        messages.add(msg(7, friendId, userId, "明白了，谢谢！",
//                baseTime().minusMinutes(1)));


        renderMessages(allMessages);
    }

    private ChatMessage msg(int id, int sender, int receiver, String content, LocalDateTime t) {
        ChatMessage m = new ChatMessage();
        m.setChatMessageId(id);
        m.setSenderId(sender);
        m.setReceiverId(receiver);
        m.setType(1);
        m.setContent(content);
        m.setStatus(1);
        m.setCreateTime(t);
        m.setUpdateTime(t);
        return m;
    }
    private LocalDateTime baseTime() {
        return LocalDateTime.now();
    }

    /*
    方案二：分页加载消息
    */
    //初始化，加载固定数量消息
    //触顶，继续加载固定数量的消息
    //加载某一页（页码从 0 开始，0=最新 50 条，1=再往前 50 条...）

    /*private void loadMessagesPage(Integer friendUserId, int page) {
        if (isLoading || !hasMore) return;
        isLoading = true;
        Result<List<ChatMessage>> result = messageFacade.getMessages(friendUserId, page, PAGE_SIZE);
        if (result.getCode() == 0 && result.getData() != null) {
            List<ChatMessage> newMessages = result.getData();
            hasMore = (newMessages.size() == PAGE_SIZE);//返回数量不足一页说明没有更多
            currentPage = page;
            if (page == 0) {
                // 第一页：直接清空并渲染
                allMessages.clear();
                allMessages.addAll(newMessages);
                renderMessages(allMessages, false);
            } else {
                // 非第一页：插入到列表头部（往历史翻）
                allMessages.addAll(0, newMessages);
                renderMessages(allMessages, true);//true：保持当前滚动位置
            }
        }
        isLoading = false;
    }

    private void scrollToBottom() {
        JScrollBar vertical = chatScroll.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    // 给 centerPanel 的滚动条加监听
    chatScroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
        private int previousValue = 0;

        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            JScrollBar sb = (JScrollBar) e.getSource();
            if (!e.getValueIsAdjusting()) {
                // 滚动到最顶部（往上滑）
                if (e.getValue() == 0 && previousValue > 0) {
                    loadMessagesPage(currentSelectedFriend.getUserid(), currentPage + 1);
                }
                previousValue = e.getValue();
            }
        }
    });*/


    // ======================================== 按钮功能 ========================================

    //发送消息按钮功能绑定
    private void handleSendMessage() {
        //1.检查信息是否为空
        if(messageArea.getText().trim().isEmpty() || (messageArea.getText().trim()==null) ){
            JOptionPane.showMessageDialog(null,"请输入要发送的消息");
        }
        if (currentSelectedFriend == null) {
            JOptionPane.showMessageDialog(null, "请先选中一个好友");
            return;
        }
        //2.调用发送消息的接口
//        Result<ChatMessage>r1= messageFacade.sendMessage(UserSession.getInstance().getCurrentUser().getUserid(),currentSelectedFriend.getUserid(), 0,messageArea.getText(),0);
//        if(r1.getCode()==0){
//            messageArea.removeAll();//发送成功就清空发送框中的消息
//            addMessage(r1.getData());//刷新新消息到当前页面
//        }else {
//            JOptionPane.showMessageDialog(null,"发送失败");
//        }
        //把对 messageFacade.sendMessage 的调用包在 try/catch 里，避免因解析异常把整个流程炸掉时显示"发送失败"（误导用户）
        try {
            Result<ChatMessage> r1 = messageFacade.sendMessage(UserSession.getInstance().getCurrentUser().getUserid(),currentSelectedFriend.getUserid(), 0,messageArea.getText(),0);
            if (r1.getCode() == 0 && r1.getData() != null) {
                messageArea.setText("");
                addMessageOfMine(r1.getData());
            } else {
                JOptionPane.showMessageDialog(null, "发送失败: " + r1.getMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "发送失败: " + ex.getMessage());
        }
    }

    // ======================================== 推送信息ui更新相关的方法 ========================================
    //在MessageView类内部新增
    public void addFriend(User friend) {
        if (friend == null || friend.getUserid() == null) {
            return;
        }
        //防止重复添加
        for (User u : currentFriends) {
            if (u.getUserid().equals(friend.getUserid())) {
                return;
            }
        }
        currentFriends.add(friend);
        renderFriendList(currentFriends);
    }

    //新增一条消息到消息页面中(我发送的)
    public void addMessageOfMine(ChatMessage chatMessage){
        if (chatMessage == null || chatMessage.getChatMessageId() == null) {
            return;
        }
        //1.将新消息添加到
        //防止重复添加
        for (ChatMessage c : allMessages) {
            if (c.getChatMessageId().equals(chatMessage.getChatMessageId())) {
                return;
            }
        }
        //判断当前打开的聊天窗口是不是这条消息对应的,关键
        Integer senderId=chatMessage.getSenderId();
        Integer receiverId=chatMessage.getReceiverId();
        Integer myId=UserSession.getInstance().getCurrentUser().getUserid();
        if(myId==senderId){
            allMessages.add(chatMessage);
            //2.重新渲染消息页面
            renderMessages(allMessages);
        }
    }
    //新增一条消息到消息页面中(发送者是其他人，需要判断发送者和当前打开的会话窗口归属者，是不是同一个好友)
        public void addMessageOfOthers(ChatMessage chatMessage){
        if (chatMessage == null || chatMessage.getChatMessageId() == null) {
            return;
        }
        //1.将新消息添加到
        //防止重复添加
        for (ChatMessage c : allMessages) {
            if (c.getChatMessageId().equals(chatMessage.getChatMessageId())) {
                return;
            }
        }
        //判断当前打开的聊天窗口是不是这条消息对应的,关键
        Integer senderId=chatMessage.getSenderId();
        Integer receiverId=chatMessage.getReceiverId();
        Integer myId=UserSession.getInstance().getCurrentUser().getUserid();

        // ===== 修复：判定"消息的对方"是不是当前打开的好友 =====
        // 消息对方 = 我发送时是 receiver，接收时是 sender
        // 只有当前打开了这个好友，才把消息渲染到当前会话窗口
        if (currentSelectedFriend == null) {
            // 当前没有打开任何会话：消息暂时不渲染，
            // 好友列表的未读小红点会由后端 <push><friendListUpdate> 触发更新
            return;
        }
        Integer otherId = senderId.equals(myId) ? receiverId : senderId;
        if (otherId.equals(currentSelectedFriend.getUserid())) {
            allMessages.add(chatMessage);
            //2.重新渲染消息页面
            renderMessages(allMessages);
        }

    }


}
