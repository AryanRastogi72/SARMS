package com.sarms.seed;

import com.sarms.model.*;
import com.sarms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Seeds SARMS database with initial structural data (departments, faculty, courses, config).
 * <p>
 * IDEMPOTENT: This seeder checks whether key data already exists before inserting.
 * If the database already contains departments, it skips seeding entirely.
 * Running it multiple times is safe — it will never delete or overwrite existing data.
 * <p>
 * To force a full re-seed, manually drop the relevant MongoDB collections first,
 * then restart the server.
 */
@Component
@Order(1) // runs before DemoDataSeeder
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepo;
    private final StudentRepository studentRepo;
    private final FacultyRepository facultyRepo;
    private final DepartmentRepository deptRepo;
    private final CourseRepository courseRepo;
    private final MarksRepository marksRepo;
    private final TimetableSlotRepository timetableRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AppConfigRepository configRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepo, StudentRepository studentRepo,
                      FacultyRepository facultyRepo, DepartmentRepository deptRepo,
                      CourseRepository courseRepo, MarksRepository marksRepo,
                      TimetableSlotRepository timetableRepo, EnrollmentRepository enrollmentRepo,
                      AppConfigRepository configRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.facultyRepo = facultyRepo;
        this.deptRepo = deptRepo;
        this.courseRepo = courseRepo;
        this.marksRepo = marksRepo;
        this.timetableRepo = timetableRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.configRepo = configRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // ── Always ensure admin user exists (even on pre-seeded DBs) ──
        ensureAdminUser();

        // ── Guard: skip seeding if database already has data ──
        long existingDepartments = deptRepo.count();
        if (existingDepartments > 0) {
            log.info("Database already seeded — skipping. ({} departments found)", existingDepartments);
            return;
        }

        log.info("=== Empty database detected — running initial seed ===");

        seedDepartments();
        seedFaculty();
        seedCoreCourses();
        seedConfig();

        log.info("=== Initial database seeding complete ===");
    }

    /**
     * Ensures at least one admin user exists with a known password. Runs on every startup.
     * Also resets all faculty passwords to ensure they work regardless of JDK version used for original hashing.
     */
    private void ensureAdminUser() {
        User admin = userRepo.findByUserId("ADM-001").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setUserId("ADM-001");
            admin.setRole("admin");
            admin.setName("System Administrator");
            log.info("Created default admin user: ADM-001");
        }
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        userRepo.save(admin);

        // Also reset all faculty passwords to ensure compatibility
        long resetCount = 0;
        for (User u : userRepo.findAll()) {
            if ("faculty".equals(u.getRole()) || "student".equals(u.getRole())) {
                u.setPasswordHash(passwordEncoder.encode("password123"));
                userRepo.save(u);
                resetCount++;
            }
        }
        log.info("Admin + {} user passwords synced to default.", resetCount);
    }

    private void seedDepartments() {
        List<String[]> depts = List.of(
            new String[]{"BTech - CSE", "CSE"},
            new String[]{"BTech - ECE", "ECE"},
            new String[]{"BTech - ME", "ME"},
            new String[]{"BTech - CE", "CE"}
        );
        for (String[] d : depts) {
            Department dept = new Department();
            dept.setName(d[0]);
            dept.setCode(d[1]);
            deptRepo.save(dept);
        }
        log.info("Seeded 4 departments: CSE, ECE, ME, CE");
    }

    private void seedFaculty() {
        Map<String, List<String>> names = Map.of(
            "CSE", List.of("Dr. Vikram Sarabhai", "Dr. A. P. J. Abdul Kalam", "Dr. Narayana Murthy", "Dr. Raj Reddy", "Dr. Nandan Nilekani"),
            "ECE", List.of("Dr. Homi J. Bhabha", "Dr. C. V. Raman", "Dr. Satyendra Nath Bose", "Dr. Meghnad Saha", "Dr. Jagadish Chandra Bose"),
            "ME", List.of("Dr. M. Visvesvaraya", "Dr. Srinivasa Ramanujan", "Dr. Har Gobind Khorana", "Dr. E. Sreedharan", "Dr. Satish Dhawan"),
            "CE", List.of("Dr. C. N. R. Rao", "Dr. Venkatraman Ramakrishnan", "Dr. Raghunath Mashelkar", "Dr. Man Mohan Sharma", "Dr. Asima Chatterjee")
        );

        int count = 1;
        for (Map.Entry<String, List<String>> entry : names.entrySet()) {
            String deptCode = entry.getKey();
            String deptName = "BTech - " + deptCode;
            for (String name : entry.getValue()) {
                String facId = String.format("FAC-%s-%03d", deptCode, count++);
                Faculty f = new Faculty();
                f.setFacultyId(facId);
                f.setName(name);
                f.setDepartment(deptName);
                f.setDesignation("Professor");
                f.setEmail(name.toLowerCase().replace("dr. ", "").replace(" ", ".") + "@mrca.edu");
                f.setPhone("+91 90000 " + (10000 + count));
                f.setSpecialization(deptCode + " Specialist");
                f.setJoinYear(2020);
                facultyRepo.save(f);

                // Create User account for faculty
                User u = new User();
                u.setUserId(facId);
                u.setPasswordHash(passwordEncoder.encode("password123"));
                u.setRole("faculty");
                u.setName(name);
                userRepo.save(u);
            }
        }
        log.info("Seeded 5 faculty members per department.");
    }

    private void seedCoreCourses() {
        List<Department> departments = deptRepo.findAll();
        for (Department dept : departments) {
            List<Faculty> deptFaculty = facultyRepo.findAll().stream()
                    .filter(f -> f.getDepartment().equals(dept.getName()))
                    .collect(Collectors.toList());
            
            Random rand = new Random();
            for (int sem = 1; sem <= 8; sem++) {
                for (int cNum = 1; cNum <= 3; cNum++) {
                    String courseCode = String.format("%s%d%02d", dept.getCode(), sem, cNum);
                    String[] courseNames = {"Data Structures", "Algorithms", "Advanced Computing", "Thermodynamics", "VLSI Design", "Structural Analysis"};
                    String courseName = courseNames[rand.nextInt(courseNames.length)] + " " + cNum;
                    Faculty fac = deptFaculty.get(rand.nextInt(deptFaculty.size()));
                    
                    Course c = new Course();
                    c.setCode(courseCode);
                    c.setName(courseName);
                    c.setCredits(4.0);
                    c.setSemester(sem);
                    c.setSemesterType(sem % 2 == 0 ? "Spring" : "Monsoon");
                    c.setYear(2026 - (sem / 2)); // rough estimate
                    c.setFacultyId(fac.getFacultyId());
                    c.setFacultyName(fac.getName());
                    c.setType("Compulsory");
                    c.setCategory("core");
                    c.setActiveSemester(sem == 4); // Set sem 4 as active for demonstration
                    c.setDepartment(dept.getName());
                    c.setDepartmentCode(dept.getCode());
                    c.setDescription(String.format("Advanced core course for %s students.", dept.getName()));
                    c.setSyllabusTopics(new ArrayList<>(List.of("Foundation", "Core Concepts", "Advanced Applications")));
                    c.setGradedComponents(new ArrayList<>(List.of(
                        new Course.GradingComponent("comp1", "Assignments", 20),
                        new Course.GradingComponent("comp2", "Midsem", 30),
                        new Course.GradingComponent("comp3", "Endsem", 50)
                    )));
                    c.setEnrolled(0);
                    c.setStatus("Active");
                    c.setResultsPublished(sem < 4);
                    courseRepo.save(c);
                }
            }
        }

        // Seed some CCC and UWE courses
        Course ccc = new Course();
        ccc.setCode("CCC101");
        ccc.setName("Communication Skills");
        ccc.setCredits(2.0);
        ccc.setSemester(1);
        ccc.setSemesterType("Both");
        ccc.setYear(2026);
        ccc.setCategory("ccc");
        ccc.setDepartment("Humanities");
        ccc.setDepartmentCode("HUM");
        ccc.setStatus("Active");
        courseRepo.save(ccc);

        Course uwe = new Course();
        uwe.setCode("UWE201");
        uwe.setName("Environmental Science");
        uwe.setCredits(3.0);
        uwe.setSemester(2);
        uwe.setSemesterType("Both");
        uwe.setYear(2026);
        uwe.setCategory("uwe");
        uwe.setDepartment("Science");
        uwe.setDepartmentCode("SCI");
        uwe.setStatus("Active");
        courseRepo.save(uwe);

        log.info("Seeded 3 core courses per department per semester (Total 96 courses).");
    }

    private void seedConfig() {
        if (configRepo.count() > 0) return;
        
        AppConfig cs = new AppConfig();
        cs.setKey("currentSemester");
        cs.setValue(Map.of("type", "Spring", "year", 2026, "number", 4));
        configRepo.save(cs);

        AppConfig un = new AppConfig();
        un.setKey("universityName");
        un.setValue("MRCA University");
        configRepo.save(un);

        AppConfig mc = new AppConfig();
        mc.setKey("maxCredits");
        mc.setValue(25);
        configRepo.save(mc);
        log.info("Seeded application configuration.");
    }

    private Course.GradingComponent gc(String id, String name, int weight) {
        return new Course.GradingComponent(id, name, weight);
    }

    private Marks.StudentMark sm(String rollNo, String name, Map<String, Double> marks) {
        return new Marks.StudentMark(rollNo, name, new HashMap<>(marks), false);
    }

    private TimetableSlot makeTimetableSlot(String code, String name, String type, String room,
                                             String faculty, List<Integer> days, int start, int end, String color) {
        TimetableSlot s = new TimetableSlot();
        s.setCourseCode(code); s.setCourseName(name); s.setType(type); s.setRoom(room);
        s.setFacultyName(faculty); s.setDays(days); s.setStartHour(start); s.setEndHour(end); s.setColorClass(color);
        return s;
    }
}
