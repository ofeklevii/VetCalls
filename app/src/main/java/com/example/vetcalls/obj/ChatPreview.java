package com.example.vetcalls.obj;

import java.util.Date;

public class ChatPreview {
    public String chatId;
    public String displayName;
    public String imageUrl;
    public String lastMessage;
    public Date lastMessageTime;

    public ChatPreview() {
        // Default constructor for Firestore
    }

    public ChatPreview(String chatId, String displayName, String imageUrl) {
        this.chatId = chatId;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
        this.lastMessage = "";
        this.lastMessageTime = new Date();
    }

    public ChatPreview(String chatId, String displayName, String imageUrl, String lastMessage, Date lastMessageTime) {
        this.chatId = chatId;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }
} 