package com.ylaneo.chat_client_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.model.ChatMessage;
import com.ylaneo.chat_client_android.session.UserSession;
import com.ylaneo.chat_client_android.util.AvatarLoader;
import com.ylaneo.chat_client_android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天消息适配器（支持左右气泡）
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    private static final int VIEW_TYPE_LEFT = 0;
    private static final int VIEW_TYPE_RIGHT = 1;

    private List<ChatMessage> messages = new ArrayList<>();

    /** 好友头像URL（左侧气泡用） */
    private String friendAvatarUrl;
    /** 自己头像URL（右侧气泡用） */
    private String selfAvatarUrl;

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setFriendAvatar(String url) {
        this.friendAvatarUrl = url;
        notifyDataSetChanged();
    }

    public void setSelfAvatar(String url) {
        this.selfAvatarUrl = url;
        notifyDataSetChanged();
    }

    //判断消息放在左右哪边
    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        Integer currentUserId = UserSession.getInstance().getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(msg.getSenderId())) {
            return VIEW_TYPE_RIGHT;
        }
        return VIEW_TYPE_LEFT;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == VIEW_TYPE_RIGHT
                ? R.layout.item_chat_message_right
                : R.layout.item_chat_message_left;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvMessage.setText(msg.getContent());
        holder.tvTime.setText(DateTimeUtils.formatShort(msg.getCreateTime()));

        // 左侧（好友）显示好友头像，右侧（自己）显示自己头像
        if (holder.getItemViewType() == VIEW_TYPE_LEFT) {
            AvatarLoader.load(holder.ivAvatar, friendAvatarUrl);
        } else {
            AvatarLoader.load(holder.ivAvatar, selfAvatarUrl);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvMessage;
        TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}
