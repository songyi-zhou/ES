package org.zhou.backend.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zhou.backend.service.StudentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    
    private final StudentService studentService;
    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    @GetMapping("/list")
    public ResponseEntity<?> getStudentList(
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String keyword) {
        try {
            List<Map<String, Object>> students = studentService.getStudentList(major, className, keyword);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", students
            ));
        } catch (Exception e) {
            log.error("获取学生列表失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
} 