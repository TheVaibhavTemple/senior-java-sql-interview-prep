-- ================================================================================================
-- S19  .  The latest row per key   (the most reused query in any codebase)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Dedup & Data Quality
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- "latest row per key" har SQL round mein aur har backend codebase mein hai
-- TECHNIQUE    : ROW_NUMBER() = 1 over PARTITION BY key ORDER BY timestamp DESC
--
-- PROBLEM
--   Return each employee's current salary from salary_history, and each customer's most
--   recent order -- the whole row, not just the date.
--
-- ALGORITHM
--   1. PARTITION BY the key that identifies the entity.
--   2. ORDER BY the recency column DESC inside the window.
--   3. Add a deterministic tiebreaker (the primary key) or the "latest" row flips between runs.
--   4. ROW_NUMBER() = 1 in an outer filter picks exactly one row per key.
--   5. The MAX-subquery form returns the right DATE but needs a second join to get the row,
--      and it returns two rows when two share the maximum timestamp.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   WHERE date = (SELECT MAX(date) ...)          acceptable    returns BOTH rows on a tie
--   GROUP BY key, MAX(date) then join back       acceptable    two passes, same tie problem
--   ROW_NUMBER() = 1                             WRITE THIS    exactly one row, tie rule explicit
--   DISTINCT ON (key)                            acceptable    shortest, PostgreSQL only
--
-- WHY THIS ONE
--   Ties are the entire question. The MAX form silently returns two rows when two records share
--   the latest timestamp, and that bug reaches production because the test data has no ties.
--   ROW_NUMBER with an explicit tiebreaker makes the choice visible and repeatable.
--
-- SAMJHO
--   Har key (employee, customer) ke rows ko PARTITION BY karo, latest pehle (ORDER BY date DESC).
--   ROW_NUMBER = 1 har key ki sirf ek row rakhta hai, poori row ke saath.
--   Tie ho sakta hai to ORDER BY ke end mein PK jaisa tiebreaker daalo.
--   MAX(date) wala tarika tie pe do rows deta hai, aur row laane ke liye join back bhi chahiye.
--
-- AISE BOLO  (30-40 sec)
--   "First I'd ask what identifies the entity, which column defines latest, and whether two rows
--   can share that timestamp, because ties are the whole question. The MAX subquery, or GROUP BY
--   and join back, finds the right date but needs a second pass for the row and returns both
--   rows on a tie. I use ROW_NUMBER partitioned by the key, ordered by the timestamp descending
--   with the primary key as tiebreaker, and keep rn = 1, so I get exactly one full row per key.
--   It is one sort, O(n log n), and an index on (key, timestamp) lets the database read the rows
--   already in order; if the business wants every tied row, I switch to RANK."
--
-- TRAP
--   ! Tiebreaker ke bina "latest" row run-to-run badal sakti hai. Test data mein tie hota hi
--     nahi, isliye bug production mein pakda jata hai.
--   ! salary_history mein PK nahi hai, isliye yahan tiebreaker salary hai. Real table mein
--     history_id jaisa PK last mein daalo.
--
-- FOLLOW-UPS KE JAWAB
--   ? Two rows share the maximum timestamp -- what does each version return?
--     -> MAX subquery dono rows deta hai (tie query mein x aur y). ROW_NUMBER sirf ek, jo
--        tiebreaker decide kare. RANK = 1 phir se dono deta hai.
--   ? Why add the primary key to the ORDER BY?
--     -> Taaki order unique ho aur har run mein wahi row aaye. Bina iske tie pe DB koi bhi row
--        de sakta hai, aur report ya API response flip karta hai.
--   ? Latest row per key for 50 million keys -- what index do you want?
--     -> (key, ts, pk) composite; B-tree ulta bhi padh leta hai, DESC alag se zaroori nahi. Keys
--        bahut hain to ek ordered pass theek; keys kam, rows bahut to LATERAL + LIMIT 1 per key.
--   ? Return the latest TWO rows per key instead.
--     -> WHERE rn <= 2, last query dekho. Tie wali rows bhi chahiye to RANK() <= 2.
--
-- ALSO UNLOCKS
--   - Current state from an event/history table
--   - Most recent order, login, price, status
--   - Slowly-changing-dimension current row
--   - First row per key (just flip the ORDER BY)
--
-- ORACLE / POSTGRES MEIN
--   Postgres : SELECT DISTINCT ON (emp_id) emp_id, effective, salary FROM salary_history
--              ORDER BY emp_id, effective DESC, salary DESC;  -- ORDER BY key se shuru ho
--   Oracle   : SELECT emp_id, MAX(salary) KEEP (DENSE_RANK LAST ORDER BY effective) AS salary
--              FROM salary_history GROUP BY emp_id;           -- ek pass, join back nahi
--   Dono     : LATERAL (Oracle 12c+ mein CROSS APPLY bhi) ke andar ORDER BY ... DESC
--              FETCH FIRST 1 ROW ONLY (Postgres LIMIT 1 bhi), jab har key pe index seek chahiye.
--
-- VARIANTS     : LC 1164 (Product Price at a Given Date), LC 512, LC 1070 (ties -> RANK)
-- ================================================================================================

-- >>> QUERY: current salary per employee, from the history table
WITH ranked AS (
    SELECT h.emp_id, e.emp_name, h.effective, h.salary,
           ROW_NUMBER() OVER (PARTITION BY h.emp_id
                              ORDER BY h.effective DESC, h.salary DESC) AS rn
    FROM   salary_history h JOIN employees e ON e.emp_id = h.emp_id)
SELECT emp_name, effective AS effective_from, salary AS current_salary
FROM   ranked WHERE rn = 1
ORDER  BY emp_name;

-- >>> QUERY: the FIRST row per key -- the same query with the ORDER BY flipped
WITH ranked AS (
    SELECT h.emp_id, e.emp_name, h.effective, h.salary,
           ROW_NUMBER() OVER (PARTITION BY h.emp_id ORDER BY h.effective ASC, h.salary ASC) AS rn
    FROM   salary_history h JOIN employees e ON e.emp_id = h.emp_id)
SELECT emp_name, effective AS joined_on, salary AS starting_salary
FROM   ranked WHERE rn = 1 ORDER BY emp_name;

-- >>> QUERY: most recent order per customer, whole row
WITH ranked AS (
    SELECT o.*, ROW_NUMBER() OVER (PARTITION BY o.cust_id
                                   ORDER BY o.order_date DESC, o.order_id DESC) AS rn
    FROM   orders o)
SELECT c.cust_name, r.order_id, r.order_date, r.amount
FROM   ranked r JOIN customers c ON c.cust_id = r.cust_id
WHERE  r.rn = 1
ORDER  BY c.cust_name;

-- >>> QUERY: the tie problem, made visible
WITH t(k, ts, v) AS (VALUES ('a','2025-01-01','x'), ('a','2025-01-01','y'), ('a','2024-12-01','z'))
SELECT 'MAX subquery' AS method, k, ts, v FROM t
WHERE  ts = (SELECT MAX(ts) FROM t t2 WHERE t2.k = t.k)
UNION ALL
SELECT 'ROW_NUMBER=1', k, ts, v FROM (
    SELECT k, ts, v, ROW_NUMBER() OVER (PARTITION BY k ORDER BY ts DESC, v) rn FROM t)
WHERE rn = 1;

-- >>> QUERY: the latest TWO per key
WITH ranked AS (
    SELECT o.*, ROW_NUMBER() OVER (PARTITION BY cust_id
                                   ORDER BY order_date DESC, order_id DESC) AS rn
    FROM   orders o)
SELECT c.cust_name, r.rn, r.order_date, r.amount
FROM   ranked r JOIN customers c ON c.cust_id = r.cust_id
WHERE  r.rn <= 2
ORDER  BY c.cust_name, r.rn;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [current salary per employee, from the history table]
--   emp_name  effective_from  current_salary
--   --------  --------------  --------------
--   Arjun     2022-04-01      140000
--   Meera     2021-07-01      95000
--   Vikram    2023-01-01      118000
--   (3 rows)
--
-- [the FIRST row per key -- the same query with the ORDER BY flipped]
--   emp_name  joined_on   starting_salary
--   --------  ----------  ---------------
--   Arjun     2017-06-15  90000
--   Meera     2019-01-20  70000
--   Vikram    2018-02-26  80000
--   (3 rows)
--
-- [most recent order per customer, whole row]
--   cust_name  order_id  order_date  amount
--   ---------  --------  ----------  ------
--   Alpha Ltd  109       2025-03-27  65000
--   Beta Corp  110       2025-04-06  28000
--   Delta Inc  107       2025-03-02  16500
--   Gamma LLP  108       2025-03-14  1800
--   (4 rows)
--
-- [the tie problem, made visible]
--   method        k  ts          v
--   ------------  -  ----------  -
--   MAX subquery  a  2025-01-01  x
--   MAX subquery  a  2025-01-01  y
--   ROW_NUMBER=1  a  2025-01-01  x
--   (3 rows)
--
-- [the latest TWO per key]
--   cust_name  rn  order_date  amount
--   ---------  --  ----------  ------
--   Alpha Ltd  1   2025-03-27  65000
--   Alpha Ltd  2   2025-02-11  3400
--   Beta Corp  1   2025-04-06  28000
--   Beta Corp  2   2025-02-18  65000
--   Delta Inc  1   2025-03-02  16500
--   Gamma LLP  1   2025-03-14  1800
--   Gamma LLP  2   2025-02-04  14000
--   (7 rows)
--
-- ======================================================================================
