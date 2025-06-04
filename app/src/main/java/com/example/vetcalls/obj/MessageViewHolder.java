package com.example.vetcalls.obj;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.vetcalls.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MessageViewHolder extends RecyclerView.ViewHolder {
    public LinearLayout messageContainer;
    public LinearLayout messageBubble;
    public TextView textMessage;
    public ImageView imageMessage;
    public VideoView videoMessage;
    public TextView messageTime;
    public TextView dateHeader;

    public MessageViewHolder(@NonNull View itemView) {
        super(itemView);
        messageContainer = itemView.findViewById(R.id.messageContainer);
        messageBubble = itemView.findViewById(R.id.messageBubble);
        textMessage = itemView.findViewById(R.id.textMessage);
        imageMessage = itemView.findViewById(R.id.imageMessage);
        videoMessage = itemView.findViewById(R.id.videoMessage);
        messageTime = itemView.findViewById(R.id.messageTime);
        dateHeader = itemView.findViewById(R.id.dateHeader);
    }
} 