package org.zhou.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.entity.SchoolClass;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.service.ClassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {
    
    private final ClassRepository classRepository;
    private final ClassService classService;
    private static final Logger log = LoggerFactory.getLogger(ClassController.class);
    
    @GetMapping
    @PreAuthorize("hasAnyRole('GROUP_LEADER', 'INSTRUCTOR')")
    public ResponseEntity<?> getAllClasses() {
        try {
            List<Map<String, Object>> classes = classRepository.findAll().stream()
                .map(clazz -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", clazz.getId());
                    map.put("name", clazz.getName());
                    map.put("department", clazz.getDepartment());
                    return map;
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", classes
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取班级列表失败"
            ));
        }
    }

    @GetMapping("/majors")
    @PreAuthorize("hasAnyRole('GROUP_LEADER', 'INSTRUCTOR')")
    public ResponseEntity<?> getMajors() {
        try {
            List<String> majors = classService.getDistinctMajors();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", majors
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/by-major")
    @PreAuthorize("hasAnyRole('GROUP_LEADER', 'INSTRUCTOR')")
    public ResponseEntity<?> getClassesByMajor(@RequestParam String major) {
        try {
            log.info("接收到按专业查询班级请求，专业名称: {}", major);
            
            List<SchoolClass> schoolClasses = classRepository.findByMajor(major);
            log.info("查询结果: 找到 {} 个班级", schoolClasses.size());
            
            List<Map<String, Object>> classes = schoolClasses.stream()
                .map(clazz -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", clazz.getId());
                    map.put("name", clazz.getName());
                    map.put("department", clazz.getDepartment());
                    map.put("major", clazz.getMajor());
                    return map;
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", classes
            ));
        } catch (Exception e) {
            log.error("获取班级列表失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取班级列表失败: " + e.getMessage()
            ));
        }
    }
} 