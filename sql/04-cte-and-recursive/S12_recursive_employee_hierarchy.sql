-- ================================================================================================
-- S12  .  Employee hierarchy with depth   (recursive CTE)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : CTEs & Recursion
-- DIFFICULTY   : HARD    (3 of 3)
-- WEIGHTAGE    : MEDIUM  **   common; shows up in most loops with a hierarchy in the data
-- PRIORITY     : P2   -- backend mein kabhi kabhi, data roles mein zyada; Oracle CONNECT BY jaano
-- TECHNIQUE    : WITH RECURSIVE -- anchor member UNION ALL recursive member
--
-- PROBLEM
--   Print the reporting tree from the CEO down, with each employee's depth and the full path
--   to the top. Then: everyone underneath one specific manager.
--
-- ALGORITHM
--   1. ANCHOR member: the roots. Here, employees with manager_id IS NULL. Seed depth = 0.
--   2. UNION ALL -- not UNION. UNION would deduplicate every iteration, which is wasted work
--      and hides genuine duplicates.
--   3. RECURSIVE member: join the CTE back to the base table on child.manager_id = cte.emp_id.
--   4. Carry depth + 1 and append to the path string as you descend.
--   5. Termination: the recursive member returns no rows once the deepest level is reached.
--      A cyclic graph never terminates -- guard with a depth cap or a visited path check.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   N self-joins for N levels                    avoid         depth must be known in advance
--   Application-side loop, one query per level   avoid         N round trips
--   WITH RECURSIVE                               WRITE THIS    any depth, one query
--   Materialised path / nested set column        acceptable    faster reads, costlier writes
--
-- WHY THIS ONE
--   Recursive CTEs are the one piece of SQL most backend engineers have never written, so they
--   separate the field fast. Name the three parts out loud -- anchor, UNION ALL, recursive
--   member -- and mention the cycle guard before they ask. A manager_id loop in production data
--   turns this query into an infinite one.
--
-- SAMJHO
--   Recursive CTE ke teen hisse: anchor (root, yahan manager_id IS NULL), UNION ALL, aur
--   recursive member jo CTE ko wapas employees se join karta hai (e.manager_id = t.emp_id).
--   Har round mein ek level neeche jaate ho, depth + 1 aur path badhate ho. Nayi row na aaye
--   to ruk jata hai; cycle ho to kabhi nahi rukta, isliye guard lagao.
--
-- AISE BOLO  (30-40 sec)
--   "I'd ask first whether the data can have cycles and how deep the tree can get. Fixed
--   self-joins only work if I know the depth, and a loop in Java costs one round trip per
--   level, so I'd use a recursive CTE: the anchor selects the root where manager_id is null,
--   and the recursive member joins employees to the CTE on manager_id, carrying depth plus one
--   and the path. Each level is one indexed join on manager_id, so it is roughly linear in the
--   size of the tree. I always add a cycle guard, a depth cap or a check that the id is not
--   already in the path, because one bad manager_id would otherwise make it run forever."
--
-- TRAP
--   ! UNION ALL likho, UNION nahi, aur cycle guard zaroor lagao -- warna ek galat manager_id
--     query ko infinite bana deta hai.
--   ! Depth cap sirf infinite loop rokta hai; cycle mein rows phir bhi 20 level tak repeat
--     hoti hain. Sahi guard path check hai: naya id path mein pehle se ho to mat jodo.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why UNION ALL and not UNION?
--     -> UNION har iteration pe duplicate check karta hai -- extra kaam, aur key select na ho to
--        asli duplicates chhup jaate hain. Cycle bhi nahi rokta, kyunki depth har baar badalta hai.
--   ? The data has a cycle (A manages B manages A) -- what happens, and how do you guard?
--     -> Recursion kabhi khatam nahi hoti. Guard: depth cap (yahan depth < 20), ya better ids ka
--        path rakho aur naya id usme ho to row mat jodo. Oracle aur Postgres 14+ mein CYCLE clause.
--   ? Count all descendants of one manager, not just direct reports.
--     -> Anchor mein us manager ko lo, recursive join se neeche jao, depth > 0 wali rows COUNT
--        karo. Third query yahi har manager ke liye karti hai (Rhea 12, Tara 3).
--   ? When would you denormalise this into a path column instead?
--     -> Jab reads bahut aur hierarchy kam badle: path '/1/6/7/' pe LIKE '/1/6/%' se poora
--        subtree ek prefix scan mein. Cost: manager badle to poore subtree ke paths update.
--
-- ALSO UNLOCKS
--   - Org charts and bill-of-materials
--   - Category trees and threaded comments
--   - Graph reachability
--   - Date spines (S13)
--
-- ORACLE / POSTGRES MEIN
--   Oracle: RECURSIVE keyword nahi likhte, aur column list zaroori hai -- WITH tree (emp_id,
--   emp_name, manager_id, depth, path) AS (...). Purana Oracle tareeka: START WITH manager_id
--   IS NULL CONNECT BY PRIOR emp_id = manager_id, saath mein LEVEL - 1 aur
--   SYS_CONNECT_BY_PATH(emp_name, ' > '); cycle ke liye CONNECT BY NOCYCLE.
--   Postgres: WITH RECURSIVE same; 14+ mein CYCLE emp_id SET is_cycle USING visited. Column
--   VARCHAR(n) ho to anchor mein CAST(emp_name AS TEXT) AS path likho, warna type error.
--
-- VARIANTS     : LC 1270 (all people who report to the given manager, up to 3 levels)
-- ================================================================================================

-- >>> QUERY: the full tree with depth and path
WITH RECURSIVE tree AS (
    -- anchor: the roots
    SELECT emp_id, emp_name, manager_id, 0 AS depth, emp_name AS path
    FROM   employees
    WHERE  manager_id IS NULL
    UNION ALL
    -- recursive member: children of everything already in the tree
    SELECT e.emp_id, e.emp_name, e.manager_id, t.depth + 1, t.path || ' > ' || e.emp_name
    FROM   employees e
    JOIN   tree t ON e.manager_id = t.emp_id
    WHERE  t.depth < 20                 -- cycle guard: never recurse forever
)
SELECT depth, substr('                    ', 1, depth * 3) || emp_name AS org_chart, path
FROM   tree
ORDER  BY path;

-- >>> QUERY: everyone underneath Tara (emp_id 6), at any depth
WITH RECURSIVE below AS (
    SELECT emp_id, emp_name, 0 AS depth FROM employees WHERE emp_id = 6
    UNION ALL
    SELECT e.emp_id, e.emp_name, b.depth + 1
    FROM   employees e JOIN below b ON e.manager_id = b.emp_id
    WHERE  b.depth < 20
)
SELECT emp_id, emp_name, depth FROM below WHERE depth > 0 ORDER BY depth, emp_name;

-- >>> QUERY: headcount of the whole sub-tree under each manager
WITH RECURSIVE tree AS (
    SELECT emp_id AS root, emp_id, 0 AS depth FROM employees
    UNION ALL
    SELECT t.root, e.emp_id, t.depth + 1
    FROM   employees e JOIN tree t ON e.manager_id = t.emp_id
    WHERE  t.depth < 20
)
SELECT m.emp_name AS manager, COUNT(*) - 1 AS total_descendants
FROM   tree t JOIN employees m ON m.emp_id = t.root
GROUP  BY t.root, m.emp_name
HAVING COUNT(*) - 1 > 0
ORDER  BY total_descendants DESC, manager;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the full tree with depth and path]
--   depth  org_chart     path
--   -----  ------------  ---------------------
--   0      Rhea          Rhea
--   1         Arjun      Rhea > Arjun
--   2            Ishaan  Rhea > Arjun > Ishaan
--   2            Sahil   Rhea > Arjun > Sahil
--   1         Farah      Rhea > Farah
--   1         Meera      Rhea > Meera
--   1         Nikhil     Rhea > Nikhil
--   2            Kabir   Rhea > Nikhil > Kabir
--   2            Kabir   Rhea > Nikhil > Kabir
--   1         Tara       Rhea > Tara
--   2            Vikram  Rhea > Tara > Vikram
--   2            Zoya    Rhea > Tara > Zoya
--   2            Zoya    Rhea > Tara > Zoya
--   (13 rows)
--
-- [everyone underneath Tara (emp_id 6), at any depth]
--   emp_id  emp_name  depth
--   ------  --------  -----
--   7       Vikram    1
--   8       Zoya      1
--   9       Zoya      1
--   (3 rows)
--
-- [headcount of the whole sub-tree under each manager]
--   manager  total_descendants
--   -------  -----------------
--   Rhea     12
--   Tara     3
--   Arjun    2
--   Nikhil   2
--   (4 rows)
--
-- ======================================================================================
