package com.sarms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One portal page view (solo addition after fork). Stores no names, grades or record content:
 * only the page, the viewer's role, and a pseudonymous hash of their user ID for distinct-user counts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "usage_events")
public class UsageEvent {
    @Id
    private String id;
    private String feature;      // "/student/report"
    private String role;         // "student" | "faculty" | "admin"
    private String userHash;     // first 16 hex chars of SHA-256(userId)
    @Indexed
    private Instant timestamp;
    private boolean demo;        // true = seeded by DemoDataSeeder, not real usage
}
