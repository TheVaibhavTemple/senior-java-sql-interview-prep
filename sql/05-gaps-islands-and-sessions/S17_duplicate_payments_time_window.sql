-- ================================================================================================
-- S17  .  Duplicate payments by the same customer within 10 minutes
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Gaps, Islands & Sessions
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : MEDIUM  **   the fintech / payments favourite
-- PRIORITY     : P1   -- fintech round ka favourite; idempotency wala follow-up har backend mein
-- TECHNIQUE    : LAG within a partition plus a time-difference predicate (or a bounded self-join)
--
-- PROBLEM
--   Find probable double-charges: the same customer, the same amount, two payments less than
--   ten minutes apart. Return the pair so a human can review it.
--
-- ALGORITHM
--   1. Partition by whatever defines "the same payment attempt" -- here customer AND amount.
--   2. Order by time inside the partition.
--   3. LAG the previous timestamp; the gap is the difference.
--   4. Keep rows whose gap is below the threshold -- each such row IS the second charge.
--   5. A self-join with a range predicate gives the same answer and generalises to N-way
--      duplicates, at the cost of a bigger intermediate result.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   GROUP BY customer, amount HAVING COUNT > 1   avoid         ignores time; flags genuine repeats
--   Self-join with ABS(time diff) < threshold    acceptable    correct, but ABS() is not sargable
--   LAG + gap predicate                          WRITE THIS    one pass, index-friendly ordering
--   Window COUNT over a RANGE time frame         acceptable    neat where the engine supports it
--
-- WHY THIS ONE
--   The naive GROUP BY answer flags a customer who legitimately buys the same thing twice in a
--   month. The whole point of the question is that "duplicate" here means "duplicate WITHIN A
--   TIME WINDOW", and that is a LAG, not a GROUP BY. Say what you are partitioning on and why
--   before writing -- the partition key IS the definition of a duplicate.
--
-- SAMJHO
--   Yahan "duplicate" = same customer, same amount, aur 10 min ke andar.
--   PARTITION BY customer, amount; ORDER BY time; LAG se pichla payment lo.
--   Gap 10 min se kam hai to yeh row doosra charge hai. GROUP BY time nahi dekhta, isliye
--   agle mahine ka genuine repeat bhi pakad leta hai.
--
-- AISE BOLO  (30-40 sec)
--   "First I'd pin down what makes two payments the same attempt: same customer and amount, in
--   real systems usually the same beneficiary and currency too, and whether exactly 10 minutes
--   counts. GROUP BY with HAVING COUNT > 1 is the naive answer, but it also flags someone who
--   genuinely pays the same amount next month. I partition by customer and amount, order by
--   paid_at and LAG the previous payment; a row less than 10 minutes after it is the probable
--   duplicate. With an index on (cust_id, amount, paid_at) that is one ordered pass, and the
--   real fix is an idempotency key with a unique constraint, so the second charge never lands."
--
-- TRAP
--   ! Naive GROUP BY is seed mein bhi wahi 2 rows deta hai, isliye sahi lagta hai. Customer 1
--     agle mahine phir 65000 pay kare to woh bhi "duplicate" ban jayega.
--   ! SQLite julianday float hai: Q1 mein 3.5 min ka gap '3' dikhta hai (3.4999998). Display ke
--     liye chalega; filter isliye seconds aur datetime() se exact compare karta hai.
--
-- FOLLOW-UPS KE JAWAB
--   ? Three charges inside the window -- how many rows does your query return, and is that right?
--     -> LAG 2 rows deta hai (2nd vs 1st, 3rd vs 2nd): har extra charge ek baar, refund ke liye
--        sahi. Self-join 3 pairs deta hai. LAG chain karta hai: 0, 8, 16 min pe 16 bhi flag.
--   ? Why partition on amount as well as customer?
--     -> Same customer 10 min mein alag amount pay kare to woh alag purchase hai. Partition key
--        hi duplicate ki definition hai; real system mein beneficiary aur currency bhi daalo.
--   ? What index would make this fast on 200 million payments?
--     -> (cust_id, amount, paid_at): PARTITION BY + ORDER BY ka same order, to sort bach sakta
--        hai. Aur poori history nahi, sirf recent window (jaise aaj ka din) scan karo.
--   ? How would you stop this at write time rather than detect it afterwards? (idempotency key)
--     -> Client har attempt ke saath idempotency key bheje, DB mein us pe UNIQUE constraint.
--        Retry aaye to insert fail, pehla result lauta do. Yeh query phir sirf safety net hai.
--
-- ALSO UNLOCKS
--   - Double-charge and double-submit detection
--   - Repeated events inside a window
--   - Deduplicating an at-least-once event stream
--   - Fraud velocity rules
--
-- ORACLE / POSTGRES MEIN
--   Postgres : gap filter = paid_at - prev_paid_at < INTERVAL '10 minutes'. Self-join range
--              form: b.paid_at < a.paid_at + INTERVAL '10 minutes' (index pe seek hota hai).
--   Oracle   : same, INTERVAL '10' MINUTE ke saath (DATE aur TIMESTAMP dono pe).
--   Window   : cnt = COUNT(*) OVER (PARTITION BY cust_id, amount ORDER BY paid_at
--                RANGE BETWEEN INTERVAL '10' MINUTE PRECEDING AND CURRENT ROW);
--              outer query mein cnt > 1. Postgres 11+ mein INTERVAL '10 minutes'. RANGE ki
--              boundary inclusive hai: exactly 10 min wala bhi count hoga.
--
-- APNI STORY SE JODO
--   "In my event-driven file-processing platform on Azure Service Bus, I used Service Bus
--   duplicate detection, so a message sent twice inside the detection window is dropped at
--   the broker. The payment-table version of the same idea is an idempotency key."
--   Note: Yeh broker level ka dedup hai, payment table ka SQL nahi; idea same hai (same key,
--   time window ke andar). DB pe UNIQUE key / idempotency check tha ya nahi: [sirf sach ho to].
--
-- VARIANTS     : no exact LeetCode match; classic: same card + merchant + amount in N seconds
-- ================================================================================================

-- >>> QUERY: the gap between consecutive same-amount payments per customer
SELECT p.payment_id, p.cust_id, p.amount, p.paid_at,
       LAG(p.paid_at) OVER (PARTITION BY p.cust_id, p.amount ORDER BY p.paid_at) AS prev_paid_at,
       CAST(ROUND((julianday(p.paid_at)
            - julianday(LAG(p.paid_at) OVER (PARTITION BY p.cust_id, p.amount ORDER BY p.paid_at)))
            * 24 * 60) AS INTEGER) AS gap_minutes
FROM   payments p
ORDER  BY p.cust_id, p.amount, p.paid_at;

-- >>> QUERY: the suspected duplicates (gap under 10 minutes)
WITH gapped AS (
    SELECT p.payment_id, p.cust_id, p.amount, p.paid_at,
           LAG(p.payment_id) OVER (PARTITION BY p.cust_id, p.amount ORDER BY p.paid_at) AS prev_id,
           strftime('%s', p.paid_at)
           - strftime('%s', LAG(p.paid_at) OVER (PARTITION BY p.cust_id, p.amount
                                                 ORDER BY p.paid_at)) AS gap_seconds
    FROM   payments p)
SELECT c.cust_name, g.prev_id AS original_payment, g.payment_id AS duplicate_payment,
       g.amount, ROUND(g.gap_seconds / 60.0, 1) AS minutes_apart
FROM   gapped g JOIN customers c ON c.cust_id = g.cust_id
WHERE  g.gap_seconds IS NOT NULL AND g.gap_seconds < 10 * 60
ORDER  BY c.cust_name;

-- >>> QUERY: the same answer with a bounded self-join (generalises to N-way duplicates)
SELECT a.payment_id AS first_id, b.payment_id AS second_id, a.cust_id, a.amount,
       ROUND((julianday(b.paid_at) - julianday(a.paid_at)) * 24 * 60, 1) AS minutes_apart
FROM   payments a
JOIN   payments b
       ON  b.cust_id = a.cust_id
       AND b.amount  = a.amount
       AND b.paid_at > a.paid_at
       AND b.paid_at < datetime(a.paid_at, '+10 minutes')
ORDER  BY a.cust_id, a.paid_at;

-- >>> QUERY: the NAIVE answer, so you can see what it over-reports
SELECT cust_id, amount, COUNT(*) AS times_charged
FROM   payments
GROUP  BY cust_id, amount
HAVING COUNT(*) > 1;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the gap between consecutive same-amount payments per customer]
--   payment_id  cust_id  amount  paid_at              prev_paid_at         gap_minutes
--   ----------  -------  ------  -------------------  -------------------  -----------
--   5           1        3400    2025-02-11 18:45:00  NULL                 NULL
--   1           1        65000   2025-01-07 10:04:00  NULL                 NULL
--   2           1        65000   2025-01-07 10:07:30  2025-01-07 10:04:00  3
--   6           2        65000   2025-02-18 11:02:00  NULL                 NULL
--   7           2        65000   2025-02-18 11:05:00  2025-02-18 11:02:00  3
--   3           2        140000  2025-01-23 15:20:00  NULL                 NULL
--   4           3        14000   2025-02-04 09:11:00  NULL                 NULL
--   8           4        16500   2025-03-02 13:30:00  NULL                 NULL
--   (8 rows)
--
-- [the suspected duplicates (gap under 10 minutes)]
--   cust_name  original_payment  duplicate_payment  amount  minutes_apart
--   ---------  ----------------  -----------------  ------  -------------
--   Alpha Ltd  1                 2                  65000   3.5
--   Beta Corp  6                 7                  65000   3.0
--   (2 rows)
--
-- [the same answer with a bounded self-join (generalises to N-way duplicates)]
--   first_id  second_id  cust_id  amount  minutes_apart
--   --------  ---------  -------  ------  -------------
--   1         2          1        65000   3.5
--   6         7          2        65000   3.0
--   (2 rows)
--
-- [the NAIVE answer, so you can see what it over-reports]
--   cust_id  amount  times_charged
--   -------  ------  -------------
--   1        65000   2
--   2        65000   2
--   (2 rows)
--
-- ======================================================================================
