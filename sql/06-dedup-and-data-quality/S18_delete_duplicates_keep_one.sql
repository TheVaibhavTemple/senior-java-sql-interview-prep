-- ================================================================================================
-- S18  .  Delete duplicate rows, keeping exactly one
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Dedup & Data Quality
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round
-- PRIORITY     : P1   -- delete duplicates har round mein aata hai; LC 196 ka senior version
-- TECHNIQUE    : ROW_NUMBER() over the duplicate key, then delete everything with rn > 1
--
-- PROBLEM
--   The employees table has exact duplicate rows. Delete them, keeping the row with the lowest
--   emp_id. Write it so you can SEE what will be deleted before you delete it.
--
-- ALGORITHM
--   1. Define "the same row" -- that tuple is the PARTITION BY.
--   2. Define which copy survives -- that is the ORDER BY inside the window.
--   3. ROW_NUMBER() gives the survivor rn = 1 and every other copy rn > 1.
--   4. SELECT the rn > 1 set first and eyeball it. Always. This is a DELETE.
--   5. Then delete those ids. Wrap it in a transaction and check the row count before COMMIT.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   SELECT DISTINCT into a new table, swap       acceptable    best at scale; needs a write freeze
--   DELETE ... WHERE id NOT IN (SELECT MIN(id))  acceptable    works; NOT IN is a NULL risk
--   ROW_NUMBER() in a CTE, delete rn > 1         WRITE THIS    explicit survivor rule, reviewable
--   GROUP BY ... HAVING, delete by hand          avoid         not reproducible
--
-- WHY THIS ONE
--   This is a destructive statement, so the answer is not the SQL -- it is the procedure.
--   Show the SELECT first, state the survivor rule out loud, run inside a transaction, check
--   the affected count against what the SELECT showed, then commit. Saying that sequence
--   unprompted is the difference between an engineer and a candidate.
--
-- SAMJHO
--   Pehle tay karo "same row" kya hai: woh columns PARTITION BY mein jaate hain.
--   Phir tay karo kaunsi copy bachegi: woh window ka ORDER BY hai (yahan lowest emp_id).
--   ROW_NUMBER survivor ko 1 deta hai, baaki copies ko 2, 3. Bas rn > 1 delete karo.
--   DELETE se pehle wahi SELECT chalao, transaction mein delete karo, count match karke COMMIT.
--
-- AISE BOLO  (30-40 sec)
--   "Before I write anything I'd ask which columns define a duplicate, which copy should survive,
--   and whether any of those columns can be NULL. Deleting by hand after a GROUP BY works once
--   but is not reproducible, so I rank the copies with ROW_NUMBER over the duplicate key,
--   ordered by the survivor rule, here the lowest emp_id. I run that as a SELECT first, then
--   delete rn > 1 inside a transaction and check the affected count before I commit. It is one
--   window pass, O(n log n); on hundreds of millions of rows I'd delete in batches, and I'd
--   finish with a unique constraint so the duplicates cannot come back."
--
-- TRAP
--   ! SELECT DISTINCT * se kuch nahi hatega: emp_id har copy mein alag hai. DISTINCT sirf
--     non-key columns pe karo.
--   ! Prevention wala UNIQUE index NULL ko distinct maanta hai (SQLite, Postgres default). Email
--     NULL ho to same copies phir aa sakti hain. Postgres 15+: UNIQUE NULLS NOT DISTINCT.
--
-- FOLLOW-UPS KE JAWAB
--   ? Which copy survives, and how do you change that to "keep the newest"?
--     -> Abhi lowest emp_id bachta hai (ORDER BY emp_id). Newest ke liye ORDER BY emp_id DESC,
--        ya created_at DESC, emp_id DESC. Baaki query same rehti hai.
--   ? Why is NOT IN risky here?
--     -> Subquery mein ek NULL aaya to NOT IN har row pe UNKNOWN, DELETE chupchap 0 rows. Yahan
--        MIN(emp_id) PK hai, NULL nahi hoga; phir bhi habit NOT EXISTS ki rakho.
--   ? The table has 400 million rows -- would you still run one DELETE? (batch it)
--     -> Nahi. Ek bada DELETE lamba transaction hai: undo/log aur locks bhaari. Chunks mein delete
--        karo, har chunk commit; zyada rows jaani hon to CTAS se clean table banao aur swap karo.
--   ? How do you stop this happening again? (a UNIQUE constraint on the tuple)
--     -> Natural key pe UNIQUE lagao, jaise email. Salary jaisi badalne wali column key mein mat
--        rakho, aur NULL wale columns ka behaviour apne engine pe check karo.
--
-- ALSO UNLOCKS
--   - Keeping the latest record per key
--   - Cleaning a loaded CSV
--   - Deduplicating an at-least-once event stream
--   - Backfilling a UNIQUE constraint
--
-- ORACLE / POSTGRES MEIN
--   Koi id hi na ho (poori row same), to physical row id se delete karo:
--   Oracle   : DELETE FROM employees WHERE ROWID IN (
--                SELECT rid FROM (SELECT ROWID AS rid, ROW_NUMBER() OVER (
--                    PARTITION BY emp_name, salary, hire_date, email ORDER BY ROWID) AS rn
--                  FROM employees) WHERE rn > 1);
--   Postgres : same shape, ROWID ki jagah ctid (ctid UPDATE / VACUUM FULL pe badalta hai, isliye
--              ek hi statement mein use karo). Ya DELETE FROM employees a USING employees b
--              WHERE a.emp_id > b.emp_id AND a.emp_name = b.emp_name AND ...; NULL ho sakta hai
--              to = ki jagah IS NOT DISTINCT FROM.
--   Oracle mein DELETE ke aage WITH nahi lagta, CTE subquery ke andar likho. Postgres mein
--   WITH ranked AS (...) DELETE FROM ... chalta hai.
--   Oracle UNIQUE: composite key mein kuch columns NULL aur baaki same hon to duplicate maanta hai.
--
-- VARIANTS     : LC 196 (Delete Duplicate Emails)
-- ================================================================================================

-- >>> QUERY: step 1 -- what WOULD be deleted (always run this first)
WITH ranked AS (
    SELECT emp_id, emp_name, salary, hire_date, email,
           ROW_NUMBER() OVER (PARTITION BY emp_name, salary, hire_date, email
                              ORDER BY emp_id) AS rn
    FROM   employees)
SELECT emp_id, emp_name, salary, rn AS copy_number
FROM   ranked
ORDER  BY emp_name, rn;

-- >>> QUERY: step 2 -- just the doomed rows
WITH ranked AS (
    SELECT emp_id, emp_name,
           ROW_NUMBER() OVER (PARTITION BY emp_name, salary, hire_date, email
                              ORDER BY emp_id) AS rn
    FROM   employees)
SELECT emp_id, emp_name FROM ranked WHERE rn > 1 ORDER BY emp_id;

-- >>> QUERY: step 3 -- the DELETE, then the proof
DELETE FROM employees
WHERE  emp_id IN (
    SELECT emp_id FROM (
        SELECT emp_id,
               ROW_NUMBER() OVER (PARTITION BY emp_name, salary, hire_date, email
                                  ORDER BY emp_id) AS rn
        FROM   employees) r
    WHERE  r.rn > 1);

-- >>> QUERY: step 4 -- verify. 13 rows before, 11 after; no duplicate tuples remain.
SELECT (SELECT COUNT(*) FROM employees) AS rows_now,
       (SELECT COUNT(*) FROM (SELECT emp_name, salary, hire_date, email
                              FROM employees
                              GROUP BY emp_name, salary, hire_date, email
                              HAVING COUNT(*) > 1)) AS duplicate_groups_left;

-- >>> QUERY: and the constraint that prevents a repeat
SELECT 'CREATE UNIQUE INDEX uq_emp ON employees(emp_name, salary, hire_date, email);' AS prevention;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [step 1 -- what WOULD be deleted (always run this first)]
--   emp_id  emp_name  salary  copy_number
--   ------  --------  ------  -----------
--   2       Arjun     140000  1
--   13      Farah     130000  1
--   5       Ishaan    72000   1
--   11      Kabir     64000   1
--   12      Kabir     64000   2
--   3       Meera     95000   1
--   10      Nikhil    99000   1
--   1       Rhea      210000  1
--   4       Sahil     95000   1
--   6       Tara      160000  1
--   7       Vikram    118000  1
--   8       Zoya      88000   1
--   9       Zoya      88000   2
--   (13 rows)
--
-- [step 2 -- just the doomed rows]
--   emp_id  emp_name
--   ------  --------
--   9       Zoya
--   12      Kabir
--   (2 rows)
--
-- [step 3 -- the DELETE, then the proof]
--   (no result set)
--
-- [step 4 -- verify. 13 rows before, 11 after; no duplicate tuples remain.]
--   rows_now  duplicate_groups_left
--   --------  ---------------------
--   11        0
--   (1 row)
--
-- [and the constraint that prevents a repeat]
--   prevention
--   ----------------------------------------------------------------------------
--   CREATE UNIQUE INDEX uq_emp ON employees(emp_name, salary, hire_date, email);
--   (1 row)
--
-- ======================================================================================
