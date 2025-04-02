package org.zhou.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleAttachmentDTO {
    private Long id;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
} 