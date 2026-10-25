package com.sarms.service;

import com.sarms.dto.LoginRequest;
import com.sarms.dto.LoginResponse;
import com.sarms.dto.PasswordResetRequest;
import com.sarms.model.Faculty;
import com.sarms.model.Student;
import com.sarms.model.User;
import com.sarms.repository.FacultyRepository;
import com.sarms.repository.StudentRepository;
import com.sarms.repository.UserRepository;
import com.sarms.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;

    /** Same message for unknown ID, wrong email and admin accounts, so the endpoint can't be used to probe IDs. */
    static final String RESET_MISMATCH = "ID and email don't match our records. Admin passwords can't be reset here.";

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       StudentRepository studentRepository, FacultyRepository facultyRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUserId(request.getUserId().trim())
                .orElseThrow(() -> new RuntimeException("Invalid ID or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid ID or password");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getRole(), user.getName());
        return new LoginResponse(token, user.getUserId(), user.getRole(), user.getName());
    }

    /**
     * Solo security fix after fork: the original version reset any account, admin included, given only its ID.
     * Now the email on the student/faculty record must match. Admins have no email on file, so they can't use it.
     * Known limit: knowing an email is weak proof; an emailed one-time link is the real fix once email sending exists.
     */
    public void resetPassword(PasswordResetRequest request) {
        if (request.getUserId() == null || request.getEmail() == null || request.getNewPassword() == null) {
            throw new RuntimeException("ID, email and new password are required");
        }
        User user = userRepository.findByUserId(request.getUserId().trim()).orElse(null);
        if (user == null || !emailMatches(emailOnFile(user), request.getEmail())) {
            throw new RuntimeException(RESET_MISMATCH);
        }

        if (request.getNewPassword().length() < 4) {
            throw new RuntimeException("Password must be at least 4 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private String emailOnFile(User user) {
        return switch (user.getRole()) {
            case "student" -> studentRepository.findByRollNo(user.getUserId()).map(Student::getEmail).orElse(null);
            case "faculty" -> facultyRepository.findByFacultyId(user.getUserId()).map(Faculty::getEmail).orElse(null);
            default -> null;
        };
    }

    static boolean emailMatches(String onFile, String given) {
        return onFile != null && !onFile.isBlank() && onFile.trim().equalsIgnoreCase(given.trim());
    }

    public User getCurrentUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
