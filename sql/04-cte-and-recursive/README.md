# SQL — Cte And Recursive

3 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| S12 | Employee hierarchy with depth   (recursive CTE) | Hard | Medium | P2 | WITH RECURSIVE -- anchor member UNION ALL recursive member | [`S12_recursive_employee_hierarchy.sql`](S12_recursive_employee_hierarchy.sql) |
| S13 | Fill missing months with a recursive date spine | Medium | Medium | P3 | Recursive CTE generating a calendar, LEFT JOIN the facts onto it | [`S13_date_spine_fill_gaps.sql`](S13_date_spine_fill_gaps.sql) |
| S14 | Hire within a budget   (running total + CTE + UNION ALL) | Hard | Medium | P3 | Greedy selection via a running total, chained across two priority tiers | [`S14_running_total_budget_hiring.sql`](S14_running_total_budget_hiring.sql) |

## Is folder ko kaise padhein

Yeh folder backend round mein kam aata hai, data roles mein zyada. Sirf S12 (P2) type karo:
anchor, UNION ALL, recursive member aur cycle guard; Oracle ka CONNECT BY bhi ek baar dekh lo.
S13 aur S14 (P3) ke liye SAMJHO aur TRAP padh lo -- idea yaad rahe, syntax nahi.

## Mock round

| Interviewer | Aap |
|---|---|
| Show the full reporting chain under the CEO, with each person's level. | I'd use a recursive CTE. The anchor picks the root where manager_id is null with depth 0, and the recursive member joins employees to the CTE on manager_id with depth plus one. |
| Why UNION ALL inside the recursive CTE? | UNION runs a duplicate check on every iteration. That is extra work, and it does not even stop a cycle, because the depth column changes every round. |
| What if the data has a cycle? | The recursion never ends. I add a guard: a depth cap at minimum, or better, I carry a path of ids and skip an id that is already in it. Oracle and PostgreSQL 14+ also have a CYCLE clause. |
| How would you write it in Oracle? | Recursive WITH without the RECURSIVE keyword and with a column list after the CTE name, or the older `CONNECT BY PRIOR emp_id = manager_id` with LEVEL and SYS_CONNECT_BY_PATH. |
| The monthly report skips months with no orders. Fix it. | GROUP BY can only return months that exist, so I generate the calendar first, with a recursive CTE or a calendar table, LEFT JOIN the monthly totals onto it and COALESCE the missing ones to zero. |
| And month-over-month growth on top of that? | I join the spine first and apply LAG after, because LAG reaches the previous row, and after a gap that is not the previous month. |
| Hire seniors cheapest first within a budget, then juniors with what is left. | That is a running total with a cutoff per tier. I keep the seniors whose running total fits, take their actual spend with `COALESCE(MAX(running_total), 0)`, and use the budget minus that as the junior cutoff. |
| Two candidates have the same salary at the boundary. | I add emp_id to the ORDER BY so the choice is deterministic, and I'd confirm with the business whether they want both or neither instead. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Recursive CTE ke teen hisse? | Anchor, UNION ALL, recursive member. |
| Recursion kab rukti hai? | Jab recursive member koi nayi row nahi deta. |
| Cycle guard ke do tarike? | Depth cap, ya ids ka path rakh ke repeat id skip karo. |
| Oracle mein RECURSIVE keyword? | Nahi likhte; CTE naam ke baad column list zaroori hai. |
| Oracle ka purana hierarchy syntax? | START WITH manager_id IS NULL CONNECT BY PRIOR emp_id = manager_id, saath mein LEVEL. |
| Postgres mein cycle detection? | 14+ mein CYCLE emp_id SET is_cycle USING path. |
| Date spine kyun chahiye? | GROUP BY khaali month nahi deta; spine + LEFT JOIN + COALESCE. |
| Postgres mein spine ka shortcut? | generate_series(start, end, INTERVAL '1 month'). |
| Missing dates kaise nikaalein? | Spine pe orders ke against NOT EXISTS. |
| Budget greedy ka core? | Salary ASC pe running total, aur running_total <= budget. |
| Koi senior fit na ho to? | MAX NULL deta hai; COALESCE(.., 0) nahi kiya to juniors bhi zero. |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
