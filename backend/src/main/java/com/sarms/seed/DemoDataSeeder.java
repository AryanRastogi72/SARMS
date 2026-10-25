package com.sarms.seed;

import com.sarms.model.*;
import com.sarms.repository.*;
import com.sarms.service.MarksService;
import com.sarms.service.UsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.sarms.service.AnalyticsService.DEMO_PREFIX;

/**
 * DEMO DATA ONLY (solo addition after fork). Seeds 40 fictional students (roll numbers start with "DEMO-")
 * with graded semesters 1-3 and partial semester-4 marks, plus 30 days of fictional page-view events, so the
 * analytics views have something to show. Every analytics response reports demoData=true while these records exist. Turn off with sarms.demo-data=false.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "sarms.demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final List<String> DEPTS = List.of("CSE", "ECE", "ME", "CE");
    private static final int STUDENTS_PER_DEPT = 10;
    private static final int ACTIVE_SEMESTER = 4;

    private final StudentRepository studentRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final MarksRepository marksRepo;
    private final UsageEventRepository usageRepo;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(StudentRepository studentRepo, UserRepository userRepo, CourseRepository courseRepo,
                          MarksRepository marksRepo, UsageEventRepository usageRepo, PasswordEncoder passwordEncoder) {
        this.studentRepo = studentRepo;
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.marksRepo = marksRepo;
        this.usageRepo = usageRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (studentRepo.countByRollNoStartingWith(DEMO_PREFIX) > 0) {
            log.info("Demo students already present — skipping.");
        } else {
            seedStudents();
        }
        if (usageRepo.existsByDemoTrueAndTimestampAfter(Instant.now().minus(30, ChronoUnit.DAYS))) {
            log.info("Recent demo usage events already present — skipping.");
        } else {
            seedUsage();
        }
    }

    private void seedStudents() {
        Random rand = new Random(42); // fixed seed: same demo data on every machine
        Map<String, Marks> rosters = new HashMap<>();
        String passwordHash = passwordEncoder.encode("password123");

        for (String dept : DEPTS) {
            String hardCourse = dept + "202"; // one tougher course per department, so outliers show up
            for (int i = 1; i <= STUDENTS_PER_DEPT; i++) {
                String rollNo = String.format("%s%s-%02d", DEMO_PREFIX, dept, i);
                String name = "Demo Student " + dept + "-" + String.format("%02d", i);
                double ability = 68 + rand.nextGaussian() * 12;

                Student s = new Student();
                s.setRollNo(rollNo);
                s.setName(name);
                s.setProgram("BTech - " + dept);
                s.setBatchYear(2024);
                s.setCurrentSemester(ACTIVE_SEMESTER);
                s.setActive(true);
                s.setDob(LocalDate.of(2005, 1, 1));
                s.setEmail(rollNo.toLowerCase() + "@demo.invalid");

                for (int sem = 1; sem <= ACTIVE_SEMESTER; sem++) {
                    Student.SemesterRecord rec = new Student.SemesterRecord();
                    rec.setSemester(sem);
                    rec.setRegistrationFinalized(true);
                    double credits = 0, points = 0;
                    for (int c = 1; c <= 3; c++) {
                        Course course = courseRepo.findByCode(String.format("%s%d%02d", dept, sem, c)).orElse(null);
                        if (course == null) continue;
                        double pct = clamp(ability + rand.nextGaussian() * 8 - (course.getCode().equals(hardCourse) ? 18 : 0));

                        if (sem < ACTIVE_SEMESTER) {
                            String grade = MarksService.getGradeLetter(pct);
                            int gp = MarksService.getGradePoints(grade);
                            rec.getCourses().add(new Student.CourseGrade(course.getCode(), course.getName(), course.getCredits(), grade, gp));
                            credits += course.getCredits();
                            points += course.getCredits() * gp;
                        } else {
                            rec.getCourses().add(new Student.CourseGrade(course.getCode(), course.getName(), course.getCredits(), "IP", 0));
                            // Assignments (20) and Midsem (30) entered; Endsem not yet held
                            Map<String, Double> marks = new HashMap<>();
                            marks.put("comp1", Math.round(pct / 100 * 20 * 10) / 10.0);
                            marks.put("comp2", Math.round(pct / 100 * 30 * 10) / 10.0);
                            rosterFor(rosters, course).getStudentMarks().add(new Marks.StudentMark(rollNo, name, marks, false));
                        }
                    }
                    rec.setSgpa(credits == 0 ? 0 : Math.round(points / credits * 100.0) / 100.0);
                    s.getAcademicRecord().add(rec);
                }
                studentRepo.save(s);
                userRepo.save(new User(null, rollNo, passwordHash, "student", name));
            }
        }
        marksRepo.saveAll(rosters.values());
        log.info("Seeded {} DEMO students (fictional data, roll numbers {}*).", DEPTS.size() * STUDENTS_PER_DEPT, DEMO_PREFIX);
    }

    /** Fictional page views over the last 30 days: per-user daily visit probabilities, quieter at weekends. */
    private void seedUsage() {
        Random rand = new Random(7);
        Map<String, Map<String, Double>> visitOdds = Map.of(
                "student", Map.of("/student/courses", 0.5, "/student/report", 0.25, "/student/progress", 0.2, "/student/registration", 0.05),
                "faculty", Map.of("/faculty/marks", 0.6, "/faculty/courses", 0.3, "/faculty/analytics", 0.25),
                "admin", Map.of("/admin/courses", 0.5, "/admin/students", 0.4, "/admin/analytics", 0.4,
                        "/admin/results", 0.1, "/admin/register", 0.1, "/admin/term-control", 0.05));
        Map<String, Integer> usersPerRole = Map.of("student", DEPTS.size() * STUDENTS_PER_DEPT, "faculty", 8, "admin", 1);

        List<UsageEvent> events = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int day = 29; day >= 0; day--) {
            LocalDate date = today.minusDays(day);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            for (var role : usersPerRole.entrySet()) {
                for (int u = 1; u <= role.getValue(); u++) {
                    String userHash = UsageService.hash(DEMO_PREFIX + role.getKey() + "-" + u);
                    for (var odds : visitOdds.get(role.getKey()).entrySet()) {
                        if (rand.nextDouble() < odds.getValue() * (weekend ? 0.4 : 1)) {
                            Instant at = date.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(8 * 3600 + rand.nextInt(12 * 3600));
                            events.add(new UsageEvent(null, odds.getKey(), role.getKey(), userHash, at, true));
                        }
                    }
                }
            }
        }
        usageRepo.saveAll(events);
        log.info("Seeded {} DEMO usage events (fictional, demo=true).", events.size());
    }

    private Marks rosterFor(Map<String, Marks> rosters, Course course) {
        return rosters.computeIfAbsent(course.getCode(), code -> marksRepo.findByCourseCode(code).orElseGet(() -> {
            Marks m = new Marks();
            m.setCourseCode(code);
            m.setCourseName(course.getName());
            m.setSemester(course.getSemester());
            m.setActiveSemester(true);
            m.setGradingComponents(new ArrayList<>(course.getGradedComponents()));
            return m;
        }));
    }

    private static double clamp(double pct) {
        return Math.max(0, Math.min(100, pct));
    }
}
