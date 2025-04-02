package org.zhou.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.entity.BonusRule;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.repository.BonusRuleRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class BonusRuleService {
    
    @Autowired
    private BonusRuleRepository ruleRepository;
    
    public List<BonusRule> getAllRules() {
        return ruleRepository.findAll();
    }
    
    public BonusRule createRule(BonusRule rule) {
        validateRule(rule);
        return ruleRepository.save(rule);
    }
    
    public BonusRule updateRule(Long id, BonusRule rule) {
        validateRule(rule);
        BonusRule existingRule = ruleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("规则不存在"));
            
        existingRule.setType(rule.getType());
        existingRule.setReason(rule.getReason());
        existingRule.setLevel(rule.getLevel());
        existingRule.setAwardLevel(rule.getAwardLevel());
        existingRule.setActivityType(rule.getActivityType());
        existingRule.setPoints(rule.getPoints());
        existingRule.setDescription(rule.getDescription());
        
        return ruleRepository.save(existingRule);
    }
    
    public void deleteRule(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new ResourceNotFoundException("规则不存在");
        }
        ruleRepository.deleteById(id);
    }
    
    private void validateRule(BonusRule rule) {
        // 验证加分类型
        if (!Arrays.asList("A", "C", "D").contains(rule.getType())) {
            throw new IllegalArgumentException("无效的加分类型");
        }
        
        // 验证C类规则
        if ("C".equals(rule.getType()) && rule.getAwardLevel() == null) {
            throw new IllegalArgumentException("C类规则必须指定获奖等级");
        }
        
        // 验证D类规则
        if ("D".equals(rule.getType()) && rule.getActivityType() == null) {
            throw new IllegalArgumentException("D类规则必须指定活动类型");
        }
        
        // 验证分值范围
        if (rule.getPoints().compareTo(BigDecimal.ZERO) < 0 || 
            rule.getPoints().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("分值必须在0-100之间");
        }
    }

    public List<BonusRule> getRulesByDepartmentAndSquad(String department, String squad) {
        return ruleRepository.findByDepartmentAndSquad(department, squad);
    }
}
