package me.sonam.authzmanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Lightweight unauthenticated endpoint for Kubernetes probes. */
@RestController
public class HealthController {

    @GetMapping("/api/health/readiness")
    public Map<String, String> readiness() {
        return Map.of("status", "UP");
    }
}
