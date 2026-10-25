package com.sarms.service;

import com.sarms.model.Course;
import com.sarms.model.Student;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for analytics logic. No database or running server needed. */
class AnalyticsServiceTest {

    private static final List<Course.GradingComponent> COMPS = List.of(
            new Course.GradingComponent("comp1", "Assignments", 20),
            new Course.GradingComponent("comp2", "Midsem", 30),
            new Course.GradingComponent("comp3", "Endsem", 50));

    @Test
    void summarizeCountsGradesPassRateAndAverage() {
        Course c = new Course();
        c.setCode("CSE101");
        var out = AnalyticsService.summarize(c, List.of("A+", "A", "F", "C"));

        assertEquals(4, out.graded());
        assertEquals(0.75, out.passRate());
        assertEquals(6.25, out.avgGradePoints()); // (10 + 9 + 0 + 6) / 4
        assertEquals(1, out.distribution().get("F"));
        assertEquals(0, out.distribution().get("B"));
        assertEquals(AnalyticsService.GRADE_ORDER, List.copyOf(out.distribution().keySet()));
    }

    @Test
    void currentPercentOnlyCountsEnteredComponents() {
        // 10/20 + 15/30 entered, endsem not held yet → 25 of 50 = 50%, not 25%
        assertEquals(50.0, AnalyticsService.currentPercent(Map.of("comp1", 10.0, "comp2", 15.0), COMPS));
        assertNull(AnalyticsService.currentPercent(Map.of(), COMPS));
        assertNull(AnalyticsService.currentPercent(null, COMPS));
    }

    @Test
    void currentPercentHandlesBestNOfMComponents() {
        var quiz = List.of(new Course.GradingComponent("q", "Best 2 of 3", 20));
        // best two of {4, 8, 6} = 14 of 20
        assertEquals(70.0, AnalyticsService.currentPercent(Map.of("q_0", 4.0, "q_1", 8.0, "q_2", 6.0), quiz));
    }

    @Test
    void cgpaIgnoresInProgressAndCountsFailsAsZero() {
        Student s = new Student();
        Student.SemesterRecord rec = new Student.SemesterRecord();
        rec.getCourses().add(new Student.CourseGrade("A1", "a", 4, "A", 9));
        rec.getCourses().add(new Student.CourseGrade("A2", "b", 4, "F", 0));
        rec.getCourses().add(new Student.CourseGrade("A3", "c", 4, "IP", 0));
        s.getAcademicRecord().add(rec);

        assertEquals(4.5, AnalyticsService.cgpa(s));
        assertNull(AnalyticsService.cgpa(new Student()));
    }

    @Test
    void flagReasonsExplainEachTrigger() {
        assertTrue(AnalyticsService.flagReasons(65.0, 7.5, 40, 5.0).isEmpty());
        assertEquals(1, AnalyticsService.flagReasons(35.0, 7.5, 40, 5.0).size());
        assertEquals(1, AnalyticsService.flagReasons(65.0, 4.2, 40, 5.0).size());
        assertEquals(2, AnalyticsService.flagReasons(35.0, 4.2, 40, 5.0).size());
        // no marks or no history yet → nothing to flag on
        assertTrue(AnalyticsService.flagReasons(null, null, 40, 5.0).isEmpty());
    }

    @Test
    void demoDetectionUsesRollNoPrefix() {
        Student real = new Student();
        real.setRollNo("STUD-2024-001");
        Student demo = new Student();
        demo.setRollNo("DEMO-CSE-01");

        assertFalse(AnalyticsService.hasDemo(List.of(real)));
        assertTrue(AnalyticsService.hasDemo(List.of(real, demo)));
    }
}
