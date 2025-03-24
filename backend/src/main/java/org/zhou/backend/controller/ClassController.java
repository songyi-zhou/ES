package org.zhou.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.entity.SchoolClass;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {
    
    private final ClassRepository classRepository;
    
    @GetMapping
    public ResponseEntity<?> getAllClasses() {
        try {
            List<SchoolClass> classes = classRepository.findAll();
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
} 