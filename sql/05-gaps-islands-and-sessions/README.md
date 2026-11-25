# SQL — Gaps Islands And Sessions

3 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| S15 | Group events into sessions when the gap exceeds 30 minutes | Hard | Medium | LAG to find gaps, then a running SUM of the gap flag as a session id | [`S15_sessionise_logins.sql`](sql/05-gaps-islands-and-sessions/S15_sessionise_logins.sql) |
| S16 | Consecutive login days   (the date-minus-row-number trick) | Hard | Medium | date - ROW_NUMBER() is constant inside a consecutive run | [`S16_consecutive_days_streak.sql`](sql/05-gaps-islands-and-sessions/S16_consecutive_days_streak.sql) |
| S17 | Duplicate payments by the same customer within 10 minutes | Medium | Medium | LAG within a partition plus a time-difference predicate (or a bounded self-join) | [`S17_duplicate_payments_time_window.sql`](sql/05-gaps-islands-and-sessions/S17_duplicate_payments_time_window.sql) |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
