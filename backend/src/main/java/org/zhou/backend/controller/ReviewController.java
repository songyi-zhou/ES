package org.zhou.backend.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.dto.BatchFeedbackDTO;
import org.zhou.backend.dto.PublishedFormQueryDTO;
import org.zhou.backend.dto.ReviewQueryDTO;
import org.zhou.backend.event.MessageEvent;
import org.zhou.backend.model.dto.EvaluationFormDTO;
import org.zhou.backend.model.dto.ResponseDTO;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {
    
    private final ReviewService reviewService;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

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

    @PostMapping("/batch-reject")
    public ResponseEntity<ResponseDTO<String>> batchReject(@RequestBody ReviewQueryDTO query) {
        reviewService.batchReject(query.getFormType(), query.getMajor(), query.getClassId(), query.getStudentIds(), query.getReason());
        return ResponseEntity.ok(ResponseDTO.success("退回成功"));
    }

    @GetMapping("/materials")
    public ResponseEntity<ResponseDTO<List<Map<String, Object>>>> getMaterials(
            @RequestParam String formType,
            @RequestParam String studentId) {
        try {
            // 根据表格类型选择对应的表名
            String tableName;
            switch (formType) {
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
                    return ResponseEntity.ok(ResponseDTO.success(List.of()));
            }

            // 从对应表中获取 material_ids
            String findMaterialIdsSql = String.format(
                "SELECT material_ids FROM %s WHERE student_id = ? AND material_ids IS NOT NULL AND material_ids != ''",
                tableName
            );
            List<String> materialIdsList = jdbcTemplate.queryForList(findMaterialIdsSql, String.class, studentId);
            
            if (materialIdsList.isEmpty()) {
                return ResponseEntity.ok(ResponseDTO.success(List.of()));
            }

            // 收集所有的材料ID
            List<String> allMaterialIds = new ArrayList<>();
            for (String materialIdsStr : materialIdsList) {
                if (materialIdsStr != null && !materialIdsStr.isEmpty()) {
                    String[] ids = materialIdsStr.split(",");
                    allMaterialIds.addAll(Arrays.asList(ids));
                }
            }

            if (allMaterialIds.isEmpty()) {
                return ResponseEntity.ok(ResponseDTO.success(List.of()));
            }

            // 获取材料信息和对应的附件
            String sql = """
                SELECT 
                    m.id, 
                    m.user_id, 
                    m.evaluation_type, 
                    m.title, 
                    m.description, 
                    m.score, 
                    m.status, 
                    m.created_at, 
                    m.updated_at, 
                    m.reviewer_id,
                    m.review_comment, 
                    m.class_id, 
                    m.reported_at, 
                    m.reviewed_at,
                    m.department, 
                    m.squad,
                    COALESCE(
                        JSON_ARRAYAGG(
                            JSON_OBJECT(
                                'id', a.id,
                                'material_id', a.material_id,
                                'file_name', a.file_name,
                                'file_path', a.file_path,
                                'file_size', a.file_size,
                                'file_type', a.file_type
                            )
                        ),
                        '[]'
                    ) as attachments
                FROM evaluation_materials m
                LEFT JOIN evaluation_attachments a ON m.id = a.material_id
                WHERE m.id IN (%s)
                GROUP BY m.id
                ORDER BY m.created_at DESC
                """.formatted(String.join(",", allMaterialIds));

            List<Map<String, Object>> materials = jdbcTemplate.queryForList(sql);

            // 处理附件字段的格式
            materials.forEach(material -> {
                Object attachments = material.get("attachments");
                if (attachments != null && !attachments.toString().equals("[]")) {
                    // 如果是字符串形式的JSON数组，直接使用
                    if (attachments instanceof String) {
                        String attachmentsStr = (String) attachments;
                        if (attachmentsStr.startsWith("[") && attachmentsStr.endsWith("]")) {
                            // 已经是JSON数组格式，不需要额外处理
                            return;
                        }
                    }
                    // 如果不是预期的格式，设置为空数组
                    material.put("attachments", new ArrayList<>());
                } else {
                    material.put("attachments", new ArrayList<>());
                }
            });

            return ResponseEntity.ok(ResponseDTO.success(materials));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ResponseDTO.error("获取材料失败: " + e.getMessage()));
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> publishEvaluationForms() {
        try {
            // 获取当前用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = String.valueOf(((UserPrincipal) authentication.getPrincipal()).getId());

            // 获取用户所属的部门和中队信息
            String getUserSql = "SELECT department, squad FROM squad_group_leader WHERE user_id = ?";
            Map<String, Object> userInfo = jdbcTemplate.queryForMap(getUserSql, userId);

            String department = (String) userInfo.get("department");
            String squad = (String) userInfo.get("squad");

            // 检查是否有未完成审核的记录（状态为0或1）
            String[] tableNames = {"moral_monthly_evaluation", "research_competition_evaluation", "sports_arts_evaluation"};

            for (String tableName : tableNames) {
                String checkSql = String.format(
                        "SELECT COUNT(*) FROM %s WHERE department = ? AND squad = ? AND status IN (0, 1)",
                        tableName
                );

                Integer pendingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, department, squad);

                if (pendingCount > 0) {
                    return ResponseEntity.ok(
                            ResponseDTO.error(
                                    String.format("还有%d条%s记录未完成审核，请先完成审核",
                                            pendingCount, getTableDisplayName(tableName))
                            )
                    );
                }
            }

            // 将状态从2更新为3
            int totalUpdated = 0;

            for (String tableName : tableNames) {
                String updateSql = String.format(
                        "UPDATE %s SET status = 3, remark = '' WHERE department = ? AND squad = ? AND status = 2",
                        tableName
                );

                int updated = jdbcTemplate.update(updateSql, department, squad);
                totalUpdated += updated;
            }

            // 获取发布者姓名
            String getNameSql = "SELECT name FROM users WHERE id = ?";
            String senderName = jdbcTemplate.queryForObject(getNameSql, String.class, userId);
            
            // 获取中队下所有学生的ID
            String findStudentsSql = "SELECT id FROM users WHERE department = ? AND squad = ?";
            List<Long> studentIds = jdbcTemplate.queryForList(findStudentsSql, Long.class, department, squad);
            
            log.info("找到对应中队的学生数量: {}, 部门: {}, 中队: {}", studentIds.size(), department, squad);
            
            // 构建通知内容
            String notificationTitle = "综测公示通知";
            String notificationContent = String.format(
                "您的综测表已经进入公示阶段，请查看并确认评测结果。如有异议，请及时反馈。"
            );
            
            // 向所有学生发送通知
            for (Long studentId : studentIds) {
                // 发送消息通知
                MessageEvent event = new MessageEvent(
                    this,
                    notificationTitle,
                    notificationContent,
                    senderName, // 使用当前用户的真实姓名作为发送者
                    studentId.toString(), // 收件人为中队下所有学生
                    "evaluation" // 类型为evaluation
                );
                eventPublisher.publishEvent(event);
                log.info("已发送综测公示通知给学生ID: {}", studentId);
            }

            Map<String, Object> result = Map.of(
                    "success", true,
                    "message", "已成功将综测表格设置为公示状态",
                    "updatedCount", totalUpdated
            );

            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (Exception e) {
            log.error("公示失败", e);
            return ResponseEntity.ok(ResponseDTO.error("公示失败: " + e.getMessage()));
        }
    }

    private String getTableDisplayName(String tableName) {
        switch (tableName) {
            case "moral_monthly_evaluation":
                return "德育测评表";
            case "research_competition_evaluation":
                return "科研竞赛表";
            case "sports_arts_evaluation":
                return "文体活动表";
            default:
                return tableName;
        }
    }

    @PostMapping("/published-forms")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getPublishedForms(
            @RequestBody PublishedFormQueryDTO query) {
        try {
            // 获取当前用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = String.valueOf(((UserPrincipal) authentication.getPrincipal()).getId());

            // 获取用户所属的部门和中队信息
            String getUserSql = "SELECT department, squad FROM users WHERE id = ?";
            Map<String, Object> userInfo = jdbcTemplate.queryForMap(getUserSql, userId);

            String department = (String) userInfo.get("department");
            String squad = (String) userInfo.get("squad");

            // 根据表单类型选择对应的表
            String tableName = reviewService.getTableName(query.getFormType());

            // 获取公示结束时间
            String getPublicityEndTimeSql = "SELECT publicity_end_time FROM " + tableName +
                    " WHERE department = ? AND squad = ? AND status = 3 LIMIT 1";

            String publicityEndTime = null;
            try {
                Map<String, Object> timeInfo = jdbcTemplate.queryForMap(
                        getPublicityEndTimeSql,
                        department,
                        squad
                );
                publicityEndTime = String.valueOf(timeInfo.get("publicity_end_time"));
            } catch (Exception e) {
                // 如果查询失败，表示没有公示结束时间或没有符合条件的记录
                publicityEndTime = null;
            }

            // 构建查询SQL
            String sql = String.format(
                    "SELECT * FROM %s WHERE department = ? AND squad = ? AND status = 3 " +
                            "AND (? IS NULL OR major = ?) " +
                            "AND (? IS NULL OR class_id = ?)",
                    tableName
            );

            // 执行查询
            List<Map<String, Object>> forms = jdbcTemplate.queryForList(
                    sql,
                    department,
                    squad,
                    query.getMajor(),
                    query.getMajor(),
                    query.getClassId(),
                    query.getClassId()
            );

            // 构建响应数据
            Map<String, Object> result = new HashMap<>();
            result.put("forms", forms);
            result.put("publicityEndTime", publicityEndTime);

            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDTO.error("查询失败: " + e.getMessage()));
        }
    }

    @PostMapping("/batch-feedback")
    public ResponseEntity<ResponseDTO<String>> submitBatchFeedback(
            @RequestBody BatchFeedbackDTO feedback) {
        try {
            // 获取表名
            String tableName = reviewService.getTableName(feedback.getFormType());

            // 构建备注信息
            String remark = feedback.getProblemType() + "," + feedback.getDescription();

            // 1. 先更新选中学生的备注
            StringBuilder updateRemarkSql = new StringBuilder();
            updateRemarkSql.append("UPDATE ").append(tableName)
                    .append(" SET remark = ? WHERE student_id IN (");

            for (int i = 0; i < feedback.getStudentIds().size(); i++) {
                if (i > 0) {
                    updateRemarkSql.append(",");
                }
                updateRemarkSql.append("?");
            }
            updateRemarkSql.append(")");

            // 准备更新备注的参数
            List<Object> remarkParams = new ArrayList<>();
            remarkParams.add(remark);
            remarkParams.addAll(feedback.getStudentIds());

            // 执行更新备注
            jdbcTemplate.update(updateRemarkSql.toString(), remarkParams.toArray());

            // 2. 获取第一个学生的班级信息
            String getClassInfoSql = "SELECT major, class_id FROM " + tableName +
                    " WHERE student_id = ?";
            Map<String, Object> classInfo = jdbcTemplate.queryForMap(
                    getClassInfoSql,
                    feedback.getStudentIds().get(0)
            );

            // 3. 更新整个班级的状态
            String updateStatusSql = "UPDATE " + tableName +
                    " SET status = 1 WHERE major = ? AND class_id = ?";

            int updatedCount = jdbcTemplate.update(
                    updateStatusSql,
                    classInfo.get("major"),
                    classInfo.get("class_id")
            );

            return ResponseEntity.ok(ResponseDTO.success(
                    String.format("成功更新备注 %d 条，更新班级状态 %d 条",
                            feedback.getStudentIds().size(), updatedCount)
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDTO.error("提交反馈失败: " + e.getMessage()));
        }
    }

}