package org.zhou.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.zhou.backend.entity.SchoolClass;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.service.ClassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhou.backend.common.Result;
import org.zhou.backend.entity.Class;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {
    
    private final ClassRepository classRepository;
    private final ClassService classService;
    private static final Logger log = LoggerFactory.getLogger(ClassController.class);
    
    @GetMapping
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

    /**
     * 获取所有班级信息（不分页）
     */
    @GetMapping("/all")
    public Result<List<SchoolClass>> getClasses() {
        return Result.success(classService.findAll());
    }

    /**
     * 分页获取班级信息
     */
    @GetMapping("/page")
    public Result<Page<SchoolClass>> getClasses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        return Result.success(classService.findPage(pageRequest));
    }

    /**
     * 添加班级
     */
    @PostMapping
    public Result<SchoolClass> addClass(@RequestBody SchoolClass classes) {
        return Result.success(classService.save(classes));
    }

    /**
     * 更新班级信息
     */
    @PutMapping("/{id}")
    public Result<SchoolClass> updateClass(@PathVariable String id, @RequestBody SchoolClass classes) {
        classes.setId(id);
        return Result.success(classService.update(classes));
    }

    /**
     * 删除班级
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteClass(@PathVariable String id) {
        classService.delete(id);
        return Result.success(null);
    }

    /**
     * 下载班级导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        classService.generateTemplate(response);
    }

    /**
     * 批量导入班级
     */
    @PostMapping("/import")
    public Result<String> importClasses(@RequestParam("file") MultipartFile file) {
        try {
            int count = classService.importClasses(file);
            return Result.success("成功导入 " + count + " 条数据");
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }
} 