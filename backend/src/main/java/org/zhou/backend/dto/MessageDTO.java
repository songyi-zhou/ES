package org.zhou.backend.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MessageDTO {
    private Long id;
    private String title;
    private String content;
    private String sender;
    private String receiver;
    private String type;
    private Boolean read;
    private LocalDateTime createTime;
    private LocalDateTime readTime;
} 