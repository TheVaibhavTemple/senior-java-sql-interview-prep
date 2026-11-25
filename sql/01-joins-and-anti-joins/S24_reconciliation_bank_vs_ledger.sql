-- ================================================================================================
-- S24  .  Reconcile the bank statement against the ledger   (FULL OUTER JOIN + status)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Joins & Anti-joins
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every fintech / payments round
-- PRIORITY     : P1   -- fintech profile ka core sawaal; daily recon har payments team chalati hai
-- TECHNIQUE    : One row per txn_ref on each side, FULL OUTER JOIN, CASE for the status
--
-- PROBLEM
--   bank_statement is what the bank says happened; ledger is what our system posted. Match them
--   on txn_ref and label every reference MATCHED, AMOUNT_MISMATCH, MISSING_IN_LEDGER or
--   MISSING_IN_BANK. Then give a summary: how many references and how much money per status.
--
-- ALGORITHM
--   1. Check duplicate txn_ref on each side FIRST. A repeated ref breaks the 1:1 match and fans
--      out the join (S03): a double posting joins twice and looks like two clean matches.
--   2. Collapse each side to one row per txn_ref (GROUP BY, SUM), so the join is 1:1 by design.
--   3. FULL OUTER JOIN the two on txn_ref: rows from both sides survive, the absent side is NULL.
--   4. CASE: ledger side NULL -> MISSING_IN_LEDGER, bank side NULL -> MISSING_IN_BANK,
--      amounts equal -> MATCHED, anything else -> AMOUNT_MISMATCH.
--   5. No FULL OUTER JOIN (MySQL, SQLite before 3.39)? LEFT JOIN bank -> ledger, then UNION ALL
--      the ledger refs that have no bank row (an anti-join with NOT EXISTS).
--   6. GROUP BY status for the summary. Its totals must equal each side's raw SUM (control total).
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Both sides into Java, match in a HashMap     acceptable    fine for a small file; for 10M rows
--                                                              it ships every row to the app
--   INNER JOIN on txn_ref                        avoid         hides every missing entry
--   Raw FULL OUTER JOIN, no duplicate check      avoid         a double posting shows as MATCHED
--   Pre-aggregate + FULL OUTER JOIN + CASE       WRITE THIS    every outcome labelled, one query
--   LEFT JOIN + UNION ALL NOT EXISTS             WRITE THIS    same rows, runs on any engine
--   NOT IN for the missing side                  avoid         one NULL ref and it returns nothing
--
-- WHY THIS ONE
--   Reconciliation is the fintech SQL question, because finance runs it every single day. The
--   sentence that lands is "duplicates first, then a full outer join with a status per
--   reference": it shows you know a reconciliation has four outcomes, not two, and that a
--   one-to-many join quietly turns a double posting into a match. Offer the portable version
--   for engines without FULL OUTER JOIN before they ask.
--
-- SAMJHO
--   Bank batata hai kya hua, ledger batata hai humne kya post kiya. Dono ko txn_ref pe milao.
--   Pehle duplicate check karo: ek ref do baar hai to join double ho jaata hai, galti chhup jaati.
--   Har side ko ek row per ref pe GROUP BY karo, phir FULL OUTER JOIN. Jo side NULL, wahi missing.
--   FULL OUTER JOIN nahi hai to LEFT JOIN + UNION ALL + NOT EXISTS, same rows.
--
-- AISE BOLO  (30-40 sec)
--   "Before matching I'd ask what the key is, whether txn_ref is unique on each side, and whether
--   amounts must match exactly or within a tolerance. I check duplicates first, because a
--   repeated ref fans out the join and a double posting would show up as two matches. Then I
--   collapse each side to one row per ref and FULL OUTER JOIN them, with a CASE for MATCHED,
--   AMOUNT_MISMATCH, MISSING_IN_LEDGER and MISSING_IN_BANK; as a hash join that is one pass over
--   each table. On MySQL, which has no FULL OUTER JOIN, I write a LEFT JOIN plus a UNION ALL of
--   a NOT EXISTS anti-join, never NOT IN."
--
-- TRAP
--   ! Duplicate check chhoda to TXN1002 (ledger mein do baar) raw join mein do MATCHED rows deta
--     hai. Double posting "match" ban ke nikal jaati hai.
--   ! CASE mein ELSE 'MATCHED' mat rakho: missing row pe amount compare UNKNOWN hota hai aur seedha
--     ELSE mein chala jaata hai. Missing pehle check karo, ELSE ko AMOUNT_MISMATCH rakho.
--
-- FOLLOW-UPS KE JAWAB
--   ? Amounts can differ by a small fee and dates by a day -- how do you match with a tolerance?
--     -> Ref pe join rakho; CASE mein ABS(bk.amount - lg.amount) <= :tol to MATCHED_WITHIN_TOL
--        (TXN1006, 50 ka fark). Date ke liye value_date vs posted_date <= 1 din (TXN1003).
--   ? Why not join on txn_ref AND amount together?
--     -> Tab mismatch wala ref dono taraf "missing" dikhega aur AMOUNT_MISMATCH status kho jaayega.
--        Ref pe join karo, amount CASE mein compare karo.
--   ? Why is NOT IN risky for the MISSING_IN_BANK side?
--     -> NOT IN (SELECT txn_ref FROM bank_statement) mein ek bhi NULL aaya to zero rows: recon
--        "sab clean" bolega jabki entries missing hain. NOT EXISTS NULL-safe hai (S21).
--   ? What index does this need?
--     -> Dono taraf txn_ref ka index (ix_bank_txn_ref, ix_ledger_txn_ref): anti-join probe ban
--        jaata hai. Daily recon date se filter karta hai, to (date, txn_ref) composite aur behtar.
--   ? The ledger has 10M rows -- how do you run this every day?
--     -> Sirf us din ki window padho (late posting ke liye +1-2 din), date index ya partition se.
--        EXPLAIN PLAN mein full scan na ho; exceptions INSERT ... SELECT se recon table mein likho.
--
-- ALSO UNLOCKS
--   - Payment gateway settlement file vs our orders
--   - Source vs target checks after a data migration (row counts, totals, missing keys)
--   - Two systems that must agree: in A only, in B only, in both but different
--   - Any "compare two lists" question
--
-- ORACLE / POSTGRES MEIN
--   - FULL OUTER JOIN dono mein hai. Oracle ka purana (+) syntax full outer nahi kar sakta
--     (dono taraf (+) error deta hai), isliye ANSI FULL OUTER JOIN likho.
--   - Date window: Oracle mein DATE - DATE din deta hai (fraction ke saath), Postgres mein
--     date - date integer din. SQLite mein julianday(a) - julianday(b).
--   - Amount column Oracle mein NUMBER(18,2), Postgres mein NUMERIC(18,2). FLOAT kabhi nahi.
--
-- APNI STORY SE JODO
--   "A daily reconciliation query I worked on ran on an Oracle table of more than 10 million rows
--   and took 45 minutes. The EXPLAIN plan showed a full table scan; the fix was a missing index,
--   and the query came down to 8 seconds."
--   Note: Bas itna hi fact hai. Kaunsa column ya kaunsa index tha, ye [yahan sach bharo]; na ho
--   to design answer do (upar index wala follow-up).
--
-- VARIANTS     : LC 1965 (ids missing on either side); classic: settlement file vs orders
-- ================================================================================================

-- >>> QUERY: step 1 -- duplicate txn_ref on either side (a duplicate breaks the 1:1 match)
SELECT 'bank_statement' AS side, txn_ref, COUNT(*) AS cnt, SUM(amount) AS total
FROM   bank_statement
GROUP  BY txn_ref
HAVING COUNT(*) > 1
UNION ALL
SELECT 'ledger', txn_ref, COUNT(*), SUM(amount)
FROM   ledger
GROUP  BY txn_ref
HAVING COUNT(*) > 1
ORDER  BY side, txn_ref;

-- >>> QUERY: FULL OUTER JOIN version (SQLite 3.39+, Oracle, PostgreSQL; not MySQL)
WITH bk AS (SELECT txn_ref, SUM(amount) AS amount FROM bank_statement GROUP BY txn_ref),
     lg AS (SELECT txn_ref, SUM(amount) AS amount FROM ledger         GROUP BY txn_ref)
SELECT COALESCE(bk.txn_ref, lg.txn_ref) AS txn_ref,
       bk.amount                        AS bank_amount,
       lg.amount                        AS ledger_amount,
       bk.amount - lg.amount            AS diff,
       CASE WHEN lg.txn_ref IS NULL    THEN 'MISSING_IN_LEDGER'
            WHEN bk.txn_ref IS NULL    THEN 'MISSING_IN_BANK'
            WHEN bk.amount = lg.amount THEN 'MATCHED'
            ELSE 'AMOUNT_MISMATCH'
       END                              AS status
FROM   bk
FULL   OUTER JOIN lg ON lg.txn_ref = bk.txn_ref
ORDER  BY txn_ref;

-- >>> QUERY: portable version -- LEFT JOIN, UNION ALL the anti-join (same rows, any engine)
WITH bk AS (SELECT txn_ref, SUM(amount) AS amount FROM bank_statement GROUP BY txn_ref),
     lg AS (SELECT txn_ref, SUM(amount) AS amount FROM ledger         GROUP BY txn_ref)
SELECT bk.txn_ref,
       bk.amount                        AS bank_amount,
       lg.amount                        AS ledger_amount,
       bk.amount - lg.amount            AS diff,
       CASE WHEN lg.txn_ref IS NULL    THEN 'MISSING_IN_LEDGER'
            WHEN bk.amount = lg.amount THEN 'MATCHED'
            ELSE 'AMOUNT_MISMATCH'
       END                              AS status
FROM   bk
LEFT   JOIN lg ON lg.txn_ref = bk.txn_ref
UNION  ALL
SELECT lg.txn_ref, NULL, lg.amount, NULL, 'MISSING_IN_BANK'
FROM   lg
WHERE  NOT EXISTS (SELECT 1 FROM bk WHERE bk.txn_ref = lg.txn_ref)   -- NULL-safe, unlike NOT IN
ORDER  BY txn_ref;

-- >>> QUERY: summary -- references and money per status (totals = each side's raw SUM)
WITH bk AS (SELECT txn_ref, SUM(amount) AS amount FROM bank_statement GROUP BY txn_ref),
     lg AS (SELECT txn_ref, SUM(amount) AS amount FROM ledger         GROUP BY txn_ref),
     recon AS (
        SELECT bk.amount AS bank_amount, lg.amount AS ledger_amount,
               CASE WHEN lg.txn_ref IS NULL    THEN 'MISSING_IN_LEDGER'
                    WHEN bk.txn_ref IS NULL    THEN 'MISSING_IN_BANK'
                    WHEN bk.amount = lg.amount THEN 'MATCHED'
                    ELSE 'AMOUNT_MISMATCH'
               END AS status
        FROM   bk
        FULL   OUTER JOIN lg ON lg.txn_ref = bk.txn_ref)
SELECT status,
       COUNT(*)                        AS refs,
       COALESCE(SUM(bank_amount), 0)   AS bank_total,
       COALESCE(SUM(ledger_amount), 0) AS ledger_total
FROM   recon
GROUP  BY status
ORDER  BY status;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [step 1 -- duplicate txn_ref on either side (a duplicate breaks the 1:1 match)]
--   side    txn_ref  cnt  total
--   ------  -------  ---  -----
--   ledger  TXN1002  2    24000
--   (1 row)
--
-- [FULL OUTER JOIN version (SQLite 3.39+, Oracle, PostgreSQL; not MySQL)]
--   txn_ref  bank_amount  ledger_amount  diff    status
--   -------  -----------  -------------  ------  -----------------
--   TXN1001  50000        50000          0       MATCHED
--   TXN1002  12000        24000          -12000  AMOUNT_MISMATCH
--   TXN1003  7500         7500           0       MATCHED
--   TXN1004  30000        30000          0       MATCHED
--   TXN1005  25000        2500           22500   AMOUNT_MISMATCH
--   TXN1006  15000        14950          50      AMOUNT_MISMATCH
--   TXN1007  9000         NULL           NULL    MISSING_IN_LEDGER
--   TXN1008  4200         NULL           NULL    MISSING_IN_LEDGER
--   TXN1009  NULL         6100           NULL    MISSING_IN_BANK
--   TXN1010  NULL         1800           NULL    MISSING_IN_BANK
--   (10 rows)
--
-- [portable version -- LEFT JOIN, UNION ALL the anti-join (same rows, any engine)]
--   txn_ref  bank_amount  ledger_amount  diff    status
--   -------  -----------  -------------  ------  -----------------
--   TXN1001  50000        50000          0       MATCHED
--   TXN1002  12000        24000          -12000  AMOUNT_MISMATCH
--   TXN1003  7500         7500           0       MATCHED
--   TXN1004  30000        30000          0       MATCHED
--   TXN1005  25000        2500           22500   AMOUNT_MISMATCH
--   TXN1006  15000        14950          50      AMOUNT_MISMATCH
--   TXN1007  9000         NULL           NULL    MISSING_IN_LEDGER
--   TXN1008  4200         NULL           NULL    MISSING_IN_LEDGER
--   TXN1009  NULL         6100           NULL    MISSING_IN_BANK
--   TXN1010  NULL         1800           NULL    MISSING_IN_BANK
--   (10 rows)
--
-- [summary -- references and money per status (totals = each side's raw SUM)]
--   status             refs  bank_total  ledger_total
--   -----------------  ----  ----------  ------------
--   AMOUNT_MISMATCH    3     52000       41450
--   MATCHED            3     87500       87500
--   MISSING_IN_BANK    2     0           7900
--   MISSING_IN_LEDGER  2     13200       0
--   (4 rows)
--
-- ======================================================================================
