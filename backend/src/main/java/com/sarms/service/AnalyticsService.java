package com.sarms.service;

import com.sarms.model.Course;
import com.sarms.model.Marks;
import com.sarms.model.Student;
import com.sarms.repository.AppConfigRepository;
import com.sarms.repository.CourseRepository;
import com.sarms.repository.MarksRepository;
import com.sarms.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Academic analytics for admins and faculty (solo addition after the fork).
 * Course outcomes come from final grades in students' academic records;
 * "needs attention" flags come from in-progress marks of active-semester courses.
 */
@Service
public class AnalyticsService {

    /** Roll-number prefix used by DemoDataSeeder; any match marks a response as demo data. */
    public static final String DEMO_PREFIX = "DEMO-";
    static final List<String> GRADE_ORDER = List.of("A+", "A", "B+", "B", "C", "D", "F");

    public record CourseOutcome(String courseCode, String courseName, String departmentCode, int semester,
                                int graded, Map<String, Integer> distribution, double passRate,
                                double avgGradePoints) {}

    public record AttentionFlag(String rollNo, String studentName, String courseCode, String courseName,
                                Double currentPercent, Double cgpa, List<String> reasons) {}

    public record SemesterPoint(int semester, double sgpa, double cgpa, double creditsEarned) {}

    public record Progress(String rollNo, String name, Double cgpa, Double cgpaChange, List<SemesterPoint> semesters,
                           double creditsRequired, double creditsEarned, double creditsInProgress,
                           double creditsFailed, double creditsRemaining, int semestersLeft,
                           Double creditsPerSemesterNeeded, int maxCreditsPerSemester, boolean onTrack,
                           boolean demoData) {}

    static final int TOTAL_SEMESTERS = 8;

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final MarksRepository marksRepository;
    private final AppConfigRepository appConfigRepository;
    private final double passPercent;
    private final double cgpaCutoff;
    private final double graduationCredits;

    public AnalyticsService(StudentRepository studentRepository,
                            CourseRepository courseRepository,
                            MarksRepository marksRepository,
                            AppConfigRepository appConfigRepository,
                            @Value("${sarms.analytics.pass-percent:40}") double passPercent,
                            @Value("${sarms.analytics.cgpa-cutoff:5.0}") double cgpaCutoff,
                            @Value("${sarms.graduation-credits:160}") double graduationCredits) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.marksRepository = marksRepository;
        this.appConfigRepository = appConfigRepository;
        this.passPercent = passPercent;
        this.cgpaCutoff = cgpaCutoff;
        this.graduationCredits = graduationCredits;
    }

    public Progress progress(String rollNo) {
        Student s = studentRepository.findByRollNo(rollNo)
                .orElseThrow(() -> new RuntimeException("Student not found: " + rollNo));
        int maxCredits = appConfigRepository.findByKey("maxCredits")
                .map(c -> c.getValue() instanceof Number n ? n.intValue() : 25).orElse(25);
        return computeProgress(s, graduationCredits, maxCredits);
    }

    /** @param facultyId null for admin (all courses), otherwise only that faculty's courses */
    public Map<String, Object> courseOutcomes(String facultyId, String departmentCode, Integer semester) {
        List<Student> students = studentRepository.findAll();
        Map<String, List<String>> gradesByCourse = new HashMap<>();
        for (Student s : students) {
            for (Student.SemesterRecord rec : s.getAcademicRecord()) {
                for (Student.CourseGrade cg : rec.getCourses()) {
                    if (GRADE_ORDER.contains(cg.getGrade())) {
                        gradesByCourse.computeIfAbsent(cg.getCourseCode(), k -> new ArrayList<>()).add(cg.getGrade());
                    }
                }
            }
        }

        List<CourseOutcome> rows = courseRepository.findAll().stream()
                .filter(c -> facultyId == null || facultyId.equals(c.getFacultyId()))
                .filter(c -> departmentCode == null || departmentCode.equals(c.getDepartmentCode()))
                .filter(c -> semester == null || semester == c.getSemester())
                .filter(c -> gradesByCourse.containsKey(c.getCode()))
                .map(c -> summarize(c, gradesByCourse.get(c.getCode())))
                .sorted(Comparator.comparing(CourseOutcome::passRate))
                .toList();

        return Map.of("demoData", hasDemo(students), "rows", rows);
    }

    /** @param facultyId null for admin (all active courses), otherwise only that faculty's courses */
    public Map<String, Object> attentionFlags(String facultyId) {
        Map<String, Student> students = studentRepository.findAll().stream()
                .collect(Collectors.toMap(Student::getRollNo, Function.identity(), (a, b) -> a));

        List<AttentionFlag> flags = new ArrayList<>();
        for (Course course : courseRepository.findByActiveSemester(true)) {
            if (facultyId != null && !facultyId.equals(course.getFacultyId())) continue;
            Marks marks = marksRepository.findByCourseCode(course.getCode()).orElse(null);
            if (marks == null) continue;
            List<Course.GradingComponent> comps = marks.getGradingComponents().isEmpty()
                    ? course.getGradedComponents() : marks.getGradingComponents();

            for (Marks.StudentMark sm : marks.getStudentMarks()) {
                Student s = students.get(sm.getRollNo());
                Double pct = currentPercent(sm.getMarks(), comps);
                Double cgpa = s == null ? null : cgpa(s);
                List<String> reasons = flagReasons(pct, cgpa, passPercent, cgpaCutoff);
                if (!reasons.isEmpty()) {
                    flags.add(new AttentionFlag(sm.getRollNo(), sm.getName(), course.getCode(), course.getName(),
                            pct, cgpa, reasons));
                }
            }
        }
        flags.sort(Comparator.comparing(f -> f.currentPercent() == null ? 100.0 : f.currentPercent()));

        return Map.of("demoData", hasDemo(students.values()), "passPercent", passPercent,
                "cgpaCutoff", cgpaCutoff, "rows", flags);
    }

    // ── Pure logic (unit-tested in AnalyticsServiceTest) ──

    static CourseOutcome summarize(Course c, List<String> grades) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        GRADE_ORDER.forEach(g -> dist.put(g, 0));
        grades.forEach(g -> dist.merge(g, 1, Integer::sum));
        long passed = grades.stream().filter(g -> !"F".equals(g)).count();
        double avg = grades.stream().mapToInt(MarksService::getGradePoints).average().orElse(0);
        return new CourseOutcome(c.getCode(), c.getName(), c.getDepartmentCode(), c.getSemester(),
                grades.size(), dist, round2((double) passed / grades.size()), round2(avg));
    }

    /**
     * Percentage scored out of the components entered so far, so a student is not flagged
     * mid-semester just because the end-sem exam hasn't happened. Null if nothing is entered.
     */
    static Double currentPercent(Map<String, Double> marks, List<Course.GradingComponent> comps) {
        if (marks == null || comps == null) return null;
        List<Course.GradingComponent> entered = comps.stream()
                .filter(c -> marks.keySet().stream().anyMatch(k -> k.equals(c.getId()) || k.startsWith(c.getId() + "_")))
                .toList();
        int weight = entered.stream().mapToInt(Course.GradingComponent::getWeight).sum();
        if (weight == 0) return null;
        return round2(MarksService.calcWeightedTotal(marks, entered) / weight * 100);
    }

    /** CGPA over finalized courses (F counts as 0 points), null if none are finalized yet. */
    static Double cgpa(Student s) {
        double credits = 0, points = 0;
        for (Student.SemesterRecord rec : s.getAcademicRecord()) {
            for (Student.CourseGrade cg : rec.getCourses()) {
                if (!GRADE_ORDER.contains(cg.getGrade())) continue;
                credits += cg.getCredits();
                points += cg.getCredits() * MarksService.getGradePoints(cg.getGrade());
            }
        }
        return credits == 0 ? null : round2(points / credits);
    }

    static List<String> flagReasons(Double currentPercent, Double cgpa, double passPercent, double cgpaCutoff) {
        List<String> reasons = new ArrayList<>();
        if (currentPercent != null && currentPercent < passPercent) {
            reasons.add("Current marks " + currentPercent + "% are below the " + passPercent + "% pass line");
        }
        if (cgpa != null && cgpa < cgpaCutoff) {
            reasons.add("CGPA " + cgpa + " is below " + cgpaCutoff);
        }
        return reasons;
    }

    /** CGPA trend and graduation progress. A semester counts as completed once none of its courses are "IP". */
    static Progress computeProgress(Student s, double creditsRequired, int maxCredits) {
        List<Student.SemesterRecord> records = s.getAcademicRecord().stream()
                .sorted(Comparator.comparingInt(Student.SemesterRecord::getSemester)).toList();
        List<SemesterPoint> points = new ArrayList<>();
        double earned = 0, inProgress = 0, failed = 0, cumCredits = 0, cumPoints = 0;
        int completed = 0;

        for (Student.SemesterRecord rec : records) {
            double semCredits = 0, semPoints = 0;
            boolean anyIp = false;
            for (Student.CourseGrade cg : rec.getCourses()) {
                if (GRADE_ORDER.contains(cg.getGrade())) {
                    double gp = MarksService.getGradePoints(cg.getGrade());
                    semCredits += cg.getCredits();
                    semPoints += cg.getCredits() * gp;
                    if ("F".equals(cg.getGrade())) failed += cg.getCredits(); else earned += cg.getCredits();
                } else {
                    anyIp = true;
                    inProgress += cg.getCredits();
                }
            }
            if (!anyIp && semCredits > 0) completed++;
            if (semCredits > 0) {
                cumCredits += semCredits;
                cumPoints += semPoints;
                points.add(new SemesterPoint(rec.getSemester(), round2(semPoints / semCredits),
                        round2(cumPoints / cumCredits), earned));
            }
        }

        double remaining = Math.max(0, creditsRequired - earned);
        int semestersLeft = Math.max(0, TOTAL_SEMESTERS - completed);
        Double perSem = semestersLeft == 0 ? null : round2(remaining / semestersLeft);
        boolean onTrack = remaining == 0 || (perSem != null && perSem <= maxCredits);
        Double cgpa = points.isEmpty() ? null : points.get(points.size() - 1).cgpa();
        Double change = points.size() < 2 ? null
                : round2(points.get(points.size() - 1).cgpa() - points.get(points.size() - 2).cgpa());

        return new Progress(s.getRollNo(), s.getName(), cgpa, change, points, creditsRequired, earned, inProgress,
                failed, remaining, semestersLeft, perSem, maxCredits, onTrack, hasDemo(List.of(s)));
    }

    static boolean hasDemo(Collection<Student> students) {
        return students.stream().anyMatch(s -> s.getRollNo() != null && s.getRollNo().startsWith(DEMO_PREFIX));
    }

    static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
