# Rota Glossary

Domain terms used across the roadmap, requirements, and code.

| Term | Definition |
| --- | --- |
| **Shift** | A block of work that must be filled: a start time, an end time, a required skill, and a required headcount. |
| **Employee** | A person who can be assigned to shifts. Has an id, a name, skills, and working limits. |
| **Skill** | A qualification an employee holds and a shift may require (for example `barista` or `first-aid`). |
| **Availability** | The times an employee is willing and able to work, including unavailability blocks. |
| **Roster** | The set of assignments for a period: which employee is assigned to which shift. |
| **Constraint** | A rule a roster must satisfy. Hard constraints are mandatory; soft constraints carry a penalty when violated. |
| **Penalty** | The numeric cost of a soft-constraint violation, used to score and compare rosters. |
| **Publication** | Releasing a finalized roster so employees can see it (board, export, email, iCal). |
