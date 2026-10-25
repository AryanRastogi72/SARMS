package com.sarms.controller;

import com.sarms.service.AnalyticsService;
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

    /** Admins see everything; faculty only the courses they teach. */
    static String facultyScope(Authentication auth) {
        return hasRole(auth, "ROLE_ADMIN") ? null : auth.getName();
    }

    static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
