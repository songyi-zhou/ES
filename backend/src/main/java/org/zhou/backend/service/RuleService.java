package org.zhou.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.dto.RuleAttachmentDTO;
import org.zhou.backend.dto.RuleDTO;
import org.zhou.backend.entity.EvaluationRule;
import org.zhou.backend.entity.RuleAttachment;
import org.zhou.backend.entity.Student;
import org.zhou.backend.event.MessageEvent;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.repository.RuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RuleService {
    private final RuleRepository ruleRepository;
    private final FileStorageService fileStorageService;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    
    public List<EvaluationRule> getRulesByDepartment(String department) {
        return ruleRepository.findByDepartmentCriteria(department);
    }
    
    public EvaluationRule uploadRule(MultipartFile file, String name, String description, 
                                   Long userId, String department, String uploaderDepartment, String squad) {
        // 验证权限
        if (department != null && !department.equals(uploaderDepartment)) {
            throw new AccessDeniedException("无权上传其他学院的规章");
        }
        
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String filePath = fileStorageService.storeFile(file);
        
        LocalDateTime now = LocalDateTime.now();
        
        EvaluationRule rule = EvaluationRule.builder()
            .title(name)
            .description(description)
            .uploadedBy(userId)
            .department(department)
            .uploadedByDepartment(uploaderDepartment)
            .squad(squad)
            .createdAt(now)
            .updatedAt(now)
            .active(true)
            .attachments(new ArrayList<>())
            .build();
        
        RuleAttachment attachment = new RuleAttachment();
        attachment.setFileName(fileName);
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setUploadTime(now);
        
        rule.addAttachment(attachment);
        
        // 保存规则
        EvaluationRule savedRule = ruleRepository.save(rule);
        
        // 获取上传者姓名
        String senderName = jdbcTemplate.queryForObject(
            "SELECT name FROM users WHERE id = ?", String.class, userId);
        
        // 获取该中队下所有学生的ID
        String findStudentsSql = "SELECT id FROM users WHERE department = ? AND squad = ?";
        List<Long> groupMemberIds = jdbcTemplate.queryForList(findStudentsSql, Long.class, department, squad);
        
        log.info("找到对应中队的学生数量: {}, 部门: {}, 中队: {}", groupMemberIds.size(), department, squad);
        
        // 构建通知内容
        String notificationTitle = "规章制度通知";
        String notificationContent = String.format(
            "新的规章制度已上传：%s。%s", 
            name, 
            description != null && !description.isEmpty() ? "说明：" + description : ""
        );
        
        // 向所有学生发送通知
        for (Long memberId : groupMemberIds) {
            // 发送消息通知
            MessageEvent event = new MessageEvent(
                this,
                notificationTitle,
                notificationContent,
                senderName, // 使用当前用户的真实姓名作为发送者
                memberId.toString(), // 收件人为中队下所有学生
                "announcement" // 类型为公告
            );
            eventPublisher.publishEvent(event);
            log.info("已发送规章制度通知给学生ID: {}", memberId);
        }
        
        return savedRule;
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

    public List<RuleDTO> getRulesBySquadId(String squadId) {
        // 获取指定中队的规则和通用规则（squadId为null的规则）
        List<EvaluationRule> rules = ruleRepository.findBySquadOrSquadIsNull(squadId);
        return rules.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public EvaluationRule getRuleByAttachmentId(Long attachmentId) {
        return ruleRepository.findByAttachmentsId(attachmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Rule not found for attachment id: " + attachmentId));
    }

    public List<RuleDTO> getAllRules() {
        List<EvaluationRule> rules = ruleRepository.findAll();
        return rules.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public RuleDTO getRuleById(Long id) {
        EvaluationRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rule not found with id: " + id));
        return convertToDTO(rule);
    }

    public List<RuleDTO> getRulesByDepartmentAndSquad(String department, String squad) {
        // 获取以下规则：
        // 1. 通用规则（department为null且squad为null）
        // 2. 学院规则（匹配department且squad为null）
        // 3. 中队规则（匹配department和squad）
        List<EvaluationRule> rules = ruleRepository.findByDepartmentAndSquad(department, squad);
        return rules.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    private RuleDTO convertToDTO(EvaluationRule rule) {
        List<RuleAttachmentDTO> attachmentDTOs = rule.getAttachments().stream()
            .map(attachment -> RuleAttachmentDTO.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .filePath(attachment.getFilePath())
                .fileSize(attachment.getFileSize())
                .fileType(attachment.getFileType())
                .build())
            .collect(Collectors.toList());
        
        return RuleDTO.builder()
            .id(rule.getId())
            .title(rule.getTitle())
            .description(rule.getDescription())
            .department(rule.getDepartment())
            .squad(rule.getSquad())
            .squadId(rule.getSquad()) // 假设squad字段同时也是squadId
            .createdAt(rule.getCreatedAt())
            .createdBy(rule.getUploadedBy().toString())
            .attachments(attachmentDTOs)
            .build();
    }
} 