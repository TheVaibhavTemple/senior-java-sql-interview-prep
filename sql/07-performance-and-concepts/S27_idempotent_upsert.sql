-- ================================================================================================
-- S27  .  Idempotent insert and upsert   (the same message arrives twice)
-- ------------------------------------------------------------------------------------------------
-- TRACK        : Performance & Concepts
-- DIFFICULTY   : MEDIUM  (2 of 3)
-- WEIGHTAGE    : HIGH    ***  asked in almost every fintech / payments round
-- PRIORITY     : P1   -- "same message do baar aaya to?" fintech round ka pakka sawaal hai
-- TECHNIQUE    : UNIQUE business key + INSERT ... ON CONFLICT DO NOTHING / DO UPDATE with excluded
--
-- PROBLEM
--   A consumer reads payment-status events from a queue that delivers at least once, so the
--   same message can arrive twice, and two consumers can hold copies at the same moment. Make
--   it idempotent in SQL: record each message exactly once, turn a duplicate into a no-op
--   instead of an error, and upsert the payment's latest status so that a replayed or late
--   event can never move it backwards.
--
-- ALGORITHM
--   1. Put a UNIQUE constraint on the business key: message_id here, an idempotency key on an
--      API. This is the real guard: the database sees both copies, the application does not.
--   2. A plain INSERT of a duplicate fails with a unique violation. Correct, but noisy, and in
--      PostgreSQL that error aborts the whole transaction.
--   3. INSERT ... ON CONFLICT (message_id) DO NOTHING turns the duplicate into a no-op. The
--      update count (or RETURNING) is the signal: 1 = new, do the work; 0 = duplicate, skip.
--   4. For state, ON CONFLICT (payment_ref) DO UPDATE SET status = excluded.status, where
--      excluded is the row you tried to insert. Add WHERE excluded.event_seq >
--      payment_status.event_seq, so a replay or a late event changes nothing.
--   5. Never SELECT first and INSERT after: two consumers both see no row and both insert.
--      Only the constraint closes that gap.
--   6. Write the dedup row and the business change in ONE transaction, so a crash between
--      them cannot leave the message marked done with the work missing.
--
-- APPROACHES
--   APPROACH                                     VERDICT       NOTE
--   ---------------------------------------------------------------------------------------------
--   SELECT first, INSERT if no row               avoid         race: both consumers see no row
--   INSERT ... WHERE NOT EXISTS, no constraint   avoid         the same race in one statement
--   Plain INSERT, catch the duplicate error      acceptable    works; Postgres aborts the txn
--   UNIQUE + ON CONFLICT DO NOTHING              WRITE THIS    duplicate = no-op, count = 0
--   ON CONFLICT DO UPDATE ... WHERE seq newer    WRITE THIS    latest state, replay-safe
--   Oracle MERGE                                 acceptable    one statement; can still ORA-00001
--
-- WHY THIS ONE
--   Queues deliver at least once and clients retry, so "what happens when the same payment
--   message comes twice" is asked in nearly every fintech round. Name the guard first: a unique
--   constraint on the business key, because only the database sees both copies at once. Then
--   ON CONFLICT DO NOTHING as the mechanism, the update count as the signal, and say plainly
--   why SELECT-then-INSERT is a race and not a check.
--
-- SAMJHO
--   Queue at-least-once deti hai, matlab same message do baar aayega hi. "Pehle SELECT, nahi
--   mila to INSERT" race hai: do consumer ek saath check karein to dono ko "nahi mila" dikhta hai.
--   Asli guard DB ka UNIQUE constraint hai; ON CONFLICT DO NOTHING duplicate ko no-op banata hai
--   aur count 0 batata hai. Status ke liye DO UPDATE excluded se, sirf tab jab event naya ho.
--
-- AISE BOLO  (30-40 sec)
--   "I'd assume the queue delivers at least once, so duplicates will come, and I'd ask what the
--   business key is: the message id, or an idempotency key from the client. A SELECT followed by
--   an INSERT is a race, because two consumers can both see no row and both pay. So I put a
--   unique constraint on the key and write INSERT ... ON CONFLICT DO NOTHING: an update count of
--   1 means new work, 0 means a duplicate I skip and acknowledge, and it costs one index lookup.
--   For the payment's status I upsert with DO UPDATE from the excluded row, guarded by a
--   sequence check, so a late or replayed event cannot move SETTLED back to SENT."
--
-- TRAP
--   ! "Pehle check kar lete hain" guard nahi hai: naive_inbox mein dono consumers ko 0 rows dikhi
--     aur message do baar process hua. Bina UNIQUE constraint ke koi app-level check safe nahi.
--   ! DO UPDATE mein WHERE event_seq guard bhoole to late SENT event SETTLED ko wapas SENT bana
--     deta hai (last query). Replay bhi state ko peeche le ja sakta hai.
--
-- FOLLOW-UPS KE JAWAB
--   ? At-least-once delivery: why do duplicates come even when the broker has duplicate detection?
--     -> Consumer kaam karke ack se pehle crash ho, ya message lock expire ho, to broker wahi
--        message dobara deta hai. Broker ka duplicate detection sender ka dobara bheja message
--        (same message id, ek time window ke andar) rokta hai; consumer ki redelivery nahi.
--   ? Why is the unique constraint the real guard, and not a check in the service?
--     -> Service ke check aur insert ke beech gap hai; do pods ek saath us gap mein ho sakte
--        hain. Constraint index pe lagta hai: doosra insert pehle ke commit ka wait karta hai,
--        phir conflict pe fail ya DO NOTHING. Dono mein se ek hi row bachti hai.
--   ? Do you keep idempotency keys forever? (TTL)
--     -> Nahi. expires_at column + index, aur ek scheduled job purane keys delete kare; ya Redis
--        mein SET key value NX EX <seconds>. TTL client / broker ki retry window se lamba rakho,
--        warna late retry naya request ban jayega.
--   ? A client retries a payment POST after a timeout. What does the retry get back?
--     -> Pehli request ka response (status + body) key ke saath usi transaction mein store karo.
--        Retry pe conflict aaye to stored response hi lautao, naya payment nahi. Pehli abhi chal
--        rahi ho to 409; same key alag body ke saath aaye to error, kyunki woh retry nahi hai.
--   ? What does this look like from Java?
--     -> int n = jdbcTemplate.update("INSERT ... ON CONFLICT (message_id) DO NOTHING", ...);
--        n == 0 to duplicate: log, ack, return. Plain INSERT ka duplicate JdbcTemplate pe
--        DuplicateKeyException banta hai (DataIntegrityViolationException ka subclass).
--
-- ALSO UNLOCKS
--   - The inbox / idempotent-consumer pattern
--   - Idempotency keys on payment APIs
--   - Upserting reference data (the latest FX rate per currency pair)
--   - Finding duplicates after the fact (S17) and cleaning them up (S18)
--
-- ORACLE / POSTGRES MEIN
--   Oracle   : MERGE. Yeh yahan run NAHI hota (SQLite mein MERGE nahi hai), isliye output nahi:
--              MERGE INTO payment_status t
--              USING (SELECT :ref AS payment_ref, :status AS status, :seq AS event_seq,
--                            :ts AS updated_at FROM dual) s
--              ON (t.payment_ref = s.payment_ref)
--              WHEN MATCHED THEN UPDATE
--                   SET t.status = s.status, t.event_seq = s.event_seq,
--                       t.updated_at = s.updated_at
--                   WHERE s.event_seq > t.event_seq
--              WHEN NOT MATCHED THEN INSERT (payment_ref, status, event_seq, updated_at)
--                   VALUES (s.payment_ref, s.status, s.event_seq, s.updated_at);
--              ON wala column SET mein nahi aa sakta (ORA-38104). Do sessions ek naye key pe saath
--              MERGE karein to dono NOT MATCHED le sakte hain aur ek ko ORA-00001 milta hai:
--              constraint rakho aur us error pe retry karo.
--   Oracle   : ORA-00001 sirf us statement ko rollback karta hai, transaction chalti rehti hai.
--              PL/SQL mein uska naam DUP_VAL_ON_INDEX hai.
--   Postgres : ON CONFLICT 9.5+ se, yahan jaisa hi (SQLite mein 3.24+). Conflict target pe UNIQUE
--              ya PK chahiye, warna error. DO UPDATE concurrency mein bhi unique error nahi deta.
--              Plain INSERT ka duplicate (23505) poora transaction abort kar deta hai.
--   Postgres : 15+ mein MERGE bhi hai, par concurrent insert pe woh unique error de sakta hai;
--              idempotent insert ke liye ON CONFLICT hi likho.
--   MySQL    : INSERT ... ON DUPLICATE KEY UPDATE status = VALUES(status) (8.0.19+: row alias).
--
-- APNI STORY SE JODO
--   "On the event-driven file-processing platform I built on Azure Service Bus, I used Service
--   Bus duplicate detection, retry with backoff, and a review of the dead-letter queue. Those
--   retries are exactly why a consumer has to be idempotent, and in SQL I would close it with
--   a unique key and ON CONFLICT DO NOTHING."
--   Note: Fact bas itna hai: duplicate detection, retry with backoff, DLQ review. DB mein UNIQUE
--   key / ON CONFLICT / processed_messages jaisi table thi ya nahi: [sirf sach ho to]. Na ho to
--   last line design answer hi rahe ("I would").
--
-- VARIANTS     : not a LeetCode type; classic: idempotent consumer (inbox), payment idempotency key
-- ================================================================================================

-- >>> QUERY: the anti-pattern, set up -- an inbox table with NO unique key
CREATE TABLE naive_inbox (
    message_id   TEXT NOT NULL,
    consumer     TEXT NOT NULL,
    processed_at TEXT NOT NULL
);

-- >>> QUERY: consumer A and consumer B each run this check before either inserts: both see 0
SELECT COUNT(*) AS rows_found FROM naive_inbox WHERE message_id = 'MSG-1001';

-- >>> QUERY: so both insert (one statement here; in production, two separate transactions)
INSERT INTO naive_inbox (message_id, consumer, processed_at) VALUES
 ('MSG-1001', 'consumer-A', '2025-06-02 10:00:01'),
 ('MSG-1001', 'consumer-B', '2025-06-02 10:00:01');

-- >>> QUERY: the result -- one message, processed twice: a double payout
SELECT message_id, consumer, processed_at
FROM   naive_inbox
ORDER  BY consumer;

-- >>> QUERY: the fix -- the business key is UNIQUE
CREATE TABLE processed_messages (
    message_id   TEXT NOT NULL,
    payment_ref  TEXT NOT NULL,
    processed_at TEXT NOT NULL,
    CONSTRAINT uq_processed_messages UNIQUE (message_id)
);

-- >>> QUERY: first delivery -- 1 row back = a new message, do the work
INSERT INTO processed_messages (message_id, payment_ref, processed_at)
VALUES ('MSG-1001', 'PAY-77', '2025-06-02 10:00:01')
ON CONFLICT (message_id) DO NOTHING                  -- upsert: SQLite 3.24+, PostgreSQL 9.5+
RETURNING message_id, payment_ref;                   -- RETURNING: SQLite 3.35+, PostgreSQL

-- >>> QUERY: the same message redelivered -- 0 rows back = duplicate, skip the work and ack
INSERT INTO processed_messages (message_id, payment_ref, processed_at)
VALUES ('MSG-1001', 'PAY-77', '2025-06-02 10:00:09')
ON CONFLICT (message_id) DO NOTHING
RETURNING message_id, payment_ref;

-- A plain INSERT of the same message_id is not a QUERY block, because run_all.py counts an
-- error as a failure. Run by hand, SQLite rejects it:
--   INSERT INTO processed_messages (message_id, payment_ref, processed_at)
--   VALUES ('MSG-1001', 'PAY-77', '2025-06-02 10:00:09');
--   -> UNIQUE constraint failed: processed_messages.message_id

-- >>> QUERY: the update count JDBC gets for a DO NOTHING duplicate: 0 (SQLite: changes())
SELECT changes() AS rows_inserted_by_last_insert;

-- >>> QUERY: still one row, and it keeps the FIRST delivery's time
SELECT message_id, payment_ref, processed_at FROM processed_messages;

-- >>> QUERY: the upsert target -- one row per payment, the newest event wins
CREATE TABLE payment_status (
    payment_ref TEXT    PRIMARY KEY,
    status      TEXT    NOT NULL,
    event_seq   INTEGER NOT NULL,                        -- order of the events for one payment
    updated_at  TEXT    NOT NULL
);

-- >>> QUERY: event 1 INITIATED -- no row yet, so it INSERTs
INSERT INTO payment_status (payment_ref, status, event_seq, updated_at)
VALUES ('PAY-77', 'INITIATED', 1, '2025-06-02 10:00:01')
ON CONFLICT (payment_ref) DO UPDATE
SET    status     = excluded.status,
       event_seq  = excluded.event_seq,
       updated_at = excluded.updated_at
WHERE  excluded.event_seq > payment_status.event_seq
RETURNING payment_ref, status, event_seq, updated_at;

-- >>> QUERY: event 3 SETTLED overtakes event 2 -- conflict, newer seq, so it UPDATEs from excluded
INSERT INTO payment_status (payment_ref, status, event_seq, updated_at)
VALUES ('PAY-77', 'SETTLED', 3, '2025-06-02 10:05:00')
ON CONFLICT (payment_ref) DO UPDATE
SET    status     = excluded.status,
       event_seq  = excluded.event_seq,
       updated_at = excluded.updated_at
WHERE  excluded.event_seq > payment_status.event_seq
RETURNING payment_ref, status, event_seq, updated_at;

-- >>> QUERY: event 2 SENT arrives late -- older seq, the WHERE guard skips it (0 rows)
INSERT INTO payment_status (payment_ref, status, event_seq, updated_at)
VALUES ('PAY-77', 'SENT', 2, '2025-06-02 10:02:00')
ON CONFLICT (payment_ref) DO UPDATE
SET    status     = excluded.status,
       event_seq  = excluded.event_seq,
       updated_at = excluded.updated_at
WHERE  excluded.event_seq > payment_status.event_seq
RETURNING payment_ref, status, event_seq, updated_at;

-- >>> QUERY: event 3 redelivered -- same seq, skipped again (0 rows): the replay is a no-op
INSERT INTO payment_status (payment_ref, status, event_seq, updated_at)
VALUES ('PAY-77', 'SETTLED', 3, '2025-06-02 10:05:00')
ON CONFLICT (payment_ref) DO UPDATE
SET    status     = excluded.status,
       event_seq  = excluded.event_seq,
       updated_at = excluded.updated_at
WHERE  excluded.event_seq > payment_status.event_seq
RETURNING payment_ref, status, event_seq, updated_at;

-- >>> QUERY: final state after four events -- SETTLED, seq 3
SELECT payment_ref, status, event_seq, updated_at FROM payment_status;

-- >>> QUERY: the bug -- the same late event WITHOUT the guard moves SETTLED back to SENT
INSERT INTO payment_status (payment_ref, status, event_seq, updated_at)
VALUES ('PAY-77', 'SENT', 2, '2025-06-02 10:02:00')
ON CONFLICT (payment_ref) DO UPDATE
SET    status     = excluded.status,
       event_seq  = excluded.event_seq,
       updated_at = excluded.updated_at
RETURNING payment_ref, status, event_seq, updated_at;

-- ================= RESULTS ========================================================================
-- Generated by run_all.py against 00-schema/01_schema.sql + 02_seed.sql
-- Engine: SQLite 3.50.4.  Fresh database per file, so each file reproduces standalone.
-- ----------------------------------------------------------------------------------------
-- [the anti-pattern, set up -- an inbox table with NO unique key]
--   (no result set)
--
-- [consumer A and consumer B each run this check before either inserts: both see 0]
--   rows_found
--   ----------
--   0
--   (1 row)
--
-- [so both insert (one statement here; in production, two separate transactions)]
--   (no result set)
--
-- [the result -- one message, processed twice: a double payout]
--   message_id  consumer    processed_at
--   ----------  ----------  -------------------
--   MSG-1001    consumer-A  2025-06-02 10:00:01
--   MSG-1001    consumer-B  2025-06-02 10:00:01
--   (2 rows)
--
-- [the fix -- the business key is UNIQUE]
--   (no result set)
--
-- [first delivery -- 1 row back = a new message, do the work]
--   message_id  payment_ref
--   ----------  -----------
--   MSG-1001    PAY-77
--   (1 row)
--
-- [the same message redelivered -- 0 rows back = duplicate, skip the work and ack]
--   message_id  payment_ref
--   ----------  -----------
--   (0 rows)
--
-- [the update count JDBC gets for a DO NOTHING duplicate: 0 (SQLite: changes())]
--   rows_inserted_by_last_insert
--   ----------------------------
--   0
--   (1 row)
--
-- [still one row, and it keeps the FIRST delivery's time]
--   message_id  payment_ref  processed_at
--   ----------  -----------  -------------------
--   MSG-1001    PAY-77       2025-06-02 10:00:01
--   (1 row)
--
-- [the upsert target -- one row per payment, the newest event wins]
--   (no result set)
--
-- [event 1 INITIATED -- no row yet, so it INSERTs]
--   payment_ref  status     event_seq  updated_at
--   -----------  ---------  ---------  -------------------
--   PAY-77       INITIATED  1          2025-06-02 10:00:01
--   (1 row)
--
-- [event 3 SETTLED overtakes event 2 -- conflict, newer seq, so it UPDATEs from excluded]
--   payment_ref  status   event_seq  updated_at
--   -----------  -------  ---------  -------------------
--   PAY-77       SETTLED  3          2025-06-02 10:05:00
--   (1 row)
--
-- [event 2 SENT arrives late -- older seq, the WHERE guard skips it (0 rows)]
--   payment_ref  status  event_seq  updated_at
--   -----------  ------  ---------  ----------
--   (0 rows)
--
-- [event 3 redelivered -- same seq, skipped again (0 rows): the replay is a no-op]
--   payment_ref  status  event_seq  updated_at
--   -----------  ------  ---------  ----------
--   (0 rows)
--
-- [final state after four events -- SETTLED, seq 3]
--   payment_ref  status   event_seq  updated_at
--   -----------  -------  ---------  -------------------
--   PAY-77       SETTLED  3          2025-06-02 10:05:00
--   (1 row)
--
-- [the bug -- the same late event WITHOUT the guard moves SETTLED back to SENT]
--   payment_ref  status  event_seq  updated_at
--   -----------  ------  ---------  -------------------
--   PAY-77       SENT    2          2025-06-02 10:02:00
--   (1 row)
--
-- ======================================================================================
