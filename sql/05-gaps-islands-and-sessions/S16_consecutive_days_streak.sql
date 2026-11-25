-- ================================================================================================
-- S16  .  Consecutive login days   (the date-minus-row-number trick)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Gaps, Islands & Sessions
-- DIFFICULTY   : HARD    (3 of 3)
-- WEIGHTAGE    : MEDIUM  **   the classic "islands" question
-- PRIORITY     : P2   -- LC 180 jaisa "consecutive" sawaal product/GCC round mein aata hai
-- TECHNIQUE    : date - ROW_NUMBER() is constant inside a consecutive run
--
-- PROBLEM
--   Find every run of consecutive days on which a user logged in, and the users with a streak
--   of at least 3 days. Then the same idea on consecutive integers.
--
-- ALGORITHM
--   1. Reduce to DISTINCT days first -- two logins on one day are still one day.
--   2. Number the days per user with ROW_NUMBER ordered by date.
--   3. Subtract the row number from the date. Inside a consecutive run both increase by
--      exactly one per row, so the difference is CONSTANT. Across a gap it jumps.
--   4. That constant is the island id: GROUP BY it.
--   5. MIN and MAX of the group give the streak boundaries; COUNT gives the length.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   Loop day by day in application code          avoid         N queries or a full table pull
--   LAG + running SUM of a gap flag              WRITE THIS    works for any gap definition
--   date - ROW_NUMBER() grouping key             WRITE THIS    shortest form for exact +1 steps
--   Self-join on date = date + 1                 avoid         only finds pairs, not runs
--
-- WHY THIS ONE
--   Both tricks here -- the gap flag of S15 and date-minus-row-number -- solve the same family.
--   Say which one you are using and why: row-number subtraction is shorter but only works when
--   the step is exactly +1; the running-sum flag handles any gap rule. Reaching for the general
--   one when the step is irregular is what a senior answer looks like.
--
-- SAMJHO
--   Pehle har user ke DISTINCT din nikalo, phir unko ROW_NUMBER do.
--   Lagatar dino mein date bhi +1 badhti hai aur rn bhi +1, to (date - rn) same rehta hai.
--   Gap aate hi date zyada badhti hai aur (date - rn) badal jata hai. Yahi island key hai.
--   GROUP BY user, key: MIN = start, MAX = end, COUNT = streak ki length.
--
-- AISE BOLO  (30-40 sec)
--   "I'd first ask whether two logins on the same day count once, and where the day boundary
--   is cut, for example which time zone. A day-by-day loop in Java, or a self-join on date + 1,
--   only finds pairs, not whole runs. I reduce to distinct days per user, number them with
--   ROW_NUMBER and subtract that from the date: inside a run both move by one, so the
--   difference is constant, and grouping on it gives start, end and length per streak. It is
--   one sort per user, O(n log n); if the step can be irregular, I switch to the LAG and
--   running-SUM flag from sessionisation."
--
-- TRAP
--   ! DISTINCT days pehle nahi kiye to ek din ke do login do rn le lete hain, key badal jati hai
--     aur streak toot jati hai.
--   ! date - rn sirf exact +1 step pe chalta hai. "Weekend skip" ya "2 din tak ka gap" rule ho to
--     S15 wala gap flag lagao.
--
-- FOLLOW-UPS KE JAWAB
--   ? Why is date - ROW_NUMBER() constant inside a run? Walk through three rows.
--     -> 05-01 rn 1 -> 04-30, 05-02 rn 2 -> 04-30, 05-03 rn 3 -> 04-30. Dono +1 badhte hain.
--        Agla din 05-05 rn 4 ho to key 05-01 ban jati hai: naya island.
--   ? Two logins on the same day -- does your query break? (DISTINCT the days first)
--     -> Haan, bina DISTINCT ke same din do rn le leta hai aur key badal jati hai. Isliye pehle
--        SELECT DISTINCT user_id, date(login_at).
--   ? Streaks of consecutive WEEKS instead of days.
--     -> Din ko week start pe le jao (SQLite date(d, 'weekday 0', '-6 days') = Monday), DISTINCT
--        karo, phir key = week_start - 7 * rn. Ya gap flag: gap 7 din se zyada to naya streak.
--   ? Which trick would you use if the step could be 1 or 2 days? (the gap flag)
--     -> Gap flag (S15): LAG se pichla din lo, gap 2 din se zyada ho to flag 1, running SUM =
--        streak id. date - rn yahan galat island banayega.
--
-- ALSO UNLOCKS
--   - Login / activity streaks
--   - Islands of consecutive ids
--   - Consecutive wins or failures
--   - "Numbers appearing 3+ times in a row"
--
-- ORACLE / POSTGRES MEIN
--   Din nikalna : Oracle TRUNC(login_at), Postgres login_at::date (yahan SQLite date(login_at)).
--   Island key  : Oracle   d - ROW_NUMBER() OVER (...)          -- DATE - number = DATE
--                 Postgres d - (ROW_NUMBER() OVER (...))::int    -- ROW_NUMBER bigint hai, aur
--                 date - bigint operator nahi hai, isliye ::int cast zaroori hai.
--   Week start  : Oracle TRUNC(d, 'IW'), Postgres date_trunc('week', d)::date (dono Monday).
--
-- VARIANTS     : LC 180 (Consecutive Numbers), LC 601 (Human Traffic of Stadium), LC 1285
-- ================================================================================================

-- >>> QUERY: the working -- day, row number, and the constant island key
WITH days AS (SELECT DISTINCT user_id, date(login_at) AS d FROM logins)
SELECT user_id, d,
       ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY d)                        AS rn,
       date(julianday(d) - ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY d))   AS island_key
FROM   days
ORDER  BY user_id, d;

-- >>> QUERY: every streak, with its start, end and length
WITH days AS (SELECT DISTINCT user_id, date(login_at) AS d FROM logins),
     keyed AS (
        SELECT user_id, d,
               date(julianday(d) - ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY d)) AS grp
        FROM   days)
SELECT user_id, MIN(d) AS streak_start, MAX(d) AS streak_end, COUNT(*) AS streak_days
FROM   keyed
GROUP  BY user_id, grp
ORDER  BY user_id, streak_start;

-- >>> QUERY: users with a streak of at least 3 consecutive days
WITH days AS (SELECT DISTINCT user_id, date(login_at) AS d FROM logins),
     keyed AS (
        SELECT user_id, d,
               date(julianday(d) - ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY d)) AS grp
        FROM   days),
     streaks AS (
        SELECT user_id, COUNT(*) AS len, MIN(d) AS s, MAX(d) AS e
        FROM   keyed GROUP BY user_id, grp)
SELECT user_id, MAX(len) AS longest_streak
FROM   streaks
GROUP  BY user_id
HAVING MAX(len) >= 3
ORDER  BY longest_streak DESC, user_id;

-- >>> QUERY: the same trick on consecutive integers -- islands of ids
WITH t(n) AS (VALUES (1),(2),(3),(7),(8),(12),(13),(14),(15))
SELECT MIN(n) AS range_from, MAX(n) AS range_to, COUNT(*) AS size
FROM   (SELECT n, n - ROW_NUMBER() OVER (ORDER BY n) AS grp FROM t)
GROUP  BY grp
ORDER  BY range_from;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the working -- day, row number, and the constant island key]
--   user_id  d           rn  island_key
--   -------  ----------  --  ----------
--   7        2025-05-01  1   2025-04-30
--   7        2025-05-02  2   2025-04-30
--   7        2025-05-03  3   2025-04-30
--   7        2025-05-04  4   2025-04-30
--   7        2025-05-05  5   2025-04-30
--   8        2025-05-01  1   2025-04-30
--   8        2025-05-03  2   2025-05-01
--   8        2025-05-04  3   2025-05-01
--   (8 rows)
--
-- [every streak, with its start, end and length]
--   user_id  streak_start  streak_end  streak_days
--   -------  ------------  ----------  -----------
--   7        2025-05-01    2025-05-05  5
--   8        2025-05-01    2025-05-01  1
--   8        2025-05-03    2025-05-04  2
--   (3 rows)
--
-- [users with a streak of at least 3 consecutive days]
--   user_id  longest_streak
--   -------  --------------
--   7        5
--   (1 row)
--
-- [the same trick on consecutive integers -- islands of ids]
--   range_from  range_to  size
--   ----------  --------  ----
--   1           3         3
--   7           8         2
--   12          15        4
--   (3 rows)
--
-- ======================================================================================
