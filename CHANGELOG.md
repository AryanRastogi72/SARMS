# Changelog

This file separates the original team project from work done after the fork.

- **Original team project**: built by Aryan Rastogi, Chhayank Gupta, Ridhima Garg and Muskan Saxena at [top-of-the-code/SARMS](https://github.com/top-of-the-code/SARMS). Fork point: commit `ef77690`.
- **Solo additions**: everything under "Unreleased" and any later version, by Aryan Rastogi only.

## [Unreleased] — solo work after the fork

### Added
- `CHANGELOG.md` (this file) and a Credits section in the README.
- `product/` folder: problem statement, personas, PRD for the next version, and a development process document.

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
- Postman API collection (`postman-api/`, 28 requests with test scripts).
- Selenium + JUnit and Playwright end-to-end tests.
