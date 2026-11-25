-- ================================================================================================
-- S09  .  Top N highest-paid employees in each department
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Window Functions
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- top N per dept lagbhag har round mein aata hai; tie ka rule pehle bolo
-- TECHNIQUE    : ROW_NUMBER / RANK / DENSE_RANK over PARTITION BY, filtered in an outer query
--
-- PROBLEM
--   Return the two highest-paid employees in every department. Decide explicitly what happens
--   when two people tie on the Nth salary.
--
-- ALGORITHM
--   1. PARTITION BY restarts the numbering for each group -- that is the whole trick.
--   2. ORDER BY inside OVER decides what "first" means within the partition.
--   3. The window function cannot be referenced in WHERE (it is computed after WHERE), so
--      wrap the query in a derived table or CTE and filter there.
--   4. Pick the function by your tie policy: ROW_NUMBER cuts arbitrarily, RANK and DENSE_RANK
--      keep every tied row.
--   5. Add a deterministic tiebreaker to ORDER BY or the "arbitrary" row changes between runs.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Correlated subquery counting bigger salaries avoid         O(n^2), unreadable
--   Self-join + GROUP BY + HAVING                avoid         the pre-window-function workaround
--   ROW_NUMBER() <= N                            WRITE THIS    exactly N rows, ties cut arbitrarily
--   DENSE_RANK() <= N                            WRITE THIS    keeps all ties, may exceed N rows
--   RANK() <= N                                  acceptable    skips numbers after a tie
--
-- WHY THIS ONE
--   The function you pick IS the answer to "what do you do about ties", so state the policy
--   before you write. Sahil and Meera tie at 95000 in Engineering, so the three numbers split
--   in the first query. For top 2 the tie that matters is the two Kabir rows in 2nd place in
--   HR: ROW_NUMBER returns 6 rows, DENSE_RANK 7. A deterministic tiebreaker belongs in ORDER BY.
--
-- SAMJHO
--   PARTITION BY har department ke liye numbering 1 se restart karta hai, aur ORDER BY salary
--   DESC batata hai "top" kaun hai. Phir bahar ki query mein rn <= N filter karo. Function ka
--   choice hi ties ka jawab hai: ROW_NUMBER exactly N rows, DENSE_RANK ties ke saath zyada bhi.
--
-- AISE BOLO  (30-40 sec)
--   "Before writing, I'd ask what should happen on a tie for the Nth salary, because that
--   decides the function. If they want exactly N rows I use ROW_NUMBER with a unique tiebreaker
--   like emp_id; if every tied row should come through I use DENSE_RANK, which can return more
--   than N rows. I number the rows in a CTE partitioned by dept_id and filter outside, because a
--   window function can't be used in WHERE; that is one scan plus a sort, O(n log n), against
--   the O(n^2) correlated count. Employees with a NULL dept_id form their own partition, so I
--   decide whether the join should drop them."
--
-- TRAP
--   ! rn ko usi query ke WHERE mein filter nahi kar sakte -- window WHERE ke baad banta hai.
--     CTE ya derived table mein wrap karo.
--   ! Tiebreaker unique hona chahiye. emp_name unique nahi hai (do Zoya, do Kabir), isliye
--     ROW_NUMBER sach mein deterministic tabhi hai jab ORDER BY mein emp_id bhi ho.
--
-- FOLLOW-UPS KE JAWAB
--   ? Two people tie for 2nd -- do you want 2 rows or 3? Which function gives each?
--     -> 2 rows chahiye to ROW_NUMBER (ek tied row kat jayegi); 3 chahiye to DENSE_RANK ya RANK.
--        HR mein yahi dikhta hai: ROW_NUMBER 2 rows, DENSE_RANK 3 (dono Kabir).
--   ? Why can't you filter on rn directly in the WHERE clause?
--     -> Logical order FROM, WHERE, GROUP BY, HAVING, SELECT hai; window SELECT ke time banta
--        hai, to WHERE ko rn dikhta hi nahi. Isliye CTE ya derived table.
--   ? What does RANK skip, and when does that matter?
--     -> Tie ke baad number skip karta hai: 1,2,3,3,5. Engineering mein "top 4" pe RANK Ishaan
--        (5) ko chhod deta hai, DENSE_RANK (4) use le aata hai.
--   ? Rewrite without window functions -- what is the complexity?
--     -> WHERE (SELECT COUNT(DISTINCT e2.salary) FROM employees e2 WHERE e2.dept_id = e.dept_id
--        AND e2.salary > e.salary) < 2. Har row pe ek scan, O(n^2); dept_id index se kam.
--
-- ALSO UNLOCKS
--   - Latest row per key
--   - Nth highest per group
--   - Top 3 products per category
--   - First order per customer
--
-- ORACLE / POSTGRES MEIN
--   ROW_NUMBER / RANK / DENSE_RANK dono mein same hain. Latest row per key ka Postgres shortcut:
--   SELECT DISTINCT ON (cust_id) ... ORDER BY cust_id, order_date DESC. Oracle 12c+ ka
--   FETCH FIRST n ROWS WITH TIES poori query ka top N deta hai, per group nahi.
--
-- VARIANTS     : LC 185 (department top three salaries), LC 184 (department highest salary)
-- ================================================================================================

-- >>> QUERY: all three functions side by side -- note the 95000 tie in Engineering
SELECT d.dept_name, e.emp_name, e.salary,
       ROW_NUMBER() OVER (PARTITION BY e.dept_id ORDER BY e.salary DESC, e.emp_name) AS rn,
       RANK()       OVER (PARTITION BY e.dept_id ORDER BY e.salary DESC)             AS rnk,
       DENSE_RANK() OVER (PARTITION BY e.dept_id ORDER BY e.salary DESC)             AS dr
FROM   employees e JOIN departments d ON d.dept_id = e.dept_id
ORDER  BY d.dept_name, e.salary DESC, e.emp_name;

-- >>> QUERY: top 2 per department with ROW_NUMBER -- exactly 2 rows, ties cut
WITH ranked AS (
    SELECT d.dept_name, e.emp_name, e.salary,
           ROW_NUMBER() OVER (PARTITION BY e.dept_id
                              ORDER BY e.salary DESC, e.emp_name) AS rn
    FROM   employees e JOIN departments d ON d.dept_id = e.dept_id)
SELECT dept_name, emp_name, salary
FROM   ranked
WHERE  rn <= 2
ORDER  BY dept_name, salary DESC;

-- >>> QUERY: top 2 per department with DENSE_RANK -- ties are KEPT
WITH ranked AS (
    SELECT d.dept_name, e.emp_name, e.salary,
           DENSE_RANK() OVER (PARTITION BY e.dept_id ORDER BY e.salary DESC) AS dr
    FROM   employees e JOIN departments d ON d.dept_id = e.dept_id)
SELECT dept_name, emp_name, salary, dr
FROM   ranked
WHERE  dr <= 2
ORDER  BY dept_name, salary DESC, emp_name;

-- >>> QUERY: latest order per customer -- same pattern, different partition
WITH last_order AS (
    SELECT o.*, ROW_NUMBER() OVER (PARTITION BY cust_id ORDER BY order_date DESC, order_id DESC) rn
    FROM   orders o)
SELECT c.cust_name, l.order_date, l.amount
FROM   last_order l JOIN customers c ON c.cust_id = l.cust_id
WHERE  l.rn = 1
ORDER  BY c.cust_name;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [all three functions side by side -- note the 95000 tie in Engineering]
--   dept_name    emp_name  salary  rn  rnk  dr
--   -----------  --------  ------  --  ---  --
--   Engineering  Rhea      210000  1   1    1
--   Engineering  Arjun     140000  2   2    2
--   Engineering  Meera     95000   3   3    3
--   Engineering  Sahil     95000   4   3    3
--   Engineering  Ishaan    72000   5   5    4
--   Finance      Tara      160000  1   1    1
--   Finance      Vikram    118000  2   2    2
--   Finance      Zoya      88000   3   3    3
--   Finance      Zoya      88000   4   3    3
--   HR           Nikhil    99000   1   1    1
--   HR           Kabir     64000   2   2    2
--   HR           Kabir     64000   3   2    2
--   (12 rows)
--
-- [top 2 per department with ROW_NUMBER -- exactly 2 rows, ties cut]
--   dept_name    emp_name  salary
--   -----------  --------  ------
--   Engineering  Rhea      210000
--   Engineering  Arjun     140000
--   Finance      Tara      160000
--   Finance      Vikram    118000
--   HR           Nikhil    99000
--   HR           Kabir     64000
--   (6 rows)
--
-- [top 2 per department with DENSE_RANK -- ties are KEPT]
--   dept_name    emp_name  salary  dr
--   -----------  --------  ------  --
--   Engineering  Rhea      210000  1
--   Engineering  Arjun     140000  2
--   Finance      Tara      160000  1
--   Finance      Vikram    118000  2
--   HR           Nikhil    99000   1
--   HR           Kabir     64000   2
--   HR           Kabir     64000   2
--   (7 rows)
--
-- [latest order per customer -- same pattern, different partition]
--   cust_name  order_date  amount
--   ---------  ----------  ------
--   Alpha Ltd  2025-03-27  65000
--   Beta Corp  2025-04-06  28000
--   Delta Inc  2025-03-02  16500
--   Gamma LLP  2025-03-14  1800
--   (4 rows)
--
-- ======================================================================================
