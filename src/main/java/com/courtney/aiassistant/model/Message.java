package com.courtney.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String role; // "user" | "assistant" | "system"
    private String content;
    private LocalDateTime timestamp;

    public Message() {}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
