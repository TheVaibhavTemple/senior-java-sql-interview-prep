-- ================================================================================================
-- S04  .  A zero-filled grid of every category x every month   (CROSS JOIN)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Joins & Anti-joins
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : MEDIUM  **   common; shows up in most reporting loops
-- PRIORITY     : P3   -- reporting/data flavour; Java backend round mein kabhi kabhi aata hai
-- TECHNIQUE    : CROSS JOIN to build a complete grid, LEFT JOIN the facts onto it
--
-- PROBLEM
--   A monthly sales report must show every category in every month, including the combinations
--   with no sales, as 0 rather than a missing row.
--
-- ALGORITHM
--   1. A GROUP BY only emits rows that exist in the data, so empty combinations vanish.
--   2. Build the complete dimension grid first: distinct categories CROSS JOIN distinct months.
--   3. LEFT JOIN the aggregated facts onto that grid.
--   4. COALESCE the measure to 0 so the gap renders as a zero, not a NULL.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Plain GROUP BY                               avoid         empty combinations disappear
--   Fill the gaps in application code            acceptable    moves the problem, does not solve it
--   CROSS JOIN grid + LEFT JOIN facts            WRITE THIS    one query, complete output
--   Recursive CTE date spine + CROSS JOIN        WRITE THIS    when months must exist with no data
--
-- WHY THIS ONE
--   Reporting questions are really "what happens to the rows that do not exist". Naming the grid
--   as a separate concept -- dimensions first, facts second -- is what an interviewer is listening
--   for. See S13 for the recursive date spine when a whole month has no rows at all.
--
-- SAMJHO
--   GROUP BY sirf wahi combination deta hai jo data mein hai; khaali combination ki row banti hi
--   nahi. Pehle poora grid banao: categories CROSS JOIN months. Phir facts ko grid pe LEFT JOIN
--   karo, aur jahan fact nahi wahan COALESCE(revenue, 0).
--
-- AISE BOLO  (30-40 sec)
--   "I'd first ask where the list of months comes from, because if it comes from orders, a month
--   with no orders at all still disappears. I build the dimension grid first, distinct categories
--   cross joined with months, then LEFT JOIN the pre-aggregated facts and COALESCE the measure to
--   zero. The grid is categories times months rows, so it stays small even when the fact table
--   is large. If a month must appear with no data, I generate months with a date spine instead."
--
-- TRAP
--   ! COALESCE final SELECT mein lagao, facts CTE ke andar nahi: missing combination ki row wahan
--     hai hi nahi, to andar ka COALESCE kuch nahi karta.
--   ! Months orders se nikale to poora khaali month phir bhi gayab rahega. Uske liye date spine.
--
-- FOLLOW-UPS KE JAWAB
--   ? A month has no sales in ANY category -- does this query still show it?
--     -> Nahi. months CTE orders se banta hai, to khaali month list mein hi nahi. Recursive date
--        spine se months banao (S13).
--   ? How many rows does a CROSS JOIN of 5 categories and 12 months produce?
--     -> 5 x 12 = 60. CROSS JOIN hamesha m x n deta hai, isliye sirf chhoti dimension lists pe.
--   ? Where does COALESCE have to go, and what happens if you put it in the wrong place?
--     -> Grid ke LEFT JOIN ke baad outer SELECT mein. facts ke andar lagaya to output mein NULL hi
--        aayega, kyunki NULL LEFT JOIN se aata hai, facts se nahi.
--   ? Same grid, but only for categories active in that month.
--     -> "Active" = us month mein sale hui, to plain GROUP BY (query 1) hi jawab hai. "Active" =
--        launch ho chuki thi, to CROSS JOIN ki jagah JOIN months m ON m.month >= c.start_month.
--
-- ALSO UNLOCKS
--   - Zero-filled time series
--   - Dimension x dimension reporting grids
--   - Finding missing dates (anti-join against a spine)
--   - Cohort tables
--
-- ORACLE / POSTGRES MEIN
--   - substr(order_date, 1, 7): Oracle mein TO_CHAR(order_date, 'YYYY-MM') ya TRUNC(order_date,
--     'MM'); Postgres mein to_char(order_date, 'YYYY-MM') ya date_trunc('month', order_date).
--   - GROUP BY month (SELECT ka alias): Postgres mein chalta hai, Oracle mein 23ai se pehle nahi.
--     Wahan expression repeat karo.
--   - Month spine: Postgres generate_series(DATE '2025-01-01', DATE '2025-04-01',
--     INTERVAL '1 month'); Oracle SELECT ADD_MONTHS(DATE '2025-01-01', LEVEL - 1) FROM dual
--     CONNECT BY LEVEL <= 4.
--   - Query 3 mein FROM nahi hai: Oracle mein 23ai se pehle FROM dual likhna padta hai.
--
-- VARIANTS     : classic -- every store x every day, zero-filled
-- ================================================================================================

-- >>> QUERY: plain GROUP BY -- note the MISSING combinations
SELECT p.category, substr(o.order_date, 1, 7) AS month, SUM(oi.qty * p.unit_price) AS revenue
FROM   orders o
JOIN   order_items oi ON oi.order_id = o.order_id
JOIN   products p     ON p.product_id = oi.product_id
GROUP  BY p.category, month
ORDER  BY p.category, month;

-- >>> QUERY: the complete grid, zero-filled
WITH months AS (SELECT DISTINCT substr(order_date, 1, 7) AS month FROM orders),
     cats   AS (SELECT DISTINCT category FROM products),
     grid   AS (SELECT c.category, m.month FROM cats c CROSS JOIN months m),
     facts  AS (
        SELECT p.category, substr(o.order_date, 1, 7) AS month,
               SUM(oi.qty * p.unit_price) AS revenue
        FROM   orders o
        JOIN   order_items oi ON oi.order_id = o.order_id
        JOIN   products p     ON p.product_id = oi.product_id
        GROUP  BY p.category, month)
SELECT g.category, g.month, COALESCE(f.revenue, 0) AS revenue
FROM   grid g
LEFT   JOIN facts f ON f.category = g.category AND f.month = g.month
ORDER  BY g.category, g.month;

-- >>> QUERY: grid size check -- categories x months
SELECT (SELECT COUNT(DISTINCT category) FROM products)                 AS categories,
       (SELECT COUNT(DISTINCT substr(order_date,1,7)) FROM orders)     AS months,
       (SELECT COUNT(DISTINCT category) FROM products)
     * (SELECT COUNT(DISTINCT substr(order_date,1,7)) FROM orders)     AS grid_rows;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [plain GROUP BY -- note the MISSING combinations]
--   category   month    revenue
--   ---------  -------  -------
--   Accessory  2025-01  2500
--   Accessory  2025-02  3400
--   Accessory  2025-03  4300
--   Hardware   2025-01  209000
--   Hardware   2025-02  79000
--   Hardware   2025-03  79000
--   Hardware   2025-04  28000
--   (7 rows)
--
-- [the complete grid, zero-filled]
--   category   month    revenue
--   ---------  -------  -------
--   Accessory  2025-01  2500
--   Accessory  2025-02  3400
--   Accessory  2025-03  4300
--   Accessory  2025-04  0
--   Hardware   2025-01  209000
--   Hardware   2025-02  79000
--   Hardware   2025-03  79000
--   Hardware   2025-04  28000
--   (8 rows)
--
-- [grid size check -- categories x months]
--   categories  months  grid_rows
--   ----------  ------  ---------
--   2           4       8
--   (1 row)
--
-- ======================================================================================
