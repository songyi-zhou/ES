package org.zhou.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDTO {
    private Long id;
    private String title;
    private String description;
    private String department;
    private String squad;
    private String squadId;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<RuleAttachmentDTO> attachments;
} 