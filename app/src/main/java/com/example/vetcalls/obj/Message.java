package com.example.vetcalls.obj;

import java.util.Date;

/**
 * Model class representing a message in the chat system.
 * Supports different message types including text, image, and video content.
 * Provides complete message metadata including sender information and timestamps.
 *
 * @author Ofek Levi
 */
public class Message {

    /** Unique identifier of the message sender */
    private String senderId;

    /** Timestamp when the message was sent */
    private Date timestamp;

    /** Type of message content (text, image, video) */
    private String type;

    /** The actual message content */
    private String content;

    /**
     * Default constructor required for Firestore serialization.
     */
    public Message() {
    }

    /**
     * Constructor for creating a message with all required information.
     *
     * @param senderId Unique identifier of the message sender
     * @param timestamp Timestamp when the message was sent
     * @param type Type of message content (text, image, video)
     * @param content The actual message content
     */
    public Message(String senderId, Date timestamp, String type, String content) {
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.type = type;
        this.content = content;
    }

    /**
     * Gets the unique identifier of the message sender.
     *
     * @return The sender's unique identifier
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Sets the unique identifier of the message sender.
     *
     * @param senderId The sender's unique identifier
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * Gets the timestamp when the message was sent.
     *
     * @return The message timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the message was sent.
     *
     * @param timestamp The message timestamp
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the type of message content.
     *
     * @return The message type (text, image, video)
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of message content.
     *
     * @param type The message type (text, image, video)
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the actual message content.
     *
     * @return The message content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the actual message content.
     *
     * @param content The message content
     */
    public void setContent(String content) {
        this.content = content;
    }
}