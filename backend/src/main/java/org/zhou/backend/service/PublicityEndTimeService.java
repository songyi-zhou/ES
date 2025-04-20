package org.zhou.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.event.MessageEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicityEndTimeService {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    
    // 定义所有需要检查的表名和其对应的中文名称
    private final Map<String, String> tableNameMap = new HashMap<String, String>() {{
        put("comprehensive_result", "综合测评结果表");
        put("moral_monthly_evaluation", "德育月表");
        put("moral_semester_evaluation", "德育学期表");
        put("research_competition_evaluation", "科研竞赛表");
        put("sports_arts_evaluation", "文体活动表");
    }};
    
    /**
     * 每30秒执行一次，检查公示截止时间并更新状态
     */
    @Scheduled(cron = "*/30 * * * * ?")
    @Transactional
    public void checkPublicityEndTime() {
        log.info("定时任务：开始检查公示截止时间...");
        LocalDateTime now = LocalDateTime.now();
        String currentTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        for (String tableName : tableNameMap.keySet()) {
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
        
        for (String tableName : tableNameMap.keySet()) {
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
            
            // 记录更新前可能会被影响的记录
            String getRecordsSql = String.format("""
                SELECT student_id, publicity_end_time
                FROM %s
                WHERE status = 3 
                AND publicity_end_time IS NOT NULL
                AND publicity_end_time <= ?
                """, tableName);
            
            List<Map<String, Object>> affectedRecords = jdbcTemplate.queryForList(getRecordsSql, currentTime);
            
            if (affectedRecords.isEmpty()) {
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
                
                // 获取表的中文名称
                String tableDisplayName = tableNameMap.getOrDefault(tableName, tableName);
                
                // 通知对应的学生
                for (Map<String, Object> record : affectedRecords) {
                    String studentId = record.get("student_id").toString();
                    Object endTimeObj = record.get("publicity_end_time");
                    String endTime = endTimeObj != null ? endTimeObj.toString() : "未设置";
                    
                    // 从users表中获取用户ID
                    String getUserIdSql = "SELECT id FROM users WHERE user_id = ?";
                    List<Long> userIds = jdbcTemplate.queryForList(getUserIdSql, Long.class, studentId);
                    
                    if (userIds.isEmpty()) {
                        log.warn("未找到学号为 {} 的用户", studentId);
                        continue;
                    }
                    
                    Long userId = userIds.get(0);
                    
                    // 发送通知
                    String notificationTitle = String.format("综测表%s公示时间截止", tableDisplayName);
                    String notificationContent = String.format(
                        "%s公示时间为%s，现在已经截止，评测结果已确认。",
                        tableDisplayName,
                        endTime
                    );
                    
                    MessageEvent event = new MessageEvent(
                        this,
                        notificationTitle,
                        notificationContent,
                        "系统通知",
                        userId.toString(),
                        "system"
                    );
                    
                    eventPublisher.publishEvent(event);
                    log.info("已发送公示截止通知给学生ID: {}, 学号: {}", userId, studentId);
                }
            }

        } catch (Exception e) {
            log.error("更新表 {} 状态时发生错误: {}", tableName, e.getMessage());
        }
    }
} 