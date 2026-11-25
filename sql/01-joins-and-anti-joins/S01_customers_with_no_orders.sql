-- ================================================================================================
-- S01  .  Customers who have never placed an order   (the anti-join)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Joins & Anti-joins
-- DIFFICULTY   : EASY    (1 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- har round ka warm-up SQL; anti-join aur NOT IN trap seedha poochhte hain
-- TECHNIQUE    : Anti-join -- LEFT JOIN ... WHERE right IS NULL, or NOT EXISTS
--
-- PROBLEM
--   List every customer who has no row in orders. Then the same shape for products
--   that were never sold.
--
-- ALGORITHM
--   1. LEFT JOIN keeps every left row and fills the right side with NULLs when there is no match.
--   2. So "no match" is exactly "the right side's key came back NULL".
--   3. Filter on the right table's PRIMARY KEY being NULL, never on a nullable column.
--   4. NOT EXISTS expresses the same thing declaratively and short-circuits on the first hit.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   NOT IN (SELECT cust_id FROM orders)          avoid         returns ZERO ROWS if the subquery
--                                                              yields a single NULL. See S21.
--   LEFT JOIN + IS NULL                          WRITE THIS    universally supported, clear plan
--   NOT EXISTS (correlated)                      WRITE THIS    NULL-safe, usually the best plan
--   EXCEPT / MINUS                               acceptable    clean, but only returns the key
--
-- WHY THIS ONE
--   LEFT JOIN + IS NULL is the version every interviewer recognises instantly, and it is the
--   one you can extend with extra columns from the right table. Say the NOT IN + NULL trap out
--   loud before they ask -- that single sentence is what separates this from a junior answer.
--
-- SAMJHO
--   LEFT JOIN har customer ko rakhta hai; jiska order nahi, uski orders wali side NULL aati hai.
--   Bas wahi rows chahiye: WHERE o.order_id IS NULL. Ya seedha NOT EXISTS likho, same jawab.
--   NOT IN se bacho: subquery mein ek bhi NULL aaya to poora result khaali.
--
-- AISE BOLO  (30-40 sec)
--   "First I'd check whether orders.cust_id can be NULL, because that decides whether NOT IN is
--   even safe. I'd write NOT EXISTS with a correlated subquery, or a LEFT JOIN with an IS NULL
--   test on the orders primary key; both are anti-joins. With an index on orders(cust_id) each
--   customer is one index probe instead of a scan of orders. I avoid NOT IN, because a single
--   NULL in the subquery makes it return zero rows."
--
-- TRAP
--   ! NOT IN (SELECT cust_id FROM orders): cust_id mein ek bhi NULL hai to zero rows, bina error.
--   ! IS NULL right table ki primary key pe lagao. Nullable column pe lagaya to matched rows bhi
--     "missing" dikh sakti hain.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why can NOT IN return zero rows here?
--     -> x NOT IN (1, NULL) ka matlab x <> 1 AND x <> NULL. Doosra hamesha UNKNOWN, to WHERE kabhi
--        TRUE nahi hota. Demo S21 mein.
--   ? Which column must the IS NULL test target, and why not any nullable column?
--     -> Right table ki PK ya join column, jo match hone par kabhi NULL nahi hota. Nullable column
--        match hone par bhi NULL ho sakta hai, to woh row galti se "missing" gin li jaayegi.
--   ? Customers who ordered in January but not in February -- same pattern, restate it.
--     -> January ke orders se JOIN, phir February pe NOT EXISTS (query 4). Is seed pe 0 rows,
--        kyunki Alpha aur Beta dono ne February mein bhi order kiya.
--   ? Which of these three can use an index on orders(cust_id)?
--     -> LEFT JOIN aur NOT EXISTS dono index probe karte hain. NOT IN ko Oracle null-aware
--        anti-join bana leta hai; Postgres nahi bana pata (hashed ya per-row subplan).
--
-- ALSO UNLOCKS
--   - Products never ordered
--   - Users who signed up but never purchased
--   - Orders with no payment row
--   - Missing dates against a date spine
--   - Rows in A missing from B, in general
--
-- ORACLE / POSTGRES MEIN
--   - EXCEPT: Oracle mein MINUS (21c se EXCEPT bhi chalta hai). Postgres mein EXCEPT.
--   - Query 4: wahan order_date DATE hoga. LIKE '2025-01%' mat likho (implicit conversion, index
--     nahi lagega). Range likho:
--     order_date >= DATE '2025-01-01' AND order_date < DATE '2025-02-01'.
--
-- VARIANTS     : LC 183, LC 1581, LC 607
-- ================================================================================================

-- >>> QUERY: customers with no orders (LEFT JOIN + IS NULL)
SELECT c.cust_id, c.cust_name, c.city
FROM   customers c
LEFT   JOIN orders o ON o.cust_id = c.cust_id
WHERE  o.order_id IS NULL          -- test the PRIMARY KEY, never a nullable column
ORDER  BY c.cust_id;

-- >>> QUERY: same answer with NOT EXISTS (NULL-safe, often the better plan)
SELECT c.cust_id, c.cust_name
FROM   customers c
WHERE  NOT EXISTS (SELECT 1 FROM orders o WHERE o.cust_id = c.cust_id)
ORDER  BY c.cust_id;

-- >>> QUERY: products that were never sold
SELECT p.product_id, p.product, p.category
FROM   products p
LEFT   JOIN order_items oi ON oi.product_id = p.product_id
WHERE  oi.product_id IS NULL;

-- >>> QUERY: customers who ordered in January but NOT in February
SELECT DISTINCT c.cust_id, c.cust_name
FROM   customers c
JOIN   orders j ON j.cust_id = c.cust_id AND j.order_date LIKE '2025-01%'
WHERE  NOT EXISTS (SELECT 1 FROM orders f
                   WHERE f.cust_id = c.cust_id AND f.order_date LIKE '2025-02%')
ORDER  BY c.cust_id;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [customers with no orders (LEFT JOIN + IS NULL)]
--   cust_id  cust_name   city
--   -------  ----------  ------
--   5        Epsilon Co  Mumbai
--   (1 row)
--
-- [same answer with NOT EXISTS (NULL-safe, often the better plan)]
--   cust_id  cust_name
--   -------  ----------
--   5        Epsilon Co
--   (1 row)
--
-- [products that were never sold]
--   product_id  product          category
--   ----------  ---------------  --------
--   5           Docking Station  Hardware
--   (1 row)
--
-- [customers who ordered in January but NOT in February]
--   cust_id  cust_name
--   -------  ---------
--   (0 rows)
--
-- ======================================================================================
