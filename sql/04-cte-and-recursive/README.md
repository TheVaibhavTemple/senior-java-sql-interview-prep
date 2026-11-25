# SQL — Cte And Recursive

3 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| S12 | Employee hierarchy with depth   (recursive CTE) | Hard | Medium | WITH RECURSIVE -- anchor member UNION ALL recursive member | [`S12_recursive_employee_hierarchy.sql`](sql/04-cte-and-recursive/S12_recursive_employee_hierarchy.sql) |
| S13 | Fill missing months with a recursive date spine | Medium | Medium | Recursive CTE generating a calendar, LEFT JOIN the facts onto it | [`S13_date_spine_fill_gaps.sql`](sql/04-cte-and-recursive/S13_date_spine_fill_gaps.sql) |
| S14 | Hire within a budget   (running total + CTE + UNION ALL) | Hard | Medium | Greedy selection via a running total, chained across two priority tiers | [`S14_running_total_budget_hiring.sql`](sql/04-cte-and-recursive/S14_running_total_budget_hiring.sql) |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
