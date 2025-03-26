package org.zhou.backend.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.User;
import org.zhou.backend.service.UserImportService;
import org.zhou.backend.model.request.UserImportRequest;
import org.zhou.backend.model.response.ImportResult;
import org.zhou.backend.entity.ImportLog;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.servlet.http.HttpServletResponse;  // 替换 javax.servlet
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-import")
@RequiredArgsConstructor
public class UserImportController {
    
    private final UserImportService userImportService;
    private final Logger log = LoggerFactory.getLogger(UserImportController.class);

    @PostMapping("/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importUsersFromExcel(
            @RequestParam("file") MultipartFile file) {
        try {
            ImportResult result = userImportService.importUsersFromExcel(file);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "导入成功",
                "data", result
            ));
        } catch (Exception e) {
            log.error("导入用户失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addUserManually(@RequestBody UserImportRequest request) {
        try {
            User newUser = userImportService.addUser(request);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "添加成功",
                "data", newUser
            ));
        } catch (Exception e) {
            log.error("添加用户失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> downloadTemplate(HttpServletResponse response) {
        try {
            userImportService.generateTemplate(response);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("下载模板失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getImportLogs() {
        try {
            List<ImportLog> logs = userImportService.getImportLogs();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", logs
            ));
        } catch (Exception e) {
            log.error("获取导入日志失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
} 