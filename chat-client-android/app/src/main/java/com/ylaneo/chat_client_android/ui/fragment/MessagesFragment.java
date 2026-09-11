package com.ylaneo.chat_client_android.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.manager.ContactManager;
import com.ylaneo.chat_client_android.model.ChatMessage;
import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.network.PushListener;
import com.ylaneo.chat_client_android.session.UserSession;
import com.ylaneo.chat_client_android.ui.ChatActivity;
import com.ylaneo.chat_client_android.ui.adapter.FriendAdapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息列表页面
 *
 * 显示所有好友的对话入口，点击跳转到聊天页面。
 * 收到推送的新消息时自动刷新。
 */
public class MessagesFragment extends Fragment implements PushListener.PushCallback {

    private RecyclerView rvMessages;
    private TextView tvEmpty;
    private ImageButton btnRefresh;

    private FriendAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PushListener.PushCallback selfRef = this;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvMessages = view.findViewById(R.id.rv_messages);
        tvEmpty = view.findViewById(R.id.tv_empty);
        btnRefresh = view.findViewById(R.id.btn_refresh_messages);

        adapter = new FriendAdapter();
        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMessages.setAdapter(adapter);

        adapter.setOnUserActionListener(new FriendAdapter.OnUserActionListener() {
            @Override
            public void onUserClick(User user) {
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra("friendId", user.getUserid());
                intent.putExtra("friendName", user.getDisplayName());
                startActivity(intent);
            }

            @Override
            public void onAddFriendClick(User user) {
            }
        });

        btnRefresh.setOnClickListener(v -> loadMessages());
        loadMessages();

        PushListener.registerCallback(selfRef);
    }

    @Override
    public void onDestroyView() {
        PushListener.unregisterCallback(selfRef);
        super.onDestroyView();
    }

    private void loadMessages() {
        Integer userId = UserSession.getInstance().getCurrentUserId();
        if (userId == null) return;

        tvEmpty.setVisibility(View.GONE);
        rvMessages.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                List<User> friends = ContactManager.getInstance().getFriendList(userId);
                mainHandler.post(() -> {
                    adapter.setUsers(friends);
                    adapter.setShowAddButton(false);
                    if (friends.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("暂无消息");
                        rvMessages.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(requireContext(), "加载消息列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @Override
    public void onPush(String pushType, List<String> params) {
        if ("friendListUpdate".equals(pushType) || "messageAdd".equals(pushType)) {
            loadMessages();
        }
    }
}
