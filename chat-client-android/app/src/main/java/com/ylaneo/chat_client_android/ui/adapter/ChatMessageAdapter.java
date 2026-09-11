package com.ylaneo.chat_client_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.model.ChatMessage;
import com.ylaneo.chat_client_android.session.UserSession;
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

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

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
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}
