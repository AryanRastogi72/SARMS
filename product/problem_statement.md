# Problem Statement

## Context

Universities manage academic records (enrollments, marks, grades, SGPA/CGPA, promotions) across students, faculty and administrative staff. In many institutions this still runs on a mix of spreadsheets, paper forms, email and a legacy ERP that each group uses differently.

SARMS v1.0 (the original team project) digitised the core record lifecycle: registration, marks entry, result publication, promotion and graduation. This document frames what v1.0 solves and what is still unsolved, to guide the next version.

> Note: the pain points below are assumptions drawn from the team's original SRS work and common university workflows. They have not been validated with user research for this fork. Validating them is listed as an open question.

## The problem

**Academic records are stored, but they are not used.** Data goes in (marks, grades, enrollments) and comes out as a transcript. Nobody gets an early signal from it.

- **Students** can see their grades after results are published, but cannot easily answer "Am I on track to graduate?" or "Is my CGPA going up or down?"
- **Faculty** enter marks per course, but have no view of how a class is doing until the semester ends, when it is too late to help a struggling student.
- **Admin staff** run end-of-semester results in one click, but have no department-level view of pass rates, grade distributions or which courses are outliers.

## What SARMS v1.0 already solves (team work)

| Pain | v1.0 solution |
|---|---|
| Manual, error-prone result processing | One-click result engine with SGPA/CGPA recomputation and promotion |
| Students registering for too many or wrong courses | 25-credit cap, locked core courses, term-aware catalog |
| Accidental destructive admin actions | Server-side double authentication |
| Inconsistent grading rules | Weighted components and "Best N of M" policies |

## What is still unsolved (scope for the next version)

1. No aggregate view of academic performance for faculty or admins.
2. No early-warning signal for students who are falling behind.
3. Students lack a forward-looking view (credits remaining, CGPA trend).
4. No visibility into which parts of the system are actually used, so there is no evidence base for product decisions.

## Goal for the next version

Turn SARMS from a **system of record** into a **system of insight**: give each role the one view that helps them act earlier, using data the system already holds.

## Non-goals

- Replacing the institution's finance, hostel or HR systems.
- Collecting new personal data about students beyond what v1.0 stores (attendance is the one exception under consideration, see the PRD).

## Open questions

- Do faculty want an at-risk list, or would they see it as surveillance or extra work?
- Which threshold defines "at risk" at this institution (CGPA cut-off, failed course count, attendance %)?
- Who is allowed to see a student's at-risk status: faculty of that course only, or the department?
