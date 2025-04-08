package org.zhou.backend.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.model.dto.EvaluationFormDTO;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    @Override
    public List<EvaluationFormDTO> getEvaluationForms(String formType, String major, String classId) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        switch (formType) {
            case "moral_monthly_evaluation":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score, remark ")
                   .append("FROM moral_monthly_evaluation ")
                   .append("WHERE status = 1 ");
                break;
            case "research_competition_evaluation":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score, remark ")
                   .append("FROM research_competition_evaluation ")
                   .append("WHERE status = 1 ");
                break;
            case "sports_arts_evaluation":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score, remark ")
                   .append("FROM sports_arts_evaluation ")
                   .append("WHERE status = 1 ");
                break;
            case "moral_semester_evaluation":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score, remark ")
                   .append("FROM moral_semester_evaluation ")
                   .append("WHERE status = 1 ");
                break;
            case "comprehensive_result":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score, remark ")
                   .append("FROM comprehensive_result ")
                   .append("WHERE status = 1 ");
                break;
            default:
                return new ArrayList<>();
        }

        // 添加专业筛选条件
        if (major != null && !major.isEmpty()) {
            sql.append("AND major = ? ");
            params.add(major);
        }

        // 添加班级筛选条件
        if (classId != null && !classId.isEmpty()) {
            sql.append("AND class_id = ? ");
            params.add(classId);
        }

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) ->
            EvaluationFormDTO.builder()
                .studentId(rs.getString("student_id"))
                .studentName(rs.getString("name"))
                .baseScore(rs.getDouble("base_score"))
                .totalBonus(rs.getDouble("total_bonus"))
                .totalPenalty(rs.getDouble("total_penalty"))
                .rawScore(rs.getDouble("raw_score"))
                    .remark(rs.getString("remark"))
                .build()
        );
    }

    @Override
    @Transactional
    public void batchApprove(String formType, String major, String classId) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        switch (formType) {
            case "moral_monthly_evaluation":
                sql.append("UPDATE moral_monthly_evaluation SET status = 2 WHERE status = 1");
                break;
            case "research_competition_evaluation":
                sql.append("UPDATE research_competition_evaluation SET status = 2 WHERE status = 1");
                break;
            case "sports_arts_evaluation":
                sql.append("UPDATE sports_arts_evaluation SET status = 2 WHERE status = 1");
                break;
            case "moral_semester_evaluation":
                sql.append("UPDATE moral_semester_evaluation SET status = 2 WHERE status = 1");
                break;
            case "comprehensive_result":
                sql.append("UPDATE comprehensive_result SET status = 2 WHERE status = 1");
                break;
            default:
                throw new IllegalArgumentException("不支持的表格类型: " + formType);
        }

        // 添加专业筛选条件
        if (major != null && !major.isEmpty()) {
            sql.append(" AND major = ?");
            params.add(major);
        }

        // 添加班级筛选条件
        if (classId != null && !classId.isEmpty()) {
            sql.append(" AND class_id = ?");
            params.add(classId);
        }

        int updatedRows = jdbcTemplate.update(sql.toString(), params.toArray());
        log.info("批量审核完成，更新了 {} 条记录", updatedRows);
    }

    @Override
    @Transactional
    public void batchReject(String formType, String major, String classId, List<String> studentIds, String reason) {
        log.info("开始批量退回操作 - 评测表类型: {}, 专业: {}, 班级: {}, 学生IDs: {}", formType, major, classId, studentIds);
        
        for (String studentId : studentIds) {
            // 1. 获取指定学生的material_ids
            String getMaterialIdsSql = "SELECT material_ids FROM " + formType + " WHERE student_id = ? AND status = 1";
            List<Object> params = new ArrayList<>();
            params.add(studentId);
            
            if (major != null && !major.isEmpty()) {
                getMaterialIdsSql += " AND major = ?";
                params.add(major);
            }
            if (classId != null && !classId.isEmpty()) {
                getMaterialIdsSql += " AND class_id = ?";
                params.add(classId);
            }

            log.info("执行查询SQL: {}, 参数: {}", getMaterialIdsSql, params);
            
            // 先检查记录是否存在
            String checkSql = "SELECT COUNT(*) FROM " + formType + " WHERE student_id = ?";
            int totalRecords = jdbcTemplate.queryForObject(checkSql, Integer.class, studentId);
            log.info("该学生在{}表中共有{}条记录", formType, totalRecords);
            
            String statusCheckSql = "SELECT COUNT(*) FROM " + formType + " WHERE student_id = ? AND status = 1";
            int statusRecords = jdbcTemplate.queryForObject(statusCheckSql, Integer.class, studentId);
            log.info("其中状态为1的记录有{}条", statusRecords);

            List<String> materialIdsList = jdbcTemplate.queryForList(getMaterialIdsSql, String.class, params.toArray());
            log.info("查询到 {} 条评测表记录", materialIdsList.size());
            
            // 2. 更新评测表状态为待审核
            String updateFormSql = "UPDATE " + formType + " SET status = 0 WHERE status = 1";
            List<Object> updateParams = new ArrayList<>();
            
            if (major != null && !major.isEmpty()) {
                updateFormSql += " AND major = ?";
                updateParams.add(major);
            }
            if (classId != null && !classId.isEmpty()) {
                updateFormSql += " AND class_id = ?";
                updateParams.add(classId);
            }
            log.info("执行更新SQL: {}, 参数: {}", updateFormSql, updateParams);
            int updatedForms = jdbcTemplate.update(updateFormSql, updateParams.toArray());
            log.info("更新评测表状态完成，更新了 {} 条记录", updatedForms);

            // 3. 更新材料状态为待修改，同时更新分数
            int totalUpdatedMaterials = 0;
            double totalDeductedScore = 0.0;
            Set<String> rejectedMaterialIds = new HashSet<>();
            if (!materialIdsList.isEmpty()) {
                for (String materialIds : materialIdsList) {
                    if (materialIds != null && !materialIds.isEmpty()) {
                        String[] ids = materialIds.split(",");
                        String inClause = String.join(",", ids);
                        
                        // 3.1 获取需要扣除的总分
                        String getScoresSql = """
                            SELECT COALESCE(SUM(score), 0) 
                            FROM evaluation_materials 
                            WHERE id IN (%s) 
                            AND status NOT IN ('DEDUCTED', 'PUNISHED')
                        """.formatted(inClause);
                        double deductScore = jdbcTemplate.queryForObject(getScoresSql, Double.class);
                        totalDeductedScore += deductScore;
                        
                        // 3.2 获取被退回的材料ID
                        String getRejectedIdsSql = """
                            SELECT id 
                            FROM evaluation_materials 
                            WHERE id IN (%s) 
                            AND status NOT IN ('DEDUCTED', 'PUNISHED')
                        """.formatted(inClause);
                        List<String> rejectedIds = jdbcTemplate.queryForList(getRejectedIdsSql, String.class);
                        rejectedMaterialIds.addAll(rejectedIds);
                        
                        // 3.3 更新材料状态和分数
                        String updateMaterialsSql = """
                            UPDATE evaluation_materials 
                            SET status = 'UNCORRECT', score = 0, review_comment = ?
                            WHERE id IN (%s) 
                            AND status NOT IN ('DEDUCTED', 'PUNISHED')
                        """.formatted(inClause);
                        log.info("执行材料更新SQL: {}", updateMaterialsSql);
                        int updatedMaterials = jdbcTemplate.update(updateMaterialsSql, reason);
                        totalUpdatedMaterials += updatedMaterials;
                        log.info("更新材料状态 - 当前批次更新了 {} 条记录", updatedMaterials);
                    }
                }
                
                // 4. 更新综测表中的total_bonus和material_ids
                if (totalDeductedScore > 0 || !rejectedMaterialIds.isEmpty()) {
                    // 4.1 获取当前的material_ids
                    String getCurrentIdsSql = "SELECT material_ids FROM " + formType + " WHERE student_id = ?";
                    String currentMaterialIds = jdbcTemplate.queryForObject(getCurrentIdsSql, String.class, studentId);
                    
                    // 4.2 从material_ids中移除被退回的ID
                    Set<String> remainingIds = new HashSet<>();
                    if (currentMaterialIds != null && !currentMaterialIds.isEmpty()) {
                        remainingIds = Arrays.stream(currentMaterialIds.split(","))
                                .filter(id -> !rejectedMaterialIds.contains(id))
                                .collect(Collectors.toSet());
                    }
                    
                    // 4.3 更新综测表
                    String updateBonusSql = """
                        UPDATE moral_monthly_evaluation 
                        SET total_bonus = total_bonus - ?,
                            material_ids = ?
                        WHERE student_id = ?
                    """;
                    String newMaterialIds = remainingIds.isEmpty() ? null : String.join(",", remainingIds);
                    jdbcTemplate.update(updateBonusSql, totalDeductedScore, newMaterialIds, studentId);
                    log.info("从综测表中扣除分数: {}, 更新后的material_ids: {}", totalDeductedScore, newMaterialIds);
                }
            }
            
            log.info("批量退回操作完成 - 评测表类型: {}, 专业: {}, 班级: {}, 学生ID: {}, 更新评测表: {} 条, 更新材料: {} 条, 扣除总分: {}, 移除材料ID: {}", 
                    formType, major, classId, studentId, updatedForms, totalUpdatedMaterials, totalDeductedScore, rejectedMaterialIds);
        }
    }

    @Override
    public String getTableName(String formType) {
        switch (formType) {
            case "moral_monthly_evaluation":
                return "moral_monthly_evaluation";
            case "research_competition_evaluation":
                return "research_competition_evaluation";
            case "sports_arts_evaluation":
                return "sports_arts_evaluation";
            case "moral_semester_evaluation":
                return "moral_semester_evaluation";
            case "comprehensive_result":
                return "comprehensive_result";
            default:
                throw new IllegalArgumentException("无效的表单类型");
        }
    }
} 