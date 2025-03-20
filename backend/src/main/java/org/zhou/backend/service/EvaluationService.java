package org.zhou.backend.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.dto.EvaluationMaterialDTO;
import org.zhou.backend.entity.EvaluationAttachment;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.repository.EvaluationAttachmentRepository;
import org.zhou.backend.repository.EvaluationMaterialRepository;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.repository.EvaluationRepository;
import org.zhou.backend.entity.User;
import org.zhou.backend.repository.ClassRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {
    private final EvaluationMaterialRepository materialRepository;
    private final EvaluationAttachmentRepository attachmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final ClassGroupMemberRepository classGroupMemberRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    
    @Value("${file.upload.path:${user.home}/evaluation-files}")
    private String uploadPath;
    
    @Transactional
    public EvaluationMaterial submitMaterial(Long userId, EvaluationMaterialDTO dto) {
        // 获取用户信息以设置班级ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
        // 验证班级是否存在
        if (!classRepository.existsById(user.getClassId())) {
            throw new RuntimeException("班级不存在: " + user.getClassId());
        }

        // 创建材料记录
        EvaluationMaterial material = new EvaluationMaterial();
        material.setUserId(userId);
        material.setEvaluationType(dto.getEvaluationType());
        material.setTitle(dto.getTitle());
        material.setDescription(dto.getDescription());
        material.setStatus("PENDING");
        material.setClassId(user.getClassId());
        
        // 保存材料记录
        material = materialRepository.save(material);
        
        // 处理附件
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            List<EvaluationAttachment> attachments = new ArrayList<>();
            
            for (MultipartFile file : dto.getFiles()) {
                try {
                    // 生成文件存储路径
                    String fileName = file.getOriginalFilename();
                    String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
                    String filePath = String.format("%s/%s_%s", 
                        uploadPath, UUID.randomUUID().toString(), fileName);
                    
                    // 保存文件
                    File dest = new File(filePath);
                    if (!dest.getParentFile().exists()) {
                        dest.getParentFile().mkdirs();
                    }
                    file.transferTo(dest);
                    
                    // 创建附件记录
                    EvaluationAttachment attachment = new EvaluationAttachment();
                    attachment.setMaterial(material);
                    attachment.setFileName(fileName);
                    attachment.setFilePath(filePath);
                    attachment.setFileSize(file.getSize());
                    attachment.setFileType(fileType);
                    
                    attachments.add(attachment);
                } catch (IOException e) {
                    log.error("File upload failed", e);
                    throw new RuntimeException("文件上传失败");
                }
            }
            
            // 保存附件记录
            attachmentRepository.saveAll(attachments);
            material.setAttachments(attachments);
        }
        
        return material;
    }

    public List<EvaluationMaterial> getMaterialsByUserId(Long userId) {
        log.info("Fetching materials for user: {}", userId);
        List<EvaluationMaterial> materials = materialRepository.findByUserId(userId);
        log.info("Found {} materials", materials.size());
        return materials;
    }

    public List<EvaluationMaterial> getMaterialsByReviewer(Long reviewerId) {
        // 获取该综测小组成员负责的所有班级ID
        List<String> classIds = classGroupMemberRepository.findClassIdsByUserId(reviewerId);
        
        // 获取这些班级的所有学生提交的材料
        return evaluationRepository.findBySubmitterClassIdIn(classIds);
    }
} 