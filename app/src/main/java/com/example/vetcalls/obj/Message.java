package com.example.vetcalls.obj;

import java.util.Date;

public class Message {
    private String senderId;
    private Date timestamp;
    private String type; // "text", "image", "video"
    private String content;

    public Message() {
        // נדרש עבור Firestore
    }

    public Message(String senderId, Date timestamp, String type, String content) {
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.type = type;
        this.content = content;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}