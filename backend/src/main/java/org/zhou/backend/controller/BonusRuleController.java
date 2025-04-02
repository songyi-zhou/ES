package org.zhou.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zhou.backend.entity.BonusRule;
import org.zhou.backend.service.BonusRuleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.zhou.backend.entity.User;
import org.zhou.backend.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/bonus-rules")
@PreAuthorize("hasRole('GROUP_LEADER')")
public class BonusRuleController {
    
    @Autowired
    private BonusRuleService bonusRuleService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public List<BonusRule> getAllRules(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        return bonusRuleService.getRulesByDepartmentAndSquad(user.getDepartment(), user.getSquad());
    }
    
    @PostMapping
    public ResponseEntity<?> createRule(@RequestBody BonusRule rule, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            
            rule.setDepartment(user.getDepartment());
            rule.setSquad(user.getSquad());
            
            BonusRule savedRule = bonusRuleService.createRule(rule);
            return ResponseEntity.ok(savedRule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRule(@PathVariable Long id, @RequestBody BonusRule rule) {
        try {
            BonusRule updatedRule = bonusRuleService.updateRule(id, rule);
            return ResponseEntity.ok(updatedRule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRule(@PathVariable Long id) {
        try {
            bonusRuleService.deleteRule(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
