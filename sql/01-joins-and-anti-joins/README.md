# SQL — Joins And Anti Joins

4 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| S01 | Customers who have never placed an order   (the anti-join) | Easy | High | Anti-join -- LEFT JOIN ... WHERE right IS NULL, or NOT EXISTS | [`S01_customers_with_no_orders.sql`](sql/01-joins-and-anti-joins/S01_customers_with_no_orders.sql) |
| S02 | Employees with their manager's name   (self-join) | Easy | High | Self-join on manager_id = emp_id, with LEFT JOIN to keep the root | [`S02_self_join_manager.sql`](sql/01-joins-and-anti-joins/S02_self_join_manager.sql) |
| S03 | Revenue is double-counted after a join -- find and fix it | Medium | High | Aggregate to a single grain BEFORE joining (pre-aggregation / derived table) | [`S03_join_grain_double_counting.sql`](sql/01-joins-and-anti-joins/S03_join_grain_double_counting.sql) |
| S04 | A zero-filled grid of every category x every month   (CROSS JOIN) | Medium | Medium | CROSS JOIN to build a complete grid, LEFT JOIN the facts onto it | [`S04_cross_join_zero_filled_grid.sql`](sql/01-joins-and-anti-joins/S04_cross_join_zero_filled_grid.sql) |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
