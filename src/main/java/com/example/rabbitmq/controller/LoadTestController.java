package com.example.rabbitmq.controller;

import com.example.rabbitmq.service.LoadTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class LoadTestController {

    private final LoadTestService loadTestService;

    /**
     * 부하 테스트 시작/중지
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> controlLoadTest(@RequestBody Map<String, Object> request) throws Exception {
        Boolean enabled = (Boolean) request.get("enabled");
        Map<String, Object> response = new HashMap<>();
        
        if (enabled != null) {
            loadTestService.setTestEnabled(enabled);
            response.put("status", "success");
            response.put("message", "부하 테스트가 " + (enabled ? "시작" : "중지") + "되었습니다");
            response.put("testEnabled", enabled);
        } else {
            response.put("status", "error");
            response.put("message", "enabled 파라미터가 필요합니다");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 현재 테스트 상태 조회
     */
    @GetMapping("/load/status")
    public ResponseEntity<Map<String, Object>> getLoadTestStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("testEnabled", loadTestService.isTestEnabled());

        return ResponseEntity.ok(status);
    }
}