package org.zhou.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.zhou.backend.entity.EvaluationRule;
import org.zhou.backend.entity.RuleAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.repository.RuleRepository;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RuleService {
    private final RuleRepository ruleRepository;
    private final FileStorageService fileStorageService;
    
    public EvaluationRule uploadRule(MultipartFile file, String name, String description, Long userId) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String filePath = fileStorageService.storeFile(file);
        
        EvaluationRule rule = new EvaluationRule();
        rule.setTitle(name);
        rule.setDescription(description);
        rule.setCreatedBy(userId);
        rule.setStatus("ACTIVE");
        
        RuleAttachment attachment = new RuleAttachment();
        attachment.setRule(rule);
        attachment.setFileName(fileName);
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        
        rule.setAttachments(Collections.singletonList(attachment));
        
        return ruleRepository.save(rule);
    }
    
    public List<EvaluationRule> getAllActiveRules() {
        return ruleRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
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