-- ================================================================================================
-- S25  .  Running balance per account   (the balance after every transaction)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Window Functions
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every fintech / payments round
-- PRIORITY     : P1   -- fintech / remittance round mein ledger se running balance pakka aata hai
-- TECHNIQUE    : Signed amount with CASE, then SUM() OVER (PARTITION BY account_id ORDER BY
--                txn_ts, txn_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
--
-- PROBLEM
--   A txn table has one row per transaction: txn_id, account_id, txn_ts, txn_type (CREDIT or
--   DEBIT) and amount (always positive). Show the balance after every transaction, per account.
--   Then: the latest balance per account, the first time each account went negative, and the
--   opening balance plus movements for 2025-07-03 to 2025-07-05.
--   Data: an inline CTE, the same in every block, 3 accounts and 13 transactions. In account 101
--   a remittance (txn 8) and its fee (txn 9) post in the same second, and the fee takes the
--   account below zero. Account 102 goes negative twice. Account 103 does not move in the range.
--
-- ALGORITHM
--   1. Turn the type into a sign first: CASE txn_type WHEN 'CREDIT' THEN amount WHEN 'DEBIT'
--      THEN -amount END. After that a balance is just a sum.
--   2. SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id ROWS BETWEEN
--      UNBOUNDED PRECEDING AND CURRENT ROW): PARTITION BY restarts the sum for every account,
--      the frame adds every earlier row of that account plus this one.
--   3. txn_id is the tiebreaker. Two postings in the same second are normal (a transfer and its
--      fee); with ORDER BY txn_ts alone the default RANGE frame gives both the same balance.
--   4. Latest balance: GROUP BY SUM when only the number is needed; ROW_NUMBER() = 1 over
--      txn_ts DESC, txn_id DESC when the last row (its time, its txn_id) is needed too.
--   5. A window result cannot be filtered in its own WHERE. Build the balance in a CTE, filter
--      balance < 0 one level out, and ROW_NUMBER() = 1 picks the first overdraft.
--   6. Date range: compute the balance over the FULL history and filter outside, or sum the rows
--      before the range as the opening balance. Filtering first restarts the sum at zero.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Correlated subquery: SUM of earlier rows     avoid         O(n^2) per account
--   Self-join on t2.txn_ts <= t1.txn_ts          avoid         same cost, same tie bug as RANGE
--   SUM() OVER (... ORDER BY txn_ts)             wrong         default RANGE: same-second rows
--                                                              share one balance
--   SUM() OVER (... ORDER BY txn_ts, txn_id      WRITE THIS    one scan plus a sort, exact for
--     ROWS BETWEEN UNBOUNDED PRECEDING ...)                    every row
--   Java loop over the rows sorted by time       acceptable    fine for one statement; ships
--                                                              every row to the app
--
-- WHY THIS ONE
--   In a fintech round this is the window question, because a statement, an overdraft check and
--   a reconciliation all start from it. What separates answers is the tie: a transfer and its
--   fee in the same second. Say "ORDER BY txn_ts, txn_id, and ROWS" before they ask, then show
--   that a window result is filtered one level out (first overdraft, date range).
--
-- SAMJHO
--   CREDIT ko plus aur DEBIT ko minus banao, phir har account ke andar time order mein SUM karte
--   jao -- yahi running balance hai. Same second mein do txn ho sakte hain, isliye ORDER BY mein
--   txn_id bhi daalo aur ROWS likho. Balance pe filter lagana ho (negative, date range) to
--   pehle CTE mein balance banao, phir bahar filter karo.
--
-- AISE BOLO  (30-40 sec)
--   "I'd turn the type into a sign with a CASE first, so a debit is a negative amount. A
--   correlated subquery over all earlier rows works but is O(n squared) per account, so I use
--   SUM of the signed amount over PARTITION BY account_id, ORDER BY txn_ts and txn_id, with
--   ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW. That is one scan plus a sort, and an
--   index on account_id, txn_ts, txn_id can remove the sort. The edge case is a transfer and
--   its fee in the same second: with txn_ts alone the default RANGE frame gives both rows the
--   same balance, so the txn_id tiebreaker and ROWS are not optional."
--
-- TRAP
--   ! ORDER BY txn_ts akela + default RANGE: txn 8 aur 9 dono -50 (sahi 100 aur -50), aur pehla
--     overdraft fee (txn 9) ki jagah remittance (txn 8) pe lagta hai. txn_id + ROWS likho.
--   ! Date range ka WHERE window ke saath lagaya to balance zero se shuru hota hai (5b ka
--     restarts_at_0: 101 pe -7400, sahi 100). Poori history pe balance banao, bahar filter karo.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why keep a ledger with a running balance instead of storing only the current balance?
--     -> Sirf balance mein history nahi hoti: dispute ya recon mein "yeh number kaise aaya" nahi
--        bata sakte. Ledger append-only source of truth hai; balance column fast read ke liye
--        rakho, aur recon check kare ki SUM(ledger) = balance.
--   ? How does a real system keep a balance column consistent with the ledger?
--     -> Ek hi transaction mein ledger INSERT + conditional UPDATE: SET balance = balance - :amt
--        WHERE account_id = :id AND balance >= :amt; 0 rows = balance kam, rollback. Ya version
--        column (JPA @Version) se optimistic lock, 0 rows pe retry. Bina lock read-add-write
--        = lost update.
--   ? What index supports this query?
--     -> (account_id, txn_ts, txn_id): PARTITION BY + ORDER BY isi order mein, to rows sorted
--        aati hain aur sort hat sakta hai. Latest balance (3a) isi index ko ulta padh sakta hai.
--   ? The table has 10 million rows -- does this still work?
--     -> Haan: PARTITION BY window ek scan + sort hai, O(n log n); correlated subquery O(n^2),
--        avoid. Sirf zaroori accounts / dates padho; roz ka report ho to daily closing balance
--        ka snapshot + us din ke movements (5a wala opening + movements).
--   ? Which data type holds money, in the database and in Java?
--     -> DB: Oracle NUMBER(18,2), Postgres NUMERIC(18,2); FLOAT kabhi nahi (0.1 + 0.2 != 0.3).
--        Java: BigDecimal, new BigDecimal("0.10") string se, compare compareTo se (equals scale
--        bhi dekhta hai: 2.0 vs 2.00 false). Ya paise mein long. Yahan SQLite ke liye INTEGER.
--   ? Latest balance: GROUP BY SUM or ROW_NUMBER -- which one, when?
--     -> Sirf number chahiye to GROUP BY SUM (3b): window ya row order ki zaroorat nahi. Last txn
--        ki row bhi chahiye, ya table har row pe balance_after rakhti hai, to ROW_NUMBER() = 1
--        (3a). Yahan dono same: 1750, 1200, 2500.
--   ? Every time an account went negative, not only the first time?
--     -> bal ke upar LAG(balance) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id) AS prev;
--        balance < 0 AND (prev >= 0 OR prev IS NULL) = naya overdraft. 102 ke do: txn 6, txn 11.
--
-- ALSO UNLOCKS
--   - Account statement with opening and closing balance
--   - Wallet / prepaid balance after every top-up and spend
--   - Stock on hand after every stock-in and stock-out
--   - First time usage crossed a credit limit
--
-- ORACLE / POSTGRES MEIN
--   - Window aur ROWS frame dono mein same hain. Real table mein txn_ts TIMESTAMP hoga; range ke
--     liye TIMESTAMP '2025-07-03 00:00:00' literal likho. Yahan txn_ts ISO TEXT hai, isliye
--     '2025-07-03' se string compare chal jaata hai -- yeh SQLite-ism hai.
--   - WITH txn (...) AS (VALUES ...) Oracle 19c mein nahi chalta: SELECT ... FROM dual UNION ALL.
--   - Latest row ka shortcut: Postgres DISTINCT ON (account_id) ... ORDER BY account_id,
--     txn_ts DESC, txn_id DESC; Oracle MAX(balance) KEEP (DENSE_RANK LAST ORDER BY txn_ts,
--     txn_id) + GROUP BY account_id.
--   - Conditional UPDATE ke baad naya balance: Postgres RETURNING balance, Oracle RETURNING
--     balance INTO :b. JDBC mein executeUpdate() ka row count check karo.
--
-- APNI STORY SE JODO
--   "Most of my 8 years have been in fintech and remittance backends, where every balance has to
--   be explained line by line. So I think of a balance as a running sum over the transactions,
--   not as a number that gets overwritten."
--   Note: Sirf domain ka framing hai. Koi project, table ya number mat jodo; poochhe to
--   [yahan sach bharo].
--
-- VARIANTS     : LC 2066 (balance after each transaction), LC 1587 (balance per account with
--                GROUP BY SUM); classic: bank statement with opening and closing balance
-- ================================================================================================

-- >>> QUERY: step 1 -- running balance per account: signed CASE, ROWS frame, txn_id tiebreaker
WITH txn (txn_id, account_id, txn_ts, txn_type, amount) AS (VALUES
    ( 1,101,'2025-07-01 09:00:00','CREDIT',10000), ( 2,102,'2025-07-01 11:30:00','CREDIT', 2000),
    ( 3,103,'2025-07-01 15:00:00','CREDIT', 4000), ( 4,103,'2025-07-02 09:05:00','DEBIT',  1500),
    ( 5,101,'2025-07-02 10:15:00','DEBIT',  2500), ( 6,102,'2025-07-02 18:40:00','DEBIT',  2500),
    ( 7,102,'2025-07-03 09:00:00','CREDIT', 1000), ( 8,101,'2025-07-03 12:00:00','DEBIT',  7400),
    ( 9,101,'2025-07-03 12:00:00','DEBIT',   150), (10,101,'2025-07-04 16:20:00','CREDIT', 3000),
    (11,102,'2025-07-05 14:10:00','DEBIT',   800), (12,101,'2025-07-06 08:00:00','DEBIT',  1200),
    (13,102,'2025-07-06 10:00:00','CREDIT', 1500)),
     signed_txn AS (
    SELECT t.*, CASE t.txn_type WHEN 'CREDIT' THEN  t.amount
                                WHEN 'DEBIT'  THEN -t.amount END AS signed_amount
    FROM   txn t)
SELECT account_id, txn_id, txn_ts, txn_type, amount, signed_amount,
       SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id
                                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance
FROM   signed_txn
ORDER  BY account_id, txn_ts, txn_id;

-- >>> QUERY: trap 2 -- ORDER BY txn_ts alone (default RANGE): txn 8 and 9 share one balance
WITH txn (txn_id, account_id, txn_ts, txn_type, amount) AS (VALUES
    ( 1,101,'2025-07-01 09:00:00','CREDIT',10000), ( 2,102,'2025-07-01 11:30:00','CREDIT', 2000),
    ( 3,103,'2025-07-01 15:00:00','CREDIT', 4000), ( 4,103,'2025-07-02 09:05:00','DEBIT',  1500),
    ( 5,101,'2025-07-02 10:15:00','DEBIT',  2500), ( 6,102,'2025-07-02 18:40:00','DEBIT',  2500),
    ( 7,102,'2025-07-03 09:00:00','CREDIT', 1000), ( 8,101,'2025-07-03 12:00:00','DEBIT',  7400),
    ( 9,101,'2025-07-03 12:00:00','DEBIT',   150), (10,101,'2025-07-04 16:20:00','CREDIT', 3000),
    (11,102,'2025-07-05 14:10:00','DEBIT',   800), (12,101,'2025-07-06 08:00:00','DEBIT',  1200),
    (13,102,'2025-07-06 10:00:00','CREDIT', 1500)),
     signed_txn AS (
    SELECT t.*, CASE t.txn_type WHEN 'CREDIT' THEN  t.amount
                                WHEN 'DEBIT'  THEN -t.amount END AS signed_amount
    FROM   txn t)
-- ts_only_range is the bug. txn_id_range is right only because (txn_ts, txn_id) is unique.
-- ORDER BY txn_ts ROWS without txn_id gives two values, but txn 8 / 9 order is not defined.
SELECT txn_id, txn_ts, signed_amount,
       SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts)         AS ts_only_range,
       SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id) AS txn_id_range,
       SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id
                                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS txn_id_rows
FROM   signed_txn
WHERE  account_id = 101      -- drops whole partitions only, so it is safe before the window
ORDER  BY txn_ts, txn_id;

-- >>> QUERY: step 3a -- latest balance per account: ROW_NUMBER() = 1 on the running balance
WITH txn (txn_id, account_id, txn_ts, txn_type, amount) AS (VALUES
    ( 1,101,'2025-07-01 09:00:00','CREDIT',10000), ( 2,102,'2025-07-01 11:30:00','CREDIT', 2000),
    ( 3,103,'2025-07-01 15:00:00','CREDIT', 4000), ( 4,103,'2025-07-02 09:05:00','DEBIT',  1500),
    ( 5,101,'2025-07-02 10:15:00','DEBIT',  2500), ( 6,102,'2025-07-02 18:40:00','DEBIT',  2500),
    ( 7,102,'2025-07-03 09:00:00','CREDIT', 1000), ( 8,101,'2025-07-03 12:00:00','DEBIT',  7400),
    ( 9,101,'2025-07-03 12:00:00','DEBIT',   150), (10,101,'2025-07-04 16:20:00','CREDIT', 3000),
    (11,102,'2025-07-05 14:10:00','DEBIT',   800), (12,101,'2025-07-06 08:00:00','DEBIT',  1200),
    (13,102,'2025-07-06 10:00:00','CREDIT', 1500)),
     signed_txn AS (
    SELECT t.*, CASE t.txn_type WHEN 'CREDIT' THEN  t.amount
                                WHEN 'DEBIT'  THEN -t.amount END AS signed_amount
    FROM   txn t),
     bal AS (
    SELECT s.*,
           SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id
                                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance
    FROM   signed_txn s)
SELECT account_id, txn_id AS last_txn_id, txn_ts AS last_txn_ts, balance
FROM  (SELECT b.*,
              ROW_NUMBER() OVER (PARTITION BY account_id
                                 ORDER BY txn_ts DESC, txn_id DESC) AS rn  -- same tiebreaker
       FROM   bal b) x
WHERE  rn = 1
ORDER  BY account_id;

-- >>> QUERY: step 3b -- current balance only: a plain GROUP BY SUM (no window, no row order)
WITH txn (txn_id, account_id, txn_ts, txn_type, amount) AS (VALUES
    ( 1,101,'2025-07-01 09:00:00','CREDIT',10000), ( 2,102,'2025-07-01 11:30:00','CREDIT', 2000),
    ( 3,103,'2025-07-01 15:00:00','CREDIT', 4000), ( 4,103,'2025-07-02 09:05:00','DEBIT',  1500),
    ( 5,101,'2025-07-02 10:15:00','DEBIT',  2500), ( 6,102,'2025-07-02 18:40:00','DEBIT',  2500),
    ( 7,102,'2025-07-03 09:00:00','CREDIT', 1000), ( 8,101,'2025-07-03 12:00:00','DEBIT',  7400),
    ( 9,101,'2025-07-03 12:00:00','DEBIT',   150), (10,101,'2025-07-04 16:20:00','CREDIT', 3000),
    (11,102,'2025-07-05 14:10:00','DEBIT',   800), (12,101,'2025-07-06 08:00:00','DEBIT',  1200),
    (13,102,'2025-07-06 10:00:00','CREDIT', 1500)),
     signed_txn AS (
    SELECT t.*, CASE t.txn_type WHEN 'CREDIT' THEN  t.amount
                                WHEN 'DEBIT'  THEN -t.amount END AS signed_amount
    FROM   txn t)
SELECT account_id,
       SUM(signed_amount) AS balance,
       COUNT(*)           AS txn_count,
       MAX(txn_ts)        AS last_txn_ts
FROM   signed_txn
GROUP  BY account_id
ORDER  BY account_id;

-- >>> QUERY: step 4 -- first time each account went negative (overdraft detection)
WITH txn (txn_id, account_id, txn_ts, txn_type, amount) AS (VALUES
    ( 1,101,'2025-07-01 09:00:00','CREDIT',10000), ( 2,102,'2025-07-01 11:30:00','CREDIT', 2000),
    ( 3,103,'2025-07-01 15:00:00','CREDIT', 4000), ( 4,103,'2025-07-02 09:05:00','DEBIT',  1500),
    ( 5,101,'2025-07-02 10:15:00','DEBIT',  2500), ( 6,102,'2025-07-02 18:40:00','DEBIT',  2500),
    ( 7,102,'2025-07-03 09:00:00','CREDIT', 1000), ( 8,101,'2025-07-03 12:00:00','DEBIT',  7400),
    ( 9,101,'2025-07-03 12:00:00','DEBIT',   150), (10,101,'2025-07-04 16:20:00','CREDIT', 3000),
    (11,102,'2025-07-05 14:10:00','DEBIT',   800), (12,101,'2025-07-06 08:00:00','DEBIT',  1200),
    (13,102,'2025-07-06 10:00:00','CREDIT', 1500)),
     signed_txn AS (
    SELECT t.*, CASE t.txn_type WHEN 'CREDIT' THEN  t.amount
                                WHEN 'DEBIT'  THEN -t.amount END AS signed_amount
    FROM   txn t),
     bal AS (
    SELECT s.*,
           SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id
                                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance
    FROM   signed_txn s),
     neg AS (
    SELECT b.*, ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id) AS rn
    FROM   bal b
    WHERE  balance < 0)      -- the window result is filtered one level out, not in its own WHERE
SELECT account_id, txn_id, txn_ts, signed_amount, balance
FROM   neg
WHERE  rn = 1                -- account 103 never goes negative, so it has no row
ORDER  BY account_id;

-- >>> QUERY: step 5a -- opening + credits - debits = closing, for 2025-07-03 to 2025-07-05
WITH txn (txn_id, account_id, txn_ts, txn_type, amount) AS (VALUES
    ( 1,101,'2025-07-01 09:00:00','CREDIT',10000), ( 2,102,'2025-07-01 11:30:00','CREDIT', 2000),
    ( 3,103,'2025-07-01 15:00:00','CREDIT', 4000), ( 4,103,'2025-07-02 09:05:00','DEBIT',  1500),
    ( 5,101,'2025-07-02 10:15:00','DEBIT',  2500), ( 6,102,'2025-07-02 18:40:00','DEBIT',  2500),
    ( 7,102,'2025-07-03 09:00:00','CREDIT', 1000), ( 8,101,'2025-07-03 12:00:00','DEBIT',  7400),
    ( 9,101,'2025-07-03 12:00:00','DEBIT',   150), (10,101,'2025-07-04 16:20:00','CREDIT', 3000),
    (11,102,'2025-07-05 14:10:00','DEBIT',   800), (12,101,'2025-07-06 08:00:00','DEBIT',  1200),
    (13,102,'2025-07-06 10:00:00','CREDIT', 1500)),
     signed_txn AS (
    SELECT t.*, CASE t.txn_type WHEN 'CREDIT' THEN  t.amount
                                WHEN 'DEBIT'  THEN -t.amount END AS signed_amount
    FROM   txn t)
SELECT account_id,
       SUM(CASE WHEN txn_ts <  '2025-07-03' THEN signed_amount ELSE 0 END) AS opening,
       SUM(CASE WHEN txn_ts >= '2025-07-03' AND txn_type = 'CREDIT'
                THEN amount ELSE 0 END)                                    AS credits,
       SUM(CASE WHEN txn_ts >= '2025-07-03' AND txn_type = 'DEBIT'
                THEN amount ELSE 0 END)                                    AS debits,
       SUM(signed_amount)                                                  AS closing
FROM   signed_txn
WHERE  txn_ts < '2025-07-06'   -- half-open: up to the END of the range, from the very first row
GROUP  BY account_id           -- so 103, with no movement in the range, still shows up
ORDER  BY account_id;

-- >>> QUERY: trap 5b -- statement lines for the range: balance over FULL history, filter outside
WITH txn (txn_id, account_id, txn_ts, txn_type, amount) AS (VALUES
    ( 1,101,'2025-07-01 09:00:00','CREDIT',10000), ( 2,102,'2025-07-01 11:30:00','CREDIT', 2000),
    ( 3,103,'2025-07-01 15:00:00','CREDIT', 4000), ( 4,103,'2025-07-02 09:05:00','DEBIT',  1500),
    ( 5,101,'2025-07-02 10:15:00','DEBIT',  2500), ( 6,102,'2025-07-02 18:40:00','DEBIT',  2500),
    ( 7,102,'2025-07-03 09:00:00','CREDIT', 1000), ( 8,101,'2025-07-03 12:00:00','DEBIT',  7400),
    ( 9,101,'2025-07-03 12:00:00','DEBIT',   150), (10,101,'2025-07-04 16:20:00','CREDIT', 3000),
    (11,102,'2025-07-05 14:10:00','DEBIT',   800), (12,101,'2025-07-06 08:00:00','DEBIT',  1200),
    (13,102,'2025-07-06 10:00:00','CREDIT', 1500)),
     signed_txn AS (
    SELECT t.*, CASE t.txn_type WHEN 'CREDIT' THEN  t.amount
                                WHEN 'DEBIT'  THEN -t.amount END AS signed_amount
    FROM   txn t),
     bal AS (
    SELECT s.*,
           SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id
                                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance
    FROM   signed_txn s)
-- balance was computed in bal over the full history. restarts_at_0 is computed here, AFTER this
-- WHERE, so it only sees the range -- the same wrong answer as filtering before the window.
SELECT account_id, txn_id, txn_ts, signed_amount,
       balance - signed_amount AS bal_before,     -- on the first row of the range = opening
       balance                 AS bal_after,
       SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id
                                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS restarts_at_0
FROM   bal
WHERE  txn_ts >= '2025-07-03' AND txn_ts < '2025-07-06'
ORDER  BY account_id, txn_ts, txn_id;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [step 1 -- running balance per account: signed CASE, ROWS frame, txn_id tiebreaker]
--   account_id  txn_id  txn_ts               txn_type  amount  signed_amount  balance
--   ----------  ------  -------------------  --------  ------  -------------  -------
--   101         1       2025-07-01 09:00:00  CREDIT    10000   10000          10000
--   101         5       2025-07-02 10:15:00  DEBIT     2500    -2500          7500
--   101         8       2025-07-03 12:00:00  DEBIT     7400    -7400          100
--   101         9       2025-07-03 12:00:00  DEBIT     150     -150           -50
--   101         10      2025-07-04 16:20:00  CREDIT    3000    3000           2950
--   101         12      2025-07-06 08:00:00  DEBIT     1200    -1200          1750
--   102         2       2025-07-01 11:30:00  CREDIT    2000    2000           2000
--   102         6       2025-07-02 18:40:00  DEBIT     2500    -2500          -500
--   102         7       2025-07-03 09:00:00  CREDIT    1000    1000           500
--   102         11      2025-07-05 14:10:00  DEBIT     800     -800           -300
--   102         13      2025-07-06 10:00:00  CREDIT    1500    1500           1200
--   103         3       2025-07-01 15:00:00  CREDIT    4000    4000           4000
--   103         4       2025-07-02 09:05:00  DEBIT     1500    -1500          2500
--   (13 rows)
--
-- [trap 2 -- ORDER BY txn_ts alone (default RANGE): txn 8 and 9 share one balance]
--   txn_id  txn_ts               signed_amount  ts_only_range  txn_id_range  txn_id_rows
--   ------  -------------------  -------------  -------------  ------------  -----------
--   1       2025-07-01 09:00:00  10000          10000          10000         10000
--   5       2025-07-02 10:15:00  -2500          7500           7500          7500
--   8       2025-07-03 12:00:00  -7400          -50            100           100
--   9       2025-07-03 12:00:00  -150           -50            -50           -50
--   10      2025-07-04 16:20:00  3000           2950           2950          2950
--   12      2025-07-06 08:00:00  -1200          1750           1750          1750
--   (6 rows)
--
-- [step 3a -- latest balance per account: ROW_NUMBER() = 1 on the running balance]
--   account_id  last_txn_id  last_txn_ts          balance
--   ----------  -----------  -------------------  -------
--   101         12           2025-07-06 08:00:00  1750
--   102         13           2025-07-06 10:00:00  1200
--   103         4            2025-07-02 09:05:00  2500
--   (3 rows)
--
-- [step 3b -- current balance only: a plain GROUP BY SUM (no window, no row order)]
--   account_id  balance  txn_count  last_txn_ts
--   ----------  -------  ---------  -------------------
--   101         1750     6          2025-07-06 08:00:00
--   102         1200     5          2025-07-06 10:00:00
--   103         2500     2          2025-07-02 09:05:00
--   (3 rows)
--
-- [step 4 -- first time each account went negative (overdraft detection)]
--   account_id  txn_id  txn_ts               signed_amount  balance
--   ----------  ------  -------------------  -------------  -------
--   101         9       2025-07-03 12:00:00  -150           -50
--   102         6       2025-07-02 18:40:00  -2500          -500
--   (2 rows)
--
-- [step 5a -- opening + credits - debits = closing, for 2025-07-03 to 2025-07-05]
--   account_id  opening  credits  debits  closing
--   ----------  -------  -------  ------  -------
--   101         7500     3000     7550    2950
--   102         -500     1000     800     -300
--   103         2500     0        0       2500
--   (3 rows)
--
-- [trap 5b -- statement lines for the range: balance over FULL history, filter outside]
--   account_id  txn_id  txn_ts               signed_amount  bal_before  bal_after  restarts_at_0
--   ----------  ------  -------------------  -------------  ----------  ---------  -------------
--   101         8       2025-07-03 12:00:00  -7400          7500        100        -7400
--   101         9       2025-07-03 12:00:00  -150           100         -50        -7550
--   101         10      2025-07-04 16:20:00  3000           -50         2950       -4550
--   102         7       2025-07-03 09:00:00  1000           -500        500        1000
--   102         11      2025-07-05 14:10:00  -800           500         -300       200
--   (5 rows)
--
-- ======================================================================================
