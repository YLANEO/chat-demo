package com.ylaneo.chat_client_android.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.manager.ContactManager;
import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.session.UserSession;
import com.ylaneo.chat_client_android.ui.adapter.FriendAdapter;

import java.util.List;

/**
 * 搜索用户页面
 */
public class SearchFragment extends Fragment {

    private TextInputEditText etKeyword;
    private MaterialButton btnSearch;
    private RecyclerView rvResults;
    private TextView tvEmpty;

    private FriendAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etKeyword = view.findViewById(R.id.et_search_keyword);
        btnSearch = view.findViewById(R.id.btn_search);
        rvResults = view.findViewById(R.id.rv_search_results);
        tvEmpty = view.findViewById(R.id.tv_empty);

        adapter = new FriendAdapter();
        adapter.setShowAddButton(true);
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(adapter);

        adapter.setOnUserActionListener(new FriendAdapter.OnUserActionListener() {
            @Override
            public void onUserClick(User user) {
                // 不跳转，只显示加好友按钮
            }

            @Override
            public void onAddFriendClick(User user) {
                handleAddFriend(user);
            }
        });

        btnSearch.setOnClickListener(v -> handleSearch());
        etKeyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handleSearch();
                return true;
            }
            return false;
        });
    }

    private void handleSearch() {
        String keyword = etKeyword.getText() != null ? etKeyword.getText().toString().trim() : "";
        if (keyword.isEmpty()) {
            Toast.makeText(requireContext(), "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer currentUserId = UserSession.getInstance().getCurrentUserId();
        if (currentUserId == null) return;

        tvEmpty.setVisibility(View.GONE);
        rvResults.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                List<User> results = ContactManager.getInstance().searchUserByKeyword(keyword, currentUserId);
                mainHandler.post(() -> {
                    adapter.setUsers(results);
                    if (results.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("未找到用户");
                        rvResults.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(requireContext(), "搜索失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void handleAddFriend(User user) {
        Integer currentUserId = UserSession.getInstance().getCurrentUserId();
        if (currentUserId == null) return;

        new Thread(() -> {
            try {
                ContactManager.getInstance().sendFriendRequest(currentUserId, user.getUserid(), "");
                mainHandler.post(() ->
                        Toast.makeText(requireContext(), "已发送好友申请", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
