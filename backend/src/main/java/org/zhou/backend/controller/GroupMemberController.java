package org.zhou.backend.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.entity.ClassGroupMember;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.User;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.model.request.BatchGroupMemberRequest;
import org.zhou.backend.model.request.ClassAssignRequest;
import org.zhou.backend.model.request.GroupMemberRequest;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.ClassGroupMemberService;
import org.zhou.backend.service.GroupMemberService;
import org.zhou.backend.service.UserService;
import org.zhou.backend.event.MessageEvent;
import org.springframework.context.ApplicationEventPublisher;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/group-members")
@RequiredArgsConstructor
public class GroupMemberController {
    
    private static final Logger log = LoggerFactory.getLogger(GroupMemberController.class);
    
    @Autowired
    private GroupMemberService groupMemberService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ClassGroupMemberService classGroupMemberService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @GetMapping
    public ResponseEntity<?> getGroupMembers(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User currentUser = userService.getUserById(userPrincipal.getId());
            
            // 从class_group_members表获取已分配班级的小组成员
            List<Map<String, Object>> members = classGroupMemberService.getAssignedGroupMembers(currentUser.getDepartment());
            
            // 添加日志调试
            log.info("当前用户部门: {}", currentUser.getDepartment());
            log.info("查询到的已分配班级成员数量: {}", members.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", members
            ));
        } catch (Exception e) {
            log.error("获取小组成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GROUP_LEADER', 'ADMIN')")
    public ResponseEntity<?> updateGroupMember(@PathVariable Long id, @RequestBody GroupMemberRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Current user roles: {}", auth.getAuthorities());
        try {
            ClassGroupMember updatedMember = groupMemberService.updateGroupMember(id, request.getMajor(), request.getClassName());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", updatedMember
            ));
        } catch (Exception e) {
            log.error("更新小组成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}/class")
    @PreAuthorize("hasAnyRole('ADMIN', 'GROUP_LEADER')")
    public ResponseEntity<?> updateMemberClass(
            @PathVariable Long id,
            @RequestParam String major,
            @RequestParam String className,
            Authentication authentication) {
        
        log.info("接收到更新组员班级请求 - ID: {}, 专业: {}, 班级: {}", id, major, className);
        
        try {
            // 获取当前用户信息
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            log.info("当前操作用户: {}, 角色: {}", userDetails.getUsername(), 
                    userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(", ")));

            Map<String, Object> updatedMember = groupMemberService.updateGroupMemberClass(id, major, className);
            log.info("更新成功 - 返回数据: {}", updatedMember);
            return ResponseEntity.ok(updatedMember);
            
        } catch (AccessDeniedException e) {
            log.error("权限不足 - 用户: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("error", "您没有权限执行此操作"));
                    
        } catch (ResourceNotFoundException e) {
            log.error("未找到资源 - ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("更新组员班级信息时发生错误 - ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "更新组员信息失败: " + e.getMessage()));
        }
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('COUNSELOR', 'GROUP_LEADER')")
    public ResponseEntity<?> batchAddGroupMembers(
            @RequestBody BatchGroupMemberRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User currentUser = userService.getUserById(userPrincipal.getId());
            
            List<GroupMember> addedMembers = groupMemberService.batchAddGroupMembers(
                request.getStudentIds(),
                currentUser.getDepartment(),
                userPrincipal.getUsername()  // 作为 instructorId 传入
            );
            
            // 为每个被任命的成员发送消息
            for (GroupMember member : addedMembers) {
                MessageEvent event = new MessageEvent(
                    this,
                    "综测小组任命",
                    "综测小组任命通知，你已被任命为综测小组成员",
                    userPrincipal.getName(), // 发送者为当前用户
                    member.getUserId().toString(), // 收件人为被任命的学生
                    "announcement"
                );
                eventPublisher.publishEvent(event);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量添加成员成功",
                "data", Map.of("addedCount", addedMembers.size())
            ));
        } catch (Exception e) {
            log.error("批量添加小组成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public ResponseEntity<?> assignMemberToClass(
            @PathVariable Long id,
            @RequestBody ClassAssignRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Map<String, Object> result = groupMemberService.assignMemberToClass(
                id, 
                request.getMajor(), 
                request.getClassName(),
                userPrincipal.getId()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "分配成功",
                "data", result
            ));
        } catch (Exception e) {
            log.error("分配班级失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
} 