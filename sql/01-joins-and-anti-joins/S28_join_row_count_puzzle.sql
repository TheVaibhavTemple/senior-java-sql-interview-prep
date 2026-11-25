-- ================================================================================================
-- S28  .  Join row-count puzzle, and ON vs WHERE on a LEFT JOIN
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Joins & Anti-joins
-- DIFFICULTY   : EASY-MEDIUM  (1.5 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round, often verbally with no computer
-- PRIORITY     : P1   -- service company aur GCC rounds mein paper pe poochte hain, bina laptop
-- TECHNIQUE    : Per key, t1 copies x t2 copies; NULL never matches; an outer join adds its
--                unmatched rows once. On a LEFT JOIN, right-table filters go in ON, not WHERE
--
-- PROBLEM
--   t1(id) = 1, 1, 2, NULL, 3   and   t2(id) = 1, 2, 2, NULL, NULL, 4.
--   Without running anything: how many rows do INNER, LEFT, RIGHT, FULL OUTER and CROSS JOIN
--   on t1.id = t2.id return? Then: "every customer with their March 2025 orders", written as a
--   LEFT JOIN with the date test in WHERE, loses two customers. Why, and what is the fix?
--
-- ALGORITHM
--   1. For each key on both sides, multiply its copies: rows = copies in t1 x copies in t2.
--      id 1: 2 x 1 = 2.   id 2: 1 x 2 = 2.   INNER = 4.
--   2. NULL = NULL is UNKNOWN, not TRUE, so a NULL key matches nothing, not even another NULL.
--   3. An outer join adds each unmatched row of its preserved side once, padded with NULLs:
--        LEFT  = 4 + unmatched t1 rows (NULL, 3)         = 4 + 2      = 6
--        RIGHT = 4 + unmatched t2 rows (NULL, NULL, 4)   = 4 + 3      = 7
--        FULL  = 4 + both unmatched sets                 = 4 + 2 + 3  = 9   (LEFT + RIGHT - INNER)
--        CROSS = every pair, no ON clause                = 5 x 6      = 30
--   4. WHERE runs after the join. A WHERE test on a right-table column removes every row that
--      fails it, NULL-padded rows included, so a left row with no passing match disappears:
--      the LEFT JOIN has silently become an INNER JOIN. Put that test in ON instead.
--   5. The exception: WHERE right.pk IS NULL is TRUE exactly on the padded rows. That is the
--      anti-join (S01), and it belongs in WHERE.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Multiply the table sizes (5 x 6)             avoid         that is only the CROSS JOIN
--   List all 30 pairs and tick the equal ones    acceptable    correct, too slow on paper
--   Per key: copies x copies, then add the       WRITE THIS    a minute on paper, any join type
--     unmatched rows of each preserved side
--   LEFT JOIN, right-table filter in WHERE       avoid         THE BUG: silently an INNER JOIN
--   LEFT JOIN, right-table filter in ON          WRITE THIS    every left row stays
--   Filter the right table in a derived table,   acceptable    same rows as ON, more to read
--     then LEFT JOIN to it
--
-- WHY THIS ONE
--   The puzzle tests three facts in two minutes: duplicate keys multiply, NULL never equals
--   NULL, and an outer join adds back only the rows that found no partner. Saying the per-key
--   formula out loud is the answer. ON vs WHERE is the same idea in production: a report that
--   must list every customer quietly drops the ones with no matching row, with no error.
--
-- SAMJHO
--   Har common key pe dono taraf ki copies multiply karo: id 1 = 2 x 1, id 2 = 1 x 2, INNER = 4.
--   NULL kisi se match nahi karta, NULL se bhi nahi. Outer join har unmatched row ek baar jodta
--   hai: LEFT 6, RIGHT 7, FULL 9. CROSS = 5 x 6 = 30.
--   LEFT JOIN ke right table ka filter WHERE mein gaya to INNER JOIN ban jaata hai; ON mein daalo.
--
-- AISE BOLO  (30-40 sec)
--   "Listing all 30 pairs of the cross product would work, but I'd count key by key instead.
--   Id 1 is twice in t1 and once in t2, so 2 rows, and id 2 is once and twice, so 2 more; INNER
--   is 4, and the NULLs add nothing because NULL equals NULL is unknown, not true. LEFT adds the
--   two t1 rows with no partner, NULL and 3, so 6; RIGHT adds NULL, NULL and 4, so 7; FULL adds
--   both, so 9; and CROSS is 5 times 6, so 30. On a LEFT JOIN I keep right-table filters in ON,
--   because in WHERE they throw away the unmatched customers and the query becomes an inner
--   join; only an IS NULL test on the right key belongs in WHERE, and that is the anti-join."
--
-- TRAP
--   ! NULL = NULL TRUE nahi, UNKNOWN hai. Dono table mein NULL hai phir bhi INNER mein unki koi
--     row nahi; outer join mein har NULL row akeli, bina partner ke, ek baar aati hai.
--   ! LEFT JOIN ke baad WHERE o.order_date >= ... lagaya to bina March order wale customers
--     gayab, koi error nahi (query 15 vs 16). Date test ON mein daalo.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why is NULL = NULL not TRUE?
--     -> NULL matlab "value pata nahi". Do unknown barabar hain ya nahi, SQL nahi jaanta, to
--        UNKNOWN; ON aur WHERE sirf TRUE rakhte hain. Par DISTINCT aur GROUP BY saare NULL ek
--        group maante hain.
--   ? How do DISTINCT or a UNIQUE key on t2.id change the counts?
--     -> Right side pe har key ek hi baar ho to kuch multiply nahi hota: INNER 3, LEFT 5 = t1 ki
--        rows (query 12, 13). Unique key pe LEFT JOIN left table ki row count kabhi nahi badhata.
--   ? JOIN vs IN vs EXISTS -- same number of rows?
--     -> Nahi. JOIN multiply karta hai: 4. IN aur EXISTS semi-join hain, har t1 row max ek baar:
--        3 (1, 1, 2); t1 ke apne duplicates rehte hain. NOT IN yahan 0 (S21), NOT EXISTS 2.
--   ? How do you sanity-check a join's row count before you aggregate?
--     -> S03 wala grain check: join se pehle aur baad COUNT(*). LEFT JOIN ke baad 5 se 6 hua to
--        right key duplicate hai; GROUP BY id HAVING COUNT(*) > 1 se pakdo (query 13, 14).
--   ? Does ON vs WHERE matter on an INNER JOIN, or for a filter on the left table?
--     -> INNER JOIN mein nahi, result same. LEFT JOIN mein left table ki condition ON mein daali
--        to left rows filter nahi hoti, bas right side NULL aati hai; left ka filter WHERE mein.
--
-- ALSO UNLOCKS
--   - Any "how many rows will this join return" question, asked on paper
--   - Fan-out before a SUM (S03)
--   - Anti-join with LEFT JOIN + IS NULL (S01), and NOT IN with NULLs (S21)
--   - "Every customer with their X, including zero" reports
--   - FULL OUTER JOIN reconciliation (S24)
--
-- ORACLE / POSTGRES MEIN
--   - Oracle ka purana syntax: FROM t1, t2 WHERE t1.id = t2.id(+) matlab t1 LEFT JOIN t2. Right
--     table ke filter pe bhi (+) lagao (o.order_date(+) >= ...), warna wahi INNER JOIN wala bug.
--   - INSERT ... VALUES (1), (1), ...: Postgres mein chalta hai; Oracle mein 23ai se pehle
--     nahi, har row ka alag INSERT likho.
--   - order_date wahan DATE hoga: DATE '2025-03-01' likho, string nahi.
--
-- VARIANTS     : classic -- the verbal "how many rows" join puzzle; ON vs WHERE on a LEFT JOIN
-- ================================================================================================

-- >>> QUERY: setup -- t1(id) = 1, 1, 2, NULL, 3
-- One statement per block: run_all.py hands each block to execute(), which runs only one.
CREATE TABLE t1 (id INTEGER);

-- >>> QUERY: setup -- fill t1
INSERT INTO t1 (id) VALUES (1), (1), (2), (NULL), (3);

-- >>> QUERY: setup -- t2(id) = 1, 2, 2, NULL, NULL, 4
CREATE TABLE t2 (id INTEGER);

-- >>> QUERY: setup -- fill t2
INSERT INTO t2 (id) VALUES (1), (2), (2), (NULL), (NULL), (4);

-- >>> QUERY: INNER JOIN -- 4 rows
-- id 1: 2 copies in t1 x 1 in t2 = 2.  id 2: 1 x 2 = 2.  NULL, 3 and 4 find no partner.
-- 2 + 2 = 4.
SELECT COUNT(*) AS inner_rows FROM t1 INNER JOIN t2 ON t2.id = t1.id;

-- >>> QUERY: LEFT JOIN -- 6 rows
-- INNER 4, plus each t1 row with no match, once, with t2.id NULL: NULL and 3.  4 + 2 = 6.
SELECT COUNT(*) AS left_rows FROM t1 LEFT JOIN t2 ON t2.id = t1.id;

-- >>> QUERY: RIGHT JOIN -- 7 rows
-- INNER 4, plus each t2 row with no match: NULL, NULL and 4.  4 + 3 = 7.
-- SQLite has RIGHT and FULL OUTER JOIN only since 3.39 (2022); this file runs on 3.50.
SELECT COUNT(*) AS right_rows FROM t1 RIGHT JOIN t2 ON t2.id = t1.id;

-- >>> QUERY: FULL OUTER JOIN -- 9 rows
-- INNER 4 + unmatched t1 (2) + unmatched t2 (3) = 9.  Check: LEFT + RIGHT - INNER = 6 + 7 - 4.
SELECT COUNT(*) AS full_rows FROM t1 FULL OUTER JOIN t2 ON t2.id = t1.id;

-- >>> QUERY: CROSS JOIN -- 30 rows
-- No ON clause: every t1 row with every t2 row, NULLs included.  5 x 6 = 30.
SELECT COUNT(*) AS cross_rows FROM t1 CROSS JOIN t2;

-- >>> QUERY: the 4 INNER JOIN rows -- id 1 twice (two t1 copies), id 2 twice (two t2 copies)
SELECT t1.id AS t1_id, t2.id AS t2_id
FROM   t1
JOIN   t2 ON t2.id = t1.id
ORDER  BY t1.id;

-- >>> QUERY: the 6 LEFT JOIN rows -- t1's 3 and NULL come back once each, with t2_id NULL
-- That t2_id NULL is padding for "no match", not t2's own NULL row.
-- NULLS LAST so the order is the same on SQLite, Oracle and Postgres.
SELECT t1.id AS t1_id, t2.id AS t2_id
FROM   t1
LEFT   JOIN t2 ON t2.id = t1.id
ORDER  BY t1.id NULLS LAST;

-- >>> QUERY: JOIN vs IN vs EXISTS -- a semi-join never multiplies
-- JOIN 4.  JOIN to DISTINCT t2: 3.  IN and EXISTS: 3, each t1 row at most once (1, 1, 2).
-- NOT IN: 0, t2 has a NULL so "3 NOT IN (...)" is UNKNOWN (S21).  NOT EXISTS: 2 (NULL and 3).
SELECT 'JOIN' AS form, COUNT(*) AS row_count FROM t1 JOIN t2 ON t2.id = t1.id
UNION ALL
SELECT 'JOIN to DISTINCT t2', COUNT(*)
FROM   t1 JOIN (SELECT DISTINCT id FROM t2) d ON d.id = t1.id
UNION ALL
SELECT 'IN', COUNT(*) FROM t1 WHERE t1.id IN (SELECT id FROM t2)
UNION ALL
SELECT 'EXISTS', COUNT(*) FROM t1 WHERE EXISTS (SELECT 1 FROM t2 WHERE t2.id = t1.id)
UNION ALL
SELECT 'NOT IN', COUNT(*) FROM t1 WHERE t1.id NOT IN (SELECT id FROM t2)
UNION ALL
SELECT 'NOT EXISTS', COUNT(*) FROM t1 WHERE NOT EXISTS (SELECT 1 FROM t2 WHERE t2.id = t1.id);

-- >>> QUERY: grain check before you aggregate -- rows in vs rows out of the LEFT JOIN
-- A LEFT JOIN returns at least the left table's rows. More rows out than in means fan-out:
-- a key repeats on the right. Dedupe the right side, or join on a unique key: back to 5.
SELECT 't1 (rows in)' AS step, COUNT(*) AS row_count FROM t1
UNION ALL
SELECT 't1 LEFT JOIN t2', COUNT(*) FROM t1 LEFT JOIN t2 ON t2.id = t1.id
UNION ALL
SELECT 't1 LEFT JOIN DISTINCT t2', COUNT(*)
FROM   t1 LEFT JOIN (SELECT DISTINCT id FROM t2) d ON d.id = t1.id;

-- >>> QUERY: which right-side key fans out -- id 2 is in t2 twice
-- NULL keys are left out: GROUP BY puts both NULLs in one group, but a NULL never matches.
SELECT id, COUNT(*) AS copies
FROM   t2
WHERE  id IS NOT NULL
GROUP  BY id
HAVING COUNT(*) > 1;

-- >>> QUERY: THE BUG -- right-table filter in WHERE: 3 rows, Beta and Epsilon are gone
-- Part 2, main schema. Customers 1-5. March 2025 orders: 109 Alpha, 108 Gamma, 107 Delta.
-- WHERE runs after the join. Beta's orders are in January, February and April, so all three
-- rows fail the test; Epsilon's NULL-padded row is UNKNOWN. Same rows as an INNER JOIN.
SELECT c.cust_id, c.cust_name, o.order_id, o.order_date
FROM   customers c
LEFT   JOIN orders o ON o.cust_id = c.cust_id
WHERE  o.order_date >= '2025-03-01' AND o.order_date < '2025-04-01'
ORDER  BY c.cust_id;

-- >>> QUERY: THE FIX -- same filter in ON: all 5 customers, NULL where there is no March order
-- In ON the date test is part of the match. Beta and Epsilon match nothing, so the LEFT JOIN
-- keeps each of them once, with NULLs on the orders side.
SELECT c.cust_id, c.cust_name, o.order_id, o.order_date
FROM   customers c
LEFT   JOIN orders o ON  o.cust_id = c.cust_id
                     AND o.order_date >= '2025-03-01' AND o.order_date < '2025-04-01'
ORDER  BY c.cust_id;

-- >>> QUERY: March orders per customer -- COUNT(o.order_id), not COUNT(*)
-- COUNT(*) counts the NULL-padded row as 1, so Beta and Epsilon would show 1 order each.
SELECT c.cust_id, c.cust_name,
       COUNT(*)          AS wrong_count,
       COUNT(o.order_id) AS march_orders
FROM   customers c
LEFT   JOIN orders o ON  o.cust_id = c.cust_id
                     AND o.order_date >= '2025-03-01' AND o.order_date < '2025-04-01'
GROUP  BY c.cust_id, c.cust_name
ORDER  BY c.cust_id;

-- >>> QUERY: the exception -- WHERE o.order_id IS NULL is the anti-join, and it is correct
-- Customers with no March order. The date test stays in ON; the IS NULL test on the right
-- table's PK goes in WHERE. Move the date test to WHERE as well and you get 0 rows, because
-- a padded row has no date.
SELECT c.cust_id, c.cust_name
FROM   customers c
LEFT   JOIN orders o ON  o.cust_id = c.cust_id
                     AND o.order_date >= '2025-03-01' AND o.order_date < '2025-04-01'
WHERE  o.order_id IS NULL
ORDER  BY c.cust_id;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [setup -- t1(id) = 1, 1, 2, NULL, 3]
--   (no result set)
--
-- [setup -- fill t1]
--   (no result set)
--
-- [setup -- t2(id) = 1, 2, 2, NULL, NULL, 4]
--   (no result set)
--
-- [setup -- fill t2]
--   (no result set)
--
-- [INNER JOIN -- 4 rows]
--   inner_rows
--   ----------
--   4
--   (1 row)
--
-- [LEFT JOIN -- 6 rows]
--   left_rows
--   ---------
--   6
--   (1 row)
--
-- [RIGHT JOIN -- 7 rows]
--   right_rows
--   ----------
--   7
--   (1 row)
--
-- [FULL OUTER JOIN -- 9 rows]
--   full_rows
--   ---------
--   9
--   (1 row)
--
-- [CROSS JOIN -- 30 rows]
--   cross_rows
--   ----------
--   30
--   (1 row)
--
-- [the 4 INNER JOIN rows -- id 1 twice (two t1 copies), id 2 twice (two t2 copies)]
--   t1_id  t2_id
--   -----  -----
--   1      1
--   1      1
--   2      2
--   2      2
--   (4 rows)
--
-- [the 6 LEFT JOIN rows -- t1's 3 and NULL come back once each, with t2_id NULL]
--   t1_id  t2_id
--   -----  -----
--   1      1
--   1      1
--   2      2
--   2      2
--   3      NULL
--   NULL   NULL
--   (6 rows)
--
-- [JOIN vs IN vs EXISTS -- a semi-join never multiplies]
--   form                 row_count
--   -------------------  ---------
--   JOIN                 4
--   JOIN to DISTINCT t2  3
--   IN                   3
--   EXISTS               3
--   NOT IN               0
--   NOT EXISTS           2
--   (6 rows)
--
-- [grain check before you aggregate -- rows in vs rows out of the LEFT JOIN]
--   step                      row_count
--   ------------------------  ---------
--   t1 (rows in)              5
--   t1 LEFT JOIN t2           6
--   t1 LEFT JOIN DISTINCT t2  5
--   (3 rows)
--
-- [which right-side key fans out -- id 2 is in t2 twice]
--   id  copies
--   --  ------
--   2   2
--   (1 row)
--
-- [THE BUG -- right-table filter in WHERE: 3 rows, Beta and Epsilon are gone]
--   cust_id  cust_name  order_id  order_date
--   -------  ---------  --------  ----------
--   1        Alpha Ltd  109       2025-03-27
--   3        Gamma LLP  108       2025-03-14
--   4        Delta Inc  107       2025-03-02
--   (3 rows)
--
-- [THE FIX -- same filter in ON: all 5 customers, NULL where there is no March order]
--   cust_id  cust_name   order_id  order_date
--   -------  ----------  --------  ----------
--   1        Alpha Ltd   109       2025-03-27
--   2        Beta Corp   NULL      NULL
--   3        Gamma LLP   108       2025-03-14
--   4        Delta Inc   107       2025-03-02
--   5        Epsilon Co  NULL      NULL
--   (5 rows)
--
-- [March orders per customer -- COUNT(o.order_id), not COUNT(*)]
--   cust_id  cust_name   wrong_count  march_orders
--   -------  ----------  -----------  ------------
--   1        Alpha Ltd   1            1
--   2        Beta Corp   1            0
--   3        Gamma LLP   1            1
--   4        Delta Inc   1            1
--   5        Epsilon Co  1            0
--   (5 rows)
--
-- [the exception -- WHERE o.order_id IS NULL is the anti-join, and it is correct]
--   cust_id  cust_name
--   -------  ----------
--   2        Beta Corp
--   5        Epsilon Co
--   (2 rows)
--
-- ======================================================================================
