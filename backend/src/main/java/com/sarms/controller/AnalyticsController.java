package com.sarms.controller;

import com.sarms.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/courses")
    public Map<String, Object> courseOutcomes(Authentication auth,
                                              @RequestParam(required = false) String department,
                                              @RequestParam(required = false) Integer semester) {
        return analyticsService.courseOutcomes(facultyScope(auth), department, semester);
    }

    @GetMapping("/attention")
    public Map<String, Object> attentionFlags(Authentication auth) {
        return analyticsService.attentionFlags(facultyScope(auth));
    }

    /** Students may only read their own progress; admins and faculty may read any student's. */
    @GetMapping("/progress/{rollNo}")
    public ResponseEntity<?> progress(Authentication auth, @PathVariable String rollNo) {
        if (!canViewStudent(auth, rollNo)) {
            return ResponseEntity.status(403).body(Map.of("error", "You can only view your own progress"));
        }
        try {
            return ResponseEntity.ok(analyticsService.progress(rollNo));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    static boolean canViewStudent(Authentication auth, String rollNo) {
        return hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ROLE_FACULTY") || auth.getName().equals(rollNo);
    }

    /** Admins see everything; faculty only the courses they teach. */
    static String facultyScope(Authentication auth) {
        return hasRole(auth, "ROLE_ADMIN") ? null : auth.getName();
    }

    static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
