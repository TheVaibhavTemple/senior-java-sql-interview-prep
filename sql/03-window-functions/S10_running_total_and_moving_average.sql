-- ================================================================================================
-- S10  .  Running total, moving average, and percent of total
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Window Functions
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- running total / cumulative sum bahut common hai; moving average bonus hai
-- TECHNIQUE    : Aggregate OVER a window frame (ROWS BETWEEN ... AND CURRENT ROW)
--
-- PROBLEM
--   A running total of revenue over time; a 3-row moving average; and each row's share of its
--   category total.
--
-- ALGORITHM
--   1. An aggregate with OVER keeps every input row instead of collapsing them -- that is the
--      difference between a window function and GROUP BY.
--   2. SUM(x) OVER (ORDER BY d) defaults to the frame RANGE UNBOUNDED PRECEDING .. CURRENT ROW.
--   3. That default is RANGE, not ROWS: duplicate ORDER BY values share one frame and get the
--      same running total. State ROWS explicitly when you want strict row-by-row accumulation.
--   4. A moving average is just a narrower frame: ROWS BETWEEN 2 PRECEDING AND CURRENT ROW.
--   5. Percent of total: an aggregate OVER (PARTITION BY ...) with NO ORDER BY spans the
--      whole partition, which is exactly the denominator you want.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Correlated subquery summing earlier rows     avoid         O(n^2); the pre-window workaround
--   Self-join on d2.date <= d1.date              avoid         same cost, worse to read
--   SUM() OVER (ORDER BY ...)                    WRITE THIS    one pass
--   SUM() OVER (... ROWS BETWEEN n PRECEDING)    WRITE THIS    explicit frame, no surprises
--   Application-side accumulation                avoid         ships every row to the client
--
-- WHY THIS ONE
--   The detail that separates a memorised answer from an understood one is RANGE vs ROWS.
--   The default frame is RANGE, so rows that tie on the ORDER BY value all see the same total.
--   Say that, then write ROWS explicitly. Interviewers notice.
--
-- SAMJHO
--   SUM() OVER (ORDER BY ...) har row tak ka total deta hai aur rows collapse nahi hoti --
--   GROUP BY se yahi farak hai. Frame batata hai kaunsi rows judengi: UNBOUNDED PRECEDING =
--   shuru se, 2 PRECEDING = last 3 rows. ORDER BY ke bina window poori partition ka total hai.
--
-- AISE BOLO  (30-40 sec)
--   "I'd first ask what makes the order unique, because two orders on the same date decide
--   which frame I need. A correlated subquery that sums all earlier rows works but is O(n^2),
--   so I use SUM(amount) OVER (ORDER BY order_date, order_id ROWS BETWEEN UNBOUNDED PRECEDING
--   AND CURRENT ROW), which is one pass after a sort. I write ROWS explicitly because the
--   default frame is RANGE, and with RANGE all rows that tie on the ORDER BY value get the same
--   total. For a moving average I narrow the frame to 2 PRECEDING, and for a total that resets
--   per customer I add PARTITION BY."
--
-- TRAP
--   ! Frame na likho to default RANGE hai: same ORDER BY value wali rows ek saath judti hain aur
--     sabko same running total milta hai. ROWS likho, aur ORDER BY mein unique column daalo.
--   ! Moving average ki pehli 2 rows mein 3 rows nahi hoti, AVG kam rows pe hota hai (33750 =
--     2 rows ka average). Strict chahiye to COUNT(*) OVER (same frame) = 3 check karo.
--
-- FOLLOW-UPS KE JAWAB
--   ? What is the DEFAULT frame when you write ORDER BY but no frame clause?
--     -> RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW. ORDER BY bhi na ho to poori
--        partition frame hai.
--   ? Show a case where RANGE and ROWS give different answers.
--     -> Third query: 2025-01-01 ki do rows. RANGE dono ko 30 deta hai, ROWS 10 aur 30.
--   ? Running total that RESETS each month -- where does that go?
--     -> PARTITION BY mein: PARTITION BY substr(order_date,1,7) ORDER BY order_date, order_id.
--        Second query yahi reset per customer karti hai.
--   ? Why does the percent-of-total window have no ORDER BY?
--     -> ORDER BY lagate hi frame running ban jata hai aur denominator har row pe badalta hai.
--        Bina ORDER BY ke frame poori partition hai, yahi total chahiye.
--
-- ALSO UNLOCKS
--   - Cumulative revenue / balance
--   - 7-day rolling active users
--   - Percent of category total
--   - Running count and running max
--
-- ORACLE / POSTGRES MEIN
--   ROWS frames dono mein same hain. Date ke hisaab se window (last 7 days, rows nahi): Oracle
--   RANGE BETWEEN INTERVAL '6' DAY PRECEDING AND CURRENT ROW, Postgres 11+ RANGE BETWEEN
--   INTERVAL '6 days' PRECEDING AND CURRENT ROW (DATE column pe). Third query ka
--   WITH t(d, v) AS (VALUES ...) Oracle 19c mein nahi chalta: SELECT ... FROM dual UNION ALL.
--
-- VARIANTS     : LC 534 (running total per player), LC 1321 (7-day moving average)
-- ================================================================================================

-- >>> QUERY: running total of order revenue, plus a 3-row moving average
SELECT order_date, amount,
       SUM(amount) OVER (ORDER BY order_date, order_id
                         ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total,
       ROUND(AVG(amount) OVER (ORDER BY order_date, order_id
                         ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 0)     AS moving_avg_3
FROM   orders
ORDER  BY order_date, order_id;

-- >>> QUERY: running total that RESETS per customer
SELECT c.cust_name, o.order_date, o.amount,
       SUM(o.amount) OVER (PARTITION BY o.cust_id ORDER BY o.order_date, o.order_id
                           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cust_running_total
FROM   orders o JOIN customers c ON c.cust_id = o.cust_id
ORDER  BY c.cust_name, o.order_date;

-- >>> QUERY: RANGE (the default) vs ROWS -- same data, different answers on ties
WITH t(d, v) AS (VALUES ('2025-01-01',10),('2025-01-01',20),('2025-01-02',30))
SELECT d, v,
       SUM(v) OVER (ORDER BY d)                                                   AS default_range,
       SUM(v) OVER (ORDER BY d ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)  AS explicit_rows
FROM   t;

-- >>> QUERY: each employee's share of their department's payroll (no ORDER BY in the window)
SELECT d.dept_name, e.emp_name, e.salary,
       SUM(e.salary) OVER (PARTITION BY e.dept_id)                                   AS dept_payroll,
       ROUND(100.0 * e.salary / SUM(e.salary) OVER (PARTITION BY e.dept_id), 1)      AS pct_of_dept
FROM   employees e JOIN departments d ON d.dept_id = e.dept_id
ORDER  BY d.dept_name, pct_of_dept DESC;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [running total of order revenue, plus a 3-row moving average]
--   order_date  amount  running_total  moving_avg_3
--   ----------  ------  -------------  ------------
--   2025-01-07  65000   65000          65000.0
--   2025-01-19  2500    67500          33750.0
--   2025-01-23  140000  207500         69167.0
--   2025-02-04  14000   221500         52167.0
--   2025-02-11  3400    224900         52467.0
--   2025-02-18  65000   289900         27467.0
--   2025-03-02  16500   306400         28300.0
--   2025-03-14  1800    308200         27767.0
--   2025-03-27  65000   373200         27767.0
--   2025-04-06  28000   401200         31600.0
--   (10 rows)
--
-- [running total that RESETS per customer]
--   cust_name  order_date  amount  cust_running_total
--   ---------  ----------  ------  ------------------
--   Alpha Ltd  2025-01-07  65000   65000
--   Alpha Ltd  2025-01-19  2500    67500
--   Alpha Ltd  2025-02-11  3400    70900
--   Alpha Ltd  2025-03-27  65000   135900
--   Beta Corp  2025-01-23  140000  140000
--   Beta Corp  2025-02-18  65000   205000
--   Beta Corp  2025-04-06  28000   233000
--   Delta Inc  2025-03-02  16500   16500
--   Gamma LLP  2025-02-04  14000   14000
--   Gamma LLP  2025-03-14  1800    15800
--   (10 rows)
--
-- [RANGE (the default) vs ROWS -- same data, different answers on ties]
--   d           v   default_range  explicit_rows
--   ----------  --  -------------  -------------
--   2025-01-01  10  30             10
--   2025-01-01  20  30             30
--   2025-01-02  30  60             60
--   (3 rows)
--
-- [each employee's share of their department's payroll (no ORDER BY in the window)]
--   dept_name    emp_name  salary  dept_payroll  pct_of_dept
--   -----------  --------  ------  ------------  -----------
--   Engineering  Rhea      210000  612000        34.3
--   Engineering  Arjun     140000  612000        22.9
--   Engineering  Meera     95000   612000        15.5
--   Engineering  Sahil     95000   612000        15.5
--   Engineering  Ishaan    72000   612000        11.8
--   Finance      Tara      160000  454000        35.2
--   Finance      Vikram    118000  454000        26.0
--   Finance      Zoya      88000   454000        19.4
--   Finance      Zoya      88000   454000        19.4
--   HR           Nikhil    99000   227000        43.6
--   HR           Kabir     64000   227000        28.2
--   HR           Kabir     64000   227000        28.2
--   (12 rows)
--
-- ======================================================================================
