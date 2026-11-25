-- ================================================================================================
-- S26  .  Keyset vs OFFSET pagination   (page by the last row seen, not by a row count)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Performance & Concepts
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every backend round that has a list API
-- PRIORITY     : P1   -- har list API aur batch job mein; "page 5000 slow kyun?" senior favourite
-- TECHNIQUE    : Keyset: WHERE (sort_col, id) > (last row seen) ORDER BY sort_col, id LIMIT n
--
-- PROBLEM
--   An API lists transfers in created_at order, a few rows per page, with LIMIT ... OFFSET.
--   Deep pages are slow, and users sometimes see the same transfer on two pages. Explain both
--   problems, rewrite it as keyset (seek) pagination, and compare the two plans.
--
-- ALGORITHM
--   1. LIMIT n OFFSET k: the engine walks the sort order from the start and throws away k rows
--      before it returns n. The cost grows with the page number: about k + n rows read.
--   2. Keyset: remember the last row of the page, (created_at, id). The next page is
--      WHERE (created_at, id) > (:last_created_at, :last_id) ORDER BY created_at, id LIMIT n.
--   3. An index on (created_at, id), same columns, same order: the WHERE becomes a seek, the
--      ORDER BY needs no sort, and LIMIT stops after n rows. About log N + n at any depth.
--   4. id is the unique tiebreaker. created_at alone is not unique, so created_at > :last
--      would skip the rest of the rows that share the last timestamp.
--   5. No row-value comparison (Oracle)? Expand it:
--      created_at > :c OR (created_at = :c AND id > :id), and put created_at >= :c in front
--      so the index always gets a plain range to seek on.
--   6. Drift: a row inserted before the current page shifts every later OFFSET by one, so the
--      next page repeats a row (a delete makes it skip one). Keyset is anchored to a row, not
--      to a count, so it does neither. A row that lands behind the cursor simply shows up on
--      the next fresh start.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   LIMIT n OFFSET k                             acceptable    any page number; slow deep, drifts
--   Keyset on created_at only                    avoid         skips rows that share a timestamp
--   WHERE (created_at, id) > (?, ?) LIMIT n      WRITE THIS    one seek per page, no drift
--   Expanded OR form with a >= prefix            WRITE THIS    same rows, no row values needed
--   Page<T> (an extra COUNT per request)         acceptable    use Slice if no total is needed
--
-- WHY THIS ONE
--   Every backend that lists transactions has this endpoint, and deep pages are where it falls
--   over. The sentence that lands is "OFFSET still reads every row it skips; keyset seeks
--   straight to the last row I returned". Then the two details that separate a real answer
--   from a textbook one: a unique tiebreaker, and an index in the same column order. Close
--   with the honest trade-off: keyset gives next and previous, not "jump to page 500".
--
-- SAMJHO
--   OFFSET 200 ka matlab: DB 200 rows padh ke phenk deta hai, phir 4 deta hai. Page jitna deep,
--   utna slow. Keyset mein pichhle page ki last row yaad rakho aur bolo "iske baad ki 4 do";
--   index seedha wahin seek karta hai. created_at unique nahi, isliye id tiebreaker hai, WHERE
--   aur ORDER BY dono mein. Beech mein nayi row aaye to OFFSET row repeat karta hai, keyset nahi.
--
-- AISE BOLO  (30-40 sec)
--   "I'd first ask whether the client needs page numbers and a total, or only next and previous,
--   because that decides the method. LIMIT with OFFSET is the simple version, but the database
--   still reads and discards every skipped row, so page k costs about k times n rows, and a row
--   inserted before the page makes the next page repeat one. I use keyset pagination: ORDER BY
--   created_at, id with WHERE (created_at, id) greater than the last row I returned, on a
--   composite index in the same order, so every page is a seek of about log N plus n rows. The
--   id is the unique tiebreaker, and the trade-off is that keyset cannot jump to page 500."
--
-- TRAP
--   ! Sirf created_at > :last likha to us timestamp ki baaki rows gayab: "no tiebreaker" wali
--     query mein rows 5 aur 6 kabhi nahi aati. (created_at, id) WHERE, ORDER BY, index teeno mein.
--   ! Newest-first (DESC) mein > ki jagah < lagta hai, aur ORDER BY created_at DESC, id DESC.
--     Ek column ASC aur doosra DESC ho to seedha tuple compare kaam nahi karta.
--
-- FOLLOW-UPS KE JAWAB
--   ? Spring Data gives you Page and Slice. Which one for a "load more" screen, and why?
--     -> Slice. Page ko total chahiye, isliye woh alag COUNT(*) query chalata hai; badi table pe
--        woh khud slow hai. Slice size + 1 rows laata hai aur bas hasNext batata hai.
--   ? Can Spring Data do keyset pagination for you?
--     -> Haan, Spring Data 3.1+ ka scroll API: return type Window<T>, parameter ScrollPosition.
--        Pehli call ScrollPosition.keyset(), agli window.positionAt(window.size() - 1), aur
--        window.hasNext() false ho to ruko. Sort mein unique column (id) rakho.
--   ? The product owner wants a "jump to page 500" box. Can keyset do it?
--     -> Nahi. Keyset sirf "is row ke baad" jaanta hai, page number nahi. Jump chahiye to OFFSET
--        (deep page slow) ya max depth limit karo. Zyada tar screens ko next / load more chahiye.
--   ? How do you page in Oracle 12c+, and how was it done before 12c?
--     -> 12c+: OFFSET k ROWS FETCH NEXT n ROWS ONLY. Pehle ROWNUM ki teen-level query (neeche
--        ORACLE / POSTGRES MEIN). Dono OFFSET hi hain: deep page pe utne hi slow.
--   ? A nightly batch job reads a whole large table in chunks. OFFSET or key?
--     -> Key: WHERE id > :last_id ORDER BY id LIMIT n, har chunk ek seek. OFFSET har chunk pe
--        pichhli rows dobara padhta hai, aur job rows ko DONE mark kare to OFFSET rows skip
--        karta hai. Spring Batch ka JdbcPagingItemReader bhi unique sort key se page karta hai.
--
-- ALSO UNLOCKS
--   - Infinite scroll and "load more" APIs (an opaque nextCursor built from the last row)
--   - Chunked batch jobs and exports over a big table
--   - Why a COUNT(*) on every page request is expensive
--   - Unique tiebreakers in any ORDER BY (S09, S19)
--
-- ORACLE / POSTGRES MEIN
--   OFFSET   : Oracle 12c+  ... ORDER BY created_at, transfer_id
--                           OFFSET 200 ROWS FETCH NEXT 4 ROWS ONLY;
--              Postgres     ... ORDER BY created_at, transfer_id LIMIT 4 OFFSET 200;
--   ROWNUM   : 12c se pehle. ROWNUM sort se pehle lagta hai, isliye ORDER BY sabse andar:
--              SELECT * FROM (SELECT a.*, ROWNUM rn
--                             FROM  (SELECT ... ORDER BY created_at, transfer_id) a
--                             WHERE ROWNUM <= 204)
--              WHERE rn > 200;
--   Keyset   : Oracle mein (a, b) > (x, y) nahi chalta (tuple sirf = / <> / IN ke saath), to
--              expanded OR form + >= prefix likho, phir FETCH FIRST 4 ROWS ONLY. Postgres row
--              value seedha leta hai aur (created_at, transfer_id) B-tree index pe seek karta hai.
--   Plan     : Neeche SQLite ka plan text hai (SCAN / SEARCH). Oracle aur Postgres ke shabd alag
--              hain (INDEX RANGE SCAN, Index Scan), idea same. SQLite ka "created_at>?" lower
--              bound hai; >= bhi aise hi dikhta hai. OFFSET ki phenki hui rows plan text mein
--              nahi dikhti; Postgres EXPLAIN ANALYZE mein Limit ke neeche wala node 204 rows deta
--              dikhta hai (offset + limit).
--
-- VARIANTS     : not a LeetCode type; classic: "load more", cursor-based REST paging, batch export
-- ================================================================================================

-- >>> QUERY: setup -- a transfers table, built here (not part of the shared schema)
CREATE TABLE transfers (
    transfer_id INTEGER PRIMARY KEY,
    created_at  TEXT    NOT NULL,                        -- ISO yyyy-mm-dd hh:mm:ss
    amount      INTEGER NOT NULL
);

-- >>> QUERY: 300 rows from a recursive CTE; every 3 rows share one created_at (ties on purpose)
INSERT INTO transfers (transfer_id, created_at, amount)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 300)
SELECT i,
       datetime('2025-06-01 09:00:00', '+' || ((i - 1) / 3) || ' minutes'),
       1000 + (i * 37) % 500
FROM   n;

-- >>> QUERY: the index keyset needs -- the sort columns, in the sort order
CREATE INDEX ix_transfers_created ON transfers(created_at, transfer_id);

-- >>> QUERY: 300 rows but only 100 distinct created_at values -- created_at alone is not unique
SELECT COUNT(*) AS total_rows, COUNT(DISTINCT created_at) AS distinct_created_at,
       MIN(created_at) AS first_at, MAX(created_at) AS last_at
FROM   transfers;

-- >>> QUERY: OFFSET page 1 (4 rows per page)
SELECT transfer_id, created_at, amount
FROM   transfers
ORDER  BY created_at, transfer_id
LIMIT  4 OFFSET 0;

-- >>> QUERY: OFFSET page 2
SELECT transfer_id, created_at, amount
FROM   transfers
ORDER  BY created_at, transfer_id
LIMIT  4 OFFSET 4;

-- >>> QUERY: keyset page 2 -- after the last row of page 1, ('2025-06-01 09:01:00', 4)
-- Literals stand in for the two bind values (?, ?), because run_all.py has no binds.
SELECT transfer_id, created_at, amount
FROM   transfers
WHERE  (created_at, transfer_id) > ('2025-06-01 09:01:00', 4)     -- row values: SQLite 3.15+
ORDER  BY created_at, transfer_id
LIMIT  4;

-- >>> QUERY: the same page in the expanded OR form, for engines without row values (Oracle)
SELECT transfer_id, created_at, amount
FROM   transfers
WHERE  created_at > '2025-06-01 09:01:00'
   OR (created_at = '2025-06-01 09:01:00' AND transfer_id > 4)
ORDER  BY created_at, transfer_id
LIMIT  4;

-- >>> QUERY: the OR form with a redundant >= prefix, so any optimizer gets a range to seek
SELECT transfer_id, created_at, amount
FROM   transfers
WHERE  created_at >= '2025-06-01 09:01:00'
  AND (created_at >  '2025-06-01 09:01:00' OR transfer_id > 4)
ORDER  BY created_at, transfer_id
LIMIT  4;

-- >>> QUERY: the bug -- no tiebreaker. Rows 5 and 6 share 09:01:00 with row 4 and are skipped
SELECT transfer_id, created_at, amount
FROM   transfers
WHERE  created_at > '2025-06-01 09:01:00'
ORDER  BY created_at, transfer_id
LIMIT  4;

-- >>> QUERY: plan for a deep OFFSET page -- SCAN walks the index from the start, discards 200
EXPLAIN QUERY PLAN
SELECT transfer_id, created_at, amount
FROM   transfers
ORDER  BY created_at, transfer_id
LIMIT  4 OFFSET 200;

-- >>> QUERY: plan for the same page by keyset -- SEARCH seeks straight to row 200's created_at
EXPLAIN QUERY PLAN
SELECT transfer_id, created_at, amount
FROM   transfers
WHERE  (created_at, transfer_id) > ('2025-06-01 10:06:00', 200)
ORDER  BY created_at, transfer_id
LIMIT  4;

-- >>> QUERY: plan for the OR form with the >= prefix -- also a SEARCH on the same index
EXPLAIN QUERY PLAN
SELECT transfer_id, created_at, amount
FROM   transfers
WHERE  created_at >= '2025-06-01 10:06:00'
  AND (created_at >  '2025-06-01 10:06:00' OR transfer_id > 200)
ORDER  BY created_at, transfer_id
LIMIT  4;

-- >>> QUERY: drift -- while the user reads page 1, a late transfer stamped 09:00:30 arrives
INSERT INTO transfers (transfer_id, created_at, amount)
VALUES (301, '2025-06-01 09:00:30', 5000);

-- >>> QUERY: OFFSET page 2 again -- row 4 was the last row of page 1, and it comes back
SELECT transfer_id, created_at, amount
FROM   transfers
ORDER  BY created_at, transfer_id
LIMIT  4 OFFSET 4;

-- >>> QUERY: keyset page 2 again -- it starts after row 4, so nothing repeats
-- Row 301 is behind the cursor, so this pass does not show it; a fresh start will.
SELECT transfer_id, created_at, amount
FROM   transfers
WHERE  (created_at, transfer_id) > ('2025-06-01 09:01:00', 4)
ORDER  BY created_at, transfer_id
LIMIT  4;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [setup -- a transfers table, built here (not part of the shared schema)]
--   (no result set)
--
-- [300 rows from a recursive CTE; every 3 rows share one created_at (ties on purpose)]
--   (no result set)
--
-- [the index keyset needs -- the sort columns, in the sort order]
--   (no result set)
--
-- [300 rows but only 100 distinct created_at values -- created_at alone is not unique]
--   total_rows  distinct_created_at  first_at             last_at
--   ----------  -------------------  -------------------  -------------------
--   300         100                  2025-06-01 09:00:00  2025-06-01 10:39:00
--   (1 row)
--
-- [OFFSET page 1 (4 rows per page)]
--   transfer_id  created_at           amount
--   -----------  -------------------  ------
--   1            2025-06-01 09:00:00  1037
--   2            2025-06-01 09:00:00  1074
--   3            2025-06-01 09:00:00  1111
--   4            2025-06-01 09:01:00  1148
--   (4 rows)
--
-- [OFFSET page 2]
--   transfer_id  created_at           amount
--   -----------  -------------------  ------
--   5            2025-06-01 09:01:00  1185
--   6            2025-06-01 09:01:00  1222
--   7            2025-06-01 09:02:00  1259
--   8            2025-06-01 09:02:00  1296
--   (4 rows)
--
-- [keyset page 2 -- after the last row of page 1, ('2025-06-01 09:01:00', 4)]
--   transfer_id  created_at           amount
--   -----------  -------------------  ------
--   5            2025-06-01 09:01:00  1185
--   6            2025-06-01 09:01:00  1222
--   7            2025-06-01 09:02:00  1259
--   8            2025-06-01 09:02:00  1296
--   (4 rows)
--
-- [the same page in the expanded OR form, for engines without row values (Oracle)]
--   transfer_id  created_at           amount
--   -----------  -------------------  ------
--   5            2025-06-01 09:01:00  1185
--   6            2025-06-01 09:01:00  1222
--   7            2025-06-01 09:02:00  1259
--   8            2025-06-01 09:02:00  1296
--   (4 rows)
--
-- [the OR form with a redundant >= prefix, so any optimizer gets a range to seek]
--   transfer_id  created_at           amount
--   -----------  -------------------  ------
--   5            2025-06-01 09:01:00  1185
--   6            2025-06-01 09:01:00  1222
--   7            2025-06-01 09:02:00  1259
--   8            2025-06-01 09:02:00  1296
--   (4 rows)
--
-- [the bug -- no tiebreaker. Rows 5 and 6 share 09:01:00 with row 4 and are skipped]
--   transfer_id  created_at           amount
--   -----------  -------------------  ------
--   7            2025-06-01 09:02:00  1259
--   8            2025-06-01 09:02:00  1296
--   9            2025-06-01 09:02:00  1333
--   10           2025-06-01 09:03:00  1370
--   (4 rows)
--
-- [plan for a deep OFFSET page -- SCAN walks the index from the start, discards 200]
--   id  parent  notused  detail
--   --  ------  -------  -----------------------------------------------
--   8   0       226      SCAN transfers USING INDEX ix_transfers_created
--   (1 row)
--
-- [plan for the same page by keyset -- SEARCH seeks straight to row 200's created_at]
--   id  parent  notused  detail
--   --  ------  -------  ----------------------------------------------------------------
--   5   0       206      SEARCH transfers USING INDEX ix_transfers_created (created_at>?)
--   (1 row)
--
-- [plan for the OR form with the >= prefix -- also a SEARCH on the same index]
--   id  parent  notused  detail
--   --  ------  -------  ----------------------------------------------------------------
--   5   0       206      SEARCH transfers USING INDEX ix_transfers_created (created_at>?)
--   (1 row)
--
-- [drift -- while the user reads page 1, a late transfer stamped 09:00:30 arrives]
--   (no result set)
--
-- [OFFSET page 2 again -- row 4 was the last row of page 1, and it comes back]
--   transfer_id  created_at           amount
--   -----------  -------------------  ------
--   4            2025-06-01 09:01:00  1148
--   5            2025-06-01 09:01:00  1185
--   6            2025-06-01 09:01:00  1222
--   7            2025-06-01 09:02:00  1259
--   (4 rows)
--
-- [keyset page 2 again -- it starts after row 4, so nothing repeats]
--   transfer_id  created_at           amount
--   -----------  -------------------  ------
--   5            2025-06-01 09:01:00  1185
--   6            2025-06-01 09:01:00  1222
--   7            2025-06-01 09:02:00  1259
--   8            2025-06-01 09:02:00  1296
--   (4 rows)
--
-- ======================================================================================
