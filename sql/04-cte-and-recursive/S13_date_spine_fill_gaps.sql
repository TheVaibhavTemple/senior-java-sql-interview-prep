-- ================================================================================================
-- S13  .  Fill missing months with a recursive date spine
-- ------------------------------------------------------------------------------------------------
-- TRACK        : CTEs & Recursion
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : MEDIUM  **   common; shows up in most reporting loops
-- PRIORITY     : P3   -- reporting / data roles ka sawaal; backend round mein kam aata hai
-- TECHNIQUE    : Recursive CTE generating a calendar, LEFT JOIN the facts onto it
--
-- PROBLEM
--   A monthly revenue report must show every month in the range, including months with no
--   orders at all, as 0. Then find which dates in the range have no orders.
--
-- ALGORITHM
--   1. A GROUP BY can only emit months that exist in the data -- a dead month simply vanishes.
--   2. Generate the complete calendar first with a recursive CTE: anchor = first month,
--      recursive member = previous month + 1, stop at the last month.
--   3. LEFT JOIN the aggregated facts onto the spine.
--   4. COALESCE the measure to 0.
--   5. The missing-dates question is the same spine with an anti-join instead of a LEFT JOIN.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   GROUP BY month                               avoid         empty months disappear silently
--   A permanent calendar / dim_date table        WRITE THIS    what a warehouse actually does
--   Recursive CTE spine                          WRITE THIS    no extra table needed
--   generate_series()                            acceptable    PostgreSQL; Oracle: CONNECT BY LEVEL
--   Fill the gaps in the application             avoid         moves the bug downstream
--
-- WHY THIS ONE
--   This is the other half of "what happens to rows that do not exist". It is also why LAG
--   alone cannot compute month-over-month growth safely: LAG reaches the previous ROW, and
--   with a gap that row is two months back. Spine first, then LAG -- say it in that order.
--
-- SAMJHO
--   GROUP BY sirf wahi months deta hai jinka data hai; khaali month gayab ho jata hai. Isliye
--   pehle poora calendar banao (recursive CTE: pehla month, phir +1 month), phir facts ko
--   LEFT JOIN karo aur COALESCE(revenue, 0). Missing dates = wahi spine + NOT EXISTS.
--
-- AISE BOLO  (30-40 sec)
--   "A plain GROUP BY can only return months that have data, so an empty month silently
--   disappears. I'd generate the calendar first, ideally from a permanent calendar table,
--   otherwise with a recursive CTE that starts at the first month and adds one month until the
--   last, then LEFT JOIN the monthly totals onto it and COALESCE the gaps to zero. The spine is
--   tiny, so the real cost is the one aggregation over orders. The order matters for growth
--   too: spine first, then LAG, because LAG only sees the previous row, not the previous month."
--
-- TRAP
--   ! Spine ke bina LAG galat month se compare karta hai. Pehle spine, phir LAG.
--   ! LAG(x, 1, 0) pehle month pe fake delta deta hai (2025-01: 207500). Report mein wahan NULL
--     zyada imaandaar hai.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why can't LAG alone give you correct month-over-month growth?
--     -> LAG pichli ROW deta hai. Month missing ho to woh row do mahine pehle ki hai, growth
--        galat. Spine se har month ki row pakki hoti hai.
--   ? Make it a daily spine instead of monthly.
--     -> Recursive member mein date(d, '+1 day'); Oracle mein d + 1, Postgres mein
--        generate_series(start, end, INTERVAL '1 day'). Third query daily spine hi hai.
--   ? When would you build a permanent dim_date table instead?
--     -> Jab bahut reports use karein, ya holiday, fiscal year, week number chahiye. Ek baar
--        banao, index lagao, har query mein JOIN karo -- recursion ki zaroorat khatam.
--   ? Which dates in the range have NO orders? (anti-join against the spine)
--     -> Spine pe NOT EXISTS (SELECT 1 FROM orders WHERE order_date = d). Third query: 1-10 Feb
--        mein sirf 4 Feb ko order hai, baaki 9 dates aati hain.
--
-- ALSO UNLOCKS
--   - Zero-filled time series
--   - Gap-free month-over-month growth
--   - Missing-date detection
--   - Cohort and retention grids
--
-- ORACLE / POSTGRES MEIN
--   Oracle: RECURSIVE keyword nahi, column list spine(month) zaroori, anchor mein FROM dual.
--   Month badhao ADD_MONTHS(m, 1) se, bucket TRUNC(order_date, 'MM'). Chhota tareeka:
--   SELECT ADD_MONTHS(DATE '2025-01-01', LEVEL - 1) FROM dual CONNECT BY LEVEL <= 6.
--   Postgres: generate_series(DATE '2025-01-01', DATE '2025-06-01', INTERVAL '1 month')::date,
--   bucket date_trunc('month', order_date)::date.
--
-- VARIANTS     : LC 1613 (find the missing IDs), LC 1767 (subtasks that did not execute)
-- ================================================================================================

-- >>> QUERY: the problem -- 2025-05 and 2025-06 simply do not exist in this output
SELECT substr(order_date,1,7) AS month, SUM(amount) AS revenue
FROM   orders GROUP BY month ORDER BY month;

-- >>> QUERY: a gap-free monthly report via a recursive spine (Jan .. Jun 2025)
WITH RECURSIVE spine(month) AS (
    SELECT '2025-01'
    UNION ALL
    SELECT strftime('%Y-%m', date(month || '-01', '+1 month'))
    FROM   spine
    WHERE  month < '2025-06'
),
facts AS (
    SELECT substr(order_date,1,7) AS month, SUM(amount) AS revenue
    FROM   orders GROUP BY month
)
SELECT s.month,
       COALESCE(f.revenue, 0) AS revenue,
       COALESCE(f.revenue, 0) - LAG(COALESCE(f.revenue, 0), 1, 0) OVER (ORDER BY s.month) AS mom_delta
FROM   spine s LEFT JOIN facts f ON f.month = s.month
ORDER  BY s.month;

-- >>> QUERY: a DAILY spine, and the dates in February with no orders
WITH RECURSIVE days(d) AS (
    SELECT '2025-02-01'
    UNION ALL
    SELECT date(d, '+1 day') FROM days WHERE d < '2025-02-10'
)
SELECT d AS date_with_no_orders
FROM   days
WHERE  NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_date = days.d)
ORDER  BY d;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the problem -- 2025-05 and 2025-06 simply do not exist in this output]
--   month    revenue
--   -------  -------
--   2025-01  207500
--   2025-02  82400
--   2025-03  83300
--   2025-04  28000
--   (4 rows)
--
-- [a gap-free monthly report via a recursive spine (Jan .. Jun 2025)]
--   month    revenue  mom_delta
--   -------  -------  ---------
--   2025-01  207500   207500
--   2025-02  82400    -125100
--   2025-03  83300    900
--   2025-04  28000    -55300
--   2025-05  0        -28000
--   2025-06  0        0
--   (6 rows)
--
-- [a DAILY spine, and the dates in February with no orders]
--   date_with_no_orders
--   -------------------
--   2025-02-01
--   2025-02-02
--   2025-02-03
--   2025-02-05
--   2025-02-06
--   2025-02-07
--   2025-02-08
--   2025-02-09
--   2025-02-10
--   (9 rows)
--
-- ======================================================================================
