-- ================================================================================================
-- S14  .  Hire within a budget   (running total + CTE + UNION ALL)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : CTEs & Recursion
-- DIFFICULTY   : HARD    (3 of 3)
-- WEIGHTAGE    : MEDIUM  **   a real reported question (Microsoft); the hardest shape in this set
-- PRIORITY     : P3   -- LC Hard puzzle, backend round mein rare; running-total cutoff idea kaafi
-- TECHNIQUE    : Greedy selection via a running total, chained across two priority tiers
--
-- PROBLEM
--   A total salary budget of 500000. Hire as many SENIOR employees as possible, cheapest first.
--   Spend whatever is left on JUNIOR employees, cheapest first. Return who gets hired.
--
-- ALGORITHM
--   1. "Cheapest first until the money runs out" is a running total with a cutoff.
--   2. Tier 1: order seniors by salary ascending, take the running total, keep rows where the
--      running total <= budget.
--   3. Compute what tier 1 actually spent -- that is MAX(running_total), or 0 if nobody fit.
--   4. Tier 2: same running total over juniors, but the cutoff is budget - tier1_spend.
--   5. UNION ALL the two tiers. Each tier is one CTE; nothing here needs a loop.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Application-side loop over sorted rows       avoid         N round trips for one decision
--   Cursor / stored procedure loop               avoid         works, but not what is being asked
--   Running total + cutoff, one CTE per tier     WRITE THIS    set-based, two scans total
--   Recursive CTE accumulating the spend         acceptable    valid, far harder to read
--
-- WHY THIS ONE
--   This is the shape behind every "greedy until a limit" interview question: budgets, capacity
--   planning, warehouse picking, rate quotas. Once you see that "keep hiring until the money
--   runs out" IS a windowed running total with a WHERE clause, the loop disappears. The one
--   detail to get right is carrying tier 1's actual spend into tier 2's cutoff.
--
-- SAMJHO
--   "Sabse saste se shuru karo jab tak paisa bache" = salary ASC pe running total, aur
--   running_total <= budget. Pehle seniors ka running total; unka asli kharcha MAX(cum), koi
--   fit na ho to 0. Bacha hua budget juniors ke running total ka cutoff hai. Loop nahi chahiye.
--
-- AISE BOLO  (30-40 sec)
--   "Cheapest first until the money runs out is a running total with a cutoff, so I don't need
--   a loop or a cursor. I take SUM(salary) OVER (PARTITION BY tier ORDER BY salary, emp_id ROWS
--   UNBOUNDED PRECEDING), keep the seniors whose running total is within 500000, and get what
--   they actually spent with COALESCE(MAX(running_total), 0). Juniors get the same running
--   total, cut at the budget minus that spend, and I UNION ALL the two tiers; it is one sort
--   per tier, O(n log n). The edge case is a tie at the boundary: emp_id in the ORDER BY makes
--   the choice between two equal salaries deterministic."
--
-- TRAP
--   ! Tier 2 ka cutoff budget minus tier 1 ka ASLI kharcha hai. Koi senior fit na ho to MAX NULL
--     deta hai; COALESCE ke bina 500000 - NULL = NULL, aur ek bhi junior hire nahi hota.
--   ! ORDER BY mein unique tiebreaker (emp_id) daalo, warna boundary pe same salary wale random.
--
-- FOLLOW-UPS KE JAWAB
--   ? Nobody fits in tier 1 -- does your tier-2 cutoff still work? (COALESCE to 0)
--     -> Haan, COALESCE(MAX(cum), 0) se spent 0 hota hai aur juniors ko poora budget milta hai.
--        COALESCE hatao to cutoff NULL, aur juniors ki list chupchaap khaali.
--   ? Two employees have the same salary at the boundary -- who gets hired, and is it stable?
--     -> emp_id decide karta hai: Kabir 11 hire (cum 64000), Kabir 12 nahi (128000 > 112000).
--        Unique key hai to har run mein same. "Dono ya koi nahi" chahiye to RANGE + sirf salary.
--   ? Maximise HEADCOUNT instead of filling tiers in order -- same query?
--     -> Nahi. Tier hatao, sab employees pe ek running total salary ASC -- cheapest first hi
--        headcount maximise karta hai. Yahan 6 log (471000), tiered version mein sirf 4.
--   ? Why is the running total ROWS and not RANGE here?
--     -> ORDER BY salary, emp_id unique hai, to yahan dono same answer dete. Sirf salary pe RANGE
--        hota to dono Kabir ek frame mein -- dono ko 128000, dono ek saath in ya out.
--
-- ALSO UNLOCKS
--   - Capacity / quota allocation
--   - Warehouse picking until stock runs out
--   - Cumulative spend against a cap
--   - Any greedy "take until the limit" problem
--
-- ORACLE / POSTGRES MEIN
--   Teesri query FROM ke bina SELECT hai: Oracle mein (23ai se pehle) FROM dual lagao.
--   Doosri query ka FROM (SELECT ... UNION ALL ...) bina alias ke hai: PostgreSQL 16 se pehle
--   "subquery in FROM must have an alias" error aata hai, alias do. Oracle mein chal jata hai.
--
-- VARIANTS     : LC 2004 and LC 2010 (seniors and juniors to join the company),
--                LC 1204 (last person to fit in the bus)
-- ================================================================================================

-- >>> QUERY: the tiers, with their running totals (the working, before the cutoff)
WITH tiered AS (
    SELECT emp_id, emp_name, salary,
           CASE WHEN salary >= 100000 THEN 'SENIOR' ELSE 'JUNIOR' END AS tier
    FROM   employees
)
SELECT tier, emp_name, salary,
       SUM(salary) OVER (PARTITION BY tier ORDER BY salary, emp_id
                         ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total
FROM   tiered
ORDER  BY tier DESC, salary, emp_id;

-- >>> QUERY: the answer -- seniors first, then juniors with whatever is left (budget 500000)
WITH tiered AS (
    SELECT emp_id, emp_name, salary,
           CASE WHEN salary >= 100000 THEN 'SENIOR' ELSE 'JUNIOR' END AS tier
    FROM   employees
),
running AS (
    SELECT t.*,
           SUM(salary) OVER (PARTITION BY tier ORDER BY salary, emp_id
                             ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cum
    FROM   tiered t
),
seniors AS (
    SELECT * FROM running WHERE tier = 'SENIOR' AND cum <= 500000
),
senior_spend AS (
    SELECT COALESCE(MAX(cum), 0) AS spent FROM seniors      -- COALESCE: nobody may have fitted
),
juniors AS (
    SELECT r.* FROM running r, senior_spend s
    WHERE  r.tier = 'JUNIOR' AND r.cum <= 500000 - s.spent
)
SELECT tier, emp_name, salary, cum AS running_total_within_tier
FROM   (SELECT * FROM seniors UNION ALL SELECT * FROM juniors)
ORDER  BY tier DESC, salary, emp_id;

-- >>> QUERY: the summary line a hiring manager actually wants
WITH tiered AS (
    SELECT emp_id, emp_name, salary,
           CASE WHEN salary >= 100000 THEN 'SENIOR' ELSE 'JUNIOR' END AS tier FROM employees),
running AS (
    SELECT t.*, SUM(salary) OVER (PARTITION BY tier ORDER BY salary, emp_id
                 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cum FROM tiered t),
seniors AS (SELECT * FROM running WHERE tier='SENIOR' AND cum <= 500000),
senior_spend AS (SELECT COALESCE(MAX(cum),0) AS spent FROM seniors),
juniors AS (SELECT r.* FROM running r, senior_spend s
            WHERE r.tier='JUNIOR' AND r.cum <= 500000 - s.spent)
SELECT (SELECT COUNT(*) FROM seniors)                    AS seniors_hired,
       (SELECT COUNT(*) FROM juniors)                    AS juniors_hired,
       (SELECT COALESCE(SUM(salary),0) FROM seniors)
     + (SELECT COALESCE(SUM(salary),0) FROM juniors)     AS total_spend,
       500000 - ((SELECT COALESCE(SUM(salary),0) FROM seniors)
     + (SELECT COALESCE(SUM(salary),0) FROM juniors))    AS budget_left;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the tiers, with their running totals (the working, before the cutoff)]
--   tier    emp_name  salary  running_total
--   ------  --------  ------  -------------
--   SENIOR  Vikram    118000  118000
--   SENIOR  Farah     130000  248000
--   SENIOR  Arjun     140000  388000
--   SENIOR  Tara      160000  548000
--   SENIOR  Rhea      210000  758000
--   JUNIOR  Kabir     64000   64000
--   JUNIOR  Kabir     64000   128000
--   JUNIOR  Ishaan    72000   200000
--   JUNIOR  Zoya      88000   288000
--   JUNIOR  Zoya      88000   376000
--   JUNIOR  Meera     95000   471000
--   JUNIOR  Sahil     95000   566000
--   JUNIOR  Nikhil    99000   665000
--   (13 rows)
--
-- [the answer -- seniors first, then juniors with whatever is left (budget 500000)]
--   tier    emp_name  salary  running_total_within_tier
--   ------  --------  ------  -------------------------
--   SENIOR  Vikram    118000  118000
--   SENIOR  Farah     130000  248000
--   SENIOR  Arjun     140000  388000
--   JUNIOR  Kabir     64000   64000
--   (4 rows)
--
-- [the summary line a hiring manager actually wants]
--   seniors_hired  juniors_hired  total_spend  budget_left
--   -------------  -------------  -----------  -----------
--   3              1              452000       48000
--   (1 row)
--
-- ======================================================================================
