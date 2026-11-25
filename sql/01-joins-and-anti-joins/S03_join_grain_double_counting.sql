-- ================================================================================================
-- S03  .  Revenue is double-counted after a join -- find and fix it
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Joins & Anti-joins
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  the single most common production SQL bug
-- PRIORITY     : P2   -- product/fintech rounds ka "report ke numbers galat hain" debugging sawaal
-- TECHNIQUE    : Aggregate to a single grain BEFORE joining (pre-aggregation / derived table)
--
-- PROBLEM
--   SUM(o.amount) over orders joined to order_items reports far more revenue than the business
--   actually made. Explain why, then write the correct query.
--
-- ALGORITHM
--   1. Count the rows: orders is one row per order; order_items is one row per LINE.
--   2. The join FANS OUT -- an order with 3 lines contributes its amount 3 times.
--   3. Never SUM a column from the one-side after joining to the many-side.
--   4. Fix A: aggregate the many-side in a derived table first, then join at matching grain.
--   5. Fix B: if you only need the one-side total, do not join the many-side at all.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   SUM(o.amount) across the joined rows         avoid         THE BUG. Inflated by fan-out.
--   SUM(DISTINCT o.amount)                       avoid         silently drops equal amounts
--   Pre-aggregate order_items, then join         WRITE THIS    both grains stay correct
--   Window SUM with a ROW_NUMBER guard           acceptable    works, harder to read
--
-- WHY THIS ONE
--   "Our revenue dashboard is too high" is a common production incident, and fan-out is the
--   usual cause. The sentence that wins the question is: "before I aggregate, I check the grain
--   of every table in the join." Say it before you write anything.
--
-- SAMJHO
--   orders mein ek row = ek order; order_items mein ek row = ek line. Join ke baad har order utni
--   baar aata hai jitni uski lines, aur SUM(o.amount) wahi amount baar baar jodta hai.
--   Fix: pehle many-side ko order_id pe GROUP BY karo, phir 1:1 join. Rule: SUM se pehle grain.
--
-- AISE BOLO  (30-40 sec)
--   "Before I trust any SUM after a join, I check the grain of each table: orders is one row per
--   order, order_items is one row per line. The join fans out, so an order with two lines adds
--   its amount twice; here 401200 becomes 561100. The fix is to pre-aggregate order_items to one
--   row per order_id in a derived table and join one-to-one, which is still one pass over each
--   table. I would not use SUM(DISTINCT amount), because two orders with the same amount collapse."
--
-- TRAP
--   ! SUM(DISTINCT o.amount) fix nahi hai: orders 101, 106 aur 109 teeno 65000 ke hain, DISTINCT
--     unhe ek bana deta hai (271200 aata hai, sahi 401200 hai).
--   ! Pre-aggregate ke baad INNER JOIN kiya to bina lines wale orders gayab. LEFT JOIN + COALESCE.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why is SUM(DISTINCT amount) not a fix?
--     -> DISTINCT values pe chalta hai, rows pe nahi. Do alag orders ka same amount ho to ek hi
--        baar judega, total kam aayega.
--   ? How do you detect fan-out before trusting a number?
--     -> Join ke baad COUNT(*) vs COUNT(DISTINCT o.order_id) dekho; barabar nahi to fan-out hai.
--        Query 1 yahi grain check hai.
--   ? Three tables, each one-to-many -- what is the row count now?
--     -> Har parent ke child counts ka product: 3 lines aur 2 payments wala order 6 rows banega.
--        Do many-sides ko seedha mat jodo, dono ko alag pre-aggregate karke join karo.
--   ? Where would you put this check in a data pipeline?
--     -> Join ke turant baad assertion: grain key pe COUNT(*) = COUNT(DISTINCT key), aur total
--        source total se match. Fail ho to job fail, report publish nahi.
--
-- ALSO UNLOCKS
--   - Any "numbers are too high after a join" bug
--   - Order totals vs line totals reconciliation
--   - Fan-out in star-schema fact joins
--
-- ORACLE / POSTGRES MEIN
--   - LIMIT 6: Oracle 12c+ mein FETCH FIRST 6 ROWS ONLY. Postgres mein LIMIT chalta hai.
--   - "rows" Oracle aur MySQL 8 mein reserved word hai, alias nahi ban sakta. Isliye row_count.
--
-- VARIANTS     : classic -- order header total vs SUM of its line totals
-- ================================================================================================

-- >>> QUERY: the grain of each table (step 1 of the diagnosis)
SELECT 'orders'      AS tbl, COUNT(*) AS row_count FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL SELECT 'joined',      COUNT(*)
          FROM orders o JOIN order_items oi ON oi.order_id = o.order_id;

-- >>> QUERY: THE BUG -- revenue inflated by the fan-out
SELECT SUM(o.amount) AS wrong_total_revenue
FROM   orders o
JOIN   order_items oi ON oi.order_id = o.order_id;

-- >>> QUERY: the truth, from the orders grain alone
SELECT SUM(amount) AS correct_total_revenue FROM orders;

-- >>> QUERY: THE FIX -- pre-aggregate the many-side, then join at matching grain
SELECT o.order_id, o.amount, COALESCE(li.line_count, 0) AS line_count, li.units
FROM   orders o
LEFT   JOIN (SELECT order_id, COUNT(*) AS line_count, SUM(qty) AS units
             FROM   order_items
             GROUP  BY order_id) li ON li.order_id = o.order_id
ORDER  BY o.order_id
LIMIT  6;

-- >>> QUERY: revenue per customer, correct, with line counts attached
SELECT c.cust_name,
       SUM(o.amount)            AS revenue,
       SUM(COALESCE(li.units,0)) AS units
FROM   customers c
JOIN   orders o ON o.cust_id = c.cust_id
LEFT   JOIN (SELECT order_id, SUM(qty) AS units FROM order_items GROUP BY order_id) li
       ON li.order_id = o.order_id
GROUP  BY c.cust_id, c.cust_name
ORDER  BY revenue DESC;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the grain of each table (step 1 of the diagnosis)]
--   tbl          row_count
--   -----------  ---------
--   orders       10
--   order_items  13
--   joined       13
--   (3 rows)
--
-- [THE BUG -- revenue inflated by the fan-out]
--   wrong_total_revenue
--   -------------------
--   561100
--   (1 row)
--
-- [the truth, from the orders grain alone]
--   correct_total_revenue
--   ---------------------
--   401200
--   (1 row)
--
-- [THE FIX -- pre-aggregate the many-side, then join at matching grain]
--   order_id  amount  line_count  units
--   --------  ------  ----------  -----
--   101       65000   1           1
--   102       2500    1           1
--   103       140000  2           3
--   104       14000   1           1
--   105       3400    2           2
--   106       65000   1           1
--   (6 rows)
--
-- [revenue per customer, correct, with line counts attached]
--   cust_name  revenue  units
--   ---------  -------  -----
--   Beta Corp  233000   6
--   Alpha Ltd  135900   5
--   Delta Inc  16500    2
--   Gamma LLP  15800    3
--   (4 rows)
--
-- ======================================================================================
