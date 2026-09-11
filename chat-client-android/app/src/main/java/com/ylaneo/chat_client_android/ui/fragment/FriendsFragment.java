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
import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.network.PushListener;
import com.ylaneo.chat_client_android.session.UserSession;
import com.ylaneo.chat_client_android.ui.ChatActivity;
import com.ylaneo.chat_client_android.ui.FriendRequestActivity;
import com.ylaneo.chat_client_android.ui.adapter.FriendAdapter;

import java.util.List;

/**
 * 好友列表页面
 */
public class FriendsFragment extends Fragment implements PushListener.PushCallback {

    private RecyclerView rvFriends;
    private TextView tvEmpty;
    private ImageButton btnFriendRequests;
    private ImageButton btnRefresh;

    private FriendAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PushListener.PushCallback selfRef = this;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvFriends = view.findViewById(R.id.rv_friends);
        tvEmpty = view.findViewById(R.id.tv_empty);
        btnFriendRequests = view.findViewById(R.id.btn_friend_requests);
        btnRefresh = view.findViewById(R.id.btn_refresh_friends);

        adapter = new FriendAdapter();
        rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFriends.setAdapter(adapter);

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

        btnFriendRequests.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FriendRequestActivity.class))
        );
        btnRefresh.setOnClickListener(v -> loadFriends());

        loadFriends();
        PushListener.registerCallback(selfRef);
    }

    @Override
    public void onDestroyView() {
        PushListener.unregisterCallback(selfRef);
        super.onDestroyView();
    }

    private void loadFriends() {
        Integer userId = UserSession.getInstance().getCurrentUserId();
        if (userId == null) return;

        tvEmpty.setVisibility(View.GONE);
        rvFriends.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                List<User> friends = ContactManager.getInstance().getFriendList(userId);
                mainHandler.post(() -> {
                    adapter.setUsers(friends);
                    adapter.setShowAddButton(false);
                    if (friends.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvFriends.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(requireContext(), "加载好友列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @Override
    public void onPush(String pushType, List<String> params) {
        if ("friendListUpdate".equals(pushType) || "statusUpdate".equals(pushType)) {
            loadFriends();
        }
    }
}
