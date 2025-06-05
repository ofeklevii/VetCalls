package com.example.vetcalls.obj;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.vetcalls.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ViewHolder class for message items in the chat RecyclerView.
 * Holds references to all UI components of each message item including
 * containers for layout, content views for different message types, and metadata displays.
 *
 * @author Ofek Levi
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {

    /** Container layout for the entire message item */
    public LinearLayout messageContainer;

    /** Layout for the message bubble background */
    public LinearLayout messageBubble;

    /** TextView for displaying text message content */
    public TextView textMessage;

    /** ImageView for displaying image message content */
    public ImageView imageMessage;

    /** VideoView for displaying video message content */
    public VideoView videoMessage;

    /** TextView for displaying the message timestamp */
    public TextView messageTime;

    /** TextView for displaying date headers between messages */
    public TextView dateHeader;

    /**
     * Constructor that initializes the ViewHolder with the given item view.
     * Finds and stores references to all UI components within the message item layout.
     *
     * @param itemView The root view of the message item layout
     */
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