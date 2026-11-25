-- ================================================================================================
-- S23  .  Employees earning more than their department average
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Window Functions
-- DIFFICULTY   : EASY-MEDIUM  (1.5 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- India ke lagbhag har SQL round mein aata hai; teeno versions yaad rakho
-- TECHNIQUE    : Compare each row with its group aggregate -- correlated subquery, AVG() OVER
--                (PARTITION BY), or JOIN to a GROUP BY derived table
--
-- PROBLEM
--   List every employee whose salary is greater than the average salary of their own
--   department. Then: employees above the company-wide average instead.
--
-- ALGORITHM
--   1. Two grains meet here: one row per employee against one number per department. Get the
--      department average first, then compare row by row.
--   2. Correlated subquery: for each employee, AVG(salary) over the rows with the same dept_id.
--   3. Window: AVG(salary) OVER (PARTITION BY dept_id) puts the average on every row without
--      collapsing rows. It cannot be used in WHERE -- window functions are computed after
--      WHERE -- so wrap it in a derived table or CTE and filter outside.
--   4. JOIN: GROUP BY dept_id in a derived table, join it back on dept_id, compare in WHERE.
--   5. Use AVG(), not SUM / COUNT(*), and decide ">" vs ">=" and the no-department rule first.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Correlated subquery in WHERE                 WRITE THIS    reads like English; lookup per row
--   AVG() OVER (PARTITION BY) + outer filter     WRITE THIS    one scan, average on every row
--   JOIN to a GROUP BY derived table             WRITE THIS    two passes, works without windows
--   GROUP BY ... HAVING salary > AVG(salary)     wrong         HAVING sees groups, not rows
--   SUM(salary) / COUNT(*) as the average        wrong         truncates; COUNT(*) counts NULLs
--
-- WHY THIS ONE
--   It is the smallest question that checks whether you can hold two grains at once. Write the
--   correlated subquery first because it reads exactly like the question, then offer the window
--   version as the one-scan answer that can also show the average next to each row. The JOIN
--   version shows you know what the window replaces. All three return the same 5 rows below.
--
-- SAMJHO
--   Har employee ko apne hi department ke average se compare karna hai. Average dept grain pe
--   hai, employee row grain pe -- pehle average nikalo, phir har row se compare karo. Teen
--   tarike: correlated subquery, AVG() OVER (PARTITION BY dept_id), ya GROUP BY wala JOIN.
--
-- AISE BOLO  (30-40 sec)
--   "First I'd confirm two things: strictly greater or greater-or-equal, and whether employees
--   without a department count. The direct answer is a correlated subquery that takes AVG for
--   the same dept_id; it reads like the question, but it is one lookup per row, so it needs an
--   index on dept_id. My default is AVG(salary) OVER (PARTITION BY dept_id) in a derived table,
--   filtered outside, because it is one scan plus a sort and it can show the average on every
--   row. One edge case: a department with one employee never qualifies, since nobody is above
--   their own average."
--
-- TRAP
--   ! Window function WHERE mein nahi chalta -- WHERE pehle evaluate hota hai, window baad mein.
--     Derived table ya CTE mein wrap karo, bahar filter karo.
--   ! SUM(salary) / COUNT(*) SQLite aur PostgreSQL mein integer division hai (HR: 75666, sahi
--     75666.67), aur COUNT(*) NULL salary wali row bhi ginta hai. Seedha AVG() likho.
--
-- FOLLOW-UPS KE JAWAB
--   ? What does the correlated subquery cost compared to the window version?
--     -> Correlated: har row pe us dept ka AVG -- index ho to sirf us dept ki rows, warna O(n^2).
--        Window: ek scan + dept_id pe sort, O(n log n). Oracle correlated ko JOIN mein unnest
--        bhi kar sakta hai, isliye plan dekh ke bolo.
--   ? Which version is the easiest to read, and which one would you ship?
--     -> Padhne mein correlated sabse seedha hai. Ship window wala karunga: ek scan, aur average
--        column bhi saath mein mil jata hai.
--   ? Show the department average next to every employee, not only the filtered rows.
--     -> Version 2 ka inner query akela chalao -- dept_avg har row pe hai. Version 4 mein
--        company_avg aur dept_avg dono dikh rahe hain.
--   ? A department has only one employee -- what happens?
--     -> Uski salary hi average hai, ">" kabhi true nahi hoga, row nahi aayegi; ">=" ho to hamesha
--        aayegi. Aise dept hatane hain to COUNT(*) OVER (PARTITION BY dept_id) > 1 lagao.
--   ? An employee has no department (dept_id NULL) -- do the three versions still agree?
--     -> Nahi. "=" NULL ko match nahi karta, to correlated aur JOIN use chhod dete hain, par
--        PARTITION BY saare NULLs ko ek group bana deta hai. Trap 5c: departments JOIN ke bina
--        Farah (130000 > 110000) window version mein aa jaati. Rule pehle poochho.
--
-- ALSO UNLOCKS
--   - Above the company-wide average (AVG() OVER ())
--   - Products priced above their category average
--   - Customers spending above their city average
--   - Payments above the customer's own average (a simple anomaly flag)
--
-- ORACLE / POSTGRES MEIN
--   Teeno versions dono mein same chalte hain. Oracle mein derived table alias ke saath AS nahi
--   likhte ("FROM (...) x"), isliye yahan AS nahi hai. Division: Oracle ka "/" NUMBER deta hai,
--   truncate nahi karta; PostgreSQL mein bigint / bigint truncate karta hai, 1.0 * lagao.
--   Postgres mein AVG(integer) NUMERIC deta hai. Trap 5b/5c ke VALUES aur FROM-less SELECT
--   Oracle 19c mein "SELECT ... FROM dual UNION ALL" se likho.
--
-- APNI STORY SE JODO
--   "On a daily reconciliation query over an Oracle table of more than 10 million rows, the
--   EXPLAIN plan showed a full table scan and the fix was a missing index: 45 minutes to 8
--   seconds. So for the correlated version my first check is the plan and the dept_id index."
--   Note: sirf tab bolo jab baat cost ya EXPLAIN plan pe aaye; woh query yeh wali nahi thi.
--
-- VARIANTS     : LC 615 (department average vs company average), LC 184 (department highest
--                salary); classic: above the average of the OTHER employees in the same dept
-- ================================================================================================

-- >>> QUERY: version 1 -- correlated subquery (reads exactly like the question)
SELECT e.emp_id, e.emp_name, d.dept_name, e.salary
FROM   employees e
JOIN   departments d ON d.dept_id = e.dept_id
WHERE  e.salary > (SELECT AVG(e2.salary)
                   FROM   employees e2
                   WHERE  e2.dept_id = e.dept_id)
ORDER  BY d.dept_name, e.salary DESC, e.emp_id;

-- >>> QUERY: version 2 -- AVG() OVER (PARTITION BY) in a derived table, filtered outside
SELECT emp_id, emp_name, dept_name, salary
FROM  (SELECT e.emp_id, e.emp_name, d.dept_name, e.salary,
              AVG(e.salary) OVER (PARTITION BY e.dept_id) AS dept_avg
       FROM   employees e
       JOIN   departments d ON d.dept_id = e.dept_id) x
WHERE  salary > dept_avg          -- windows are computed after WHERE, so this filter is outside
ORDER  BY dept_name, salary DESC, emp_id;

-- >>> QUERY: version 3 -- JOIN to a GROUP BY derived table (works even without window functions)
SELECT e.emp_id, e.emp_name, d.dept_name, e.salary
FROM   employees e
JOIN  (SELECT dept_id, AVG(salary) AS avg_salary
       FROM   employees
       GROUP  BY dept_id) a ON a.dept_id = e.dept_id
JOIN   departments d ON d.dept_id = e.dept_id
WHERE  e.salary > a.avg_salary
ORDER  BY d.dept_name, e.salary DESC, e.emp_id;

-- >>> QUERY: version 4 -- above the COMPANY-wide average, both averages shown on every row
SELECT emp_id, emp_name, dept_name, salary,
       ROUND(company_avg, 2) AS company_avg, ROUND(dept_avg, 2) AS dept_avg
FROM  (SELECT e.emp_id, e.emp_name, COALESCE(d.dept_name, '(none)') AS dept_name, e.salary,
              AVG(e.salary) OVER ()                       AS company_avg,   -- empty OVER()
              AVG(e.salary) OVER (PARTITION BY e.dept_id) AS dept_avg
       FROM   employees e
       LEFT   JOIN departments d ON d.dept_id = e.dept_id) x   -- LEFT: Farah is in the company
WHERE  salary > company_avg       -- compare the unrounded value; round only for display
ORDER  BY salary DESC, emp_id;

-- >>> QUERY: trap 5a -- SUM / COUNT(*) is INTEGER division in SQLite and PostgreSQL (look at HR)
SELECT d.dept_name,
       SUM(e.salary) / COUNT(*)                 AS int_division,
       ROUND(1.0 * SUM(e.salary) / COUNT(*), 2) AS fixed_with_1_0,
       ROUND(AVG(e.salary), 2)                  AS avg_salary
FROM   employees e
JOIN   departments d ON d.dept_id = e.dept_id
GROUP  BY d.dept_id, d.dept_name
ORDER  BY d.dept_name;

-- >>> QUERY: trap 5b -- a NULL salary: AVG skips it, COUNT(*) still counts it
WITH t(salary) AS (VALUES (100), (201), (NULL))
SELECT AVG(salary)                       AS avg_salary,
       SUM(salary) / COUNT(*)            AS sum_div_count_star,
       SUM(salary) / COUNT(salary)       AS sum_div_count_col,
       1.0 * SUM(salary) / COUNT(salary) AS same_as_avg
FROM   t;

-- >>> QUERY: trap 5c -- NULL dept_id: PARTITION BY groups the NULLs, = never matches them
WITH emp AS (
    SELECT emp_id, emp_name, dept_id, salary FROM employees
    UNION ALL
    SELECT 99, 'Dev', NULL, 90000               -- hypothetical second employee with no dept
)
SELECT e.emp_name, e.salary,
       (SELECT AVG(e2.salary) FROM emp e2 WHERE e2.dept_id = e.dept_id) AS correlated_avg,
       AVG(e.salary) OVER (PARTITION BY e.dept_id)                      AS window_avg
FROM   emp e
WHERE  e.dept_id IS NULL
ORDER  BY e.emp_id;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [version 1 -- correlated subquery (reads exactly like the question)]
--   emp_id  emp_name  dept_name    salary
--   ------  --------  -----------  ------
--   1       Rhea      Engineering  210000
--   2       Arjun     Engineering  140000
--   6       Tara      Finance      160000
--   7       Vikram    Finance      118000
--   10      Nikhil    HR           99000
--   (5 rows)
--
-- [version 2 -- AVG() OVER (PARTITION BY) in a derived table, filtered outside]
--   emp_id  emp_name  dept_name    salary
--   ------  --------  -----------  ------
--   1       Rhea      Engineering  210000
--   2       Arjun     Engineering  140000
--   6       Tara      Finance      160000
--   7       Vikram    Finance      118000
--   10      Nikhil    HR           99000
--   (5 rows)
--
-- [version 3 -- JOIN to a GROUP BY derived table (works even without window functions)]
--   emp_id  emp_name  dept_name    salary
--   ------  --------  -----------  ------
--   1       Rhea      Engineering  210000
--   2       Arjun     Engineering  140000
--   6       Tara      Finance      160000
--   7       Vikram    Finance      118000
--   10      Nikhil    HR           99000
--   (5 rows)
--
-- [version 4 -- above the COMPANY-wide average, both averages shown on every row]
--   emp_id  emp_name  dept_name    salary  company_avg  dept_avg
--   ------  --------  -----------  ------  -----------  --------
--   1       Rhea      Engineering  210000  109461.54    122400.0
--   6       Tara      Finance      160000  109461.54    113500.0
--   2       Arjun     Engineering  140000  109461.54    122400.0
--   13      Farah     (none)       130000  109461.54    130000.0
--   7       Vikram    Finance      118000  109461.54    113500.0
--   (5 rows)
--
-- [trap 5a -- SUM / COUNT(*) is INTEGER division in SQLite and PostgreSQL (look at HR)]
--   dept_name    int_division  fixed_with_1_0  avg_salary
--   -----------  ------------  --------------  ----------
--   Engineering  122400        122400.0        122400.0
--   Finance      113500        113500.0        113500.0
--   HR           75666         75666.67        75666.67
--   (3 rows)
--
-- [trap 5b -- a NULL salary: AVG skips it, COUNT(*) still counts it]
--   avg_salary  sum_div_count_star  sum_div_count_col  same_as_avg
--   ----------  ------------------  -----------------  -----------
--   150.5       100                 150                150.5
--   (1 row)
--
-- [trap 5c -- NULL dept_id: PARTITION BY groups the NULLs, = never matches them]
--   emp_name  salary  correlated_avg  window_avg
--   --------  ------  --------------  ----------
--   Farah     130000  NULL            110000.0
--   Dev       90000   NULL            110000.0
--   (2 rows)
--
-- ======================================================================================
