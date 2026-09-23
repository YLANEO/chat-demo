package com.ylaneo.chat_client_android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.manager.ContactManager;
import com.ylaneo.chat_client_android.manager.MessageManager;
import com.ylaneo.chat_client_android.manager.UserManager;
import com.ylaneo.chat_client_android.model.ChatMessage;
import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.network.PushListener;
import com.ylaneo.chat_client_android.session.UserSession;
import com.ylaneo.chat_client_android.ui.adapter.ChatMessageAdapter;

import java.util.List;

/**
 * 聊天页面
 */
public class ChatActivity extends AppCompatActivity implements PushListener.PushCallback {

    public static final String EXTRA_FRIEND_ID = "friendId";
    public static final String EXTRA_FRIEND_NAME = "friendName";
    public static final String EXTRA_FRIEND_AVATAR = "friendAvatar";

    private ImageButton btnBack;
    private TextView tvTitle;
    private RecyclerView rvMessages;
    private TextInputEditText etInput;
    private View btnSend;

    private int friendId;
    private String friendName;
    private String friendAvatar;
    private Integer currentUserId;

    private ChatMessageAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PushListener.PushCallback selfRef = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        friendId = getIntent().getIntExtra(EXTRA_FRIEND_ID, -1);
        friendName = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        friendAvatar = getIntent().getStringExtra(EXTRA_FRIEND_AVATAR);
        currentUserId = UserSession.getInstance().getCurrentUserId();

        if (friendId == -1 || currentUserId == null) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadMessages();

        PushListener.registerCallback(selfRef);
    }

    @Override
    protected void onDestroy() {
        PushListener.unregisterCallback(selfRef);
        super.onDestroy();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        rvMessages = findViewById(R.id.rv_messages);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);

        tvTitle.setText(friendName != null ? friendName : "聊天");

        adapter = new ChatMessageAdapter();
        // 双方头像 URL 先注入 adapter，onBindViewHolder 里再据此选择显示哪张
        adapter.setFriendAvatar(friendAvatar);
        if (UserSession.getInstance().getCurrentUser() != null) {
            adapter.setSelfAvatar(UserSession.getInstance().getCurrentUser().getAvatarUrl());
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> handleSend());

        // 自己头像：登录接口只回了 userId/username，没回 avatarUrl，
        // 这里主动拉一次 <getUserData> 把头像补上（顺带也是拿最新的）。
        refreshSelfAvatar();
    }

    private void refreshSelfAvatar() {
        Integer uid = UserSession.getInstance().getCurrentUserId();
        if (uid == null) return;
        new Thread(() -> {
            User fresh = UserManager.getInstance().getUserData(uid);
            if (fresh == null || fresh.getAvatarUrl() == null || fresh.getAvatarUrl().isEmpty()) {
                return;
            }
            // 回填 session，避免下次进来再走一次网络
            User cur = UserSession.getInstance().getCurrentUser();
            if (cur != null) {
                cur.setAvatarUrl(fresh.getAvatarUrl());
                if (fresh.getNickname() != null) cur.setNickname(fresh.getNickname());
                if (fresh.getUsername() != null) cur.setUsername(fresh.getUsername());
            }
            mainHandler.post(() -> adapter.setSelfAvatar(fresh.getAvatarUrl()));
        }).start();
    }

    private void loadMessages() {
        new Thread(() -> {
            try {
                List<ChatMessage> messages = MessageManager.getInstance()
                        .getAllMessages(currentUserId, friendId);
                mainHandler.post(() -> {
                    adapter.setMessages(messages);
                    scrollToBottom();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "加载消息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void handleSend() {
        String content = etInput.getText() != null ? etInput.getText().toString().trim() : "";
        if (content.isEmpty()) {
            return;
        }

        etInput.setText("");

        new Thread(() -> {
            try {
                ChatMessage sent = MessageManager.getInstance()
                        .sendMessage(currentUserId, friendId, 0, content, 0);
                mainHandler.post(() -> {
                    adapter.addMessage(sent);
                    scrollToBottom();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    /**
     * 收到推送时回调（来自 PushListener，在主线程）
     */
    @Override
    public void onPush(String pushType, List<String> params) {
        if ("messageAdd".equals(pushType)) {
            // params[0] = senderId
            // params[1] = chatMessageId
            if (params.size() >= 2) {
                try {
                    int senderId = Integer.parseInt(params.get(0));
                    int chatMessageId = Integer.parseInt(params.get(1));
                    if (senderId == friendId) {
                        // 是当前聊天对象发来的消息，拉取完整消息内容
                        new Thread(() -> {
                            try {
                                ChatMessage msg = MessageManager.getInstance().findMessageById(chatMessageId);
                                if (msg != null) {
                                    mainHandler.post(() -> {
                                        adapter.addMessage(msg);
                                        scrollToBottom();
                                    });
                                }
                            } catch (Exception ignore) {
                            }
                        }).start();
                    }
                } catch (NumberFormatException ignore) {
                }
            }
        }
    }
}
