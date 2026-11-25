# SQL — Aggregation And Grouping

4 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| S05 | Second and Nth highest salary | Easy | High | DENSE_RANK over the distinct salary ladder; MAX of a bounded subquery | [`S05_nth_highest_salary.sql`](sql/02-aggregation-and-grouping/S05_nth_highest_salary.sql) |
| S06 | Find duplicates -- the values, and then every offending row | Easy | High | GROUP BY ... HAVING COUNT(*) > 1, then join back for the full rows | [`S06_duplicate_detection.sql`](sql/02-aggregation-and-grouping/S06_duplicate_detection.sql) |
| S07 | Conditional aggregation -- rows into columns without PIVOT | Medium | Medium | SUM(CASE WHEN ... THEN ... ELSE 0 END) -- portable pivoting | [`S07_conditional_aggregation_pivot.sql`](sql/02-aggregation-and-grouping/S07_conditional_aggregation_pivot.sql) |
| S08 | WHERE vs HAVING, and how NULL behaves in aggregates | Easy | High | Logical clause order; NULL semantics in comparisons and aggregates | [`S08_having_vs_where_and_nulls.sql`](sql/02-aggregation-and-grouping/S08_having_vs_where_and_nulls.sql) |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
