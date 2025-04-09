package org.zhou.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicityEndTimeService {

    private final JdbcTemplate jdbcTemplate;
    
    // 定义所有需要检查的表名
    private final List<String> tableNames = Arrays.asList(
        "comprehensive_result", 
        "moral_monthly_evaluation", 
        "moral_semester_evaluation", 
        "research_competition_evaluation", 
        "sports_arts_evaluation"
    );
    
    /**
     * 每30秒执行一次，检查公示截止时间并更新状态
     */
    @Scheduled(cron = "*/30 * * * * ?")
    @Transactional
    public void checkPublicityEndTime() {
        log.info("定时任务：开始检查公示截止时间...");
        LocalDateTime now = LocalDateTime.now();
        String currentTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        for (String tableName : tableNames) {
            updateTableStatus(tableName, currentTime);
        }
        
        log.info("公示截止时间检查完成");
    }
    
    /**
     * 手动触发更新状态
     */
    @Transactional
    public void manualCheckPublicityEndTime() {
        log.info("手动触发：开始检查公示截止时间...");
        LocalDateTime now = LocalDateTime.now();
        String currentTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        for (String tableName : tableNames) {
            updateTableStatus(tableName, currentTime);
        }
        
        log.info("手动检查公示截止时间完成");
    }
    
    /**
     * 检查表是否存在并更新状态
     */
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

            // 检查publicity_end_time列是否存在
            String checkColumnSql = """
                SELECT COUNT(*) 
                FROM information_schema.columns 
                WHERE table_schema = DATABASE() 
                AND table_name = ? 
                AND column_name = 'publicity_end_time'
                """;
            Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName);
            
            if (columnExists == null || columnExists == 0) {
                log.warn("表 {} 中不存在publicity_end_time列，跳过更新", tableName);
                return;
            }
            
            // 更新status为-1，当前时间超过公示截止时间且状态为3
            String updateSql = String.format("""
                UPDATE %s 
                SET status = -1
                WHERE status = 3 
                AND publicity_end_time IS NOT NULL
                AND publicity_end_time <= ?
                """, tableName);
            
            int affectedRows = jdbcTemplate.update(updateSql, currentTime);
            
            if (affectedRows > 0) {
                log.info("表 {} 中有 {} 条记录的公示期已结束，状态已更新为-1", tableName, affectedRows);
            }

        } catch (Exception e) {
            log.error("更新表 {} 状态时发生错误: {}", tableName, e.getMessage());
        }
    }
} 