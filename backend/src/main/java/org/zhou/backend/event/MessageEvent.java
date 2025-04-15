package org.zhou.backend.event;

import org.springframework.context.ApplicationEvent;

public class MessageEvent extends ApplicationEvent {
    private String title;
    private String content;
    private String sender;
    private String receiver;
    private String type;

    public MessageEvent(Object source, String title, String content, String sender, String receiver, String type) {
        super(source);
        this.title = title;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getType() {
        return type;
    }
} 