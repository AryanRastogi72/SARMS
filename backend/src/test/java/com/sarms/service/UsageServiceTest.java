package com.sarms.service;

import com.sarms.model.UsageEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for usage analytics logic. No database or running server needed. */
class UsageServiceTest {

    @Test
    void featureForAcceptsOnlyPagesInTheCallersOwnPortal() {
        assertEquals("/student/report", UsageService.featureFor("/student/report", "student"));
        assertEquals("/admin", UsageService.featureFor("/admin", "admin"));
        assertEquals("/faculty/marks", UsageService.featureFor("/faculty/marks/CSE401", "faculty")); // ids trimmed
        assertNull(UsageService.featureFor("/admin/analytics", "student"));   // can't log another portal
        assertNull(UsageService.featureFor("/student/<script>", "student"));
        assertNull(UsageService.featureFor("https://evil.example/student", "student"));
        assertNull(UsageService.featureFor(null, "student"));
    }

    @Test
    void hashIsStableAndDoesNotContainTheUserId() {
        String h = UsageService.hash("STUD-2024-001");
        assertEquals(16, h.length());
        assertEquals(h, UsageService.hash("STUD-2024-001"));
        assertNotEquals(h, UsageService.hash("STUD-2024-002"));
        assertFalse(h.contains("STUD"));
    }

    private static UsageEvent ev(String feature, String role, String user, int daysAgo, boolean demo) {
        return new UsageEvent(null, feature, role, user, Instant.now().minus(daysAgo, ChronoUnit.DAYS), demo);
    }

    @Test
    @SuppressWarnings("unchecked")
    void summarizeCountsEventsDistinctUsersAndRoles() {
        var events = List.of(
                ev("/student/report", "student", "u1", 0, false),
                ev("/student/report", "student", "u1", 1, false),
                ev("/student/report", "student", "u2", 1, false),
                ev("/admin/analytics", "admin", "a1", 2, false));

        Map<String, Object> s = UsageService.summarize(events, 7);

        assertEquals(4, s.get("totalEvents"));
        assertEquals(false, s.get("demoData"));
        var features = (List<UsageService.FeatureUsage>) s.get("features");
        assertEquals("/student/report", features.get(0).feature());   // most used first
        assertEquals(3, features.get(0).events());
        assertEquals(2, features.get(0).users());
        assertEquals(1.5, features.get(0).eventsPerUser());
        assertEquals(3L, features.get(0).eventsByRole().get("student"));
        assertEquals(Map.of("student", 2L, "faculty", 0L, "admin", 1L), s.get("activeUsers"));

        var daily = (List<UsageService.DailyCount>) s.get("daily");
        assertEquals(7, daily.size());                                // zero-filled
        assertEquals(4, daily.stream().mapToLong(UsageService.DailyCount::events).sum());
    }

    @Test
    void summarizeReportsDemoData() {
        var s = UsageService.summarize(List.of(ev("/student/courses", "student", "u1", 0, true)), 1);
        assertEquals(true, s.get("demoData"));
    }
}
