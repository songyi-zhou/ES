package org.zhou.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zhou.backend.common.ResponseResult;
import org.zhou.backend.entity.Feedback;
import org.zhou.backend.entity.User;
import org.zhou.backend.service.FeedbackService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.zhou.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    
    // 普通用户接口：提交反馈
    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Feedback feedback) {
        try {
            Feedback savedFeedback = feedbackService.save(feedback);
            return ResponseEntity.ok().body(new ResponseResult<Feedback>(200, "反馈提交成功", savedFeedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseResult<Void>(400, "反馈提交失败：" + e.getMessage(), null));
        }
    }
}

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
class AdminFeedbackController {
    
    private final FeedbackService feedbackService;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(AdminFeedbackController.class);
    
    // 管理员接口：获取反馈列表
    @GetMapping
    public ResponseEntity<?> getFeedbackList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Feedback> feedbackPage = feedbackService.findByTypeAndStatus(type, status, PageRequest.of(page, size));
            return ResponseEntity.ok().body(new ResponseResult<>(200, "获取反馈列表成功", 
                Map.of(
                    "content", feedbackPage.getContent(),
                    "totalElements", feedbackPage.getTotalElements(),
                    "totalPages", feedbackPage.getTotalPages()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseResult<>(400, "获取反馈列表失败：" + e.getMessage(), null));
        }
    }
    
    // 管理员接口：获取反馈详情
    @GetMapping("/{id}")
    public ResponseEntity<?> getFeedbackById(@PathVariable Long id) {
        try {
            Feedback feedback = feedbackService.findById(id);
            return ResponseEntity.ok().body(new ResponseResult<Feedback>(200, "获取反馈详情成功", feedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseResult<Void>(400, "获取反馈详情失败：" + e.getMessage(), null));
        }
    }
    
    // 管理员接口：处理反馈
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveFeedback(
            @PathVariable Long id,
            @RequestParam(required = false) String resolution) {
        try {
            // 获取当前管理员信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminName = "系统管理员"; // 默认名称
            
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String userId = userDetails.getUsername();
                
                // 查询用户的真实姓名
                Optional<User> adminUser = userRepository.findByUserId(userId);
                if (adminUser.isPresent()) {
                    adminName = adminUser.get().getName();
                    log.info("当前处理管理员: {}, 姓名: {}", userId, adminName);
                }
            }
            
            // 调用服务处理反馈
            Feedback feedback = feedbackService.resolveFeedback(id, adminName, resolution);
            return ResponseEntity.ok().body(new ResponseResult<Feedback>(200, "反馈处理成功", feedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseResult<Void>(400, "反馈处理失败：" + e.getMessage(), null));
        }
    }
}
