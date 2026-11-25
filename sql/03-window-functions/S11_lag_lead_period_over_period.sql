-- ================================================================================================
-- S11  .  LAG and LEAD -- month-over-month change and first salary increase
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Window Functions
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- TECHNIQUE    : LAG / LEAD to reach the previous or next row without a self-join
--
-- PROBLEM
--   Monthly revenue with the change versus the previous month, as an absolute delta and a
--   percentage. Then: each employee's first salary increase from salary_history.
--
-- ALGORITHM
--   1. Aggregate to the grain you want to compare first -- here, one row per month.
--   2. LAG(col, 1) OVER (ORDER BY month) reaches the previous row in that ordering.
--   3. The first row has no predecessor, so LAG returns NULL -- supply a default or COALESCE it.
--   4. Guard the percentage against a zero or NULL denominator with NULLIF.
--   5. PARTITION BY restarts the lookback per group, which is what you want per employee.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Self-join on month = month - 1               avoid         breaks across year boundaries
--   Correlated subquery for the previous value   avoid         one extra scan per row
--   LAG() OVER (ORDER BY ...)                    WRITE THIS    one pass, no join, no date maths
--   LAG(col, 1, default)                         WRITE THIS    kills the first-row NULL inline
--
-- WHY THIS ONE
--   The self-join version looks fine until December-to-January, and that is the bug the
--   interviewer is waiting for. LAG orders the rows and never does arithmetic on the date, so
--   it cannot have that bug. Mention the missing-month case too -- LAG reaches the previous
--   ROW, not the previous MONTH, so a gap needs a date spine (see S16).
--
-- FOLLOW-UPS
--   ? A month has no orders at all -- does LAG still give you the previous calendar month?
--   ? What does LAG return on the first row, and how do you control it?
--   ? Why NULLIF in the percentage?
--   ? Same query with LEAD -- what question does it answer instead?
--
-- ALSO UNLOCKS
--   - Month-over-month / day-over-day growth
--   - Time between consecutive events
--   - First increase / first change per entity
--   - Sessionisation (S18) and streaks (S19)
-- ================================================================================================

-- >>> QUERY: monthly revenue with month-over-month delta and percentage
WITH monthly AS (
    SELECT substr(order_date,1,7) AS month, SUM(amount) AS revenue
    FROM   orders GROUP BY month)
SELECT month, revenue,
       LAG(revenue) OVER (ORDER BY month)                        AS prev_revenue,
       revenue - LAG(revenue) OVER (ORDER BY month)              AS delta,
       ROUND(100.0 * (revenue - LAG(revenue) OVER (ORDER BY month))
             / NULLIF(LAG(revenue) OVER (ORDER BY month), 0), 1) AS pct_change
FROM   monthly
ORDER  BY month;

-- >>> QUERY: LAG with a default value, so the first row is not NULL
WITH monthly AS (
    SELECT substr(order_date,1,7) AS month, SUM(amount) AS revenue
    FROM   orders GROUP BY month)
SELECT month, revenue,
       LAG(revenue, 1, 0) OVER (ORDER BY month) AS prev_or_zero,
       revenue - LAG(revenue, 1, 0) OVER (ORDER BY month) AS delta_no_null
FROM   monthly ORDER BY month;

-- >>> QUERY: each employee's salary changes, and the FIRST increase
WITH changes AS (
    SELECT h.emp_id, e.emp_name, h.effective, h.salary,
           LAG(h.salary) OVER (PARTITION BY h.emp_id ORDER BY h.effective) AS prev_salary
    FROM   salary_history h JOIN employees e ON e.emp_id = h.emp_id)
SELECT emp_name, effective, prev_salary, salary,
       salary - prev_salary AS raise
FROM   changes
WHERE  prev_salary IS NOT NULL AND salary > prev_salary
ORDER  BY emp_name, effective;

-- >>> QUERY: just the FIRST increase per employee
WITH changes AS (
    SELECT h.emp_id, e.emp_name, h.effective, h.salary,
           LAG(h.salary) OVER (PARTITION BY h.emp_id ORDER BY h.effective) AS prev_salary
    FROM   salary_history h JOIN employees e ON e.emp_id = h.emp_id)
SELECT emp_name, MIN(effective) AS first_increase_on
FROM   changes
WHERE  prev_salary IS NOT NULL AND salary > prev_salary
GROUP  BY emp_id, emp_name
ORDER  BY emp_name;

-- >>> QUERY: LEAD -- how long until the NEXT order from the same customer
SELECT c.cust_name, o.order_date,
       LEAD(o.order_date) OVER (PARTITION BY o.cust_id ORDER BY o.order_date) AS next_order,
       CAST(julianday(LEAD(o.order_date) OVER (PARTITION BY o.cust_id ORDER BY o.order_date))
            - julianday(o.order_date) AS INTEGER) AS days_to_next
FROM   orders o JOIN customers c ON c.cust_id = o.cust_id
ORDER  BY c.cust_name, o.order_date;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.53.1.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [monthly revenue with month-over-month delta and percentage]
--   month    revenue  prev_revenue  delta    pct_change
--   -------  -------  ------------  -------  ----------
--   2025-01  207500   NULL          NULL     NULL
--   2025-02  82400    207500        -125100  -60.3
--   2025-03  83300    82400         900      1.1
--   2025-04  28000    83300         -55300   -66.4
--   (4 rows)
--
-- [LAG with a default value, so the first row is not NULL]
--   month    revenue  prev_or_zero  delta_no_null
--   -------  -------  ------------  -------------
--   2025-01  207500   0             207500
--   2025-02  82400    207500        -125100
--   2025-03  83300    82400         900
--   2025-04  28000    83300         -55300
--   (4 rows)
--
-- [each employee's salary changes, and the FIRST increase]
--   emp_name  effective   prev_salary  salary  raise
--   --------  ----------  -----------  ------  -----
--   Arjun     2019-04-01  90000        115000  25000
--   Arjun     2022-04-01  115000       140000  25000
--   Meera     2021-07-01  70000        95000   25000
--   Vikram    2020-01-01  80000        99000   19000
--   Vikram    2023-01-01  99000        118000  19000
--   (5 rows)
--
-- [just the FIRST increase per employee]
--   emp_name  first_increase_on
--   --------  -----------------
--   Arjun     2019-04-01
--   Meera     2021-07-01
--   Vikram    2020-01-01
--   (3 rows)
--
-- [LEAD -- how long until the NEXT order from the same customer]
--   cust_name  order_date  next_order  days_to_next
--   ---------  ----------  ----------  ------------
--   Alpha Ltd  2025-01-07  2025-01-19  12
--   Alpha Ltd  2025-01-19  2025-02-11  23
--   Alpha Ltd  2025-02-11  2025-03-27  44
--   Alpha Ltd  2025-03-27  NULL        NULL
--   Beta Corp  2025-01-23  2025-02-18  26
--   Beta Corp  2025-02-18  2025-04-06  47
--   Beta Corp  2025-04-06  NULL        NULL
--   Delta Inc  2025-03-02  NULL        NULL
--   Gamma LLP  2025-02-04  2025-03-14  38
--   Gamma LLP  2025-03-14  NULL        NULL
--   (10 rows)
--
-- ======================================================================================
