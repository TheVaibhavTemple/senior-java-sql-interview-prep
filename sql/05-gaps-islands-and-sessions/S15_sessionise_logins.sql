-- ================================================================================================
-- S15  .  Group events into sessions when the gap exceeds 30 minutes
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Gaps, Islands & Sessions
-- DIFFICULTY   : HARD    (3 of 3)
-- WEIGHTAGE    : MEDIUM  **   the signature "senior SQL" question
-- PRIORITY     : P3   -- data / product-analytics round ka sawaal; backend service round mein kam
-- TECHNIQUE    : LAG to find gaps, then a running SUM of the gap flag as a session id
--
-- PROBLEM
--   A stream of login events per user. A gap of more than 30 minutes starts a new session.
--   Return each session with its start, end, duration and event count.
--
-- ALGORITHM
--   1. LAG the timestamp within each user to get the previous event time.
--   2. Flag each row: 1 if the gap from the previous event exceeds the threshold, else 0.
--      The very first row of a user has no predecessor, so it is also a 1.
--   3. A RUNNING SUM of that flag is the session id -- it increments exactly at each new
--      session and stays constant inside one. This is the whole trick.
--   4. GROUP BY (user, session id) and aggregate MIN, MAX, COUNT.
--   5. The threshold is the only parameter: change 30 minutes and nothing else moves.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Loop in application code                     avoid         pulls every event to the client
--   Self-join on "no event in between"           avoid         O(n^2) and hard to read
--   LAG + running SUM of a gap flag              WRITE THIS    two window passes, O(n log n)
--   MATCH_RECOGNIZE                              acceptable    Oracle / Snowflake only
--
-- WHY THIS ONE
--   "A running sum of a boolean flag becomes a group id" is one idea that solves sessions,
--   streaks, islands and change-point detection. Say that sentence before you write anything;
--   it is the thing being tested, not the date arithmetic.
--
-- SAMJHO
--   Har user ke events time order mein lagao, LAG se pichla time lo.
--   Gap 30 min se zyada hai to flag 1, warna 0. User ka pehla event bhi 1.
--   Flag ka running SUM har naye session pe ek badhta hai aur session ke andar same rehta hai.
--   Wahi session id hai. Phir GROUP BY user, session id: MIN, MAX, COUNT.
--
-- AISE BOLO  (30-40 sec)
--   "First I'd confirm the grain: one row per login, and does a gap of exactly 30 minutes start
--   a new session? Looping over the events in Java works, but it pulls the whole table over the
--   wire. In SQL I LAG the previous login per user, flag the row 1 when there is no previous row
--   or the gap is over 30 minutes, and a running SUM of that flag becomes the session id; then I
--   group by user and session id. It is one sort per user, O(n log n), and an index on
--   (user_id, login_at) can hand the rows over already in order."
--
-- TRAP
--   ! Pehle event ka LAG NULL hai, aur CASE ka ELSE 0 usko chupchap 0 bana deta hai. Tab
--     SUM(flag) se session count har user pe ek kam aata hai. IS NULL wali condition mat bhoolo.
--   ! SQLite julianday float hai: exactly 30 min ka gap 30.0000002 aata hai aur galat naya session
--     ban jata hai. Isliye flag datetime(prev, '+30 minutes') se exact compare karta hai.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why is a running SUM of the flag a valid session id?
--     -> Flag sirf session ki pehli row pe 1 hai. To running SUM wahin +1 hota hai aur andar
--        constant rehta hai: ek session = ek number.
--   ? What is the flag for the very first event of a user, and why does it matter?
--     -> 1, kyunki LAG NULL hai. Na do to numbering 0 se shuru hoti hai aur SUM(flag) se
--        session count har user pe ek kam aata hai.
--   ? Change the threshold to 5 minutes -- what else in the query changes? (nothing)
--     -> Kuch nahi, sirf '+30 minutes' ko '+5 minutes' karo. Java se isko bind parameter bana do.
--   ? Sessions that cross midnight -- does your query care?
--     -> Nahi. Poora timestamp compare hota hai, date nahi: 23:50 aur 00:10 ek hi session hai.
--        Business din pe todna chahe to PARTITION BY mein date bhi daalo.
--
-- ALSO UNLOCKS
--   - Web / app session reconstruction
--   - Streaks and consecutive runs (S16)
--   - Islands of consecutive ids
--   - Change-point detection on a status column
--
-- ORACLE / POSTGRES MEIN
--   julianday() aur datetime(x, '+30 minutes') sirf SQLite ke hain.
--   Postgres : flag = login_at > prev + INTERVAL '30 minutes'   (prev = LAG(login_at) OVER (...))
--              minutes chahiye to EXTRACT(EPOCH FROM (login_at - prev)) / 60.
--   Oracle   : flag = login_at > prev + INTERVAL '30' MINUTE   (DATE aur TIMESTAMP dono pe).
--              DATE - DATE din deta hai (minutes = diff * 24 * 60); TIMESTAMP - TIMESTAMP
--              INTERVAL deta hai, isliye seedha INTERVAL se compare karo.
--   MATCH_RECOGNIZE Oracle 12c+ mein hai, Postgres mein nahi.
--
-- VARIANTS     : no exact LeetCode match; classic web-analytics sessionisation (30-min idle rule)
-- ================================================================================================

-- >>> QUERY: step 1 and 2 -- the gap and the flag, shown explicitly
SELECT user_id, login_at,
       LAG(login_at) OVER (PARTITION BY user_id ORDER BY login_at) AS prev_login,
       CAST(ROUND((julianday(login_at)
            - julianday(LAG(login_at) OVER (PARTITION BY user_id ORDER BY login_at)))
            * 24 * 60) AS INTEGER) AS gap_minutes,
       CASE WHEN LAG(login_at) OVER (PARTITION BY user_id ORDER BY login_at) IS NULL
             OR login_at > datetime(LAG(login_at) OVER (PARTITION BY user_id ORDER BY login_at),
                                    '+30 minutes')
            THEN 1 ELSE 0 END AS new_session_flag
FROM   logins
ORDER  BY user_id, login_at;

-- >>> QUERY: step 3 and 4 -- running sum of the flag becomes the session id
WITH flagged AS (
    SELECT user_id, login_at,
           CASE WHEN LAG(login_at) OVER (PARTITION BY user_id ORDER BY login_at) IS NULL
                 OR login_at > datetime(LAG(login_at) OVER (PARTITION BY user_id
                                                            ORDER BY login_at), '+30 minutes')
                THEN 1 ELSE 0 END AS is_new
    FROM   logins
),
sessioned AS (
    SELECT user_id, login_at,
           SUM(is_new) OVER (PARTITION BY user_id ORDER BY login_at
                             ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS session_no
    FROM   flagged
)
SELECT user_id, session_no,
       MIN(login_at) AS session_start,
       MAX(login_at) AS session_end,
       COUNT(*)      AS events,
       CAST(ROUND((julianday(MAX(login_at)) - julianday(MIN(login_at))) * 24 * 60) AS INTEGER)
                     AS duration_minutes
FROM   sessioned
GROUP  BY user_id, session_no
ORDER  BY user_id, session_no;

-- >>> QUERY: sessions per user, and the average session length
WITH flagged AS (
    SELECT user_id, login_at,
           CASE WHEN LAG(login_at) OVER (PARTITION BY user_id ORDER BY login_at) IS NULL
                 OR login_at > datetime(LAG(login_at) OVER (PARTITION BY user_id
                                                            ORDER BY login_at), '+30 minutes')
                THEN 1 ELSE 0 END AS is_new
    FROM logins),
sessioned AS (
    SELECT user_id, login_at,
           SUM(is_new) OVER (PARTITION BY user_id ORDER BY login_at
                             ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS session_no
    FROM flagged),
per_session AS (
    SELECT user_id, session_no, COUNT(*) AS events,
           (julianday(MAX(login_at)) - julianday(MIN(login_at))) * 24 * 60 AS mins
    FROM sessioned GROUP BY user_id, session_no)
SELECT user_id, COUNT(*) AS sessions, SUM(events) AS total_events,
       ROUND(AVG(mins), 1) AS avg_session_minutes
FROM   per_session GROUP BY user_id ORDER BY user_id;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [step 1 and 2 -- the gap and the flag, shown explicitly]
--   user_id  login_at             prev_login           gap_minutes  new_session_flag
--   -------  -------------------  -------------------  -----------  ----------------
--   7        2025-05-01 09:00:00  NULL                 NULL         1
--   7        2025-05-01 09:12:00  2025-05-01 09:00:00  12           0
--   7        2025-05-01 09:25:00  2025-05-01 09:12:00  13           0
--   7        2025-05-01 11:40:00  2025-05-01 09:25:00  135          1
--   7        2025-05-01 11:55:00  2025-05-01 11:40:00  15           0
--   7        2025-05-02 08:10:00  2025-05-01 11:55:00  1215         1
--   7        2025-05-03 08:15:00  2025-05-02 08:10:00  1445         1
--   7        2025-05-04 20:01:00  2025-05-03 08:15:00  2146         1
--   7        2025-05-05 07:45:00  2025-05-04 20:01:00  704          1
--   8        2025-05-01 10:00:00  NULL                 NULL         1
--   8        2025-05-03 10:00:00  2025-05-01 10:00:00  2880         1
--   8        2025-05-04 10:00:00  2025-05-03 10:00:00  1440         1
--   (12 rows)
--
-- [step 3 and 4 -- running sum of the flag becomes the session id]
--   user_id  session_no  session_start        session_end          events  duration_minutes
--   -------  ----------  -------------------  -------------------  ------  ----------------
--   7        1           2025-05-01 09:00:00  2025-05-01 09:25:00  3       25
--   7        2           2025-05-01 11:40:00  2025-05-01 11:55:00  2       15
--   7        3           2025-05-02 08:10:00  2025-05-02 08:10:00  1       0
--   7        4           2025-05-03 08:15:00  2025-05-03 08:15:00  1       0
--   7        5           2025-05-04 20:01:00  2025-05-04 20:01:00  1       0
--   7        6           2025-05-05 07:45:00  2025-05-05 07:45:00  1       0
--   8        1           2025-05-01 10:00:00  2025-05-01 10:00:00  1       0
--   8        2           2025-05-03 10:00:00  2025-05-03 10:00:00  1       0
--   8        3           2025-05-04 10:00:00  2025-05-04 10:00:00  1       0
--   (9 rows)
--
-- [sessions per user, and the average session length]
--   user_id  sessions  total_events  avg_session_minutes
--   -------  --------  ------------  -------------------
--   7        6         9             6.7
--   8        3         3             0.0
--   (2 rows)
--
-- ======================================================================================
