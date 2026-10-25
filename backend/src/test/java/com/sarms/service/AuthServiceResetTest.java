package com.sarms.service;

import com.sarms.dto.PasswordResetRequest;
import com.sarms.model.Faculty;
import com.sarms.model.Student;
import com.sarms.model.User;
import com.sarms.repository.FacultyRepository;
import com.sarms.repository.StudentRepository;
import com.sarms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Solo security fix after fork: password reset must not work with an ID alone. */
class AuthServiceResetTest {

    private UserRepository users;
    private StudentRepository students;
    private FacultyRepository faculty;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        students = mock(StudentRepository.class);
        faculty = mock(FacultyRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("hashed");
        auth = new AuthService(users, encoder, null, students, faculty);

        when(users.findByUserId("STUD-2024-001")).thenReturn(Optional.of(new User(null, "STUD-2024-001", "old", "student", "S")));
        Student s = new Student();
        s.setEmail("priya.sharma@mrca.edu");
        when(students.findByRollNo("STUD-2024-001")).thenReturn(Optional.of(s));

        when(users.findByUserId("FAC-CSE-001")).thenReturn(Optional.of(new User(null, "FAC-CSE-001", "old", "faculty", "F")));
        Faculty f = new Faculty();
        f.setEmail("vikram.sarabhai@mrca.edu");
        when(faculty.findByFacultyId("FAC-CSE-001")).thenReturn(Optional.of(f));

        when(users.findByUserId("ADM-001")).thenReturn(Optional.of(new User(null, "ADM-001", "old", "admin", "A")));
    }

    private static PasswordResetRequest req(String id, String email) {
        PasswordResetRequest r = new PasswordResetRequest();
        r.setUserId(id);
        r.setEmail(email);
        r.setNewPassword("newpass123");
        return r;
    }

    @Test
    void studentWithMatchingEmailCanReset() {
        auth.resetPassword(req("STUD-2024-001", "  Priya.Sharma@MRCA.edu "));   // case and spaces ignored
        verify(users).save(argThat(u -> "hashed".equals(u.getPasswordHash())));
    }

    @Test
    void facultyWithMatchingEmailCanReset() {
        auth.resetPassword(req("FAC-CSE-001", "vikram.sarabhai@mrca.edu"));
        verify(users).save(any());
    }

    @Test
    void wrongEmailIsRejectedAndNothingIsSaved() {
        var e = assertThrows(RuntimeException.class, () -> auth.resetPassword(req("STUD-2024-001", "someone@else.com")));
        assertEquals(AuthService.RESET_MISMATCH, e.getMessage());
        verify(users, never()).save(any());
    }

    @Test
    void adminCannotBeResetWithAnyEmail() {
        var e = assertThrows(RuntimeException.class, () -> auth.resetPassword(req("ADM-001", "admin@mrca.edu")));
        assertEquals(AuthService.RESET_MISMATCH, e.getMessage());
        verify(users, never()).save(any());
    }

    @Test
    void unknownIdGetsTheSameMessageAsWrongEmail() {
        var e = assertThrows(RuntimeException.class, () -> auth.resetPassword(req("STUD-9999-999", "x@y.z")));
        assertEquals(AuthService.RESET_MISMATCH, e.getMessage());
    }

    @Test
    void missingEmailIsRejected() {
        assertThrows(RuntimeException.class, () -> auth.resetPassword(req("STUD-2024-001", null)));
        verify(users, never()).save(any());
    }
}
