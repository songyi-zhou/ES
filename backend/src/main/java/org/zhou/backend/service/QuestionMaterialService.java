package org.zhou.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.entity.SchoolClass;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.request.ReportRequest;
import org.zhou.backend.model.request.ReviewRequest;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.repository.EvaluationMaterialRepository;
import org.zhou.backend.repository.GradeGroupLeaderRepository;
import org.zhou.backend.repository.QuestionMaterialRepository;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.event.MessageEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.security.Principal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class QuestionMaterialService {
    
    private static final Logger log = LoggerFactory.getLogger(QuestionMaterialService.class);
    
    @Autowired
    private QuestionMaterialRepository questionMaterialRepository;
    
    @Autowired
    private ClassGroupMemberRepository classGroupMemberRepository;
    
    @Autowired
    private GradeGroupLeaderRepository gradeGroupLeaderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ClassRepository classRepository;
    
    @Autowired
    private EvaluationMaterialRepository materialRepository;
    
    @Autowired
    private EvaluationService evaluationService;  // 注入 EvaluationService
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public Page<EvaluationMaterial> getQuestionMaterials(
            String department,
            String squad,
            List<String> statuses,
            int page,
            int size,
            String keyword) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<EvaluationMaterial> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 添加院系条件
            if (department != null && !department.isEmpty()) {
                predicates.add(cb.equal(root.get("department"), department));
            }
            
            // 添加中队条件
            if (squad != null && !squad.isEmpty()) {
                predicates.add(cb.equal(root.get("squad"), squad));
            }
            
            // 添加状态条件
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            
            // 添加关键词搜索条件
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("title"), likePattern),
                    cb.like(root.get("description"), likePattern)
                ));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return materialRepository.findAll(spec, pageable);
    }
    
    public void reviewMaterial(ReviewRequest request) {
        if (request.getMaterialId() == null) {
            throw new IllegalArgumentException("材料ID不能为空");
        }
        
        log.info("Processing review for material ID: {}", request.getMaterialId());
        
        EvaluationMaterial material = questionMaterialRepository.findById(request.getMaterialId())
            .orElseThrow(() -> new IllegalArgumentException("材料不存在"));
        
        // 验证状态转换的合法性
        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("审核状态不能为空");
        }
        
        // 验证审核通过时的必填字段
        if ("APPROVED".equals(request.getStatus())) {
            if (request.getEvaluationType() == null || request.getEvaluationType().trim().isEmpty()) {
                throw new IllegalArgumentException("审核通过时必须指定加分种类");
            }
            if (request.getScore() == null || request.getScore() <= 0) {
                throw new IllegalArgumentException("审核通过时必须指定有效的加分分数");
            }
        }
        
        try {
            material.setStatus(request.getStatus());
            material.setReviewComment(request.getComment());
            
            // 如果审核通过，更新加分相关字段并调用加分函数
            if ("APPROVED".equals(request.getStatus())) {
                log.info("Approving material with type: {} and score: {}", 
                        request.getEvaluationType(), request.getScore());
                
                // 更新材料表中的加分信息
                material.setEvaluationType(request.getEvaluationType());
                material.setScore(request.getScore());
                
                try {
                    evaluationService.updateTotalBonus(
                        material.getUserId(),
                        request.getEvaluationType(),
                        request.getScore(),
                        material.getId()
                    );
                    log.info("Successfully updated total bonus for user: {}", material.getUserId());
                } catch (IllegalStateException e) {
                    log.error("Failed to update total bonus: {}", e.getMessage());
                    throw new IllegalStateException("加分更新失败: " + e.getMessage());
                } catch (Exception e) {
                    log.error("Unexpected error while updating total bonus", e);
                    throw new RuntimeException("加分操作失败：" + e.getMessage());
                }
            }
            
            // 如果状态为UNCORRECT（材料需要修正），向综测小组成员发送通知
            if ("UNCORRECT".equals(request.getStatus())) {
                log.info("Material needs correction, sending notification to group members");
                
                try {
                    // 1. 从evaluation_materials表获取用户ID
                    Long materialUserId = material.getUserId();
                    
                    // 2. 从users表获取对应的user_id字段
                    String findUserIdSql = "SELECT user_id FROM users WHERE id = ?";
                    String studentId = jdbcTemplate.queryForObject(findUserIdSql, String.class, materialUserId);
                    
                    // 3. 从students表获取class_id
                    String findClassIdSql = "SELECT class_id FROM students WHERE student_id = ?";
                    String classId = jdbcTemplate.queryForObject(findClassIdSql, String.class, studentId);
                    
                    // 4. 从class_group_members表获取该班级的所有综测小组成员ID
                    String findGroupMembersSql = "SELECT user_id FROM class_group_members WHERE class_id = ?";
                    List<Long> groupMemberIds = jdbcTemplate.queryForList(findGroupMembersSql, Long.class, classId);
                    
                    log.info("找到 {} 个综测小组成员需要通知", groupMemberIds.size());
                    
                    // 获取当前用户姓名作为发送者
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    String currentUserName = authentication.getName();
                    
                    // 获取材料标题和问题描述
                    String materialTitle = material.getTitle();
                    String problemReason = request.getComment() != null ? request.getComment() : "需要修正";
                    
                    // 向每个综测小组成员发送通知
                    for (Long memberId : groupMemberIds) {
                        MessageEvent event = new MessageEvent(
                            this,
                            "综测材料仍有问题",
                            String.format("材料 %s 仍有问题，原因：%s", materialTitle, problemReason),
                            currentUserName, // 使用当前用户名作为发送者
                            memberId.toString(), // 发送给该材料对应的综测小组成员
                            "evaluation"
                        );
                        eventPublisher.publishEvent(event);
                        log.info("已发送材料问题通知给综测小组成员ID: {}", memberId);
                    }
                } catch (Exception e) {
                    log.error("发送通知失败: {}", e.getMessage(), e);
                    // 发送通知失败不应该影响审核流程，所以这里只记录日志，不抛出异常
                }
            }
            
            questionMaterialRepository.save(material);
            log.info("Successfully reviewed material: {}", material.getId());
        } catch (Exception e) {
            log.error("Error while reviewing material: {}", e.getMessage());
            throw new RuntimeException("审核处理失败：" + e.getMessage());
        }
    }
    
    @Transactional
    public Map<String, Object> reportToInstructor(ReportRequest request) {
        // 1. 获取材料列表并记录日志
        List<EvaluationMaterial> materials = questionMaterialRepository.findAllById(request.getMaterialIds());
        log.info("Found {} materials to report", materials.size());
        
        // 从第一个材料中获取部门和中队信息
        if (materials.isEmpty()) {
            log.warn("没有找到需要上报的材料");
            return Map.of("success", false, "message", "没有找到需要上报的材料");
        }
        
        EvaluationMaterial firstMaterial = materials.get(0);
        String department = firstMaterial.getDepartment();
        String squad = firstMaterial.getSquad();
        log.info("材料所属部门: {}, 中队: {}", department, squad);
        
        // 查询负责这个部门和中队的导员
        List<User> instructors = userRepository.findInstructorsByDepartmentAndSquad(department, squad);
        log.info("找到 {} 个负责的导员", instructors.size());
        
        // 4. 更新材料状态
        for (EvaluationMaterial material : materials) {
            material.setStatus("REPORTED");
            material.setReviewComment(request.getNote());
            material.setReportedAt(LocalDateTime.now());
            // 设置第一个匹配的导员为审核人
            if (!instructors.isEmpty()) {
                material.setReviewerId(instructors.get(0).getId());
            }
        }
        materials = questionMaterialRepository.saveAll(materials);
        log.info("Updated {} materials", materials.size());
        
        // 5. 返回导员信息
        List<Map<String, Object>> instructorInfos = instructors.stream()
            .map(i -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", i.getId());
                map.put("name", i.getName());
                map.put("department", i.getDepartment());
                return map;
            })
            .collect(Collectors.toList());
        
        // 6. 发送消息通知给导员
        if (!instructors.isEmpty()) {
            User instructor = instructors.get(0);
            
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("authentication: {}", authentication);
            String currentUsername = authentication.getName();
            
            // 获取当前用户的真实姓名
            User currentUser = userRepository.findByUserId(currentUsername)
                .orElse(null);
            String senderName = currentUser != null ? currentUser.getName() : currentUsername;
            
            // 构建提示内容
            String title = "综测材料上报";
            String content = String.format("有%d个新的综测材料上报通知，请尽快审核", materials.size());
            
            MessageEvent event = new MessageEvent(
                this,
                title,
                content,
                senderName, // 使用当前用户的真实姓名作为发送者
                instructor.getId().toString(), // 收件人为导员
                "evaluation"
            );
            eventPublisher.publishEvent(event);
            log.info("已发送消息通知给导员: {}", instructor.getName());
        }
        
        return Map.of(
            "success", true,
            "message", "材料已成功上报",
            "instructors", instructorInfos,
            "updatedMaterials", materials.size()
        );
    }


    /**
     * 获取用户作为中队长的信息
     * @param userId 用户ID
     * @return 包含department和squad的Map
     */
    public Map<String, String> getSquadLeaderInfo(String userId) {
        String sql = """
            SELECT department, squad
            FROM squad_group_leader
            WHERE user_id = ?
            """;
        
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Map<String, String> result = new HashMap<>();
                result.put("department", rs.getString("department"));
                result.put("squad", rs.getString("squad"));
                return result;
            }, userId);
        } catch (Exception e) {
            log.error("获取中队长信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查指定部门和中队是否有未处理的材料
     * @param department 部门
     * @param squad 中队
     * @return 是否存在未处理材料
     */
    public boolean checkUnprocessedMaterialsBySquad(String department, String squad) {
        // 构建查询条件
        String sql = """
            SELECT COUNT(*)
            FROM evaluation_materials
            WHERE department = ?
            AND squad = ?
            AND status IN ('UNCORRECT', 'CORRECTED')
            """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, department, squad);
        return count != null && count > 0;
    }

    /**
     * 更新指定部门和中队的评估状态
     * @param department 部门
     * @param squad 中队
     * @return 更新的记录数
     */
    @Transactional
    public int updateEvaluationStatusBySquad(String department, String squad) {
        // 获取当前时间
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // 需要更新的表格列表
        List<String> tableNames = List.of(
            "moral_monthly_evaluation", 
            "research_competition_evaluation", 
            "sports_arts_evaluation"
        );
        
        int totalAffectedRows = 0;
        
        // 循环更新每个表
        for (String tableName : tableNames) {
            try {
                // 检查表是否存在
                String checkTableSql = """
                    SELECT COUNT(*) 
                    FROM information_schema.tables 
                    WHERE table_schema = DATABASE() 
                    AND table_name = ?
                    """;
                Integer tableExists = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);
                
                if (tableExists == null || tableExists == 0) {
                    log.warn("表 {} 不存在，跳过更新", tableName);
                    continue;
                }
                
                // 更新综测表状态和原始分数
                String updateSql = String.format("""
                    UPDATE %s 
                    SET status = 1,
                        raw_score = base_score + COALESCE(total_bonus, 0) - COALESCE(total_penalty, 0)
                    WHERE department = ? 
                    AND squad = ?
                    AND status = 0 
                    AND review_end_time <= ?
                    """, tableName);
                
                int affectedRows = jdbcTemplate.update(updateSql, department, squad, currentTime);
                totalAffectedRows += affectedRows;
                
                log.info("更新表 {} 状态和原始分数，部门: {}, 中队: {}, 影响记录数: {}", 
                    tableName, department, squad, affectedRows);
            } catch (Exception e) {
                log.error("更新表 {} 状态时发生错误: {}", tableName, e.getMessage());
                // 继续处理下一个表，不中断事务
            }
        }
        
        return totalAffectedRows;
    }
} 