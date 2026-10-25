package com.sarms.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Access rules for analytics endpoints, built the same way JwtAuthFilter builds the principal. */
class AnalyticsControllerTest {

    private static Authentication user(String userId, String role) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void studentCanOnlyViewOwnProgress() {
        assertTrue(AnalyticsController.canViewStudent(user("STUD-2024-001", "STUDENT"), "STUD-2024-001"));
        assertFalse(AnalyticsController.canViewStudent(user("STUD-2024-001", "STUDENT"), "STUD-2024-002"));
    }

    @Test
    void staffCanViewAnyStudentsProgress() {
        assertTrue(AnalyticsController.canViewStudent(user("ADM-001", "ADMIN"), "STUD-2024-002"));
        assertTrue(AnalyticsController.canViewStudent(user("FAC-CSE-001", "FACULTY"), "STUD-2024-002"));
    }

    @Test
    void facultyAnalyticsAreScopedToThemselvesAndAdminSeesAll() {
        assertEquals("FAC-CSE-001", AnalyticsController.facultyScope(user("FAC-CSE-001", "FACULTY")));
        assertNull(AnalyticsController.facultyScope(user("ADM-001", "ADMIN")));
    }
}
