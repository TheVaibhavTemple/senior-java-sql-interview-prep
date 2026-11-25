-- ================================================================================================
-- S20  .  Make a WHERE clause index-usable   (sargability)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Performance & Concepts
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every senior round
-- PRIORITY     : P1   -- "query slow hai, kya karoge" har senior round mein; yeh pehla jawab hai
-- TECHNIQUE    : Keep the indexed column bare on the left of the predicate; rewrite to a range
--
-- PROBLEM
--   WHERE YEAR(order_date) = 2025 does a full scan even though order_date is indexed.
--   Explain why and rewrite it. Then: which index would you create for a given query, and
--   in what column order?
--
-- ALGORITHM
--   1. An index stores the COLUMN's values in order. It knows nothing about f(column).
--   2. The moment you wrap the column in a function, the engine must compute f for every row,
--      so the index is useless. That predicate is "non-sargable".
--   3. Rewrite as a half-open RANGE on the bare column: col >= start AND col < next_start.
--      Half-open avoids both the "23:59:59" bug and any precision surprise.
--   4. Same rule for LIKE: 'abc%' can seek, '%abc' cannot.
--   5. Composite index column order: equality columns first, then the range or sort column.
--      A composite on (a, b) serves "a" and "a, b" but not "b" alone -- the leftmost prefix rule.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   WHERE YEAR(col) = 2025                       avoid         non-sargable: full scan
--   WHERE col BETWEEN '...' AND '...23:59:59'    acceptable    works; the end boundary is a trap
--   WHERE col >= start AND col < next_start      WRITE THIS    sargable, half-open, no boundary bug
--   A functional / expression index on YEAR(col) acceptable    valid fix; costs writes
--
-- WHY THIS ONE
--   This is the single highest-value performance answer at this level, because it is the bug
--   you actually find in other people's code. Give the rule in one line -- "keep the indexed
--   column bare on the left-hand side" -- then rewrite, then mention the leftmost-prefix rule
--   for composite indexes. SELECT * belongs in the same answer: it defeats covering indexes.
--
-- SAMJHO
--   Index column ki values sorted rakhta hai, function(column) ki nahi.
--   Column ko YEAR, TRUNC, substr mein lapeta to DB ko har row pe function chalana padta hai,
--   index seek nahi hota. Column ko bare rakho aur range likho: >= start AND < next_start.
--   Composite (a, b) left se kaam karta hai: a, ya a + b. Akele b pe seek nahi.
--
-- AISE BOLO  (30-40 sec)
--   "First I'd ask how big the table is and which indexes exist, and I'd read the plan instead
--   of guessing. YEAR(order_date) = 2025 wraps the indexed column in a function, so the index
--   cannot seek and the database evaluates the function on every row. I rewrite it as a
--   half-open range on the bare column, order_date >= '2025-01-01' and order_date <
--   '2026-01-01', which returns the same rows and lets the index seek. For a composite index I
--   put the equality columns first and the range column last, because it only serves its
--   leftmost prefix. The trade-off is that every index slows writes, so I add one only when the
--   plan shows the scan."
--
-- TRAP
--   ! Is schema mein order_date akela indexed nahi, woh (cust_id, order_date) ka second column
--     hai. Isliye pehle do plans mein cust_id = 1 dono taraf hai: farak sirf date predicate ka.
--   ! Oracle mein VARCHAR2 column ko number se compare kiya (account_no = 12345) to Oracle column
--     pe TO_NUMBER lagata hai aur index nahi lagta. Literal / bind ka type column jaisa rakho.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why is '%abc' non-sargable but 'abc%' is not?
--     -> 'abc%' ka prefix pata hai, woh range ban jata hai (>= 'abc' AND < 'abd') aur seek hota
--        hai. '%abc' ka start pata nahi, to poora index ya table padhna padta hai.
--   ? You have an index on (cust_id, order_date). Which of these can it serve?
--     -> cust_id = ? haan; cust_id = ? + order_date range haan, dono pe seek; order_date akela
--        seek nahi (Q5 ka SCAN). Covering ho to poora index scan, ya Oracle INDEX SKIP SCAN.
--   ? When does an index HURT? (write amplification, low selectivity, tiny tables)
--     -> Har INSERT/UPDATE/DELETE ko har index update karna padta hai. Status jaisi low-selectivity
--        column ya chhoti table pe full scan sasta hai; index bas bojh hai.
--   ? What is a covering index, and what does SELECT * do to it?
--     -> Query ke saare columns index mein hon to table touch hi nahi hoti (Postgres Index Only
--        Scan). SELECT * baaki columns bhi maangta hai, to har row pe table lookup lagta hai.
--   ? Read an EXPLAIN plan: what are you looking for first?
--     -> Badi table pe full scan (Oracle TABLE ACCESS FULL, Postgres Seq Scan), estimated vs
--        actual rows ka bada farak (stale stats), aur galat join method ya disk pe spill hoti sort.
--
-- ALSO UNLOCKS
--   - Slow query triage
--   - Composite index design
--   - Covering indexes
--   - Why SELECT * is a performance problem, not just a style one
--
-- ORACLE / POSTGRES MEIN
--   Plan     : SQLite EXPLAIN QUERY PLAN <query>
--              Oracle EXPLAIN PLAN FOR <query>;  SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
--              Oracle asli rows: query mein /*+ GATHER_PLAN_STATISTICS */, phir
--              SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
--              Postgres EXPLAIN (ANALYZE, BUFFERS) <query>  -- query sach mein chalti hai, DML ho
--              to BEGIN ... ROLLBACK ke andar chalao.
--   YEAR()   : Oracle aur Postgres mein nahi; EXTRACT(YEAR FROM d) / TO_CHAR(d, 'YYYY') bhi
--              utne hi non-sargable hain.
--              Classic Oracle galti TRUNC(d) = DATE '2025-01-07'. Range likho:
--              d >= DATE '2025-01-01' AND d < DATE '2026-01-01'.
--   Func idx : Oracle CREATE INDEX ix_ord_day ON orders (TRUNC(order_date)); Postgres expression
--              index ON orders ((expr)), expr IMMUTABLE hona chahiye. WHERE mein wahi expression.
--   LIKE     : Postgres non-C collation pe 'abc%' ke liye text_pattern_ops index; '%abc%' ke liye
--              pg_trgm GIN index.
--
-- APNI STORY SE JODO
--   "On a daily reconciliation query against an Oracle table of more than 10 million rows, the
--   EXPLAIN plan showed a full table scan. The fix was a missing index, and the run went from
--   45 minutes to 8 seconds."
--   Note: Fact bas itna hai. Index kis column pe tha aur WHERE kya tha: [column + predicate, jo
--   sach hai]. Function-wrapped column ya composite order ki baat tabhi jodo jab wahi hua tha.
--
-- VARIANTS     : not a LeetCode type; classic: TRUNC(date) = x, UPPER(email) = x, col + 0 = x
-- ================================================================================================

-- >>> QUERY: the plan for the NON-SARGABLE version (the function stops the seek at cust_id)
EXPLAIN QUERY PLAN
SELECT order_id, amount FROM orders
WHERE  cust_id = 1 AND substr(order_date, 1, 4) = '2025';

-- >>> QUERY: the plan for the SARGABLE rewrite (SEARCH ... USING INDEX)
EXPLAIN QUERY PLAN
SELECT order_id, amount FROM orders
WHERE  cust_id = 1 AND order_date >= '2025-01-01' AND order_date < '2026-01-01';

-- >>> QUERY: both return the same rows -- the rewrite changes the plan, not the answer
SELECT 'non-sargable' AS version, COUNT(*) AS rows, SUM(amount) AS total
FROM   orders WHERE substr(order_date,1,4) = '2025'
UNION ALL
SELECT 'sargable', COUNT(*), SUM(amount)
FROM   orders WHERE order_date >= '2025-01-01' AND order_date < '2026-01-01';

-- >>> QUERY: leftmost-prefix rule -- idx_orders_cust is (cust_id, order_date)
EXPLAIN QUERY PLAN SELECT * FROM orders WHERE cust_id = 1;

-- >>> QUERY: the same index CANNOT serve a predicate on the second column alone
EXPLAIN QUERY PLAN SELECT * FROM orders WHERE order_date = '2025-01-07';

-- >>> QUERY: the indexes that exist on this schema
SELECT name AS index_name, tbl_name AS on_table
FROM   sqlite_master WHERE type = 'index' AND name LIKE 'idx_%'
ORDER  BY tbl_name, name;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the plan for the NON-SARGABLE version (the function stops the seek at cust_id)]
--   id  parent  notused  detail
--   --  ------  -------  -----------------------------------------------------
--   3   0       63       SEARCH orders USING INDEX idx_orders_cust (cust_id=?)
--   (1 row)
--
-- [the plan for the SARGABLE rewrite (SEARCH ... USING INDEX)]
--   id  parent  notused  detail
--   --  ------  -------  ---------------------------------------------------------------------------------------
--   3   0       50       SEARCH orders USING INDEX idx_orders_cust (cust_id=? AND order_date>? AND order_date<?)
--   (1 row)
--
-- [both return the same rows -- the rewrite changes the plan, not the answer]
--   version       rows  total
--   ------------  ----  ------
--   non-sargable  10    401200
--   sargable      10    401200
--   (2 rows)
--
-- [leftmost-prefix rule -- idx_orders_cust is (cust_id, order_date)]
--   id  parent  notused  detail
--   --  ------  -------  -----------------------------------------------------
--   3   0       63       SEARCH orders USING INDEX idx_orders_cust (cust_id=?)
--   (1 row)
--
-- [the same index CANNOT serve a predicate on the second column alone]
--   id  parent  notused  detail
--   --  ------  -------  -----------
--   2   0       216      SCAN orders
--   (1 row)
--
-- [the indexes that exist on this schema]
--   index_name         on_table
--   -----------------  ---------
--   idx_emp_dept       employees
--   idx_emp_manager    employees
--   idx_logins_user    logins
--   idx_orders_cust    orders
--   idx_payments_cust  payments
--   (5 rows)
--
-- ======================================================================================
