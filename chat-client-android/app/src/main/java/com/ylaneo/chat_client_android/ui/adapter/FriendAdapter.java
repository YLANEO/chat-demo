package com.ylaneo.chat_client_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.util.AvatarLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用用户列表适配器
 *
 * 支持两种模式：
 * - 显示好友列表（带在线状态）
 * - 显示搜索结果（带加好友按钮）
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private List<User> users = new ArrayList<>();
    private boolean showAddButton = false;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onUserClick(User user);
        void onAddFriendClick(User user);
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setShowAddButton(boolean show) {
        this.showAddButton = show;
        notifyDataSetChanged();
    }

    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvName.setText(user.getDisplayName());

        if (showAddButton) {
            holder.tvStatus.setVisibility(View.GONE);
            holder.btnAddFriend.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.btnAddFriend.setVisibility(View.GONE);
            Integer status = user.getStatus();
            holder.tvStatus.setText((status != null && status == 1) ? "在线" : "离线");
            holder.tvStatus.setTextColor(0xFF999999);
        }

        // 加载头像
        AvatarLoader.load(holder.ivAvatar, user.getAvatarUrl());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });

        holder.btnAddFriend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddFriendClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;
        TextView tvStatus;
        MaterialButton btnAddFriend;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnAddFriend = itemView.findViewById(R.id.btn_add_friend);
        }
    }
}
