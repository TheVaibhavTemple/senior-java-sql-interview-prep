-- ================================================================================================
-- S06  .  Find duplicates -- the values, and then every offending row
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Aggregation & Grouping
-- DIFFICULTY   : EASY    (1 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- duplicate nikaalna aur phir delete karna har round mein aata hai
-- TECHNIQUE    : GROUP BY ... HAVING COUNT(*) > 1, then join back for the full rows
--
-- PROBLEM
--   Find duplicate email addresses. Then list every duplicate ROW, not just the value.
--   Then find customers with more than one order on the same day.
--
-- ALGORITHM
--   1. GROUP BY the column(s) that define "the same thing".
--   2. HAVING COUNT(*) > 1 keeps only the groups that repeat. WHERE cannot do this.
--   3. That gives you the VALUES. To get the ROWS, join the original table back to that list.
--   4. Or skip the join: COUNT(*) OVER (PARTITION BY email) attaches the group count to each row.
--   5. For composite duplicates, group by the whole tuple.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   WHERE COUNT(*) > 1                           avoid         illegal: WHERE runs before grouping
--   GROUP BY + HAVING                            WRITE THIS    returns the duplicate VALUES
--   Join the grouped list back to the table      WRITE THIS    returns the duplicate ROWS
--   COUNT(*) OVER (PARTITION BY ...)             WRITE THIS    one pass, no self-join
--   SELECT DISTINCT                              avoid         hides the problem instead of
--                                                              finding it
--
-- WHY THIS ONE
--   "Find duplicates" and "show me the duplicate rows" are two different questions, and most
--   candidates only answer the first. Lead with HAVING, then say "if you want the rows and not
--   just the values, I'd use a window count" -- that is the senior half of the answer.
--
-- SAMJHO
--   Jis column se "same" decide hota hai, us pe GROUP BY, phir HAVING COUNT(*) > 1. Ye VALUES deta.
--   Poori rows chahiye to COUNT(*) OVER (PARTITION BY email) har row pe group ka count laga deta
--   hai; dup_count > 1 wali rakh lo. WHERE mein COUNT nahi chalta, WHERE grouping se pehle hai.
--
-- AISE BOLO  (30-40 sec)
--   "I'd first ask what duplicate means here: same email only, or name, salary and hire date
--   together, and whether case and trailing spaces count. For the values I GROUP BY that key
--   with HAVING COUNT(*) > 1. For the actual rows, which you need before deleting anything, I
--   use COUNT(*) OVER (PARTITION BY email) in one pass instead of joining the grouped list back.
--   Both are one sort or hash on the key; the edge case is NULL emails, which GROUP BY puts
--   together in one group."
--
-- TRAP
--   ! Saare NULL email GROUP BY mein ek hi group bante hain, to woh bhi "duplicate" dikhenge.
--     Zaroorat ho to WHERE email IS NOT NULL.
--   ! Real data mein 'Zoya@corp.com' aur 'zoya@corp.com ' alag gine jaate hain. LOWER(TRIM(email))
--     pe group karo.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why can't you put COUNT(*) > 1 in the WHERE clause?
--     -> WHERE har row pe grouping se pehle chalta hai, tab tak COUNT bana hi nahi. Aggregate pe
--        filter HAVING mein.
--   ? Now DELETE the duplicates keeping the lowest id. (see S18)
--     -> ROW_NUMBER() OVER (PARTITION BY email ORDER BY emp_id) > 1 wali ids delete karo, ya
--        EXISTS se: same email aur chhoti emp_id wali row ho to delete. Poora flow S18 mein.
--   ? Duplicates on a combination of three columns.
--     -> GROUP BY teeno columns, HAVING COUNT(*) > 1 (query 3). Window version mein PARTITION BY
--        a, b, c.
--   ? COUNT(*) vs COUNT(column) when the column is NULL.
--     -> COUNT(*) har row ginta hai, COUNT(dept_id) NULL chhod deta hai: 13 vs 12, kyunki Farah
--        ka dept_id NULL hai (query 5).
--
-- ALSO UNLOCKS
--   - Delete duplicates keeping one
--   - Customers with more than one order per day
--   - Data-quality checks on a loaded file
--   - Duplicate transactions within a time window (S17)
--
-- VARIANTS     : LC 182, LC 196
-- ================================================================================================

-- >>> QUERY: duplicate email VALUES
SELECT email, COUNT(*) AS occurrences
FROM   employees
GROUP  BY email
HAVING COUNT(*) > 1
ORDER  BY email;

-- >>> QUERY: every duplicate ROW, via a window count (one pass, no self-join)
SELECT emp_id, emp_name, email, dup_count
FROM   (SELECT emp_id, emp_name, email,
               COUNT(*) OVER (PARTITION BY email) AS dup_count
        FROM   employees) t
WHERE  dup_count > 1
ORDER  BY email, emp_id;

-- >>> QUERY: duplicates on a COMPOSITE key (name + salary + hire_date)
SELECT emp_name, salary, hire_date, COUNT(*) AS occurrences
FROM   employees
GROUP  BY emp_name, salary, hire_date
HAVING COUNT(*) > 1
ORDER  BY emp_name;

-- >>> QUERY: customers with more than one order on the same day
SELECT c.cust_name, o.order_date, COUNT(*) AS orders_that_day
FROM   orders o JOIN customers c ON c.cust_id = o.cust_id
GROUP  BY o.cust_id, c.cust_name, o.order_date
HAVING COUNT(*) > 1;

-- >>> QUERY: COUNT(*) vs COUNT(column) when the column is NULL
SELECT COUNT(*) AS count_star, COUNT(dept_id) AS count_dept_id,
       COUNT(*) - COUNT(dept_id) AS employees_with_no_department
FROM   employees;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [duplicate email VALUES]
--   email           occurrences
--   --------------  -----------
--   kabir@corp.com  2
--   zoya@corp.com   2
--   (2 rows)
--
-- [every duplicate ROW, via a window count (one pass, no self-join)]
--   emp_id  emp_name  email           dup_count
--   ------  --------  --------------  ---------
--   11      Kabir     kabir@corp.com  2
--   12      Kabir     kabir@corp.com  2
--   8       Zoya      zoya@corp.com   2
--   9       Zoya      zoya@corp.com   2
--   (4 rows)
--
-- [duplicates on a COMPOSITE key (name + salary + hire_date)]
--   emp_name  salary  hire_date   occurrences
--   --------  ------  ----------  -----------
--   Kabir     64000   2023-01-16  2
--   Zoya      88000   2021-07-19  2
--   (2 rows)
--
-- [customers with more than one order on the same day]
--   cust_name  order_date  orders_that_day
--   ---------  ----------  ---------------
--   (0 rows)
--
-- [COUNT(*) vs COUNT(column) when the column is NULL]
--   count_star  count_dept_id  employees_with_no_department
--   ----------  -------------  ----------------------------
--   13          12             1
--   (1 row)
--
-- ======================================================================================
