package com.unihub.backend.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/stress")
    public ResponseEntity<Map<String, String>> stressTest(
            @RequestHeader(value = "X-User-Id", defaultValue = "unknown") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "unknown") String userRole) {
        
        // This simulates a fast, light endpoint that proves Nginx headers are passed to Spring Boot
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Stress test endpoint responding",
                "user_id", userId,
                "role", userRole
        ));
    }
}
