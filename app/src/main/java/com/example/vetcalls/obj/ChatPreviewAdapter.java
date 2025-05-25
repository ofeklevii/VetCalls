package com.example.vetcalls.obj;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vetcalls.R;

import java.util.*;

public class ChatPreviewAdapter extends RecyclerView.Adapter<ChatPreviewAdapter.ChatViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(ChatPreview chat);
    }

    private List<ChatPreview> chatList;
    private OnChatClickListener listener;

    public ChatPreviewAdapter(List<ChatPreview> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_preview, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatPreview chat = chatList.get(position);

        // Set name and image
        holder.name.setText(chat.displayName);
        Glide.with(holder.itemView.getContext())
                .load(chat.imageUrl)
                .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                .into(holder.image);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageProfile);
            name = itemView.findViewById(R.id.textName);
        }
    }
}