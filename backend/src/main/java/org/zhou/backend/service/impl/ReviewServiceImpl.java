package org.zhou.backend.service.impl;

import java.util.ArrayList;
import java.util.List;

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
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score ")
                   .append("FROM moral_monthly_evaluation ")
                   .append("WHERE status = 1 ");
                break;
            case "research_competition_evaluation":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score ")
                   .append("FROM research_competition_evaluation ")
                   .append("WHERE status = 1 ");
                break;
            case "sports_arts_evaluation":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score ")
                   .append("FROM sports_arts_evaluation ")
                   .append("WHERE status = 1 ");
                break;
            case "moral_semester_evaluation":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score ")
                   .append("FROM moral_semester_evaluation ")
                   .append("WHERE status = 1 ");
                break;
            case "comprehensive_result":
                sql.append("SELECT student_id, name, base_score, total_bonus, total_penalty, raw_score ")
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
} 