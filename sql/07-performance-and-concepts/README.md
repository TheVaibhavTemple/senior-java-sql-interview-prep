# SQL — Performance And Concepts

5 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| S20 | Make a WHERE clause index-usable   (sargability) | Medium | High | P1 | Keep the indexed column bare on the left of the predicate; rewrite to a range | [`S20_sargability_and_indexes.sql`](S20_sargability_and_indexes.sql) |
| S21 | NOT IN with a NULL returns nothing   (the three-valued-logic trap) | Medium | High | P1 | NOT EXISTS instead of NOT IN; understand UNKNOWN in three-valued logic | [`S21_not_in_null_trap.sql`](S21_not_in_null_trap.sql) |
| S22 | Quick-fire concepts -- the ones asked in every single round | Easy | High | P1 | Definitions you must be able to give in one sentence, each with a live example | [`S22_concepts_quickfire.sql`](S22_concepts_quickfire.sql) |
| S26 | Keyset vs OFFSET pagination   (page by the last row seen, not by a row count) | Medium | High | P1 | Keyset: WHERE (sort_col, id) > (last row seen) ORDER BY sort_col, id LIMIT n | [`S26_keyset_vs_offset_pagination.sql`](S26_keyset_vs_offset_pagination.sql) |
| S27 | Idempotent insert and upsert   (the same message arrives twice) | Medium | High | P1 | UNIQUE business key + INSERT ... ON CONFLICT DO NOTHING / DO UPDATE with excluded | [`S27_idempotent_upsert.sql`](S27_idempotent_upsert.sql) |

## Is folder ko kaise padhein

Paanchon P1 hain. S20 sabse pehle: "query slow hai" har senior round mein aata hai, aur aapki
reconciliation story (45 minute se 8 second) yahin judti hai. Phir S21 ka NOT IN + NULL expansion
zubaani yaad karo. S22 ki definitions ek-ek line mein bolo, aur har baar engine (Oracle ya Postgres)
ka naam lo. Phir S26 (keyset pagination) aur S27 (idempotent upsert): ye dono Spring
Boot backend ke follow-up hain, Pageable aur at-least-once consumer se jude.

## Mock round

Slow-query drill-down: har jawab agle sawaal ka darwaza kholta hai.

| Interviewer | Aap |
|---|---|
| A daily report query that ran in seconds now takes 40 minutes. What do you do first? | I get the actual plan instead of guessing: EXPLAIN PLAN with DBMS_XPLAN in Oracle, or EXPLAIN (ANALYZE, BUFFERS) in PostgreSQL. I also check what changed: data volume, statistics, or the query itself. |
| The plan shows a full table scan on a 10 million row table. | Then I read the WHERE clause. If the filter column is wrapped in a function, like TRUNC(created_at) = :d, the index cannot seek, so I rewrite it as a half-open range. If there is simply no index on the filter column, I add one. |
| Which index exactly? | A composite with the equality columns first and the range column last, for example (account_id, created_at). It only serves its leftmost prefix, so the order has to match how the query filters. |
| You added the index and the plan still shows a full scan. | I check three things: an implicit conversion, like a VARCHAR2 column compared with a number, which puts TO_NUMBER on the column; stale statistics; and low selectivity, where a full scan really is cheaper. |
| The estimate says 100 rows, the actual is 2 million. | That points to stale or missing statistics, so I gather stats and read the plan again. A wrong estimate usually brings a wrong join method, like a nested loop where a hash join was needed. |
| The query does SELECT *. Does that matter? | Yes. If the query only needed columns that are in the index, it could skip the table entirely. SELECT * forces a table lookup for every row, so I select only the columns I need. |
| The same report finds accounts with no transactions using NOT IN, and it returns nothing. | One NULL in the subquery makes NOT IN never TRUE for any row. I switch to NOT EXISTS, which is NULL-safe and usually runs as an anti-join. |
| The same report is also an API that pages with OFFSET. Page 1 is instant, page 5000 takes seconds. | OFFSET still reads and throws away every row it skips, so the cost grows with the page number. I switch to keyset pagination: remember the last (created_at, id) and ask for WHERE (created_at, id) > (?, ?) ORDER BY created_at, id with a LIMIT, on an index in that order, so every page is a seek. |
| So should we just index every column we filter on? | No. Every index slows each insert, update and delete, and on a small table or a low-selectivity column a full scan is cheaper. I add an index only when the plan shows it is needed. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Sargable ka matlab? | Aisa predicate jo index seek use kar sake: column bare, function ke bahar. |
| YEAR(d) = 2025 ka fix? | d >= '2025-01-01' AND d < '2026-01-01', yaani half-open range. |
| BETWEEN ... '23:59:59' mein kya galat hai? | Fractional seconds miss ho jaate hain; half-open range likho. |
| LIKE 'abc%' vs '%abc'? | Pehla range seek kar sakta hai, doosra nahi (Postgres mein pg_trgm GIN help karta hai). |
| Composite (a, b) kis pe kaam karta hai? | a pe, ya a + b pe; akele b pe seek nahi. |
| Covering index? | Query ke saare columns index mein hain, to table lookup nahi lagta. |
| Oracle mein plan kaise dekhte ho? | EXPLAIN PLAN FOR ...; phir SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY); |
| Postgres mein asli time aur rows? | EXPLAIN (ANALYZE, BUFFERS); yeh query sach mein chalata hai. |
| Oracle mein VARCHAR2 = number? | Column pe implicit TO_NUMBER lagta hai, index nahi lagta. |
| NOT IN + NULL? | Ek NULL = zero rows; NOT EXISTS likho. |
| TRUNCATE rollback hota hai? | Oracle mein nahi (implicit commit), Postgres mein transaction ke andar haan. |
| Oracle aur Postgres ka default isolation? | Dono ka READ COMMITTED. |
| OFFSET ka deep page slow kyun? | DB skip ki hui saari rows padh ke phenkta hai; keyset WHERE (created_at, id) > (?, ?) seedha seek karta hai. |
| Duplicate message ka asli guard? | Business key pe UNIQUE + INSERT ... ON CONFLICT DO NOTHING; update count 0 = duplicate. |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
