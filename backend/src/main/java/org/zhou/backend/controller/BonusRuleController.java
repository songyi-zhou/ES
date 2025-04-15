package org.zhou.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.entity.BonusRule;
import org.zhou.backend.entity.User;
import org.zhou.backend.service.BonusRuleService;
import org.zhou.backend.service.UserService;

@RestController
@RequestMapping("/api/bonus-rules")
@PreAuthorize("hasAnyRole('GROUP_LEADER','GROUP_MEMBER')")
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
