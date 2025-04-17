package org.zhou.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zhou.backend.common.ResponseResult;
import org.zhou.backend.entity.Feedback;
import org.zhou.backend.service.FeedbackService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

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
    
    // 管理员接口：获取反馈列表
    @GetMapping
    public ResponseEntity<?> getFeedbackList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Feedback> feedbackList = feedbackService.findByTypeAndStatus(type, status, page, size);
            return ResponseEntity.ok().body(new ResponseResult<List<Feedback>>(200, "获取反馈列表成功", feedbackList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseResult<Void>(400, "获取反馈列表失败：" + e.getMessage(), null));
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
            @RequestParam String resolvedBy,
            @RequestParam String resolution) {
        try {
            Feedback feedback = feedbackService.resolveFeedback(id, resolvedBy, resolution);
            return ResponseEntity.ok().body(new ResponseResult<Feedback>(200, "反馈处理成功", feedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseResult<Void>(400, "反馈处理失败：" + e.getMessage(), null));
        }
    }
}
