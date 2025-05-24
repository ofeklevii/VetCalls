package com.example.vetcalls.obj;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vetcalls.R;

import java.text.SimpleDateFormat;
import java.util.*;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private ArrayList<Message> messageList;
    private String currentUserId;
    private SimpleDateFormat timeFormat;

    public MessageAdapter(Context context, ArrayList<Message> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messageList.get(position);

        // איפוס תצוגות
        holder.textMessage.setVisibility(View.GONE);
        holder.imageMessage.setVisibility(View.GONE);
        holder.videoMessage.setVisibility(View.GONE);

        // הגדרת כיוון לבועת ההודעה לפי השולח
        LinearLayout messageBubble = holder.messageBubble;
        LinearLayout messageContainer = holder.messageContainer;

        // לוגים לדיבוג יישור
        android.util.Log.d("MessageAdapter", "currentUserId=" + currentUserId + ", senderId=" + msg.getSenderId());

        if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
            messageContainer.setGravity(Gravity.END);
            messageBubble.setBackgroundResource(R.drawable.message_background_self);
        } else {
            messageContainer.setGravity(Gravity.START);
            messageBubble.setBackgroundResource(R.drawable.message_background_other);
        }

        // הגדרת תוכן ההודעה לפי סוג
        switch (msg.getType()) {
            case "text":
                holder.textMessage.setText(msg.getContent());
                holder.textMessage.setVisibility(View.VISIBLE);
                break;
            case "image":
                holder.imageMessage.setVisibility(View.VISIBLE);
                holder.videoMessage.setVisibility(View.GONE);
                Glide.with(context)
                        .load(msg.getContent())
                        .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                        .into(holder.imageMessage);
                // קליק להגדלה
                holder.imageMessage.setOnClickListener(v -> showFullScreenMedia(msg.getContent(), "image"));
                break;
            case "video":
                holder.imageMessage.setVisibility(View.GONE);
                holder.videoMessage.setVisibility(View.VISIBLE);
                holder.videoMessage.setVideoURI(Uri.parse(msg.getContent()));
                holder.videoMessage.seekTo(1); // תצוגה מקדימה
                // קליק להגדלה
                holder.videoMessage.setOnClickListener(v -> showFullScreenMedia(msg.getContent(), "video"));
                break;
        }

        // הגדרת שעה
        if (msg.getTimestamp() != null) {
            String timeString = timeFormat.format(msg.getTimestamp());
            holder.messageTime.setText(timeString);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        LinearLayout messageBubble;
        TextView textMessage;
        ImageView imageMessage;
        VideoView videoMessage;
        TextView messageTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            messageBubble = itemView.findViewById(R.id.messageBubble);
            textMessage = itemView.findViewById(R.id.textMessage);
            imageMessage = itemView.findViewById(R.id.imageMessage);
            videoMessage = itemView.findViewById(R.id.videoMessage);
            messageTime = itemView.findViewById(R.id.messageTime);
        }
    }

    private void showFullScreenMedia(String url, String type) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if ("image".equals(type)) {
            ImageView imageView = new ImageView(context);
            imageView.setBackgroundColor(0xFF000000);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(context).load(url).into(imageView);
            dialog.setContentView(imageView);
            imageView.setOnClickListener(v -> dialog.dismiss());
        } else if ("video".equals(type)) {
            FrameLayout layout = new FrameLayout(context);
            VideoView videoView = new VideoView(context);
            videoView.setVideoURI(Uri.parse(url));
            videoView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            videoView.setOnPreparedListener(mp -> videoView.start());
            layout.addView(videoView);
            dialog.setContentView(layout);
            layout.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }
}