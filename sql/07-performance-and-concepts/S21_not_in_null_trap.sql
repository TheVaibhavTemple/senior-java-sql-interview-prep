-- ================================================================================================
-- S21  .  NOT IN with a NULL returns nothing   (the three-valued-logic trap)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Performance & Concepts
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every senior round
-- PRIORITY     : P1   -- NOT IN + NULL senior round ka favourite trick sawaal hai
-- TECHNIQUE    : NOT EXISTS instead of NOT IN; understand UNKNOWN in three-valued logic
--
-- PROBLEM
--   "Find departments with no employees" written with NOT IN returns zero rows. The data is
--   fine. Explain exactly why, and give two correct rewrites.
--
-- ALGORITHM
--   1. NOT IN (a, b, NULL) expands to: x <> a AND x <> b AND x <> NULL.
--   2. x <> NULL is UNKNOWN, never TRUE and never FALSE.
--   3. TRUE AND UNKNOWN is UNKNOWN. WHERE keeps only TRUE rows, so every row is discarded.
--   4. One NULL anywhere in the subquery therefore empties the entire result.
--   5. Fix A: NOT EXISTS -- correlated, and it compares row-by-row, so NULL cannot poison it.
--      Fix B: keep NOT IN but add WHERE col IS NOT NULL to the subquery.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   NOT IN (subquery that can yield NULL)        avoid         silently returns zero rows
--   NOT IN (subquery WHERE col IS NOT NULL)      acceptable    correct, but easy to forget later
--   NOT EXISTS                                   WRITE THIS    NULL-safe by construction
--   LEFT JOIN ... WHERE right_pk IS NULL         WRITE THIS    also NULL-safe, often same plan
--
-- WHY THIS ONE
--   This bug returns no error, no warning and no rows -- a report quietly goes blank. The
--   answer that lands is the expansion: say "NOT IN becomes a chain of <> ANDed together, and
--   anything <> NULL is UNKNOWN, so the whole predicate is UNKNOWN and WHERE drops the row".
--   Then make NOT EXISTS your default and you never meet it again.
--
-- SAMJHO
--   NOT IN (a, b, NULL) ka matlab hai x <> a AND x <> b AND x <> NULL.
--   x <> NULL kabhi TRUE nahi hota, UNKNOWN hota hai. AND mein ek UNKNOWN ho to result kabhi
--   TRUE nahi banta, aur WHERE sirf TRUE rakhta hai. Ek NULL = zero rows.
--   NOT EXISTS sirf match dhoondta hai, NULL usko kharab nahi karta.
--
-- AISE BOLO  (30-40 sec)
--   "Before writing it I'd check whether the subquery column is nullable, because that decides
--   whether NOT IN is safe. NOT IN expands to a chain of not-equals ANDed together, and anything
--   compared with NULL is UNKNOWN, so with one NULL dept_id the predicate is never TRUE for any
--   department and WHERE drops them all, with no error. I write NOT EXISTS, or a LEFT JOIN where
--   the right side's primary key IS NULL; both are NULL-safe, and most optimizers run them as
--   the same anti-join, typically a hash anti-join, roughly O(n + m). NOT IN with an IS NOT
--   NULL filter also works, but it is easy to lose in the next refactor."
--
-- TRAP
--   ! Seed mein Farah ka dept_id NULL hai, isliye NOT IN zero rows deta hai, koi error nahi.
--     Report chupchap khali aati hai.
--   ! LEFT JOIN form mein right table ka PK ya NOT NULL column check karo (e.emp_id IS NULL);
--     koi nullable column check kiya to galat rows aayengi.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why does IN with a NULL still return the matching rows, but NOT IN returns none?
--     -> IN = OR chain: match pe TRUE OR UNKNOWN = TRUE, row aati hai. NOT IN = AND chain:
--        TRUE AND UNKNOWN = UNKNOWN, isliye koi row nahi aati.
--   ? Does the same thing happen with NOT EXISTS? Why not?
--     -> Nahi. NOT EXISTS sirf poochta hai "matching row mili ya nahi"; NULL wali row = se match
--        hi nahi hoti, ignore ho jati hai. Result hamesha TRUE ya FALSE.
--   ? Which of the three has the best plan on a large table?
--     -> NOT EXISTS aur LEFT JOIN/IS NULL dono anti-join bante hain. Oracle 11g+ NOT IN pe bhi
--        null-aware anti join karta hai; Postgres NOT IN ko anti-join nahi bana pata.
--   ? Where else does three-valued logic surprise people? (CHECK constraints, <> comparisons)
--     -> CHECK (amount > 0) NULL ko pass kar deta hai, kyunki UNKNOWN reject nahi hota. Aur
--        WHERE status <> 'FAILED' NULL status wali rows bhi gira deta hai.
--
-- ALSO UNLOCKS
--   - Anti-joins done safely (S01)
--   - NULL semantics across the language
--   - COALESCE and NULLIF
--   - Why = NULL never matches
--
-- ORACLE / POSTGRES MEIN
--   Oracle   : '' (empty string) NULL hi hai. WHERE col <> '' koi row nahi deta: wahi trap.
--   Oracle   : 23ai se pehle SELECT list mein (1 = 1) jaisa boolean nahi chalta. Likho: CASE
--              WHEN c THEN 'TRUE' WHEN NOT c THEN 'FALSE' ELSE 'UNKNOWN' END ... FROM dual.
--              Postgres seedha true / false / NULL dikhata hai.
--   Plan     : Oracle mein NOT IN ka plan HASH JOIN ANTI NA (null-aware). Postgres NOT IN
--              (subquery) hashed subplan banata hai; memory se bada ho to row-by-row, bahut slow.
--
-- VARIANTS     : LC 183 (Customers Who Never Order), LC 584 (Find Customer Referee), LC 608
-- ================================================================================================

-- >>> QUERY: the data -- note that one employee has a NULL dept_id
SELECT COUNT(*) AS employees, COUNT(dept_id) AS with_dept,
       COUNT(*) - COUNT(dept_id) AS with_null_dept FROM employees;

-- >>> QUERY: THE BUG -- departments with no employees, via NOT IN. Returns ZERO rows.
SELECT dept_id, dept_name
FROM   departments
WHERE  dept_id NOT IN (SELECT dept_id FROM employees);

-- >>> QUERY: the truth -- Legal (4) really does have no employees
SELECT dept_id, dept_name
FROM   departments
WHERE  NOT EXISTS (SELECT 1 FROM employees e WHERE e.dept_id = departments.dept_id);

-- >>> QUERY: NOT IN made safe by filtering the NULL out of the subquery
SELECT dept_id, dept_name
FROM   departments
WHERE  dept_id NOT IN (SELECT dept_id FROM employees WHERE dept_id IS NOT NULL);

-- >>> QUERY: and the LEFT JOIN form, also NULL-safe
SELECT d.dept_id, d.dept_name
FROM   departments d LEFT JOIN employees e ON e.dept_id = d.dept_id
WHERE  e.emp_id IS NULL;

-- >>> QUERY: the logic itself -- TRUE, FALSE and UNKNOWN
SELECT (1 = 1)    AS true_is,
       (1 = 2)    AS false_is,
       (1 = NULL) AS unknown_is,
       (1 <> NULL) AS also_unknown,
       CASE WHEN (1 <> NULL) THEN 'kept' ELSE 'dropped' END AS what_where_does;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the data -- note that one employee has a NULL dept_id]
--   employees  with_dept  with_null_dept
--   ---------  ---------  --------------
--   13         12         1
--   (1 row)
--
-- [THE BUG -- departments with no employees, via NOT IN. Returns ZERO rows.]
--   dept_id  dept_name
--   -------  ---------
--   (0 rows)
--
-- [the truth -- Legal (4) really does have no employees]
--   dept_id  dept_name
--   -------  ---------
--   4        Legal
--   (1 row)
--
-- [NOT IN made safe by filtering the NULL out of the subquery]
--   dept_id  dept_name
--   -------  ---------
--   4        Legal
--   (1 row)
--
-- [and the LEFT JOIN form, also NULL-safe]
--   dept_id  dept_name
--   -------  ---------
--   4        Legal
--   (1 row)
--
-- [the logic itself -- TRUE, FALSE and UNKNOWN]
--   true_is  false_is  unknown_is  also_unknown  what_where_does
--   -------  --------  ----------  ------------  ---------------
--   1        0         NULL        NULL          dropped
--   (1 row)
--
-- ======================================================================================
