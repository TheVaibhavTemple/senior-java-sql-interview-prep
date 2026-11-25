-- ================================================================================================
-- S02  .  Employees with their manager's name   (self-join)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Joins & Anti-joins
-- DIFFICULTY   : EASY    (1 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- employee-manager self-join Indian rounds ka classic hai
-- TECHNIQUE    : Self-join on manager_id = emp_id, with LEFT JOIN to keep the root
--
-- PROBLEM
--   List every employee with their manager's name. The CEO has no manager and must still
--   appear. Then: which manager has the most direct reports?
--
-- ALGORITHM
--   1. Join the table to itself with two different aliases -- one plays "employee", one "manager".
--   2. The join predicate is e.manager_id = m.emp_id.
--   3. Use LEFT JOIN, not INNER: the root has manager_id NULL and an INNER JOIN would drop it.
--   4. COALESCE the manager name so the output reads cleanly.
--   5. For report counts, group by the MANAGER alias and count the employee alias.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   INNER JOIN employees m                       avoid         silently drops the CEO
--   LEFT JOIN employees m                        WRITE THIS    keeps every employee
--   Correlated scalar subquery for the name      acceptable    one extra lookup per row
--   Recursive CTE                                acceptable    needed only for depth, see S12
--
-- WHY THIS ONE
--   The whole question is whether you notice that an INNER JOIN silently loses the root of the
--   hierarchy. Reach for LEFT JOIN first and say why. The second half -- counting direct reports
--   -- catches people who group by the wrong alias.
--
-- SAMJHO
--   Ek hi table ko do alias do: e = employee, m = manager. Join: e.manager_id = m.emp_id.
--   INNER JOIN CEO ko gira deta hai, kyunki uska manager_id NULL hai. Isliye LEFT JOIN.
--   Reports ginne hon to manager alias pe GROUP BY karo aur employee alias ko COUNT karo.
--
-- AISE BOLO  (30-40 sec)
--   "I'd self-join employees as e and m on e.manager_id = m.emp_id, and I'd use a LEFT JOIN,
--   because the CEO has a NULL manager_id and an inner join would silently drop that row.
--   COALESCE gives the root a readable label. With emp_id as the primary key each employee is
--   one index lookup, so it stays linear. For direct-report counts I group by the manager alias,
--   and I'd ask whether a tie for 'most reports' should return every tied manager."
--
-- TRAP
--   ! INNER JOIN likha to CEO gayab, aur koi error bhi nahi aata: 13 ki jagah 12 rows.
--   ! "Managers with no reports" ke liye NOT IN (SELECT manager_id FROM employees) mat likho:
--     CEO ka NULL manager_id poora result khaali kar deta hai. NOT EXISTS likho.
--
-- FOLLOW-UPS KE JAWAB
--   ? What happens to the CEO row with an INNER JOIN?
--     -> Rhea ka manager_id NULL hai aur NULL = kuch bhi kabhi TRUE nahi, to row drop ho jaati
--        hai. Query 2: 12 vs 13.
--   ? Employees earning more than their manager -- same join, one extra predicate.
--     -> Wahi self-join, bas WHERE e.salary > m.salary (query 3). Is data mein 0 rows.
--   ? Managers with no reports at all.
--     -> Anti-join: WHERE NOT EXISTS (SELECT 1 FROM employees r WHERE r.manager_id = m.emp_id).
--        Is seed pe 9 log aate hain. NOT IN yahan 0 deta hai (upar TRAP).
--   ? How would you get the full chain to the top? (recursive CTE -- S12)
--     -> Recursive CTE: anchor CEO (manager_id IS NULL), phir har level pe join karke depth + 1.
--        Oracle mein CONNECT BY PRIOR bhi chalta hai. S12 dekho.
--
-- ALSO UNLOCKS
--   - Employees earning more than their manager
--   - Manager with the most direct reports
--   - Pairs of rows from the same table
--   - Duplicate payments within a time window (S17)
--
-- ORACLE / POSTGRES MEIN
--   - LIMIT 3: Oracle 12c+ mein FETCH FIRST 3 ROWS ONLY. Purane Oracle mein ROWNUM, par ORDER BY
--     wali subquery ke bahar, kyunki ROWNUM sort se pehle lagta hai. Postgres mein LIMIT chalega.
--   - Tie ke saath top: FETCH FIRST 1 ROWS WITH TIES (Oracle 12c+, Postgres 13+).
--   - Hierarchy: Oracle mein START WITH manager_id IS NULL CONNECT BY PRIOR emp_id = manager_id.
--
-- VARIANTS     : LC 181, LC 570
-- ================================================================================================

-- >>> QUERY: every employee with their manager (LEFT JOIN keeps the CEO)
SELECT e.emp_id, e.emp_name, COALESCE(m.emp_name, '(none - top of tree)') AS manager
FROM   employees e
LEFT   JOIN employees m ON m.emp_id = e.manager_id
ORDER  BY e.emp_id;

-- >>> QUERY: what an INNER JOIN silently loses
SELECT COUNT(*) AS rows_with_inner_join FROM employees e JOIN employees m ON m.emp_id = e.manager_id
UNION ALL
SELECT COUNT(*) FROM employees;

-- >>> QUERY: employees earning MORE than their manager
SELECT e.emp_name AS employee, e.salary AS emp_salary,
       m.emp_name AS manager,  m.salary AS mgr_salary
FROM   employees e
JOIN   employees m ON m.emp_id = e.manager_id
WHERE  e.salary > m.salary
ORDER  BY e.emp_name;

-- >>> QUERY: manager with the most direct reports
SELECT m.emp_name AS manager, COUNT(e.emp_id) AS direct_reports
FROM   employees m
JOIN   employees e ON e.manager_id = m.emp_id
GROUP  BY m.emp_id, m.emp_name
ORDER  BY direct_reports DESC, manager
LIMIT  3;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [every employee with their manager (LEFT JOIN keeps the CEO)]
--   emp_id  emp_name  manager
--   ------  --------  --------------------
--   1       Rhea      (none - top of tree)
--   2       Arjun     Rhea
--   3       Meera     Rhea
--   4       Sahil     Arjun
--   5       Ishaan    Arjun
--   6       Tara      Rhea
--   7       Vikram    Tara
--   8       Zoya      Tara
--   9       Zoya      Tara
--   10      Nikhil    Rhea
--   11      Kabir     Nikhil
--   12      Kabir     Nikhil
--   13      Farah     Rhea
--   (13 rows)
--
-- [what an INNER JOIN silently loses]
--   rows_with_inner_join
--   --------------------
--   12
--   13
--   (2 rows)
--
-- [employees earning MORE than their manager]
--   employee  emp_salary  manager  mgr_salary
--   --------  ----------  -------  ----------
--   (0 rows)
--
-- [manager with the most direct reports]
--   manager  direct_reports
--   -------  --------------
--   Rhea     5
--   Tara     3
--   Arjun    2
--   (3 rows)
--
-- ======================================================================================
