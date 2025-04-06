package org.zhou.backend.controller;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.model.dto.ResponseDTO;
import org.zhou.backend.service.EvaluationStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling  // 添加这行启用定时任务
@RestController
@RequestMapping("/api/backend")
@RequiredArgsConstructor
@Slf4j
public class BackendApplicationController {
    private final EvaluationStatusService evaluationStatusService;

    public static void main(String[] args) {
        SpringApplication.run(BackendApplicationController.class, args);
    }

    @PostMapping("/update-evaluation-status")
    public ResponseEntity<?> manualUpdateStatus() {
        try {
            evaluationStatusService.manualUpdateStatus();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "测评表状态更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "测评表状态更新失败：" + e.getMessage()
            ));
        }
    }

    @PostMapping("/force-update")
    public ResponseEntity<ResponseDTO<String>> forceUpdateStatus() {
        log.info("接收到强制更新状态请求");
        evaluationStatusService.forceUpdateStatus();
        return ResponseEntity.ok(ResponseDTO.success("强制更新状态成功"));
    }
}
