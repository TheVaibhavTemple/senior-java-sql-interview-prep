-- ================================================================================================
-- S14  .  Hire within a budget   (running total + CTE + UNION ALL)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : CTEs & Recursion
-- DIFFICULTY   : HARD    (3 of 3)
-- WEIGHTAGE    : MEDIUM  **   a real reported question (Microsoft); the hardest shape in this set
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
-- FOLLOW-UPS
--   ? Nobody fits in tier 1 -- does your tier-2 cutoff still work? (COALESCE to 0)
--   ? Two employees have the same salary at the boundary -- who gets hired, and is it stable?
--   ? Maximise HEADCOUNT instead of filling tiers in order -- same query?
--   ? Why is the running total ROWS and not RANGE here?
--
-- ALSO UNLOCKS
--   - Capacity / quota allocation
--   - Warehouse picking until stock runs out
--   - Cumulative spend against a cap
--   - Any greedy "take until the limit" problem
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
-- Engine: SQLite 3.53.1.  Fresh database per file, so each file reproduces standalone.
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
