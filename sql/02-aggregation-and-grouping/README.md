# SQL — Aggregation And Grouping

4 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| S05 | Second and Nth highest salary | Easy | High | P1 | DENSE_RANK over the distinct salary ladder; MAX of a bounded subquery | [`S05_nth_highest_salary.sql`](S05_nth_highest_salary.sql) |
| S06 | Find duplicates -- the values, and then every offending row | Easy | High | P1 | GROUP BY ... HAVING COUNT(*) > 1, then join back for the full rows | [`S06_duplicate_detection.sql`](S06_duplicate_detection.sql) |
| S07 | Conditional aggregation -- rows into columns without PIVOT | Medium | Medium | P2 | SUM(CASE WHEN ... THEN ... ELSE 0 END) -- portable pivoting | [`S07_conditional_aggregation_pivot.sql`](S07_conditional_aggregation_pivot.sql) |
| S08 | WHERE vs HAVING, and how NULL behaves in aggregates | Easy | High | P1 | Logical clause order; NULL semantics in comparisons and aggregates | [`S08_having_vs_where_and_nulls.sql`](S08_having_vs_where_and_nulls.sql) |

## Is folder ko kaise padhein

S05, S06 aur S08 P1 hain: inhe memory se do baar type karo. S05 mein ROW_NUMBER, RANK aur
DENSE_RANK ka farq ek tie wale example pe zubaani bolna aana chahiye. S08 theory hai, uske TRAP
yaad karo. S07 ek baar type karo aur integer division wala trap bolke dekho.

## Mock round

| Interviewer | Aap |
|---|---|
| Find the second highest salary. | First I'd confirm they mean the second distinct salary. Then MAX(salary) WHERE salary < (SELECT MAX(salary) FROM employees), which returns NULL if there isn't one. |
| Now make it the Nth. | I use DENSE_RANK() OVER (ORDER BY salary DESC), filter on rank N and wrap it in MAX, so an out-of-range N gives NULL instead of no rows. |
| Why not ROW_NUMBER? | ROW_NUMBER numbers rows, not distinct values. If the top salary is tied, ROW_NUMBER = 2 returns the top salary again. |
| Find duplicate emails, and show me the rows. | GROUP BY email HAVING COUNT(*) > 1 gives the values. For the rows I use COUNT(*) OVER (PARTITION BY email) and keep the ones above 1, in one pass. |
| Why can't the COUNT go in WHERE? | WHERE runs on each row before grouping, so the count doesn't exist yet. Conditions on aggregates go in HAVING. |
| One row per customer, one column per month. | Conditional aggregation: SUM(CASE WHEN month = '2025-01' THEN amount ELSE 0 END) for each column, grouped by customer. It's one pass and runs on every engine. |
| Your ratio shows 0. | That's integer division on PostgreSQL or SQLite. I multiply by 1.0 or cast to numeric, and wrap the denominator in NULLIF to avoid dividing by zero. |
| What does AVG do with NULLs? | It skips them: the sum of the non-NULL values over the non-NULL count. If NULL should count as zero, I write AVG(COALESCE(col, 0)). |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| RANK vs DENSE_RANK? | Tie ke baad RANK gap chhodta hai (1, 1, 3), DENSE_RANK nahi (1, 1, 2). |
| Nth highest ka safe pattern? | DENSE_RANK subquery pe MAX, WHERE dr = N; out of range ho to NULL. |
| LIMIT 1 OFFSET 1 mein problem? | Bina DISTINCT ties pe galat, aur na mile to khaali set aata hai, NULL nahi. |
| WHERE vs HAVING? | WHERE rows ko grouping se pehle chhaanta hai, HAVING groups ko baad mein. |
| COUNT(*) vs COUNT(col)? | COUNT(*) har row ginta hai, COUNT(col) sirf non-NULL. |
| COUNT(DISTINCT col) NULL ginta hai? | Nahi, NULL chhod deta hai. |
| col = NULL kab TRUE hota hai? | Kabhi nahi, hamesha UNKNOWN; IS NULL likho. |
| GROUP BY mein NULL? | Saare NULL ek hi group mein aate hain. |
| Duplicate rows (sirf values nahi) kaise? | COUNT(*) OVER (PARTITION BY key) > 1 wali rows. |
| PIVOT ke bina pivot? | Har column ke liye SUM(CASE WHEN ... THEN x ELSE 0 END). |
| COUNT(CASE WHEN c THEN 1 ELSE 0 END)? | Galat, 0 bhi gina jaata hai; ELSE hatao. |
| Oracle mein '' kya hai? | NULL. WHERE col = '' kabhi match nahi karta. |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
