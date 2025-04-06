package org.zhou.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.model.dto.EvaluationFormDTO;
import org.zhou.backend.model.dto.ResponseDTO;
import org.zhou.backend.model.dto.ReviewQueryDTO;
import org.zhou.backend.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @PostMapping("/evaluation-forms")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getEvaluationForms(
            @RequestBody ReviewQueryDTO query) {
        
        List<EvaluationFormDTO> forms = reviewService.getEvaluationForms(
            query.getFormType(), 
            query.getMajor(), 
            query.getClassId()
        );
        
        Map<String, Object> response = Map.of(
            "forms", forms,
            "pendingCount", forms.size()
        );
        
        return ResponseEntity.ok(ResponseDTO.success(response));
    }

    @PostMapping("/batch-approve")
    public ResponseEntity<ResponseDTO<String>> batchApprove(@RequestBody ReviewQueryDTO query) {
        reviewService.batchApprove(query.getFormType(), query.getMajor(), query.getClassId());
        return ResponseEntity.ok(ResponseDTO.success("批量审核成功"));
    }
}
