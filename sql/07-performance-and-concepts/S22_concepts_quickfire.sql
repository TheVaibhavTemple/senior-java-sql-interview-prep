-- ================================================================================================
-- S22  .  Quick-fire concepts -- the ones asked in every single round
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Performance & Concepts
-- DIFFICULTY   : EASY    (1 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every round, usually in the first five minutes
-- PRIORITY     : P1   -- pehle 5 minute ke warm-up sawaal, har company mein
-- TECHNIQUE    : Definitions you must be able to give in one sentence, each with a live example
--
-- PROBLEM
--   DELETE vs TRUNCATE vs DROP. UNION vs UNION ALL. Isolation levels and the anomalies they
--   prevent. Normalisation and when to denormalise. Reading an execution plan.
--
-- ALGORITHM  (the one-sentence answers)
--   1. DELETE is DML: row by row, fires triggers, supports WHERE, logged, rollback-able.
--      TRUNCATE is DDL: deallocates pages, no WHERE, no row triggers, far faster, and on many
--      engines it cannot be rolled back. Identity reset is engine-specific (MySQL and SQL Server
--      reset it; PostgreSQL only with RESTART IDENTITY). DROP removes the table itself.
--   2. UNION deduplicates (an implicit sort or hash); UNION ALL concatenates. Default to ALL
--      and only pay for UNION when you genuinely need distinct rows.
--   3. The anomalies: DIRTY READ (reading uncommitted data), NON-REPEATABLE READ (the same row
--      changes between two reads), PHANTOM READ (new rows appear in the same range).
--      READ UNCOMMITTED allows all three. READ COMMITTED stops dirty reads.
--      REPEATABLE READ also stops non-repeatable reads. SERIALIZABLE stops all three.
--   4. Normalisation: 1NF atomic values, 2NF no partial dependency on part of a composite key,
--      3NF no transitive dependency on a non-key. Denormalise deliberately for read-heavy
--      reporting, and accept that you now own the duplication.
--   5. Execution plan: read it inside-out. Look for a full SCAN on a large table, an index
--      SEARCH where you expected one, the join ORDER, and the estimated vs actual row counts.
--
-- APPROACHES  (reflexes worth having)
--   TOPIC                                        REFLEX
--   ---------------------------------------------------------------------------------------------
--   Remove all rows, keep the table              TRUNCATE, unless you need WHERE or triggers
--   Combine two result sets                      UNION ALL by default
--   Count rows                                   COUNT(*); COUNT(col) silently skips NULLs
--   Deduplicate                                  GROUP BY or DISTINCT, never a self-join
--   A slow query                                 EXPLAIN first; never guess at an index
--
-- WHY THIS ONE
--   These are warm-ups, and the interviewer is calibrating how deep to go. One crisp sentence
--   each, with a consequence attached ("TRUNCATE is faster because it deallocates pages rather
--   than logging row deletions") buys you the rest of the hour at a senior level. Waffling here
--   costs more than getting a hard query slightly wrong later.
--
-- SAMJHO
--   Har definition ek line + ek consequence. DELETE = DML, WHERE chalta hai, rollback hota hai.
--   TRUNCATE = DDL, saari rows, bahut fast; rollback hoga ya nahi, engine pe depend karta hai.
--   UNION duplicate hatane ke liye sort/hash karta hai, UNION ALL bas jod deta hai.
--   Isolation level jitna upar, utni anomalies band, par utna zyada locking ya retry.
--
-- AISE BOLO  (30-40 sec)
--   "I'd first say which engine I mean, because TRUNCATE and isolation differ between Oracle and
--   PostgreSQL. DELETE is DML: it takes a WHERE, fires triggers, is logged row by row and can be
--   rolled back. TRUNCATE is DDL that drops the data in one go, so it is much faster but has no
--   WHERE; in Oracle it commits implicitly, while in PostgreSQL it can be rolled back inside a
--   transaction. I default to UNION ALL, because UNION adds a sort or hash over the whole result
--   just to remove duplicates, and both Oracle and PostgreSQL default to READ COMMITTED."
--
-- TRAP
--   ! "TRUNCATE rollback nahi hota" har engine pe sach nahi: Oracle aur MySQL mein implicit
--     commit, par Postgres mein transaction ke andar rollback ho jata hai. Engine bol ke jawab do.
--   ! COUNT(col) NULL skip karta hai: is seed mein COUNT(*) = 13 par COUNT(dept_id) = 12.
--
-- FOLLOW-UPS KE JAWAB
--   ? Can TRUNCATE be rolled back? (depends on the engine -- say so, do not guess)
--     -> Oracle: nahi, DDL hai, pehle aur baad mein implicit commit. Postgres: haan, BEGIN ke
--        andar. MySQL: nahi. SQL Server: explicit transaction ke andar haan.
--   ? Which isolation level does your production database actually use?
--     -> Oracle aur Postgres dono ka default READ COMMITTED; Spring ka Isolation.DEFAULT wahi
--        leta hai. [confirm: kisi service ne level change kiya tha ya nahi]
--   ? UNION ALL is faster -- by how much, and why?
--     -> Fixed number nahi. UNION poore result pe sort ya hash karta hai; bade result pe memory
--        aur temp disk lagti hai. UNION ALL rows seedha aage bhej deta hai.
--   ? Give a case where you deliberately denormalised, and what it cost you.
--     -> Design answer: reporting ke liye summary table ya materialized view; read fast, par sync
--        logic aur stale data ka risk. [apna real case ho to woh, warna design answer hi do]
--
-- ALSO UNLOCKS
--   - DDL vs DML vs DCL vs TCL
--   - ACID properties
--   - Transaction anomalies and locking
--   - Index types: B-tree, clustered vs non-clustered, composite, partial, covering
--
-- ORACLE / POSTGRES MEIN
--   Transaction : Oracle mein BEGIN nahi likhte; pehla DML transaction shuru karta hai, phir
--                 COMMIT / ROLLBACK. Postgres mein BEGIN; ... ROLLBACK; yahan jaisa hi.
--   Isolation   : Oracle sirf READ COMMITTED (default) aur SERIALIZABLE deta hai; dirty read
--                 kabhi nahi. Postgres READ UNCOMMITTED ko READ COMMITTED jaisa chalata hai, aur
--                 uska REPEATABLE READ (snapshot) phantom bhi rokta hai.
--   TRUNCATE    : Postgres sequence reset nahi karta jab tak TRUNCATE ... RESTART IDENTITY na
--                 likho. Oracle TRUNCATE ke baad ROLLBACK se data wapas nahi aata.
--   Constant    : Oracle mein SELECT 'x' ke saath FROM dual chahiye (23ai se pehle).
--
-- VARIANTS     : not LeetCode; classic: DELETE vs TRUNCATE vs DROP, UNION vs UNION ALL, ACID
-- ================================================================================================

-- >>> QUERY: UNION removes duplicates, UNION ALL does not
SELECT 'UNION (distinct)' AS variant, COUNT(*) AS rows FROM (
    SELECT city FROM customers UNION     SELECT location FROM departments)
UNION ALL
SELECT 'UNION ALL',               COUNT(*) FROM (
    SELECT city FROM customers UNION ALL SELECT location FROM departments);

-- >>> QUERY: the distinct values, to see what UNION collapsed
SELECT city AS value FROM customers UNION SELECT location FROM departments ORDER BY value;

-- >>> QUERY: DELETE is transactional and takes a WHERE; here it is rolled back
SELECT COUNT(*) AS before_delete FROM orders;

-- >>> QUERY: open a transaction
BEGIN;

-- >>> QUERY: delete inside it
DELETE FROM orders WHERE amount < 5000;

-- >>> QUERY: the rows are gone, but only inside this transaction
SELECT COUNT(*) AS after_delete_inside_txn FROM orders;

-- >>> QUERY: ROLLBACK restores every row -- this is what TRUNCATE often cannot do
ROLLBACK;

-- >>> QUERY: proof the rollback worked
SELECT COUNT(*) AS after_rollback FROM orders;

-- >>> QUERY: COUNT(*) vs COUNT(col) vs COUNT(DISTINCT col)
SELECT COUNT(*) AS count_star, COUNT(dept_id) AS count_col,
       COUNT(DISTINCT dept_id) AS count_distinct FROM employees;

-- >>> QUERY: the ANSI isolation levels and what each allows (a typed table, not engine output)
SELECT 'READ UNCOMMITTED' AS level, 'dirty, non-repeatable, phantom' AS allows
UNION ALL SELECT 'READ COMMITTED',  'non-repeatable, phantom'
UNION ALL SELECT 'REPEATABLE READ', 'phantom'
UNION ALL SELECT 'SERIALIZABLE',    'none';

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [UNION removes duplicates, UNION ALL does not]
--   variant           rows
--   ----------------  ----
--   UNION (distinct)  3
--   UNION ALL         9
--   (2 rows)
--
-- [the distinct values, to see what UNION collapsed]
--   value
--   ------
--   Indore
--   Mumbai
--   Pune
--   (3 rows)
--
-- [DELETE is transactional and takes a WHERE; here it is rolled back]
--   before_delete
--   -------------
--   10
--   (1 row)
--
-- [open a transaction]
--   (no result set)
--
-- [delete inside it]
--   (no result set)
--
-- [the rows are gone, but only inside this transaction]
--   after_delete_inside_txn
--   -----------------------
--   7
--   (1 row)
--
-- [ROLLBACK restores every row -- this is what TRUNCATE often cannot do]
--   (no result set)
--
-- [proof the rollback worked]
--   after_rollback
--   --------------
--   10
--   (1 row)
--
-- [COUNT(*) vs COUNT(col) vs COUNT(DISTINCT col)]
--   count_star  count_col  count_distinct
--   ----------  ---------  --------------
--   13          12         3
--   (1 row)
--
-- [the ANSI isolation levels and what each allows (a typed table, not engine output)]
--   level             allows
--   ----------------  ------------------------------
--   READ UNCOMMITTED  dirty, non-repeatable, phantom
--   READ COMMITTED    non-repeatable, phantom
--   REPEATABLE READ   phantom
--   SERIALIZABLE      none
--   (4 rows)
--
-- ======================================================================================
