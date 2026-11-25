# SQL — Dedup And Data Quality

2 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| S18 | Delete duplicate rows, keeping exactly one | Medium | High | P1 | ROW_NUMBER() over the duplicate key, then delete everything with rn > 1 | [`S18_delete_duplicates_keep_one.sql`](S18_delete_duplicates_keep_one.sql) |
| S19 | The latest row per key   (the most reused query in any codebase) | Medium | High | P1 | ROW_NUMBER() = 1 over PARTITION BY key ORDER BY timestamp DESC | [`S19_latest_row_per_key.sql`](S19_latest_row_per_key.sql) |

## Is folder ko kaise padhein

Dono P1 hain, dono memory se type karo. S19 pehle, kyunki "latest row per key" roz ke code mein bhi
likhna padta hai; phir S18. Dono mein pehla sawaal ek hi hai: duplicate ya "latest" ki definition
kya hai, aur tie pe kaunsi row bachegi. S18 mein DELETE se pehle SELECT wala procedure zubaani bolo.

## Mock round

| Interviewer | Aap |
|---|---|
| Our employees table has duplicate rows. Remove them. | First: which columns define a duplicate, and which copy survives? I'll assume name, salary, hire date and email, keeping the lowest emp_id. I rank the copies with ROW_NUMBER partitioned by those columns and ordered by emp_id, and I show the rn > 1 rows as a SELECT first. |
| Then? | I delete those ids inside a transaction, compare the affected count with what the SELECT showed, and only then commit. |
| The table has 400 million rows. | I wouldn't run one DELETE. I'd delete in chunks with a commit per chunk to keep each transaction small, or, if most rows have to go, build a clean copy with CREATE TABLE AS SELECT and swap it in. |
| There is no id column; the duplicate rows are fully identical. | Then I use the physical row id inside the same ROW_NUMBER pattern: ROWID in Oracle, ctid in PostgreSQL. |
| How do you stop it coming back? | A unique constraint on the natural key, for example email, and I'd check how that constraint treats NULLs on our engine, because PostgreSQL lets several NULLs through by default. |
| Now give me each employee's current salary from the history table. | ROW_NUMBER partitioned by emp_id, ordered by effective date descending with a tiebreaker, and keep rn = 1. That gives exactly one full row per employee. |
| Why not WHERE effective = (SELECT MAX(effective) ...)? | It works until two rows share the latest date; then it returns both. ROW_NUMBER makes the tie rule explicit, and if the business wants all tied rows I use RANK instead. |
| 50 million customers, latest order for each. Which index? | (cust_id, order_date, order_id). With that many keys, one ordered pass with ROW_NUMBER is fine; with few keys and many rows each, a LATERAL lookup with LIMIT 1 per key uses the index better. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| PARTITION BY mein kya jaata hai? | Woh columns jo "same row" define karte hain. |
| Window ka ORDER BY kya decide karta hai? | Kaunsi copy bachegi, yaani survivor rule. |
| DELETE se pehle kya karte ho? | Wahi query SELECT bana ke rn > 1 rows dekho, phir transaction mein delete. |
| NOT IN (SELECT MIN(id) ...) ka risk? | Subquery mein NULL aaya to 0 rows delete; PK pe safe, phir bhi NOT EXISTS ki habit. |
| SELECT DISTINCT * se dedup kyun nahi hua? | emp_id har copy mein alag hai; DISTINCT sirf non-key columns pe karo. |
| Poori row same ho, koi id nahi? | Oracle mein ROWID, Postgres mein ctid se delete. |
| UNIQUE index aur NULL? | Postgres/SQLite NULL ko distinct maante hain; Postgres 15+ mein NULLS NOT DISTINCT. |
| Latest row mein tiebreaker kyun? | Tie pe row har run mein badal sakti hai; PK ORDER BY ke end mein daalo. |
| MAX subquery tie pe kya deta hai? | Dono rows. |
| Tie wali saari rows chahiye to? | RANK() = 1. |
| Postgres shortcut? | DISTINCT ON (key) ... ORDER BY key, ts DESC. |
| Oracle shortcut? | MAX(col) KEEP (DENSE_RANK LAST ORDER BY ts) ke saath GROUP BY key. |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
