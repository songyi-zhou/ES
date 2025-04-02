package org.zhou.backend.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.dto.EvaluationMaterialDTO;
import org.zhou.backend.entity.EvaluationAttachment;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.request.ReviewRequest;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.repository.EvaluationAttachmentRepository;
import org.zhou.backend.repository.EvaluationMaterialRepository;
import org.zhou.backend.repository.EvaluationRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.repository.GroupMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;

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
    private final GroupMemberRepository groupMemberRepository;
    
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
        material.setClassId(user.getClassId());
        
        // 调用 createMaterial 方法设置审核人
        material = createMaterial(material, userId);
        
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

    public EvaluationAttachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("附件不存在"));
    }

    public EvaluationMaterial getMaterialById(Long materialId) {
        return materialRepository.findById(materialId)
            .orElseThrow(() -> new RuntimeException("材料不存在"));
    }

    public void raiseQuestion(Long materialId, String description) {
        EvaluationMaterial material = materialRepository.findById(materialId)
            .orElseThrow(() -> new RuntimeException("材料不存在"));
        material.setStatus("QUESTIONED");
        material.setReviewComment(description);
        materialRepository.save(material);
    }

    public void rejectMaterial(Long materialId, String reason) {
        EvaluationMaterial material = materialRepository.findById(materialId)
            .orElseThrow(() -> new RuntimeException("材料不存在"));
        material.setStatus("REJECTED");
        material.setReviewComment(reason);
        materialRepository.save(material);
    }

    public Page<EvaluationMaterial> getReportedMaterialsForInstructor(
            Long instructorId, String status, int page, int size) {
        
        // 获取导员信息
        User instructor = userRepository.findById(instructorId)
            .orElseThrow(() -> new RuntimeException("导员不存在"));
        
        Pageable pageable = PageRequest.of(page - 1, size);
        
        // 使用 Specification 构建查询条件
        Specification<EvaluationMaterial> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 只查询已上报的材料
            predicates.add(cb.equal(root.get("status"), "REPORTED"));
            
            // 按导员所在院系和中队筛选
            predicates.add(cb.equal(root.get("department"), instructor.getDepartment()));
            predicates.add(cb.equal(root.get("squad"), instructor.getSquad()));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return materialRepository.findAll(spec, pageable);
    }
    
    public void reviewReportedMaterial(ReviewRequest request) {
        EvaluationMaterial material = evaluationRepository.findById(request.getMaterialId())
            .orElseThrow(() -> new RuntimeException("材料不存在"));
            
        // 验证状态转换的合法性
        if (!"REPORTED".equals(material.getStatus())) {
            throw new RuntimeException("只能审核已上报的材料");
        }
        
        material.setStatus(request.getStatus());
        material.setReviewComment(request.getComment());
        material.setReviewedAt(LocalDateTime.now());
        
        evaluationRepository.save(material);
    }

    public void reviewMaterialByGroupMember(ReviewRequest request) {
        EvaluationMaterial material = materialRepository.findById(request.getMaterialId())
            .orElseThrow(() -> new RuntimeException("材料不存在"));
            
        // 验证状态转换的合法性
        if (!"PENDING".equals(material.getStatus())) {
            throw new RuntimeException("只能审核待审核的材料");
        }
        
        material.setStatus(request.getStatus());
        material.setReviewComment(request.getComment());
        material.setReviewedAt(LocalDateTime.now());
        
        materialRepository.save(material);
    }

    public List<EvaluationMaterial> getAllMaterials(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 如果是管理员或导员，可以看到所有材料
        if (currentUser.getRoles().contains("ROLE_ADMIN") || 
            currentUser.getRoles().contains("ROLE_COUNSELOR")) {
            return materialRepository.findAll();
        }
        
        // 如果是综测小组成员，只能看到自己负责班级的材料
        if (currentUser.getRoles().contains("ROLE_GROUP_MEMBER")) {
            // 从 group_member 表中获取负责的班级
            GroupMember groupMember = groupMemberRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("未找到组员信息"));
                
            // 查询该班级的所有材料
            return materialRepository.findByClassId(groupMember.getClassId());
        }
        
        // 如果是普通学生，只能看到自己的材料
        return materialRepository.findByUserId(currentUserId);
    }

    public EvaluationMaterial createMaterial(EvaluationMaterial material, Long currentUserId) {
        // 1. 通过 id 找到学生信息
        User student = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("学生不存在"));
            
        // 2. 通过学生的专业和年级找到对应的导员
        User counselor = findCounselor(student);
        
        // 3. 设置审核人ID
        material.setReviewerId(counselor.getId());
        material.setStatus("PENDING");  // 设置初始状态为待审核
        
        // 添加这些字段
        material.setDepartment(student.getDepartment());
        material.setSquad(student.getSquad());
        
        return materialRepository.save(material);
    }

    private User findCounselor(User student) {
        // 直接从users表中查询对应院系和中队的导员
        return userRepository.findByDepartmentAndSquadAndRoles(
            student.getDepartment(),
            student.getSquad(),
            "ROLE_COUNSELOR"
        ).orElseThrow(() -> new RuntimeException(
            String.format("未找到%s %s的导员", student.getDepartment(), student.getSquad())
        ));
    }
} 