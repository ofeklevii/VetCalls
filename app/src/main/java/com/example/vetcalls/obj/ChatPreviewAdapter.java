package com.example.vetcalls.obj;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vetcalls.R;

import java.util.*;

/**
 * RecyclerView adapter for displaying chat preview items in a list.
 * Handles the binding of ChatPreview data to view holders and manages click events.
 *
 * @author Ofek Levi
 */
public class ChatPreviewAdapter extends RecyclerView.Adapter<ChatViewHolder> {

    /**
     * Interface for handling chat item click events.
     */
    public interface OnChatClickListener {
        /**
         * Called when a chat item is clicked.
         *
         * @param chat The ChatPreview object that was clicked
         */
        void onChatClick(ChatPreview chat);
    }

    private List<ChatPreview> chatList;
    private OnChatClickListener listener;

    /**
     * Constructor for creating the adapter with chat data and click listener.
     *
     * @param chatList List of ChatPreview objects to display
     * @param listener Listener for handling chat item clicks
     */
    public ChatPreviewAdapter(List<ChatPreview> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    /**
     * Creates a new ViewHolder by inflating the chat preview item layout.
     *
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new ChatViewHolder that holds a View for the chat preview item
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_preview, parent, false);
        return new ChatViewHolder(view);
    }

    /**
     * Binds chat data to the ViewHolder at the specified position.
     * Sets up the display name, profile image, and click listener.
     *
     * @param holder The ViewHolder which should be updated
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatPreview chat = chatList.get(position);

        holder.name.setText(chat.displayName);
        Glide.with(holder.itemView.getContext())
                .load(chat.imageUrl)
                .placeholder(R.drawable.user_person_profile_avatar_icon_190943)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of chat items
     */
    @Override
    public int getItemCount() {
        return chatList.size();
    }
}