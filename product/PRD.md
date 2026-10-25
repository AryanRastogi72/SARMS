# PRD: SARMS v2.0 "System of Insight"

| | |
|---|---|
| Owner | Aryan Rastogi (solo, post-fork) |
| Status | Draft |
| Builds on | SARMS v1.0 (team project, see [CHANGELOG](../CHANGELOG.md)) |
| Related | [Problem statement](problem_statement.md) · [Personas](personas.md) · [Process](process.md) |

## 1. Summary

v1.0 stores academic records well. v2.0 makes them useful: each role gets a view that helps them act earlier, built on data SARMS already holds (grades, credits, enrollments, marks).

## 2. Goals

1. Faculty and admins can see course and department performance without exporting to Excel.
2. Struggling students are flagged before results are final, not after.
3. Students can see their CGPA trend and graduation progress on one screen.
4. Product decisions for later versions are backed by usage data, not guesses.

## 3. Non-goals

- Predictive or ML-based dropout models. A transparent rule-based flag comes first.
- Notifications by email or SMS (already on the v1.0 future list, separate effort).
- Any change to grading rules or the result engine.

## 4. User stories and MoSCoW priority

| ID | As a… | I want to… | So that… | Priority |
|---|---|---|---|---|
| US-1 | Admin | see grade distribution and pass rate per course | I can spot outlier courses and report to the dean | **Must** |
| US-2 | Faculty | see a list of at-risk students in my courses | I can reach out before the final exam | **Must** |
| US-3 | Student | see credits completed vs. credits required to graduate | I know if I am on track | **Must** |
| US-4 | Student | see my CGPA trend across semesters | I know whether I am improving | **Should** |
| US-5 | Admin | filter analytics by department and semester | reports match how the dean reviews them | **Should** |
| US-6 | Admin | see which features are used, by which role, how often | I can tell what to improve or retire | **Should** |
| US-7 | Faculty | record attendance per course session | at-risk flags can use attendance, not only marks | **Could** |
| US-8 | Student | ask questions about my own record in plain language | I get answers without reading tables | **Could** |
| US-9 | Admin | export any dashboard view to CSV/PDF | I can attach it to reports | **Could** |
| US-10 | Any | get predictive "likely to fail" scores | — | **Won't** (this version) |

## 5. Requirements detail

**At-risk rule (US-2), initial version.** A student is flagged in a course if any of these is true. Thresholds are config values, not hard-coded.
- Current weighted marks below the pass threshold for the course.
- CGPA below a cut-off (default 5.0 on a 10-point scale).
- Attendance below 75%, only once US-7 exists.

Every flag shows the reason. Faculty see flags only for courses they teach.

**Graduation progress (US-3).** Credits required is a config value per program. Credits completed counts only courses with a final grade (not "IP" or a fail grade).

**Usage analytics (US-6).** Log role, feature/route and timestamp. Do not log grades, names or any record content.

## 6. Success metrics

No user numbers are claimed here. SARMS has no production users yet, so all v2.0 dashboards run on clearly labelled **demo/seed data**. The metrics below describe how success *would* be measured in a real deployment, and the targets are hypotheses to test.

| Goal | Metric | How measured | Hypothesis target |
|---|---|---|---|
| Faster reporting | Time for admin to produce a department performance summary | Task timing, before vs. after, with test users | Under 2 minutes (vs. manual Excel) |
| Earlier intervention | % of flagged students contacted by faculty before final exam | Faculty action logged on the flag | 50%+ of flags acted on |
| Flag quality | Precision of at-risk flag (flagged students who did end up failing or below cut-off) | Compare flags to final results each semester | Tune thresholds each term |
| Student self-service | % of active students who open the progress view at least once per semester | Usage analytics (US-6) | Baseline in first term |
| Evidence-based roadmap | Features with usage data available at planning time | Usage analytics (US-6) | 100% of portal routes tracked |

## 7. Risks and mitigations

| Risk | Mitigation |
|---|---|
| At-risk labels stigmatise students | Flags visible only to the student's own faculty and admins; reason always shown; wording is "needs attention", not "at risk" in the UI |
| Thresholds wrong for this institution | Config values, reviewed each semester against flag precision |
| Demo data mistaken for real results | Every analytics view shows a "Demo data" badge when seeded data is in use |
| Usage logging seen as tracking | Log feature-level events only, no record content; document it in the README |

## 8. Release plan

1. **v2.0-a**: US-1, US-3, US-4 (uses existing data only).
2. **v2.0-b**: US-2 (marks/CGPA rule), US-5.
3. **v2.0-c**: US-6, then US-7 if attendance is in scope.
