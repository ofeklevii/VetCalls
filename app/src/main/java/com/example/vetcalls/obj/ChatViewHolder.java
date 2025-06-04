package com.example.vetcalls.obj;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.vetcalls.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ChatViewHolder extends RecyclerView.ViewHolder {
    public ImageView image;
    public TextView name;

    public ChatViewHolder(@NonNull View itemView) {
        super(itemView);
        image = itemView.findViewById(R.id.imageProfile);
        name = itemView.findViewById(R.id.textName);
    }
}