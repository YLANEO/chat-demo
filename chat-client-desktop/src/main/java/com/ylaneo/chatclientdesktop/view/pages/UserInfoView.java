package com.ylaneo.chatclientdesktop.view.pages;

import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.facade.impl.UserFacadeImpl;
import com.ylaneo.chatclientdesktop.session.UserSession;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UserInfoView extends JPanel {
    // ======================================== 组件 ========================================
    // 头像目录
    private String AVATAR_DIR = "D:/files_D/avatar_chat_demo";
    private String currentAvatarUrl;//
    //
    private JLabel avatarLabel;
    private JLabel usernameLabel;
    private JTextField nicknameField;
    private JButton updateNicknameBtn;
    private JButton selectAvatarBtn;
    private JLabel statusLabel;
    //
    UserFacade userFacade=new UserFacadeImpl();
    // ========================================  ========================================
    public UserInfoView(){
        //
        initComponents();//初始化组件
        loadUserInfo();//加载所有此页面所有的用户信息
    }
    // ======================================== 初始化此页面的所有组件 ========================================
    private void initComponents(){
        //
        setLayout(new BorderLayout(0, 0));//设置布局方式，指定组件之间的横向、纵向间距
//        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 2, 1));//创建透明边框
        setBackground(new Color(245, 245, 250));//设置背景的颜色
        //

        //设置整体的布局管理器
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 230), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        //
        // 标题
        JLabel title = new JLabel("个人信息");
        title.setFont(new Font("微软雅黑", Font.BOLD, 20));
        title.setForeground(new Color(50, 50, 50));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);//设置对齐方式
        // 分隔线
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(new Color(220, 220, 230));
        // 头像
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(200, 200));//头像尺寸设置
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
//        avatarLabel.setLayout(new GridBagLayout());//
        avatarLabel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 210), 1));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(new Color(240, 240, 245));
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);//

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



        // 用户信息卡片
        JPanel infoCard = new JPanel();
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBackground(Color.WHITE);
        infoCard.setAlignmentX(Component.CENTER_ALIGNMENT);//

        // 用户名行
        JPanel usernameRow = createInfoRow("用户名", null, false);
        usernameLabel = (JLabel) usernameRow.getClientProperty("valueLabel");
        usernameRow.setAlignmentX(Component.CENTER_ALIGNMENT);//

        // 昵称行
        JPanel nicknameRow = createInfoRow("昵称", null, true);
        JLabel nicknameTitle = (JLabel) nicknameRow.getClientProperty("titleLabel");
        nicknameField = (JTextField) nicknameRow.getClientProperty("valueField");
        updateNicknameBtn = (JButton) nicknameRow.getClientProperty("actionBtn");
        styleButton(updateNicknameBtn, new Color(66, 133, 244));
        updateNicknameBtn.setText("保存");
        updateNicknameBtn.addActionListener(e -> updateNickname());
        nicknameRow.setAlignmentX(Component.CENTER_ALIGNMENT);//

        // 在线状态行
        JPanel statusRow = createInfoRow("在线状态", null, false);
        statusLabel = (JLabel) statusRow.getClientProperty("valueLabel");
        statusRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        //将用户信息添加到这个卡片中，卡片对这些信息行进行统一的控制
        infoCard.add(Box.createVerticalStrut(10));
        infoCard.add(usernameRow);
        infoCard.add(Box.createVerticalStrut(20));
        infoCard.add(nicknameRow);
        infoCard.add(Box.createVerticalStrut(20));
        infoCard.add(statusRow);
        infoCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        //将所有组件按照顺序添加到页面中
        contentPanel.add(title);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(separator);
        contentPanel.add(Box.createVerticalGlue());//一个弹性空间
        //
        contentPanel.add(avatarLabel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(avatarTip);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(selectAvatarBtn);
//        contentPanel.add(Box.createVerticalGlue());//一个弹性空间
        //
        contentPanel.add(Box.createVerticalStrut(25));
        contentPanel.add(infoCard);
        contentPanel.add(Box.createVerticalGlue());//一个弹性空间
        //

        //不要忘了这一步，不然是一片空白
//        add(contentPanel);
        add(contentPanel,BorderLayout.CENTER);
    }
    // ======================================== 加载所有此页面所有的用户信息 ========================================
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
    // ======================================== 工厂方法？？ ========================================
    //按钮创建的工厂方法
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
    //创建信息行的方法
    private JPanel createInfoRow(String title, String value, boolean editable) {
        JPanel row = new JPanel();
        //
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));//对齐方式
//        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setMaximumSize(new Dimension(600, 40));//最大宽度限制
        row.setAlignmentX(Component.CENTER_ALIGNMENT);//置于父组件中间
        //
        row.putClientProperty("titleLabel", new JLabel(title));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        titleLabel.setPreferredSize(new Dimension(90, 30));//每一行前面的文字的宽度
        titleLabel.setForeground(new Color(100, 100, 110));
        JLabel sign=new JLabel(":  ");
        sign.setFont(new Font("微软雅黑", Font.BOLD, 16));
        row.add(titleLabel);
        row.add(sign);

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

    // ======================================== 其他方法 ========================================
    //加载头像
    private void loadAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            File file = new File(avatarUrl);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(avatarUrl);
                Image img = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);//头像图片缩放显示尺寸
                avatarLabel.setIcon(new ImageIcon(img));
                return;
            }
        }
        // 默认头像
        setDefaultAvatar();
    }

    //默认头像
    private void setDefaultAvatar() {
        avatarLabel.setIcon(null);
        avatarLabel.setText("暂无头像");
        avatarLabel.setForeground(new Color(180, 180, 190));
        avatarLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
    }

    //选择头像
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

    //上传头像
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

    //获取文件拓展名
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "png";
    }


    //修改昵称
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
