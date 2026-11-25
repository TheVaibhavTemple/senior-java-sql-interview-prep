# SQL — Window Functions

3 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| S09 | Top N highest-paid employees in each department | Medium | High | ROW_NUMBER / RANK / DENSE_RANK over PARTITION BY, filtered in an outer query | [`S09_top_n_per_group.sql`](sql/03-window-functions/S09_top_n_per_group.sql) |
| S10 | Running total, moving average, and percent of total | Medium | High | Aggregate OVER a window frame (ROWS BETWEEN ... AND CURRENT ROW) | [`S10_running_total_and_moving_average.sql`](sql/03-window-functions/S10_running_total_and_moving_average.sql) |
| S11 | LAG and LEAD -- month-over-month change and first salary increase | Medium | High | LAG / LEAD to reach the previous or next row without a self-join | [`S11_lag_lead_period_over_period.sql`](sql/03-window-functions/S11_lag_lead_period_over_period.sql) |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
