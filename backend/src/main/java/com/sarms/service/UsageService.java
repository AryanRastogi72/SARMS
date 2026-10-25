package com.sarms.service;

import com.sarms.model.UsageEvent;
import com.sarms.repository.UsageEventRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Feature usage analytics (solo addition after fork): which portal pages are used, by which role, how often. */
@Service
public class UsageService {

    // "/student/report" or "/admin/analytics"; deeper paths are trimmed to their first page segment
    private static final Pattern ROUTE = Pattern.compile(
            "^/(student|faculty|admin)(?:(/[a-z-]{1,40})(?:/[A-Za-z0-9_-]{1,40})*)?/?$");
    static final List<String> ROLES = List.of("student", "faculty", "admin");

    public record FeatureUsage(String feature, long events, long users, double eventsPerUser, Map<String, Long> eventsByRole) {}
    public record DailyCount(LocalDate date, long events) {}

    private final UsageEventRepository repository;

    public UsageService(UsageEventRepository repository) {
        this.repository = repository;
    }

    public void record(String route, String userId, String role) {
        String feature = featureFor(route, role);
        if (feature == null) throw new IllegalArgumentException("Unknown or mismatched route: " + route);
        repository.save(new UsageEvent(null, feature, role, hash(userId), Instant.now(), false));
    }

    public Map<String, Object> summary(int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return summarize(repository.findByTimestampAfter(since), days);
    }

    // ── Pure logic (unit-tested in UsageServiceTest) ──

    /** Normalized feature for a route, or null if the route isn't a page in the caller's own portal. */
    static String featureFor(String route, String role) {
        if (route == null || role == null) return null;
        Matcher m = ROUTE.matcher(route);
        if (!m.matches() || !m.group(1).equals(role)) return null;
        return "/" + m.group(1) + (m.group(2) == null ? "" : m.group(2));
    }

    static Map<String, Object> summarize(List<UsageEvent> events, int days) {
        Map<String, List<UsageEvent>> byFeature = events.stream().collect(Collectors.groupingBy(UsageEvent::getFeature));
        List<FeatureUsage> features = byFeature.entrySet().stream().map(e -> {
            List<UsageEvent> ev = e.getValue();
            long users = ev.stream().map(UsageEvent::getUserHash).distinct().count();
            Map<String, Long> byRole = new LinkedHashMap<>();
            ROLES.forEach(r -> byRole.put(r, ev.stream().filter(x -> r.equals(x.getRole())).count()));
            return new FeatureUsage(e.getKey(), ev.size(), users,
                    AnalyticsService.round2((double) ev.size() / users), byRole);
        }).sorted(Comparator.comparingLong(FeatureUsage::events).reversed()).toList();

        Map<String, Long> activeUsers = new LinkedHashMap<>();
        ROLES.forEach(r -> activeUsers.put(r, events.stream().filter(x -> r.equals(x.getRole()))
                .map(UsageEvent::getUserHash).distinct().count()));

        Map<LocalDate, Long> perDay = events.stream().collect(Collectors.groupingBy(
                x -> LocalDate.ofInstant(x.getTimestamp(), ZoneOffset.UTC), Collectors.counting()));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<DailyCount> daily = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            daily.add(new DailyCount(d, perDay.getOrDefault(d, 0L)));   // zero-fill quiet days
        }

        return Map.of("demoData", events.stream().anyMatch(UsageEvent::isDemo), "days", days,
                "totalEvents", events.size(), "activeUsers", activeUsers, "features", features, "daily", daily);
    }

    /** Pseudonymous, not anonymous: anyone with the user list could recompute it. Used only for distinct counts. */
    public static String hash(String userId) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(userId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
