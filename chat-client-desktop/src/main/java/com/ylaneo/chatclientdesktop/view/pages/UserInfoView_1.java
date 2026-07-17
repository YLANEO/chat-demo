package com.ylaneo.chatclientdesktop.view.pages;

import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.session.UserSession;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UserInfoView_1 extends JPanel {

    // 头像目录
    private static final String AVATAR_DIR = "D:/files_D/avatar_chat_demo";

    // 组件
    private JLabel avatarLabel;
    private JLabel usernameLabel;
    private JTextField nicknameField;
    private JButton updateNicknameBtn;
    private JButton selectAvatarBtn;
    private JLabel statusLabel;

    // 头像路径
    private String currentAvatarUrl;

    //
    UserFacade userFacade=new UserFacadeImpl();
    //
    public UserInfoView_1() {
        initComponents();
        loadUserInfo();
    }

    private void initComponents() {
        //
        setLayout(new BorderLayout(20, 0));//设置布局方式，指定组件之间的横向、纵向间距
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));//创建透明边框
        setBackground(new Color(245, 245, 250));//设置背景的颜色

        // ==================== 左侧：头像区域 ====================
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 230), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // 头像
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(120, 120));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 210), 2));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(new Color(240, 240, 245));

        // 头像下方说明
        JLabel avatarTip = new JLabel("点击下方按钮更换头像");
        avatarTip.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        avatarTip.setForeground(new Color(120, 120, 130));
        avatarTip.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 选择头像按钮
        selectAvatarBtn = new JButton("选择头像");
        styleButton(selectAvatarBtn, new Color(66, 133, 244));
        selectAvatarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectAvatarBtn.addActionListener(e -> selectAvatar());

        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(avatarLabel);
        leftPanel.add(Box.createVerticalStrut(15));
        leftPanel.add(avatarTip);
        leftPanel.add(Box.createVerticalStrut(15));
        leftPanel.add(selectAvatarBtn);
        leftPanel.add(Box.createVerticalGlue());

        // ==================== 右侧：用户信息区域 ====================
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 230), 1),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));

        // 标题
        JLabel title = new JLabel("个人信息");
        title.setFont(new Font("微软雅黑", Font.BOLD, 20));
        title.setForeground(new Color(50, 50, 50));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 分隔线
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(new Color(220, 220, 230));

        // 用户信息卡片
        JPanel infoCard = new JPanel();
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBackground(Color.WHITE);
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 用户名行
        JPanel usernameRow = createInfoRow("用户名", null, false);
        usernameLabel = (JLabel) usernameRow.getClientProperty("valueLabel");

        // 昵称行
        JPanel nicknameRow = createInfoRow("昵称", null, true);
        JLabel nicknameTitle = (JLabel) nicknameRow.getClientProperty("titleLabel");
        nicknameField = (JTextField) nicknameRow.getClientProperty("valueField");
        updateNicknameBtn = (JButton) nicknameRow.getClientProperty("actionBtn");
        styleButton(updateNicknameBtn, new Color(66, 133, 244));
        updateNicknameBtn.setText("保存");
        updateNicknameBtn.addActionListener(e -> updateNickname());

        // 在线状态行
        JPanel statusRow = createInfoRow("在线状态", null, false);
        statusLabel = (JLabel) statusRow.getClientProperty("valueLabel");

        infoCard.add(Box.createVerticalStrut(10));
        infoCard.add(usernameRow);
        infoCard.add(Box.createVerticalStrut(20));
        infoCard.add(nicknameRow);
        infoCard.add(Box.createVerticalStrut(20));
        infoCard.add(statusRow);

        rightPanel.add(title);
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(separator);
        rightPanel.add(Box.createVerticalStrut(25));
        rightPanel.add(infoCard);
        rightPanel.add(Box.createVerticalGlue());

        // 添加到主面板
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    /**
     * 创建信息行
     */
    private JPanel createInfoRow(String title, String value, boolean editable) {
        JPanel row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.putClientProperty("titleLabel", new JLabel(title));

        JLabel titleLabel = new JLabel(title + "：");
        titleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        titleLabel.setPreferredSize(new Dimension(70, 30));
        titleLabel.setForeground(new Color(100, 100, 110));
        row.add(titleLabel);

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
            row.putClientProperty("valueField", textField);

            JButton btn = new JButton("保存");
            btn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            btn.setPreferredSize(new Dimension(70, 32));
            row.add(Box.createHorizontalStrut(10));
            row.add(btn);
            row.putClientProperty("actionBtn", btn);
        } else {
            JLabel valueLabel = new JLabel(value != null ? value : "");
            valueLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            valueLabel.setForeground(new Color(50, 50, 50));
            row.add(valueLabel);
            row.putClientProperty("valueLabel", valueLabel);
        }

        return row;
    }

    /**
     * 按钮样式
     */
    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 35));

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

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        Integer userId = UserSession.getInstance().getCurrentUser().getUserid();
        User user=userFacade.getUserData(userId).getData();
        if (user == null) return;

        // 用户名
        usernameLabel.setText(user.getUsername());

        // 昵称
        String nickname = user.getNickname();
        nicknameField.setText(nickname != null ? nickname : "");

        // 在线状态
        Integer status = user.getStatus();
        if (status != null && status == 1) {
            statusLabel.setText("● 在线");
            statusLabel.setForeground(new Color(76, 175, 80));
        } else {
            statusLabel.setText("○ 离线");
            statusLabel.setForeground(new Color(150, 150, 150));
        }

        // 头像
        currentAvatarUrl = user.getAvatar_url();
        loadAvatar(currentAvatarUrl);
    }

    /**
     * 加载头像
     */
    private void loadAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            File file = new File(avatarUrl);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(avatarUrl);
                Image img = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                avatarLabel.setIcon(new ImageIcon(img));
                return;
            }
        }
        // 默认头像
        setDefaultAvatar();
    }

    /**
     * 设置默认头像
     */
    private void setDefaultAvatar() {
        avatarLabel.setIcon(null);
        avatarLabel.setText("暂无头像");
        avatarLabel.setForeground(new Color(180, 180, 190));
        avatarLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
    }

    /**
     * 选择头像
     */
    private void selectAvatar() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择头像图片");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "图片文件 (*.jpg;*.jpeg;*.png;*.gif)", "jpg", "jpeg", "png", "gif"
        ));
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            uploadAvatar(selectedFile);
        }
    }

    /**
     * 上传头像
     */
    private void uploadAvatar(File file) {
        try {
            // 确保目录存在
            File dir = new File(AVATAR_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名：user_{id}.png
            Integer userId = UserSession.getInstance().getCurrentUser().getUserid();
            String extension = getFileExtension(file.getName());
            String newFileName = "user_" + userId + "." + extension;
            File destFile = new File(AVATAR_DIR, newFileName);

            // 复制文件到目标目录
            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 构造URL路径
            String avatarUrl = AVATAR_DIR + "/" + newFileName;

            // 上传到服务器
            UserFacade facade = new UserFacadeImpl();
            Result result = facade.updateUserAvatar(userId, avatarUrl);

            if (result.getCode() == 0) {
                // 更新Session
                UserSession.getInstance().getCurrentUser().setAvatar_url(avatarUrl);
                currentAvatarUrl = avatarUrl;

                // 显示新头像
                loadAvatar(avatarUrl);

                JOptionPane.showMessageDialog(this, "头像上传成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "头像上传失败：" + result.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "头像上传失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "png";
    }

    /**
     * 修改昵称
     */
    private void updateNickname() {
        String newNickname = nicknameField.getText().trim();
        if (newNickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "昵称不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer userId = UserSession.getInstance().getCurrentUser().getUserid();
        UserFacade facade = new UserFacadeImpl();
        Result result = facade.updateNickname(userId, newNickname);

        if (result.getCode() == 0) {
            // 更新Session
            UserSession.getInstance().getCurrentUser().setNickname(newNickname);
            JOptionPane.showMessageDialog(this, "昵称修改成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "昵称修改失败：" + result.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}