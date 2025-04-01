package org.zhou.backend.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.dto.BatchClassMemberRequest;
import org.zhou.backend.entity.User;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.ClassGroupMemberService;
import org.zhou.backend.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/class-group-members")
@RequiredArgsConstructor
public class ClassGroupMemberController {
    
    private final ClassGroupMemberService classGroupMemberService;
    private final UserService userService;
    private final ClassGroupMemberRepository classGroupMemberRepository;
    private static final Logger log = LoggerFactory.getLogger(ClassGroupMemberController.class);
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public ResponseEntity<?> getClassGroupMembers(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("获取综测负责人(ID:{})负责的中队成员列表", userPrincipal.getId());
        
        try {
            // 查询当前用户负责的班级成员
            List<Map<String, Object>> members = classGroupMemberService.getGroupMembersByLeaderId(userPrincipal.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", members
            ));
        } catch (Exception e) {
            log.error("获取班级成员列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "获取班级成员列表失败: " + e.getMessage()
                    ));
        }
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('GROUP_LEADER', 'INSTRUCTOR')")
    public ResponseEntity<?> getAvailableMembers(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User currentUser = userService.getUserById(userPrincipal.getId());
            List<Map<String, Object>> availableMembers = classGroupMemberService.getAvailableMembers(
                currentUser.getDepartment(),
                currentUser.getClassId()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", availableMembers
            ));
        } catch (Exception e) {
            log.error("获取可用成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public ResponseEntity<?> addClassMembers(
            @RequestBody BatchClassMemberRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User currentUser = userService.getUserById(userPrincipal.getId());
            classGroupMemberService.batchAddClassMembers(
                request.getMemberIds(),
                currentUser.getClassId()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "添加班级成员成功"
            ));
        } catch (Exception e) {
            log.error("添加班级成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public ResponseEntity<?> removeClassMember(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User currentUser = userService.getUserById(userPrincipal.getId());
            classGroupMemberService.removeClassMember(id, currentUser.getClassId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "移除班级成员成功"
            ));
        } catch (Exception e) {
            log.error("移除班级成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
} 