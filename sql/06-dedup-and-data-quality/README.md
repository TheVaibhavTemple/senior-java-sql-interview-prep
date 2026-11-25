# SQL — Dedup And Data Quality

2 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| S18 | Delete duplicate rows, keeping exactly one | Medium | High | ROW_NUMBER() over the duplicate key, then delete everything with rn > 1 | [`S18_delete_duplicates_keep_one.sql`](sql/06-dedup-and-data-quality/S18_delete_duplicates_keep_one.sql) |
| S19 | The latest row per key   (the most reused query in any codebase) | Medium | High | ROW_NUMBER() = 1 over PARTITION BY key ORDER BY timestamp DESC | [`S19_latest_row_per_key.sql`](sql/06-dedup-and-data-quality/S19_latest_row_per_key.sql) |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
