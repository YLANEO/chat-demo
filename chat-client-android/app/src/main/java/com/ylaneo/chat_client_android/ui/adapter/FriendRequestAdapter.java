package com.ylaneo.chat_client_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.model.FriendRequestDto;
import com.ylaneo.chat_client_android.util.AvatarLoader;
import com.ylaneo.chat_client_android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 好友申请列表适配器
 */
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private List<FriendRequestDto> requests = new ArrayList<>();
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAccept(FriendRequestDto request);
        void onReject(FriendRequestDto request);
    }

    public void setRequests(List<FriendRequestDto> requests) {
        this.requests = requests != null ? requests : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnRequestActionListener(OnRequestActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequestDto request = requests.get(position);
        holder.tvSenderName.setText(request.getSenderDisplayNameWithAt());
        holder.tvMessage.setText(request.getMessage() != null ? request.getMessage() : "");
        holder.tvTime.setText(DateTimeUtils.format(request.getCreateTime()));

        // 待处理才显示按钮，已处理则隐藏
        boolean isPending = request.getStatus() == 0;
        holder.layoutActions.setVisibility(isPending ? View.VISIBLE : View.GONE);

        // 加载头像
        AvatarLoader.load(holder.ivSenderAvatar, request.getSenderAvatar());

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(request);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(request);
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSenderAvatar;
        TextView tvSenderName;
        TextView tvMessage;
        TextView tvTime;
        LinearLayout layoutActions;
        MaterialButton btnAccept;
        MaterialButton btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSenderAvatar = itemView.findViewById(R.id.iv_sender_avatar);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
