package org.zhou.backend.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.zhou.backend.repository.EvaluationRepository;
import org.zhou.backend.repository.GroupMemberRepository;
import org.zhou.backend.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final EvaluationAttachmentRepository attachmentRepository;
    private final ClassGroupMemberRepository classGroupMemberRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final JdbcTemplate jdbcTemplate;
    
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
        List<EvaluationMaterial> materials = evaluationRepository.findByUserId(userId);
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
        return evaluationRepository.findById(materialId)
            .orElseThrow(() -> new RuntimeException("材料不存在"));
    }

    public void raiseQuestion(Long materialId, String description) {
        EvaluationMaterial material = evaluationRepository.findById(materialId)
            .orElseThrow(() -> new RuntimeException("材料不存在"));
        material.setStatus("QUESTIONED");
        material.setReviewComment(description);
        evaluationRepository.save(material);
    }

    public void rejectMaterial(Long materialId, String reason) {
        EvaluationMaterial material = evaluationRepository.findById(materialId)
            .orElseThrow(() -> new RuntimeException("材料不存在"));
        material.setStatus("REJECTED");
        material.setReviewComment(reason);
        evaluationRepository.save(material);
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
        
        return evaluationRepository.findAll(spec, pageable);
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
        
        // 如果审核通过，更新加分相关字段并调用加分函数
        if ("APPROVED".equals(request.getStatus())) {
            // 更新材料表中的加分信息
            material.setEvaluationType(request.getEvaluationType());
            material.setScore(request.getScore());
            
            try {
                updateTotalBonus(
                    material.getUserId(),
                    request.getEvaluationType(),
                    request.getScore(),
                    material.getId()
                );
            } catch (IllegalStateException e) {
                throw new RuntimeException(e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("加分操作失败：" + e.getMessage());
            }
        }
        
        evaluationRepository.save(material);
    }

    @Transactional
    public void reviewMaterialByGroupMember(ReviewRequest request) {
        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        log.info("开始处理综测小组成员的评审请求 - 用户: {}", currentUsername);
        log.info("评审请求详情: materialId={}, status={}, evaluationType={}, score={}", 
                request.getMaterialId(), request.getStatus(), request.getEvaluationType(), request.getScore());
        
        // 获取材料信息
        EvaluationMaterial material = evaluationRepository.findById(request.getMaterialId())
            .orElseThrow(() -> new IllegalArgumentException("材料不存在"));
        log.info("找到待评审材料 - ID: {}, 当前状态: {}, 提交用户: {}", 
                material.getId(), material.getStatus(), material.getUserId());
        
        // 检查材料状态
        if (!"PENDING".equals(material.getStatus())) {
            log.error("材料状态错误 - 当前状态: {}, 期望状态: PENDING", material.getStatus());
            throw new IllegalArgumentException("只能审核待审核状态的材料");
        }
        
        // 更新材料状态
        material.setStatus(request.getStatus());
        material.setReviewComment(request.getComment());
        material.setReviewedAt(LocalDateTime.now());
        log.info("更新材料状态 - 新状态: {}, 评论: {}", request.getStatus(), request.getComment());
        
        // 如果是通过状态，则更新加分信息
        if ("APPROVED".equals(request.getStatus())) {
            material.setEvaluationType(request.getEvaluationType());
            material.setScore(request.getScore());
            log.info("材料审核通过，更新加分信息 - 类型: {}, 分数: {}", 
                    request.getEvaluationType(), request.getScore());
            
            try {
                // 传入 materialId
                updateTotalBonus(material.getUserId(), request.getEvaluationType(), 
                               request.getScore(), material.getId());
                log.info("加分更新成功");
            } catch (Exception e) {
                log.error("加分更新失败", e);
                throw e;
            }
        }
        
        try {
            evaluationRepository.save(material);
            log.info("材料评审完成 - ID: {}, 最终状态: {}", material.getId(), material.getStatus());
        } catch (Exception e) {
            log.error("保存材料失败", e);
            throw new RuntimeException("保存评审结果失败：" + e.getMessage());
        }
    }

    public void updateTotalBonus(Long userId, String evaluationType, Double score, Long materialId) {
        // 获取当前学年和学期
        String academicYear = getCurrentAcademicYear();
        Integer semester = getCurrentSemester();
        log.info("开始更新加分 - 用户ID: {}, 类型: {}, 分数: {}, 材料ID: {}", 
                userId, evaluationType, score, materialId);
        log.info("当前学年: {}, 学期: {}", academicYear, semester);
        
        // 通过 userId 查询 users 表获取 student_id
        String findStudentIdSql = "SELECT user_id FROM users WHERE id = ?";
        String studentId = jdbcTemplate.queryForObject(findStudentIdSql, String.class, userId);
        
        if (studentId == null) {
            log.error("未找到学生信息 - 用户ID: {}", userId);
            throw new IllegalStateException("未找到对应的学生信息");
        }
        log.info("找到学生ID: {}", studentId);
        
        String tableName;
        String updateSql;
        
        switch (evaluationType) {
            case "A":
                tableName = "moral_monthly_evaluation";
                break;
            case "C":
                tableName = "research_competition_evaluation";
                break;
            case "D":
                tableName = "sports_arts_evaluation";
                break;
            default:
                log.error("无效的评估类型: {}", evaluationType);
                throw new IllegalArgumentException("无效的评估类型: " + evaluationType);
        }
        log.info("使用表: {}", tableName);
        
        String monthCondition = "";
        if ("A".equals(evaluationType)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            int lastMonth = cal.get(Calendar.MONTH) + 1;
            monthCondition = " AND `month` = " + lastMonth;
            log.info("A类评估，添加月份条件: {}", monthCondition);
        }
        
        // 先检查记录是否存在
        String checkRecordSql = "SELECT COUNT(*) FROM " + tableName + 
                               " WHERE student_id = ? AND academic_year = ? AND semester = ?" + 
                               monthCondition;
        log.info("执行查询SQL: {}", checkRecordSql);
        log.info("参数: studentId={}, academicYear={}, semester={}", 
                studentId, academicYear, semester);
        
        int recordCount = jdbcTemplate.queryForObject(checkRecordSql, Integer.class, 
                                                    studentId, academicYear, semester);
        log.info("查询结果数量: {}", recordCount);
        
        if (recordCount == 0) {
            // 检查是否是因为月份不对（A类）或其他原因
            if ("A".equals(evaluationType)) {
                String checkMonthSql = "SELECT COUNT(*) FROM " + tableName + 
                                     " WHERE student_id = ? AND academic_year = ? AND semester = ?";
                int monthCount = jdbcTemplate.queryForObject(checkMonthSql, Integer.class, 
                                                           studentId, academicYear, semester);
                log.info("A类评估月份检查 - 结果数量: {}", monthCount);
                
                if (monthCount > 0) {
                    log.error("当前月份不在审核期内");
                    throw new IllegalStateException("当前月份不在审核期内");
                }
            } else if ("C".equals(evaluationType) || "D".equals(evaluationType)) {
                String checkOtherSemesterSql = "SELECT COUNT(*) FROM " + tableName + 
                                             " WHERE student_id = ? AND academic_year = ?";
                int otherSemesterCount = jdbcTemplate.queryForObject(checkOtherSemesterSql, Integer.class, 
                                                                   studentId, academicYear);
                log.info("C/D类评估学期检查 - 结果数量: {}", otherSemesterCount);
                
                if (otherSemesterCount > 0) {
                    log.error("当前学期不在审核期内");
                    throw new IllegalStateException("当前学期不在审核期内");
                }
            }
            
            log.error("当前类型的综测还未开始");
            throw new IllegalStateException("当前类型的综测还未开始，请耐心等待");
        }
        
        // 获取现有的 material_ids
        String getMaterialIdsSql = "SELECT material_ids FROM " + tableName + 
                                  " WHERE student_id = ? AND academic_year = ? AND semester = ?" + 
                                  monthCondition;
        log.info("查询现有材料IDs - SQL: {}", getMaterialIdsSql);
        
        String existingMaterialIds = jdbcTemplate.queryForObject(getMaterialIdsSql, String.class, 
                                                               studentId, academicYear, semester);
        log.info("现有材料IDs: {}", existingMaterialIds);
        
        // 构建新的 material_ids
        String newMaterialIds;
        if (existingMaterialIds == null || existingMaterialIds.isEmpty()) {
            newMaterialIds = String.valueOf(materialId);
        } else {
            newMaterialIds = existingMaterialIds + "," + materialId;
        }
        log.info("新的材料IDs: {}", newMaterialIds);
        
        // 更新 SQL 语句，同时更新 total_bonus 和 material_ids
        updateSql = "UPDATE " + tableName + 
                    " SET total_bonus = total_bonus + ?, material_ids = ? " +
                    "WHERE student_id = ? AND academic_year = ? AND semester = ? " +
                    "AND status = 0" + monthCondition;
        log.info("执行更新SQL: {}", updateSql);
        log.info("更新参数: score={}, newMaterialIds={}, studentId={}, academicYear={}, semester={}", 
                score, newMaterialIds, studentId, academicYear, semester);
        
        int updatedRows = jdbcTemplate.update(updateSql, score, newMaterialIds, 
                                            studentId, academicYear, semester);
        log.info("更新影响行数: {}", updatedRows);
        
        if (updatedRows == 0) {
            // 检查是否是因为状态不是待审核
            String checkSql = "SELECT status FROM " + tableName + 
                             " WHERE student_id = ? AND academic_year = ? AND semester = ?";
            
            // 如果是A类评估，添加月份条件
            if ("A".equals(evaluationType)) {
                checkSql += monthCondition;
            }
            
            log.info("检查状态SQL: {}", checkSql);
            
            Integer status = jdbcTemplate.queryForObject(checkSql, Integer.class, 
                                                       studentId, academicYear, semester);
            log.info("当前状态: {}", status);
            
            if (status != null) {
                String errorMsg;
                if (status == 1) {
                    errorMsg = "表格正在审核中，无法进行加分操作";
                } else if (status == 2) {
                    errorMsg = "表格已审核完成，无法进行加分操作";
                } else if (status == 3) {
                    errorMsg = "表格在公示期，无法进行加分操作";
                } else if (status == -1) {
                    errorMsg = "公示时间结束，无法进行操作";
                } else {
                    errorMsg = "表格状态异常（" + status + "），无法进行加分操作";
                }
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            } else {
                log.error("未找到学生的测评记录");
                throw new IllegalStateException("未找到学生的测评记录");
            }
        }
        
        log.info("加分更新成功完成");
    }

    // 获取当前学年
    private String getCurrentAcademicYear() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        
        if (month >= 9) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    // 获取当前学期
    private Integer getCurrentSemester() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        
        if (month >= 2 && month <= 8) {
            return 2; // 第二学期
        } else {
            return 1; // 第一学期
        }
    }

    public List<EvaluationMaterial> getAllMaterials(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 如果是管理员或导员，可以看到所有材料
        if (currentUser.getRoles().contains("ROLE_ADMIN") || 
            currentUser.getRoles().contains("ROLE_COUNSELOR")) {
            return evaluationRepository.findAll();
        }
        
        // 如果是综测小组成员，只能看到自己负责班级的材料
        if (currentUser.getRoles().contains("ROLE_GROUP_MEMBER")) {
            // 从 group_member 表中获取负责的班级
            GroupMember groupMember = groupMemberRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("未找到组员信息"));
                
            // 查询该班级的所有材料
            return evaluationRepository.findByClassId(groupMember.getClassId());
        }
        
        // 如果是普通学生，只能看到自己的材料
        return evaluationRepository.findByUserId(currentUserId);
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
        
        return evaluationRepository.save(material);
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

    public EvaluationMaterial save(EvaluationMaterial material) {
        return evaluationRepository.save(material);
    }

    @Transactional
    public void correctMaterial(Long materialId, String evaluationType, Double score, String reviewComment) {
        EvaluationMaterial material = evaluationRepository.findById(materialId)
            .orElseThrow(() -> new RuntimeException("材料不存在"));
            
        // 验证加分数额
        if (score < 0.0 || score > 5) {
            throw new RuntimeException("加分数额必须在0.0到5分之间");
        }
        
        // 更新材料状态和信息
        material.setStatus("CORRECTED");
        material.setEvaluationType(evaluationType);
        material.setScore(score);
        material.setReviewComment(reviewComment);
        material.setReviewedAt(LocalDateTime.now());
        
        evaluationRepository.save(material);
        
        log.info("材料已改正 - ID: {}, 类型: {}, 分数: {}", materialId, evaluationType, score);
    }
} 