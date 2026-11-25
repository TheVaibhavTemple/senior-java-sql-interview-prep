-- ================================================================================================
-- S05  .  Second and Nth highest salary
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Aggregation & Grouping
-- DIFFICULTY   : EASY    (1 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- sabse zyada poochha jaane wala SQL sawaal; DENSE_RANK pe hi judge hota hai
-- TECHNIQUE    : DENSE_RANK over the distinct salary ladder; MAX of a bounded subquery
--
-- PROBLEM
--   Return the second highest DISTINCT salary. Then generalise to the Nth. The answer must be
--   NULL -- not an empty result set -- when there is no such salary.
--
-- ALGORITHM
--   1. "Second highest" means second on the DISTINCT salary ladder, not the second row.
--   2. DENSE_RANK gives 1,1,2 for ties -- exactly the ladder semantics you want.
--   3. ROW_NUMBER would give 1,2,3 and return a duplicate of the top salary. That is the bug.
--   4. Wrap in MAX(...) so an out-of-range N yields a NULL row instead of no rows.
--   5. LIMIT/OFFSET is the shortest form but is dialect-specific and loses the NULL guarantee.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   ORDER BY salary DESC LIMIT 1 OFFSET 1        avoid         ties return the top salary again
--   MAX(salary) WHERE salary < (SELECT MAX..)    WRITE THIS    no window functions needed at all
--   DENSE_RANK() = N                             WRITE THIS    the version that generalises to N
--   ROW_NUMBER() = N                             avoid         counts rows, not distinct salaries
--   SELECT MAX over a DENSE_RANK subquery        WRITE THIS    returns NULL when N is too large
--
-- WHY THIS ONE
--   The entire question is RANK vs DENSE_RANK vs ROW_NUMBER. Two employees here earn 95000,
--   so the three functions give three different answers and the interviewer can see instantly
--   whether you know which one models a "ladder". Then say "and if N is out of range I want
--   NULL, not zero rows" and wrap it in MAX.
--
-- SAMJHO
--   "Second highest" matlab distinct salaries ki seedhi (ladder) mein doosra number, doosri row
--   nahi. DENSE_RANK tie ko same number deta hai aur gap nahi chhodta: 1, 2, 2, 3. Isliye dr = N.
--   Bahar MAX() lagao: N bahut bada ho to bhi ek row aayegi, value NULL.
--
-- AISE BOLO  (30-40 sec)
--   "First I'd ask whether second highest means the second distinct salary, and whether they want
--   NULL or no row when it doesn't exist. Without window functions I'd take MAX(salary) below the
--   overall MAX, two aggregates that an index on salary answers directly. For the Nth I use
--   DENSE_RANK over salary descending, filter dr = N and wrap it in MAX, so an out-of-range N
--   gives NULL instead of an empty set. ROW_NUMBER is the trap: a tied top salary comes back."
--
-- TRAP
--   ! ROW_NUMBER = 2, ya bina DISTINCT ke LIMIT 1 OFFSET 1: top salary tie ho to wahi salary
--     dobara aa jaati hai.
--   ! RANK = 2 tie ke baad 2 skip kar deta hai, to NULL aata hai. Ladder ke liye DENSE_RANK.
--
-- FOLLOW-UPS KE JAWAB
--   ? What do RANK, DENSE_RANK and ROW_NUMBER each return for 210000, 160000, 140000, 95000, 95000?
--     -> ROW_NUMBER 1,2,3,4,5; RANK 1,2,3,4,4; DENSE_RANK 1,2,3,4,4. Farq tie ke BAAD dikhta hai:
--        agli salary ko RANK 6, DENSE_RANK 5. Query 1 mein: 95000 ke baad 88000 = RANK 9, DR 8.
--   ? N is larger than the number of distinct salaries -- what does your query return?
--     -> MAX wrap ki wajah se ek row, value NULL (query 4). Bina MAX ke zero rows aati; LC 177
--        mein woh galat maana jaata hai.
--   ? Second highest salary PER DEPARTMENT -- what changes? (see S09)
--     -> DENSE_RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC), phir dr = 2. Bas
--        PARTITION BY judta hai.
--   ? Which version can use an index on salary?
--     -> MAX wala: index ke end se seedha max milta hai, doosra MAX ek range seek. DENSE_RANK ko
--        ordered scan chahiye; index sort bacha leta hai par top N tak rows padhni padti hain.
--
-- ALSO UNLOCKS
--   - Nth highest / lowest anything
--   - Top N per group
--   - Median via row numbering
--   - Dense ranking leaderboards
--
-- ORACLE / POSTGRES MEIN
--   - Oracle 12c+: SELECT DISTINCT salary FROM employees ORDER BY salary DESC
--     OFFSET 1 ROWS FETCH NEXT 1 ROWS ONLY. Postgres: LIMIT 1 OFFSET 1. Dono khaali set dete
--     hain, NULL nahi; NULL chahiye to (SELECT ...) ko scalar subquery bana do.
--   - Purana Oracle (11g, FETCH FIRST nahi): WHERE ROWNUM = 2 kabhi row nahi deta, kyunki ROWNUM
--     filter pass hone par hi badhta hai. Wahan DENSE_RANK ya MAX < MAX hi likho.
--   - Last query ka WITH tied(...) AS (VALUES ...): Postgres mein chalta hai; Oracle mein 23ai se
--     pehle SELECT 'a', 210000 FROM dual UNION ALL ... likho.
--
-- VARIANTS     : LC 176, LC 177
-- ================================================================================================

-- >>> QUERY: the three ranking functions side by side -- this is the real lesson
-- (no DISTINCT here: the ties at 95000, 88000 and 64000 are exactly what the lesson needs)
SELECT emp_name, salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC, emp_id) AS row_num,
       RANK()       OVER (ORDER BY salary DESC)         AS rnk,
       DENSE_RANK() OVER (ORDER BY salary DESC)         AS dense_rnk
FROM   employees
ORDER  BY salary DESC, emp_id;

-- >>> QUERY: second highest, no window functions
SELECT MAX(salary) AS second_highest
FROM   employees
WHERE  salary < (SELECT MAX(salary) FROM employees);

-- >>> QUERY: Nth highest, parameterised (N = 3), NULL-safe when out of range
SELECT MAX(salary) AS nth_highest
FROM   (SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS dr FROM employees) r
WHERE  dr = 3;

-- >>> QUERY: N out of range returns NULL, not an empty result set
SELECT MAX(salary) AS nth_highest_n_99
FROM   (SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS dr FROM employees) r
WHERE  dr = 99;

-- >>> QUERY: on THIS data the top salary is unique, so ROW_NUMBER happens to agree
SELECT salary AS row_number_answer
FROM   (SELECT salary, ROW_NUMBER() OVER (ORDER BY salary DESC) AS rn FROM employees) r
WHERE  rn = 2;

-- >>> QUERY: now duplicate the TOP salary -- this is where ROW_NUMBER breaks
WITH tied(emp, salary) AS (VALUES ('a',210000),('b',210000),('c',160000),('d',140000))
SELECT 'ROW_NUMBER = 2' AS method,
       (SELECT salary FROM (SELECT salary, ROW_NUMBER() OVER (ORDER BY salary DESC) rn
                            FROM tied) x
        WHERE rn = 2) AS answer,
       '210000 - the SAME salary again. WRONG.' AS verdict
UNION ALL
SELECT 'DENSE_RANK = 2',
       (SELECT MAX(salary) FROM (SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) dr
                                 FROM tied) x
        WHERE dr = 2),
       'second rung of the ladder. CORRECT.'
UNION ALL
SELECT 'RANK = 2',
       (SELECT MAX(salary) FROM (SELECT salary, RANK() OVER (ORDER BY salary DESC) rk
                                 FROM tied) x
        WHERE rk = 2),
       'NULL - RANK skipped 2 entirely after the tie.';

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the three ranking functions side by side -- this is the real lesson]
--   emp_name  salary  row_num  rnk  dense_rnk
--   --------  ------  -------  ---  ---------
--   Rhea      210000  1        1    1
--   Tara      160000  2        2    2
--   Arjun     140000  3        3    3
--   Farah     130000  4        4    4
--   Vikram    118000  5        5    5
--   Nikhil    99000   6        6    6
--   Meera     95000   7        7    7
--   Sahil     95000   8        7    7
--   Zoya      88000   9        9    8
--   Zoya      88000   10       9    8
--   Ishaan    72000   11       11   9
--   Kabir     64000   12       12   10
--   Kabir     64000   13       12   10
--   (13 rows)
--
-- [second highest, no window functions]
--   second_highest
--   --------------
--   160000
--   (1 row)
--
-- [Nth highest, parameterised (N = 3), NULL-safe when out of range]
--   nth_highest
--   -----------
--   140000
--   (1 row)
--
-- [N out of range returns NULL, not an empty result set]
--   nth_highest_n_99
--   ----------------
--   NULL
--   (1 row)
--
-- [on THIS data the top salary is unique, so ROW_NUMBER happens to agree]
--   row_number_answer
--   -----------------
--   160000
--   (1 row)
--
-- [now duplicate the TOP salary -- this is where ROW_NUMBER breaks]
--   method          answer  verdict
--   --------------  ------  ---------------------------------------------
--   ROW_NUMBER = 2  210000  210000 - the SAME salary again. WRONG.
--   DENSE_RANK = 2  160000  second rung of the ladder. CORRECT.
--   RANK = 2        NULL    NULL - RANK skipped 2 entirely after the tie.
--   (3 rows)
--
-- ======================================================================================
