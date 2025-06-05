package com.example.vetcalls.obj;

import java.util.Date;

/**
 * Model class representing a chat preview item in the chat list.
 * Contains essential information for displaying chat previews including
 * participant details, last message, and timestamp.
 *
 * @author Ofek Levi
 */
public class ChatPreview {

    /** Unique identifier for the chat */
    public String chatId;

    /** Display name for the chat (participant name) */
    public String displayName;

    /** Profile image URL for the chat participant */
    public String imageUrl;

    /** The last message content in the chat */
    public String lastMessage;

    /** Timestamp of the last message */
    public Date lastMessageTime;

    /**
     * Default constructor required for Firestore serialization.
     */
    public ChatPreview() {
    }

    /**
     * Constructor for creating a basic chat preview without message details.
     * Sets default values for last message and timestamp.
     *
     * @param chatId Unique identifier for the chat
     * @param displayName Display name for the chat participant
     * @param imageUrl Profile image URL for the chat participant
     */
    public ChatPreview(String chatId, String displayName, String imageUrl) {
        this.chatId = chatId;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
        this.lastMessage = "";
        this.lastMessageTime = new Date();
    }

    /**
     * Full constructor for creating a complete chat preview with message details.
     *
     * @param chatId Unique identifier for the chat
     * @param displayName Display name for the chat participant
     * @param imageUrl Profile image URL for the chat participant
     * @param lastMessage The last message content in the chat
     * @param lastMessageTime Timestamp of the last message
     */
    public ChatPreview(String chatId, String displayName, String imageUrl, String lastMessage, Date lastMessageTime) {
        this.chatId = chatId;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }
}