# SQL — Gaps Islands And Sessions

3 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| S15 | Group events into sessions when the gap exceeds 30 minutes | Hard | Medium | P3 | LAG to find gaps, then a running SUM of the gap flag as a session id | [`S15_sessionise_logins.sql`](S15_sessionise_logins.sql) |
| S16 | Consecutive login days   (the date-minus-row-number trick) | Hard | Medium | P2 | date - ROW_NUMBER() is constant inside a consecutive run | [`S16_consecutive_days_streak.sql`](S16_consecutive_days_streak.sql) |
| S17 | Duplicate payments by the same customer within 10 minutes | Medium | Medium | P1 | LAG within a partition plus a time-difference predicate (or a bounded self-join) | [`S17_duplicate_payments_time_window.sql`](S17_duplicate_payments_time_window.sql) |

## Is folder ko kaise padhein

S17 sabse pehle: fintech round mein duplicate payment aur idempotency ka sawaal almost pakka aata hai.
Phir S16 (streak, LC 180 type). S15 sessionisation data aur product-analytics roles mein zyada aata
hai, backend service round mein kam; uska "running SUM of a flag = group id" idea samjho, baaki
SAMJHO aur TRAP padh lo.

## Mock round

| Interviewer | Aap |
|---|---|
| Some customers were charged twice. Find the double charges in SQL. | First I'd confirm what makes two payments the same attempt: same customer and amount, probably the same beneficiary and currency, inside what window? Say 10 minutes. I partition by customer and amount, order by paid_at and LAG the previous payment; a row less than 10 minutes after it is a probable duplicate. |
| Why not GROUP BY customer, amount HAVING COUNT(*) > 1? | It ignores time. A customer who pays the same amount every month would be flagged. Here the time window is the definition of a duplicate. |
| Three charges inside five minutes. What do you return? | With LAG I get two rows, the second and third charge, each against the one before it, which is what a refund needs. A self-join would return three pairs. LAG also chains: with charges at 0, 8 and 16 minutes it flags both 8 and 16, though 16 is outside the window from the first. |
| This table has 200 million rows. Make it fast. | A composite index on (cust_id, amount, paid_at) matches the partition and the order, so the window can read rows already sorted. And I'd run it on a recent window, like the last day, not the whole history. |
| How do you stop it at the source? | An idempotency key sent by the client, with a unique constraint on it. A retry with the same key fails the insert and gets the original result back, so this query becomes a safety net. |
| Different question: group logins into sessions with a 30-minute idle rule. | I LAG the previous login per user and flag 1 when there is no previous row or the gap is over 30 minutes. A running SUM of that flag is the session id, and I group by user and session id. |
| And the longest streak of consecutive login days? | Distinct days per user, numbered with ROW_NUMBER; date minus row number stays constant inside a run, so I group on it and take the biggest count. If the step can be irregular, I go back to the gap flag. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Running SUM of flag session id kyun banta hai? | Flag sirf naye session pe 1 hai, to sum wahin +1 hota hai aur andar same rehta hai. |
| User ke pehle event ka flag? | 1, kyunki LAG NULL hai; warna session count ek kam aata hai. |
| date - ROW_NUMBER kab kaam karta hai? | Sirf jab step exact +1 ho; irregular gap pe gap flag lagao. |
| Ek din mein do login? | Pehle DISTINCT days nikalo, warna rn do baar badhta hai aur streak tootti hai. |
| Midnight cross karne wala session? | Koi farak nahi, poora timestamp compare hota hai, date nahi. |
| Duplicate payment ki definition kahan likhi hai? | PARTITION BY mein: customer + amount (real system mein beneficiary, currency bhi). |
| GROUP BY HAVING COUNT > 1 kyun galat? | Time window nahi dekhta, genuine repeat bhi flag ho jata hai. |
| Duplicate detection ke liye index? | (cust_id, amount, paid_at), partition + order ke same order mein. |
| Write time pe duplicate kaise roko? | Idempotency key + UNIQUE constraint. |
| SQLite julianday ka trap? | Float hai; exactly 30 min jaisi boundary pe galat compare, datetime() ya seconds se compare karo. |
| Postgres mein 30 min ka gap check? | login_at > prev + INTERVAL '30 minutes'. |
| LC 180 ka trick? | ROW_NUMBER() OVER (ORDER BY id) - ROW_NUMBER() OVER (PARTITION BY num ORDER BY id) same rehta hai jab tak num lagatar repeat ho. |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
