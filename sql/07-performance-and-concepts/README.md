# SQL — Performance And Concepts

3 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| S20 | Make a WHERE clause index-usable   (sargability) | Medium | High | Keep the indexed column bare on the left of the predicate; rewrite to a range | [`S20_sargability_and_indexes.sql`](sql/07-performance-and-concepts/S20_sargability_and_indexes.sql) |
| S21 | NOT IN with a NULL returns nothing   (the three-valued-logic trap) | Medium | High | NOT EXISTS instead of NOT IN; understand UNKNOWN in three-valued logic | [`S21_not_in_null_trap.sql`](sql/07-performance-and-concepts/S21_not_in_null_trap.sql) |
| S22 | Quick-fire concepts -- the ones asked in every single round | Easy | High | Definitions you must be able to give in one sentence, each with a live example | [`S22_concepts_quickfire.sql`](sql/07-performance-and-concepts/S22_concepts_quickfire.sql) |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
