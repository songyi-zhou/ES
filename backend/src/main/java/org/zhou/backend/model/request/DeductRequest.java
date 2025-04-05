package org.zhou.backend.model.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class DeductRequest {
    private String studentId;
    private String title;
    private String reviewComment;
    private Long reviewerId;
    private Double score;
    private List<MultipartFile> attachments;
    
    // Getters and Setters
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getReviewComment() {
        return reviewComment;
    }
    
    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
    
    public Long getReviewerId() {
        return reviewerId;
    }
    
    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public List<MultipartFile> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<MultipartFile> attachments) {
        this.attachments = attachments;
    }
} 