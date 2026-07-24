package com.ylaneo.chatclientdesktop.view.dialogs;

import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.impl.ContactFacadeImpl;
import com.ylaneo.chatclientdesktop.session.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchUserDialogView extends JDialog {
    // ======================================== 组件 ========================================
    private JPanel resultListPanel;
    private List<User> userList = new ArrayList<>();
    private User selectedUser;
    // ======================================== 构造函数 ========================================
    public SearchUserDialogView(Frame parent) {
        super(parent, "搜索用户", true);
        setLayout(new BorderLayout());//整体设置布局管理器
        //这里用setsize设置的尺寸不生效，要用setpreferredsize，为什么？答：因为pack（）方法发力了
//        setSize(450, 540);//抛弃糟粕
        setPreferredSize(new Dimension(850, 600));
        setMinimumSize(new Dimension(850, 400));// 可以拖小的最小尺寸
        setMaximumSize(new Dimension(Integer.MAX_VALUE,600));
        setLocationRelativeTo(parent);//

        // 顶部提示板块
        JLabel title = new JLabel("搜索结果");
        title.setFont(new Font("微软雅黑", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(16, 16, 8, 16));
        add(title, BorderLayout.NORTH);

        // 结果列表板块
        resultListPanel = new JPanel();//放置查询到的用户信息的板块
        resultListPanel.setLayout(new BoxLayout(resultListPanel, BoxLayout.Y_AXIS));
        resultListPanel.setBackground(Color.WHITE);
        resultListPanel.setPreferredSize(new Dimension(800, 300));//
        resultListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));//
        resultListPanel.setMinimumSize(new Dimension(800,Integer.MAX_VALUE));
        //
        JScrollPane scroll = new JScrollPane(resultListPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
//        scroll.setPreferredSize(new Dimension(420, 540));//不要给这个组件设置固定的尺寸，给容器设置尺寸，把这个组件放进去就行了
        add(scroll, BorderLayout.CENTER);

        // 底部关闭按钮
        JPanel bottom = new JPanel();
        JButton closeBtn = new JButton("关闭");
        styleButton(closeBtn,new Color(66, 133, 244));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);
        bottom.setBorder(new EmptyBorder(8, 0, 12, 0));
        add(bottom, BorderLayout.SOUTH);

//        pack();//不要重新计算尺寸，不然有些尺寸会变得奇怪
    }
    //加载用户头像到信息行中
    private void loadAvatarInto(JLabel label, String path, int size) {
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            ImageIcon icon = new ImageIcon(path);
            Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(img));
            label.setText("");
        } else {
            label.setIcon(null);
            label.setText("头像");
            label.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            label.setForeground(new Color(180, 180, 190));
            label.setHorizontalAlignment(SwingConstants.CENTER);
        }
    }
    //渲染一条用户数据
    private JPanel createUserRow(User user) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        //这里尺寸设置，三行都需要
        row.setPreferredSize(new Dimension(750, 60));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row.setMinimumSize(new Dimension(50, 60));
        //
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 230)));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 头像
        JLabel avatar = new JLabel();
        avatar.setPreferredSize(new Dimension(44, 44));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setVerticalAlignment(SwingConstants.CENTER);
        loadAvatarInto(avatar, user.getAvatar_url(), 44);

        //昵称+用户名，整体容器
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.setPreferredSize(new Dimension(750, 60));
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        info.setMinimumSize(new Dimension(50, 60));
        //昵称
        JLabel nicknameLabel = new JLabel(user.getNickname() != null ? user.getNickname() : "-");
        nicknameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        //用户名部分
        JLabel usernameLabel = new JLabel("@" + user.getUsername());
        usernameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        usernameLabel.setForeground(new Color(130, 130, 140));
        //添加按钮部分
        JButton addBtn=new JButton("发送好友申请");
        styleButton(addBtn,new Color(66, 133, 244));
        addBtn.setPreferredSize(new Dimension(100,35));
        addBtn.setMaximumSize(new Dimension(100,40));
        //按钮需要包裹一层才能让尺寸设置生效
        JPanel eastBox = new JPanel();
        eastBox.setLayout(new BoxLayout(eastBox, BoxLayout.Y_AXIS));
        eastBox.setOpaque(false);//设置为透明
        eastBox.setBackground(Color.WHITE);
        eastBox.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        //用Glue让按钮垂直居中
        eastBox.add(Box.createVerticalGlue());
        eastBox.add(addBtn);
        eastBox.add(Box.createVerticalGlue());
        //设置对齐方式
        nicknameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
//        addBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        //
        info.add(nicknameLabel);
        info.add(usernameLabel);
        //将每行的三个部分添加到每行的指定位置
        row.add(avatar, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
//        row.add(addBtn,BorderLayout.EAST);
        row.add(eastBox,BorderLayout.EAST);
        // 点击选择
        final JPanel finalRow = row;
        row.addMouseListener(new MouseAdapter() {
            //点击该信息行后触发的事件
            @Override public void mouseClicked(MouseEvent e) {
                selectedUser = user;
                JOptionPane.showMessageDialog(SearchUserDialogView.this,
                        "已选中：" + (user.getNickname() != null ? user.getNickname() : user.getUsername()));
//                dispose();
            }
            @Override public void mouseEntered(MouseEvent e) { finalRow.setBackground(new Color(230, 238, 255)); }
            @Override public void mouseExited(MouseEvent e)  { finalRow.setBackground(Color.WHITE); }
        });
        //绑定添加按钮的事件
        addBtn.addActionListener(e->{
            //通过二次弹窗填写备注信息
            String message = JOptionPane.showInputDialog(
                    SearchUserDialogView.this,
                    "请输入备注信息：",
                    "添加好友 - " + (user.getNickname() != null ? user.getNickname() : user.getUsername()),
                    JOptionPane.PLAIN_MESSAGE
            );
            //直接取消，没有获得数据，就直接返回
            if (message == null) return;
            Integer currentUserId = UserSession.getInstance().getCurrentUser().getUserid();
            if (user.getUserid() == null) {
                JOptionPane.showMessageDialog(this, "该用户ID为空，无法发送好友申请");
                return;
            }
            Integer receiverId = user.getUserid();
            Result result = new ContactFacadeImpl().sendFriendRequest(currentUserId, receiverId, message);
            //
            if (result.getCode()==0) {
                JOptionPane.showMessageDialog(SearchUserDialogView.this,
                        "好友申请已发送！");
            } else {
                JOptionPane.showMessageDialog(SearchUserDialogView.this,
                        "发送失败：" + result.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        return row;
    }

    // ======================================== 工具方法 ========================================
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
    // ======================================== 暴露给外部的方法 ========================================

    //提供给外部调用的：搜索用户的方法，需要从外部传入查询出来的用户列表信息
    public void setUserList(List<User> users) {
        this.userList = users;
        resultListPanel.removeAll();
        if (users == null || users.isEmpty()) {
            JLabel empty = new JLabel("  没有找到匹配的用户");
            empty.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            empty.setForeground(new Color(150, 150, 160));
            resultListPanel.add(Box.createVerticalStrut(20));
            resultListPanel.add(empty);
        } else {
            for (User u : users) {
                resultListPanel.add(createUserRow(u));
            }
        }
        resultListPanel.revalidate();
        resultListPanel.repaint();
    }
    //提供给外界获取选中的用户信息行的方法
    public User getSelectedUser() { return selectedUser; }
}