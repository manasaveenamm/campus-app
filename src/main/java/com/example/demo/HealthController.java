package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/actuator/health")
    public String healthCheck() {
        return "{\"status\":\"UP\"}";
    }

    @GetMapping("/")
    public String home() {
        return "Campus Release Platform - Active";
    }
}
