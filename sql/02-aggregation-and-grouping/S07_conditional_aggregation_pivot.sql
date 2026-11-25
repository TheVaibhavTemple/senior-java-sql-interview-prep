-- ================================================================================================
-- S07  .  Conditional aggregation -- rows into columns without PIVOT
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Aggregation & Grouping
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : MEDIUM  **   common; shows up in most reporting loops
-- PRIORITY     : P2   -- product/fintech rounds mein month-wise ya status-wise columns ka sawaal
-- TECHNIQUE    : SUM(CASE WHEN ... THEN ... ELSE 0 END) -- portable pivoting
--
-- PROBLEM
--   One row per customer, with a column per month showing that month's revenue. Then:
--   a click-through style ratio computed from two conditional counts in a single pass.
--
-- ALGORITHM
--   1. Decide the grain of the OUTPUT -- here, one row per customer. That is the GROUP BY.
--   2. Every column you want becomes an aggregate over a CASE expression.
--   3. SUM(CASE WHEN month='2025-01' THEN amount ELSE 0 END) folds one month into one column.
--   4. COUNT(CASE WHEN cond THEN 1 END) counts conditionally -- NULL is not counted, so no ELSE.
--   5. Ratios: divide two conditional counts, and cast to avoid integer division.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   One subquery per column                      avoid         N passes over the table
--   Vendor PIVOT / crosstab                      acceptable    not portable; fixed column list
--   SUM(CASE WHEN ...)                           WRITE THIS    one pass, runs on every engine
--   FILTER (WHERE ...)                           acceptable    cleaner, PostgreSQL / SQLite only
--
-- WHY THIS ONE
--   Conditional aggregation is the portable pivot, and it is the single most reusable reporting
--   trick in SQL. The detail interviewers watch for is integer division: COUNT/COUNT returns 0
--   on PostgreSQL, SQLite and SQL Server unless you cast (Oracle and MySQL return a decimal).
--   Mention it before they point at it.
--
-- SAMJHO
--   Pehle output ka grain decide karo: ek row per customer. Wahi GROUP BY hai.
--   Har column ek aggregate hai CASE ke upar: SUM(CASE WHEN month = '2025-01' THEN amount ELSE 0
--   END). Ek hi pass mein saare columns ban jaate hain. Ratio mein 1.0 * lagao, warna 0 aayega.
--
-- AISE BOLO  (30-40 sec)
--   "I'd confirm the output grain first, one row per customer, and whether the month list is
--   fixed or has to be dynamic. Each column is then an aggregate over a CASE, the amount when the
--   month matches and zero otherwise, which is one pass over the table instead of one subquery
--   per column. For conditional counts I use COUNT of a CASE with no ELSE, since NULL is not
--   counted. For ratios I multiply by 1.0 first, because integer division truncates to zero on
--   PostgreSQL and SQLite."
--
-- TRAP
--   ! Postgres/SQLite mein COUNT/COUNT integer division hai: 2/5 = 0. 1.0 * ya CAST AS NUMERIC.
--   ! COUNT(CASE WHEN c THEN 1 ELSE 0 END) galat hai: 0 bhi gina jaata hai, har row count ho
--     jaati hai. ELSE hatao, ya SUM(CASE ... THEN 1 ELSE 0 END) likho.
--
-- FOLLOW-UPS KE JAWAB
--   ? How do you make the month columns dynamic rather than hard-coded?
--     -> SQL ko columns parse time pe chahiye. Dynamic SQL banao (DISTINCT months se CASE list
--        generate karke EXECUTE IMMEDIATE / EXECUTE), ya rows lao aur pivot app layer mein karo.
--   ? Why does COUNT(CASE WHEN c THEN 1 END) need no ELSE clause?
--     -> Condition false ho to CASE NULL deta hai, aur COUNT NULL nahi ginta. ELSE 0 likha to 0
--        bhi gina jaayega.
--   ? Your ratio returns 0 -- why, and what is the fix?
--     -> Integer / integer truncate hota hai (query 3: ratio_integer_div = 0). 1.0 * numerator ya
--        CAST; denominator 0 ho sakta ho to NULLIF(COUNT(*), 0).
--   ? Same output using FILTER (WHERE ...) -- which engines support it?
--     -> COUNT(*) FILTER (WHERE e.salary >= 100000): PostgreSQL 9.4+ aur SQLite 3.30+. Oracle aur
--        MySQL mein nahi, wahan CASE hi.
--
-- ALSO UNLOCKS
--   - Monthly sales pivot
--   - Click-through and conversion rates
--   - Status breakdown in one row (active/inactive/pending)
--   - Cohort tables
--
-- ORACLE / POSTGRES MEIN
--   - Oracle mein NUMBER type hai, COUNT/COUNT seedha 0.4 deta hai. Integer division ka trap
--     Postgres mein hai (bigint / bigint).
--   - Oracle 11g+ PIVOT: ... PIVOT (SUM(amount) FOR mon IN ('2025-01' AS jan_2025,
--     '2025-02' AS feb_2025)). Postgres mein tablefunc extension ka crosstab().
--   - substr(order_date, 1, 7) ki jagah TO_CHAR(order_date, 'YYYY-MM') (dono mein). Last query ka
--     GROUP BY month (alias) Oracle mein 23ai se pehle nahi chalta; expression repeat karo.
--
-- VARIANTS     : LC 1179, LC 1193
-- ================================================================================================

-- >>> QUERY: revenue per customer, one column per month
SELECT c.cust_name,
       SUM(CASE WHEN substr(o.order_date,1,7) = '2025-01' THEN o.amount ELSE 0 END) AS jan_2025,
       SUM(CASE WHEN substr(o.order_date,1,7) = '2025-02' THEN o.amount ELSE 0 END) AS feb_2025,
       SUM(CASE WHEN substr(o.order_date,1,7) = '2025-03' THEN o.amount ELSE 0 END) AS mar_2025,
       SUM(CASE WHEN substr(o.order_date,1,7) = '2025-04' THEN o.amount ELSE 0 END) AS apr_2025,
       SUM(o.amount) AS total
FROM   customers c
JOIN   orders o ON o.cust_id = c.cust_id
GROUP  BY c.cust_id, c.cust_name
ORDER  BY total DESC;

-- >>> QUERY: conditional COUNT needs no ELSE -- NULL is simply not counted
SELECT d.dept_name,
       COUNT(*)                                        AS headcount,
       COUNT(CASE WHEN e.salary >= 100000 THEN 1 END)  AS six_figure,
       COUNT(CASE WHEN e.salary <  100000 THEN 1 END)  AS below
FROM   employees e JOIN departments d ON d.dept_id = e.dept_id
GROUP  BY d.dept_id, d.dept_name
ORDER  BY d.dept_name;

-- >>> QUERY: the integer-division trap, and the fix
SELECT d.dept_name,
       COUNT(CASE WHEN e.salary >= 100000 THEN 1 END) / COUNT(*)               AS ratio_integer_div,
       ROUND(1.0 * COUNT(CASE WHEN e.salary >= 100000 THEN 1 END) / COUNT(*), 3) AS ratio_correct
FROM   employees e JOIN departments d ON d.dept_id = e.dept_id
GROUP  BY d.dept_id, d.dept_name
ORDER  BY d.dept_name;

-- >>> QUERY: category revenue as columns (the classic "product A vs B" question)
SELECT substr(o.order_date,1,7) AS month,
       SUM(CASE WHEN p.category = 'Hardware'  THEN oi.qty * p.unit_price ELSE 0 END) AS hardware,
       SUM(CASE WHEN p.category = 'Accessory' THEN oi.qty * p.unit_price ELSE 0 END) AS accessory
FROM   orders o
JOIN   order_items oi ON oi.order_id = o.order_id
JOIN   products p     ON p.product_id = oi.product_id
GROUP  BY month
ORDER  BY month;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [revenue per customer, one column per month]
--   cust_name  jan_2025  feb_2025  mar_2025  apr_2025  total
--   ---------  --------  --------  --------  --------  ------
--   Beta Corp  140000    65000     0         28000     233000
--   Alpha Ltd  67500     3400      65000     0         135900
--   Delta Inc  0         0         16500     0         16500
--   Gamma LLP  0         14000     1800      0         15800
--   (4 rows)
--
-- [conditional COUNT needs no ELSE -- NULL is simply not counted]
--   dept_name    headcount  six_figure  below
--   -----------  ---------  ----------  -----
--   Engineering  5          2           3
--   Finance      4          2           2
--   HR           3          0           3
--   (3 rows)
--
-- [the integer-division trap, and the fix]
--   dept_name    ratio_integer_div  ratio_correct
--   -----------  -----------------  -------------
--   Engineering  0                  0.4
--   Finance      0                  0.5
--   HR           0                  0.0
--   (3 rows)
--
-- [category revenue as columns (the classic "product A vs B" question)]
--   month    hardware  accessory
--   -------  --------  ---------
--   2025-01  209000    2500
--   2025-02  79000     3400
--   2025-03  79000     4300
--   2025-04  28000     0
--   (4 rows)
--
-- ======================================================================================
