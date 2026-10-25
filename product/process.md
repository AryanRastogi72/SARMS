# Development Process

SARMS has been built in two phases with two different processes. This document keeps them separate so credit is clear.

```
Phase A: team project (v1.0)                Phase B: solo fork (v2.0)
Waterfall, 4 people                         Iterative sprints, 1 person

Requirements ─► Design ─► Build ─► Test      Requirements ─► Design ─► Sprint 1 ─► Sprint 2 ─► ...
  SRS          DFD, UML   React +  Selenium    problem stmt,  API +       build +     build +
  use cases               Spring   Playwright  personas, PRD  UI sketch   tests       tests
                          Boot     Postman                                   ▲           ▲
                                                                             └ review ◄──┘
```

## Phase A: original team project (Waterfall)

All of this was **team work** by the four original contributors.

| Stage | What was produced |
|---|---|
| Requirements | SRS document, use case document (`docs/`) |
| Design | DFD Level 0/1 with data dictionary, UML class and sequence diagrams (`docs/`) |
| Implementation | React frontend, Spring Boot backend, MongoDB Atlas |
| Testing | Postman API collection (`postman-api/`), Selenium + JUnit tests (`backend/src/test`), Playwright specs (`frontend/tests`) |

**Testing result.** The team reported a 35/35 passing run of the Postman API tests. The committed collection (`postman-api/SARMS_Postman_Collection.json`) contains 28 requests with test scripts; the run report itself is not stored in the repository. This is the team's result, not part of the solo fork.

## Phase B: solo fork (iterative)

Work after the fork point (`ef77690`) is solo, by Aryan Rastogi. One feature per sprint, so each can be reviewed and shipped on its own.

### 1. Requirements
- [problem_statement.md](problem_statement.md) and [personas.md](personas.md) frame the user problem.
- [PRD.md](PRD.md) sets user stories, MoSCoW priority and success metrics.

### 2. Design
Per feature, before code:
- Which existing data it uses (no new collections unless the PRD says so).
- API endpoint shape and which role can call it.
- A rough screen layout.

### 3. Sprints
Each sprint delivers one PRD user story (or a small group of them):
1. Backend endpoint + unit test.
2. Frontend view.
3. Demo/seed data, labelled as demo data in the UI.
4. CHANGELOG entry under "solo work".

### 4. Testing
- **Kept from v1.0:** the team's existing Postman, Selenium and Playwright suites must keep passing.
- **Added per feature:** JUnit tests for the new service logic (for example the at-risk rule and credits-remaining calculation), plus a Postman request for each new endpoint.

### Definition of done
- Tests pass (old and new).
- Feature works against seeded demo data.
- CHANGELOG updated, original vs. solo kept separate.
