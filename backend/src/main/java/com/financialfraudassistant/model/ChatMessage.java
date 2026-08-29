package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    public enum Sender { USER, ASSISTANT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sender sender;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ChatMessage() { }

    public ChatMessage(ChatConversation conversation, Sender sender, String content) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
    }

    public Integer getId() { return id; }
    public ChatConversation getConversation() { return conversation; }
    public Sender getSender() { return sender; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}