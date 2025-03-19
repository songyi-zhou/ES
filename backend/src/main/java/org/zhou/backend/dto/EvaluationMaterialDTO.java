package org.zhou.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationMaterialDTO {
    private String evaluationType;
    private String title;
    private String description;
    private List<MultipartFile> files;
} 