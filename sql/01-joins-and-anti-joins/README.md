# SQL — Joins And Anti Joins

6 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| S01 | Customers who have never placed an order   (the anti-join) | Easy | High | P1 | Anti-join -- LEFT JOIN ... WHERE right IS NULL, or NOT EXISTS | [`S01_customers_with_no_orders.sql`](S01_customers_with_no_orders.sql) |
| S02 | Employees with their manager's name   (self-join) | Easy | High | P1 | Self-join on manager_id = emp_id, with LEFT JOIN to keep the root | [`S02_self_join_manager.sql`](S02_self_join_manager.sql) |
| S03 | Revenue is double-counted after a join -- find and fix it | Medium | High | P2 | Aggregate to a single grain BEFORE joining (pre-aggregation / derived table) | [`S03_join_grain_double_counting.sql`](S03_join_grain_double_counting.sql) |
| S04 | A zero-filled grid of every category x every month   (CROSS JOIN) | Medium | Medium | P3 | CROSS JOIN to build a complete grid, LEFT JOIN the facts onto it | [`S04_cross_join_zero_filled_grid.sql`](S04_cross_join_zero_filled_grid.sql) |
| S24 | Reconcile the bank statement against the ledger   (FULL OUTER JOIN + status) | Medium | High | P1 | One row per txn_ref on each side, FULL OUTER JOIN, CASE for the status | [`S24_reconciliation_bank_vs_ledger.sql`](S24_reconciliation_bank_vs_ledger.sql) |
| S28 | Join row-count puzzle (INNER / LEFT / RIGHT / FULL / CROSS), and ON vs WHERE on a LEFT JOIN | Easy-Medium | High | P1 | Per key: t1 copies x t2 copies, NULL never matches, an outer join adds its unmatched rows once; right-table filter in ON, not WHERE | [`S28_join_row_count_puzzle.sql`](S28_join_row_count_puzzle.sql) |

## Is folder ko kaise padhein

Pehle P1 karo: S01, S02 aur S24 ko memory se do baar type karo. S28 ki join counts kagaz par
nikalo, ye aksar bina computer ke poocha jaata hai. S24 fintech profile ka sabse
important sawaal hai, uska AISE BOLO zor se bolo. Phir S03 ek baar type karke "grain pehle" wali
line bolo. S04 ke liye sirf SAMJHO aur TRAP padho.

## Mock round

| Interviewer | Aap |
|---|---|
| Find customers who never placed an order. | I'd use NOT EXISTS against orders on cust_id, or a LEFT JOIN with WHERE o.order_id IS NULL. Both are anti-joins and both can use the index on orders(cust_id). |
| Why not NOT IN? | If the subquery returns even one NULL, NOT IN is never TRUE, so I get zero rows and no error. NOT EXISTS doesn't have that problem. |
| List every employee with their manager's name. | I self-join employees as e and m on e.manager_id = m.emp_id, and I use a LEFT JOIN so the CEO, whose manager_id is NULL, stays in the result. |
| Revenue looks too high after joining orders to order_items. Why? | The join fans out: each order repeats once per line, so SUM(o.amount) counts it several times. I pre-aggregate order_items per order_id and then join one-to-one. |
| Show every category in every month, with zero where nothing sold. | I build the grid first with a CROSS JOIN of categories and months, LEFT JOIN the aggregated facts onto it, and COALESCE the revenue to zero. |
| Reconcile the bank statement with our ledger. | I check for duplicate txn_ref on both sides first, collapse each side to one row per ref, then FULL OUTER JOIN and label each ref MATCHED, AMOUNT_MISMATCH, MISSING_IN_LEDGER or MISSING_IN_BANK. |
| MySQL has no FULL OUTER JOIN. Now what? | I LEFT JOIN bank to ledger and UNION ALL the ledger refs that have no bank row, found with NOT EXISTS. It returns the same rows. |
| The ledger has 10 million rows. How do you keep it fast? | I read only the day's window, index txn_ref and the date column, and check the EXPLAIN plan for a full table scan. A daily reconciliation query I worked on, on an Oracle table of more than 10 million rows, went from 45 minutes to 8 seconds once the plan showed a full table scan and the missing index was added. |
| t1 is 1, 1, 2, NULL, 3 and t2 is 1, 2, 2, NULL, NULL, 4. How many rows does each join return? | I go key by key: id 1 gives 2 x 1 and id 2 gives 1 x 2, so INNER is 4, and NULL never matches NULL. LEFT adds NULL and 3 for 6, RIGHT adds NULL, NULL and 4 for 7, FULL is 9, and CROSS is 5 x 6 = 30. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Anti-join kya hai? | A ki woh rows jinka B mein match nahi: NOT EXISTS ya LEFT JOIN + IS NULL. |
| NOT IN aur NULL? | Subquery mein ek NULL aaya to zero rows; NOT EXISTS likho. |
| LEFT JOIN ke baad IS NULL kis column pe? | Right table ki PK ya join column pe, kisi nullable column pe nahi. |
| LEFT JOIN ke right table pe WHERE filter lagaya to? | Woh INNER JOIN ban jaata hai; filter ON mein daalo. |
| Self-join mein CEO kyun gayab hota hai? | INNER JOIN NULL manager_id ko match nahi karta; LEFT JOIN lagao. |
| Fan-out kya hai? | One-to-many join ke baad parent row repeat hoti hai, aur SUM inflate ho jaata hai. |
| SUM(DISTINCT amount) fix hai? | Nahi, same amount wale alag orders ek ban jaate hain. |
| CROSS JOIN kitni rows deta hai? | m x n, isliye sirf chhoti dimension lists pe. |
| Zero-fill kaise karein? | CROSS JOIN se grid, facts LEFT JOIN, COALESCE(x, 0) bahar wale SELECT mein. |
| FULL OUTER JOIN kahan nahi hai? | MySQL mein (SQLite 3.39 se pehle bhi nahi); wahan LEFT JOIN + UNION ALL anti-join. |
| Recon ka pehla step? | Dono taraf duplicate txn_ref check, warna join double ho jaata hai. |
| Recon ke chaar status? | MATCHED, AMOUNT_MISMATCH, MISSING_IN_LEDGER, MISSING_IN_BANK. |
| Join ki rows paper pe kaise gino? | Har common key pe copies multiply karo, NULL ka zero; phir outer side ki har unmatched row +1. CROSS = m x n. |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
