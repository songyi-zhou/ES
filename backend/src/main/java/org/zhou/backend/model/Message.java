package org.zhou.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String receiver;

    @Column(nullable = false)
    private String type; // system, evaluation, announcement

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "read_time")
    private LocalDateTime readTime;

//    @Column(name = "receiver_id")
//    private Long receiverId;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

//    public Long getReceiverId() {
//        return receiverId;
//    }
} 