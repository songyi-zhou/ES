package org.zhou.backend.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.EvaluationRule;
import org.zhou.backend.entity.RuleAttachment;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.repository.RuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RuleService {
    private final RuleRepository ruleRepository;
    private final FileStorageService fileStorageService;
    
    public List<EvaluationRule> getRulesByDepartment(String department) {
        return ruleRepository.findByDepartmentCriteria(department);
    }
    
    public EvaluationRule uploadRule(MultipartFile file, String name, String description, 
                                   Long userId, String department, String uploaderDepartment) {
        // 验证权限
        if (department != null && !department.equals(uploaderDepartment)) {
            throw new AccessDeniedException("无权上传其他学院的规章");
        }
        
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String filePath = fileStorageService.storeFile(file);
        
        EvaluationRule rule = EvaluationRule.builder()
            .title(name)
            .description(description)
            .uploadedBy(userId)
            .department(department)
            .uploadedByDepartment(uploaderDepartment)
            .createdAt(LocalDateTime.now())
            .active(true)
            .attachments(new ArrayList<>())
            .build();
        
        RuleAttachment attachment = new RuleAttachment();
        attachment.setFileName(fileName);
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setUploadTime(LocalDateTime.now());
        
        rule.addAttachment(attachment);
        
        return ruleRepository.save(rule);
    }
    
    public List<EvaluationRule> getAllActiveRules() {
        return ruleRepository.findByActiveTrueOrderByCreatedAtDesc();
    }
    
    public RuleAttachment getRuleAttachment(Long id) {
        return ruleRepository.findById(id)
            .map(rule -> rule.getAttachments().get(0))
            .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
    }
    
    public void deleteRule(Long id) {
        EvaluationRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
            
        // 删除文件
        rule.getAttachments().forEach(attachment -> 
            fileStorageService.deleteFile(attachment.getFilePath()));
            
        ruleRepository.delete(rule);
    }
} 