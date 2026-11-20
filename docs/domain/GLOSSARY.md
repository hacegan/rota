# Rota Glossary

Domain terms used across the roadmap, requirements, and code.

| Term | Definition |
| --- | --- |
| **Shift** | A block of work that must be filled: a start time, an end time, a required skill, and a required headcount. |
| **Employee** | A person who can be assigned to shifts. Has an id, a name, skills, and working limits. |
| **Skill** | A qualification an employee holds and a shift may require (for example `barista` or `first-aid`). |
| **Availability** | The datetime intervals an employee is willing and able to work, which may cross midnight. |
| **Roster** | The set of assignments for a period: which employee is assigned to which shift. |
| **Constraint** | A rule a roster must satisfy. Hard constraints are mandatory; medium are best-effort; soft carry a penalty. |
| **Penalty** | The numeric cost of a medium or soft violation, used to score and compare rosters. |
| **Publication** | Releasing a finalized roster so employees can see it (board, export, email, iCal). |
| **Night** | Any shift overlapping 00:00-06:00 local time. |
| **Weekend** | Saturday and Sunday. |
| **Timezone** | Site configuration; Europe/Istanbul in the fixtures. |
| **contractHoursPerWeek** | Contracted weekly hours per employee. |
| **Preference** | A PREFER/AVOID statement by an employee about a date, weekday, or shift. |
| **Pinning** | Marking an assignment as locked so a solver repair cannot change it. |
| **Stability** | Keeping an employee's previous week pattern when re-planning. |
| **Coverage** | Staffing a shift to its required headcount (medium priority). |
| **Hard / Medium / Soft** | Score levels: legality (hard) > coverage (medium) > quality (soft). |
