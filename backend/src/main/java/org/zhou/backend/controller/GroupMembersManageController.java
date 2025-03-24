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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.entity.GroupMember;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.request.GroupMemberRequest;
import org.zhou.backend.security.UserPrincipal;
import org.zhou.backend.service.GroupMemberService;
import org.zhou.backend.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/group-members-manage")
@RequiredArgsConstructor
public class GroupMembersManageController {
    
    private final UserService userService;
    private final GroupMemberService groupMemberService;
    private static final Logger log = LoggerFactory.getLogger(GroupMembersManageController.class);

    @GetMapping
    public ResponseEntity<?> getGroupMembers(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User currentUser = userService.getUserById(userPrincipal.getId());
            
            // 获取当前用户所在班级的成员
            String classId = currentUser.getClassId();
            String grade = classId != null ? "20" + classId.substring(2, 4) : null;
            
            List<Map<String, Object>> members = groupMemberService.getSquadGroupMembers(
                currentUser.getDepartment(), 
                grade
            );
            
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

    @PostMapping
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public ResponseEntity<?> addGroupMember(@RequestBody GroupMemberRequest request,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User currentUser = userService.getUserById(userPrincipal.getId());
            
            GroupMember newMember = groupMemberService.addGroupMember(
                request.getMemberId(),
                request.getMajorId(),
                request.getClassName(),
                currentUser.getName(), // 设置当前用户为上级
                currentUser.getDepartment()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", newMember
            ));
        } catch (Exception e) {
            log.error("添加小组成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public ResponseEntity<?> updateGroupMember(@PathVariable Long id,
                                             @RequestBody GroupMemberRequest request) {
        try {
            GroupMember updatedMember = groupMemberService.updateGroupMember(
                id,
                request.getMajor(),
                request.getClassName()
            );
            
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public ResponseEntity<?> deleteGroupMember(@PathVariable Long id) {
        try {
            groupMemberService.deleteGroupMember(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "删除成功"
            ));
        } catch (Exception e) {
            log.error("删除小组成员失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
