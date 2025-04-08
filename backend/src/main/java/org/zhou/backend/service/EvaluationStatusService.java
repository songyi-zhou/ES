package org.zhou.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationStatusService {

    private final JdbcTemplate jdbcTemplate;
    // 每天凌晨1点
    @Scheduled(cron = "0 0 1 * * ?")

    // 每30秒执行一次
    // @Scheduled(cron = "*/30 * * * * ?")
    public void updateEvaluationStatus() {
        log.info("定时任务：开始检查并更新测评表状态...");
        doUpdateStatus();
    }

    // 手动触发更新状态
    @Transactional
    public void manualUpdateStatus() {
        log.info("手动触发：开始检查并更新测评表状态...");
        doUpdateStatus();
    }

    @Transactional
    public void forceUpdateStatus() {
        log.info("强制触发：开始更新测评表状态和分数...");
        forceDoUpdateStatus();
    }

    // 强制更新状态的逻辑
    private void forceDoUpdateStatus() {
        // 检查表是否存在并更新状态
        forceUpdateTableStatus("moral_monthly_evaluation");
        forceUpdateTableStatus("research_competition_evaluation");
        forceUpdateTableStatus("sports_arts_evaluation");

        log.info("测评表状态强制更新完成");
    }

    // 强制更新表状态和分数
    private void forceUpdateTableStatus(String tableName) {
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
                return;
            }

            // 检查表中是否有数据
            String checkDataSql = "SELECT COUNT(*) FROM " + tableName;
            Integer dataCount = jdbcTemplate.queryForObject(checkDataSql, Integer.class);
            
            if (dataCount == null || dataCount == 0) {
                log.info("表 {} 中没有数据，跳过更新", tableName);
                return;
            }

            // 直接更新状态和原始分数
            String updateSql = String.format("""
                UPDATE %s 
                SET status = 1,
                    raw_score = base_score + COALESCE(total_bonus, 0) - COALESCE(total_penalty, 0)
                WHERE status = 0
                """, tableName);
            
            int affectedRows = jdbcTemplate.update(updateSql);
            log.info("强制更新表 {} 状态和原始分数，影响记录数: {}", tableName, affectedRows);

        } catch (Exception e) {
            log.error("强制更新表 {} 状态时发生错误: {}", tableName, e.getMessage());
        }
    }

    // 统一的更新状态逻辑
    private void doUpdateStatus() {
        LocalDateTime now = LocalDateTime.now();
        String currentTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 检查表是否存在并更新状态
        updateTableStatus("moral_monthly_evaluation", currentTime);
        updateTableStatus("research_competition_evaluation", currentTime);
        updateTableStatus("sports_arts_evaluation", currentTime);

        log.info("测评表状态更新完成");
    }

    // 检查表是否存在并更新状态
    private void updateTableStatus(String tableName, String currentTime) {
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
                return;
            }

            // 检查表中是否有数据
            String checkDataSql = "SELECT COUNT(*) FROM " + tableName;
            Integer dataCount = jdbcTemplate.queryForObject(checkDataSql, Integer.class);
            
            if (dataCount == null || dataCount == 0) {
                log.info("表 {} 中没有数据，跳过更新", tableName);
                return;
            }

            // 检查review_end_time列是否存在
            String checkColumnSql = """
                SELECT COUNT(*) 
                FROM information_schema.columns 
                WHERE table_schema = DATABASE() 
                AND table_name = ? 
                AND column_name = 'review_end_time'
                """;
            Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName);
            
            if (columnExists == null || columnExists == 0) {
                log.warn("表 {} 中不存在review_end_time列，跳过更新", tableName);
                return;
            }

            // 更新状态和原始分数
            String updateSql = String.format("""
                UPDATE %s 
                SET status = 1,
                    raw_score = base_score + COALESCE(total_bonus, 0) - COALESCE(total_penalty, 0)
            WHERE status = 0 
            AND review_end_time <= ?
                """, tableName);
            
            int affectedRows = jdbcTemplate.update(updateSql, currentTime);
            log.info("更新表 {} 状态和原始分数，影响记录数: {}", tableName, affectedRows);

        } catch (Exception e) {
            log.error("更新表 {} 状态时发生错误: {}", tableName, e.getMessage());
        }
    }
}
