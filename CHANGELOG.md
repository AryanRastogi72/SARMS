# Changelog

OnTrack was called SARMS until the fork was renamed.

This file separates the original team project from work done after the fork.

- **Original team project**: built by Aryan Rastogi, Chhayank Gupta, Ridhima Garg and Muskan Saxena at [top-of-the-code/SARMS](https://github.com/top-of-the-code/SARMS). Fork point: commit `ef77690`.
- **Solo additions**: everything under "Unreleased" and any later version, by Aryan Rastogi only.

## [Unreleased] — solo work after the fork

### Added
- `CHANGELOG.md` (this file) and a Credits section in the README.
- `product/` folder: problem statement, personas, PRD for the next version, and a development process document.
- Project renamed from SARMS to OnTrack (repo and README). Internal package and database names (`com.sarms`, `sarms_db`) are unchanged.
- README rewritten to lead with the problem and users, label every feature Team or Solo, and list known issues; screenshots of the solo features in `docs/screenshots/` (demo data).
- **Academic analytics dashboard** (PRD US-1, US-2, US-5) for admins (`/admin/analytics`) and faculty (`/faculty/analytics`, own courses only):
  - pass rate and grade distribution per course from final grades, with department/semester filters for admins;
  - "needs attention" list for the active semester: marks below the pass line (scored only against components entered so far) or CGPA below a cut-off, each with its reason. Thresholds are config values.
  - API: `GET /api/analytics/courses`, `GET /api/analytics/attention` (admin + faculty).
  - Unit tests: `AnalyticsServiceTest` (6 tests).
- **Student progress view** (PRD US-3, US-4) at `/student/progress`: CGPA trend line with SGPA per semester, credits earned / in progress / remaining against a configurable graduation requirement (`sarms.graduation-credits`, default 160), and the credits-per-semester pace needed versus the registration cap.
  - API: `GET /api/analytics/progress/{rollNo}`; students can read only their own, staff can read any.
  - Unit tests: 3 progress tests in `AnalyticsServiceTest`, access rules in `AnalyticsControllerTest` (3 tests).
- **Feature usage analytics** (PRD US-6) at `/admin/usage`: page views per day, most-used pages, views per user, and active users by role over 7/30/90 days.
  - Every portal page view is logged by the layout as `{page, role, pseudonymous user hash, time}`; no names or record content. Role is taken from the JWT, and routes outside the caller's own portal are rejected.
  - API: `POST /api/usage/events` (any signed-in user), `GET /api/usage/summary` (admin).
  - Unit tests: `UsageServiceTest` (4 tests).
- **Postman**: folder "8. Analytics & Usage" with 9 requests for the new endpoints, including access-denied cases (collection now 37 requests).
- **Demo data seeder** (`DemoDataSeeder`): 40 fictional students with roll numbers `DEMO-*`, plus 30 days of fictional page-view events (`demo=true`). Analytics and usage views show a "Demo data" badge whenever these records are present. Disable with `sarms.demo-data=false`.

### Security
- **Password reset required only a user ID** (found during the post-fork end-to-end run). `POST /api/auth/forgot-password` in v1.0 reset any account, including admin accounts, given just its ID. Now:
  - the email on the student or faculty record must match (case-insensitive);
  - admin accounts can't be reset through this endpoint (they have no email on file);
  - unknown IDs, wrong emails and admin IDs all get the same error, so the endpoint can't be used to check which IDs exist;
  - the login page's reset form asks for the registered email.
  - Known limit: knowing someone's email is weak proof. The proper fix is an emailed one-time reset link, which needs email sending (on the v1.0 "future enhancements" list).
  - Unit tests: `AuthServiceResetTest` (6 tests). Postman folder "9. Security Fix: Password Reset" (4 requests).
  - Side effect: the team's Selenium `ForgotPasswordTest` resets `ADM-001` by ID and now fails by design.

## [1.0.0] — original team project (at fork point `ef77690`)

### Features
- Three role-based portals (Admin, Faculty, Student) with JWT auth and Spring Security role guards.
- One-click result engine: grade finalization, SGPA/CGPA recomputation, semester promotion.
- Auto-enrollment into next-semester core courses on result publication.
- Graduation detection after semester 8.
- Double authentication (server-side credential re-check) for destructive admin actions.
- Configurable Monsoon/Spring academic term.
- Credit-capped (25) course registration with locked core courses.
- Weighted grading components with "Best N of M" policies.
- Self-service password reset.
- Student academic report with SGPA-by-semester chart and PDF export.
- Timetable API (`/api/timetable`); no frontend page for it yet.

### Engineering
- Idempotent database seeder.
- SRS, DFD, use case, class and sequence diagram documents (`docs/`).
- Postman API collection (`postman-api/`, 28 requests with test scripts at the fork point).
- Selenium + JUnit and Playwright end-to-end tests.
