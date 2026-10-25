package com.sarms.controller;

import com.sarms.service.UsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    /** Role comes from the JWT, never the request body, so a client can't log views as another role. */
    @PostMapping("/events")
    public ResponseEntity<?> record(Authentication auth, @RequestBody Map<String, String> body) {
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase();
        try {
            usageService.record(body.get("route"), auth.getName(), role);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(defaultValue = "30") int days) {
        return usageService.summary(Math.max(1, Math.min(days, 365)));
    }
}
