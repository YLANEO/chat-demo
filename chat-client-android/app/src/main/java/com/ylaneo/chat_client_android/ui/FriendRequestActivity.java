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

import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.manager.ContactManager;
import com.ylaneo.chat_client_android.model.FriendRequestDto;
import com.ylaneo.chat_client_android.session.UserSession;
import com.ylaneo.chat_client_android.ui.adapter.FriendRequestAdapter;

import java.util.List;

/**
 * 好友请求页面
 */
public class FriendRequestActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvRequests;
    private TextView tvEmpty;

    private FriendRequestAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);

        btnBack = findViewById(R.id.btn_back);
        rvRequests = findViewById(R.id.rv_requests);
        tvEmpty = findViewById(R.id.tv_empty);

        adapter = new FriendRequestAdapter();
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);

        adapter.setOnRequestActionListener(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequestDto request) {
                handleRequest(request, 1); // 1=同意
            }

            @Override
            public void onReject(FriendRequestDto request) {
                handleRequest(request, 2); // 2=拒绝
            }
        });

        btnBack.setOnClickListener(v -> finish());

        loadRequests();
    }

    private void loadRequests() {
        Integer userId = UserSession.getInstance().getCurrentUserId();
        if (userId == null) return;

        tvEmpty.setVisibility(View.GONE);
        rvRequests.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                List<FriendRequestDto> requests = ContactManager.getInstance().getFriendRequests(userId);
                mainHandler.post(() -> {
                    adapter.setRequests(requests);
                    if (requests.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("暂无好友请求");
                        rvRequests.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void handleRequest(FriendRequestDto request, int action) {
        Integer userId = UserSession.getInstance().getCurrentUserId();
        if (userId == null) return;

        new Thread(() -> {
            try {
                ContactManager.getInstance().handleRequest(
                        request.getRequestId(),
                        request.getSenderId(),
                        userId,
                        action
                );
                mainHandler.post(() -> {
                    Toast.makeText(this, action == 1 ? "已同意" : "已拒绝", Toast.LENGTH_SHORT).show();
                    loadRequests();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
