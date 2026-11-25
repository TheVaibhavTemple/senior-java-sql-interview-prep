/* ==============================================================================================
 * Q50  .  Retry with exponential backoff and jitter, and an idempotent consumer
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Core Java & Java 21
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- fintech aur messaging rounds mein common; apni story bhi isi par hai
 * TECHNIQUE    : Retry only transient errors; capped exponential backoff with full jitter;
 *                putIfAbsent on the message id as the idempotency check
 *
 * PROBLEM
 *   Write a generic retry(callable, maxAttempts, baseDelay) for a flaky downstream such as a
 *   payment gateway. It must retry only transient errors, fail fast on permanent ones, back
 *   off exponentially with a cap and jitter, and be testable without real sleeping. Then make
 *   a message consumer idempotent, so a redelivered message is processed exactly once.
 *
 * ALGORITHM
 *   1. Classify the failure: transient (timeout, 503, throttling) is retried; permanent
 *      (validation, unknown account) is rethrown at once.
 *   2. Delay before retry n = random in [0, min(cap, base * 2^(n-1))]: exponential, capped,
 *      full jitter.
 *   3. Stop after maxAttempts and rethrow the last error, so the caller, or the dead-letter
 *      queue, sees it.
 *   4. Inject the sleeper and the Random, so a test checks the delays without waiting.
 *   5. Consumer: putIfAbsent(messageId) is one atomic "first time?" check. If processing
 *      fails, remove the mark so the redelivery can try again.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Retry every exception, fixed delay             --            --        avoid
 *       > Wastes attempts on permanent errors; every client retries at the same moment.
 *   Exponential backoff, no jitter                 --            --        acceptable
 *       > Spreads retries over time, but clients that failed together stay in lockstep.
 *   Transient only + capped backoff + full jitter  --            --        WRITE THIS
 *       > The standard policy. In real code it comes from a library.
 *   Consumer with no idempotency                   --            --        avoid
 *       > A redelivered message credits the account twice.
 *   containsKey, then put                          O(1)          O(n)      avoid
 *       > Check-then-act race: two threads can both see "absent".
 *   putIfAbsent / DB unique key on the message id  O(1)          O(n)      WRITE THIS
 *       > One atomic check-and-mark. In production the key lives in the database or Redis.
 *
 * WHY THIS ONE
 *   In payments, "just retry it" is wrong twice over: it retries errors that can never pass,
 *   and every retry can create a duplicate. The interviewer wants both halves together: a
 *   polite retry policy on the caller side (transient only, backoff, cap, jitter) and
 *   idempotency on the receiving side, because at-least-once delivery means duplicates WILL
 *   arrive.
 *
 * SAMJHO
 *   Retry sirf temporary error par karo (timeout, 503). Permanent error (invalid account) par
 *   turant fail karo. Har attempt ke baad delay double karo, upar ek cap rakho, aur random
 *   jitter daalo taaki saare clients ek saath wapas na aayein. Duplicate message aayega hi,
 *   isliye consumer message id dekh ke dobara process na kare.
 *
 * AISE BOLO  (30-40 sec)
 *   "A naive retry loop retries every error with a fixed delay, so it wastes attempts on
 *   errors that can never succeed and sends every client back at the same moment. I retry only
 *   transient failures such as timeouts or a 503, fail fast on permanent ones such as a
 *   validation error, and wait an exponentially growing delay with a cap and full jitter. Since
 *   retries and at-least-once delivery mean a message can arrive twice, the consumer records
 *   the message id atomically, with putIfAbsent here or a unique key in the database, and
 *   skips anything already processed. In real code I would take the retry part from Spring
 *   Retry or Resilience4j rather than hand-write it."
 *
 * TRAP
 *   ! Har exception par retry mat karo. Validation ya "account not found" par retry sirf load
 *     badhata hai aur fail hona der se hota hai (output line 3).
 *   ! Service Bus duplicate detection sirf same MessageId ke duplicate SEND ko time window ke
 *     andar rokta hai. Lock expire ya abandon hone par wahi message consumer ko dobara aata
 *     hai, isliye consumer side idempotency phir bhi chahiye.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why jitter? Is exponential backoff not enough?
 *     -> Bina jitter ke jo clients ek saath fail hue, wo ek saath 100, 200, 400 ms par wapas
 *        aate hain: thundering herd, recover hoti service phir gir jaati hai. Full jitter unhe
 *        0 se ceiling tak faila deta hai (output line 5).
 *   ? Why cap the delay, and what is a retry budget?
 *     -> Cap ke bina 2^n delay minutes tak pahunch jaata hai, request ka timeout pehle khatam.
 *        Retry budget: retries ko total requests ke chhote hisse tak rokho, kyunki 3 layers
 *        har ek 3 attempts kare to neeche ek user call 3 x 3 x 3 = 27 calls ban sakti hai.
 *   ? Which errors are transient?
 *     -> Timeout, connection reset, HTTP 503 ya 429 (Retry-After header maano), DB deadlock
 *        victim. Permanent: 400, 401/403, 404, validation, business rule. Unknown ko permanent
 *        maano.
 *   ? Is it safe to retry a payment POST?
 *     -> Tabhi jab server idempotent ho. Client har payment ke saath ek idempotency key (UUID)
 *        bhejta hai; server use unique constraint ke saath save karta hai aur duplicate par
 *        pehla result lauta deta hai.
 *   ? Where do idempotency keys live, and for how long?
 *     -> In-memory map restart aur multiple instances mein kaam nahi karta. Redis
 *        `SET key value NX EX <ttl>` ya DB table with unique key + cleanup job. TTL broker ke
 *        redelivery window aur client ke retry window se lamba rakho.
 *   ? Mark first or process first?
 *     -> Mark pehle kiya aur processing fail hui to mark hatao (code mein remove), warna
 *        message kabhi process nahi hoga. Sabse safe: business write aur idempotency key
 *        insert ek hi DB transaction mein.
 *   ? Would you hand-write this in production?
 *     -> Nahi. Spring Retry (@Retryable + backoff) ya Resilience4j Retry, saath mein circuit
 *        breaker taaki down service par retry storm na ho. Ye file concept samjhane ke liye hai.
 *   ? What if the thread is interrupted during the backoff sleep?
 *     -> Retry band karo, interrupt flag restore karo (Thread.currentThread().interrupt()) aur
 *        caller ko batao. Swallow kiya to shutdown atak jaata hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Exponential backoff with full jitter
 *   - Idempotent consumer and idempotency keys
 *   - At-least-once vs exactly-once delivery
 *   - Circuit breaker and retry budget
 *   - Dead-letter queue handling
 *
 * APNI STORY SE JODO
 *   "On the event-driven file-processing platform I built, eight microservices on Azure
 *   Service Bus with Spring Boot and PostgreSQL on AKS, the @Scheduled batch jobs use retry
 *   with backoff, Service Bus duplicate detection drops duplicate sends, and we review the
 *   dead-letter queue for messages that keep failing."
 *   Note: DLQ ka matlab: jo message baar baar fail ho, wo infinite retry nahi karta, DLQ mein
 *   jaata hai aur koi review karta hai. Consumer side duplicate kaise roka, wo [bhar do: jo
 *   sach ho]. Resilience4j production mein use kiya, ye mat bolo.
 *
 * VARIANTS     : a retry decorator / annotation, exactly-once processing over at-least-once
 *                delivery, an idempotent payment API
 * ============================================================================================== */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class Q50RetryWithBackoffAndIdempotency {

    /* ---- what is worth retrying: decide by exception TYPE ---- */
    static final class TransientException extends RuntimeException {    // timeout, 503, throttled
        TransientException(String msg) { super(msg); }
    }
    static final class PermanentException extends RuntimeException {    // 400, bad account
        PermanentException(String msg) { super(msg); }
    }

    /* ---- injected sleeper: production really sleeps, the demo only records the delays ---- */
    interface Sleeper { void sleep(long ms) throws InterruptedException; }

    static final class RecordingSleeper implements Sleeper {
        final List<Long> delays = new ArrayList<>();
        @Override public void sleep(long ms) { delays.add(ms); }
    }

    /* ---- THE TRAP : retry every exception, fixed delay, no jitter ---- */
    static <T> T naiveRetry(Callable<T> call, int maxAttempts, long delayMs, Sleeper sleeper)
            throws Exception {
        for (int attempt = 1; ; attempt++) {
            try {
                return call.call();
            } catch (Exception e) {
                if (attempt >= maxAttempts) throw e;
                sleeper.sleep(delayMs);                 // the same delay for every client
            }
        }
    }

    /* ---- THE FIX : transient only, exponential backoff, a cap, full jitter ---- */
    static final class Retry {
        private final long maxDelayMs;
        private final Predicate<Exception> isTransient;
        private final Random random;
        private final Sleeper sleeper;

        Retry(long maxDelayMs, Predicate<Exception> isTransient, Random random, Sleeper sleeper) {
            this.maxDelayMs = maxDelayMs;
            this.isTransient = isTransient;
            this.random = random;
            this.sleeper = sleeper;
        }

        <T> T retry(Callable<T> call, int maxAttempts, long baseDelayMs) throws Exception {
            for (int attempt = 1; ; attempt++) {
                try {
                    return call.call();
                } catch (Exception e) {
                    if (!isTransient.test(e)) throw e;          // permanent: fail fast
                    if (attempt >= maxAttempts) throw e;        // attempts used up: give up
                    sleeper.sleep(delayFor(attempt, baseDelayMs));
                }
            }
        }

        /** Full jitter: uniform in [0, min(cap, base * 2^(attempt - 1))]. */
        long delayFor(int attempt, long baseDelayMs) {
            long ceiling = Math.min(maxDelayMs, baseDelayMs << Math.min(attempt - 1, 30));
            return random.nextInt((int) ceiling + 1);           // shift capped: no overflow
        }
    }

    /** A fake downstream: fails `failures` times with `error`, then succeeds. */
    static Callable<String> flaky(int failures, RuntimeException error, AtomicInteger calls) {
        return () -> {
            if (calls.incrementAndGet() <= failures) throw error;
            return "SETTLED";
        };
    }

    /* ---- idempotency: a ledger, a naive consumer and an idempotent one ---- */
    record Message(String messageId, String account, long amount) { }

    static final class Ledger {
        private final ConcurrentHashMap<String, Long> balances = new ConcurrentHashMap<>();
        volatile boolean down;                                  // simulate a DB outage
        void credit(String account, long amount) {
            if (down) throw new TransientException("db down");
            balances.merge(account, amount, Long::sum);
        }
        long balance(String account) { return balances.getOrDefault(account, 0L); }
    }

    /* ---- THE TRAP : no idempotency, so a redelivery credits twice ---- */
    static final class NaiveConsumer {
        final Ledger ledger = new Ledger();
        void onMessage(Message m) { ledger.credit(m.account(), m.amount()); }
    }

    /* ---- THE FIX : putIfAbsent on the message id is the atomic "first time?" check ---- */
    static final class IdempotentConsumer {
        final Ledger ledger = new Ledger();
        final ConcurrentHashMap<String, Boolean> seen = new ConcurrentHashMap<>();
        final AtomicInteger processed = new AtomicInteger(), skipped = new AtomicInteger();

        void onMessage(Message m) {
            if (seen.putIfAbsent(m.messageId(), Boolean.TRUE) != null) {   // already seen
                skipped.incrementAndGet();
                return;                                    // duplicate: acknowledge and skip
            }
            try {
                ledger.credit(m.account(), m.amount());
                processed.incrementAndGet();
            } catch (RuntimeException e) {
                seen.remove(m.messageId());                // not done: let the redelivery retry
                throw e;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Predicate<Exception> transientOnly = e -> e instanceof TransientException;

        // 1. three timeouts, then success
        RecordingSleeper s1 = new RecordingSleeper();
        AtomicInteger c1 = new AtomicInteger();
        String result = new Retry(2_000, transientOnly, new Random(42), s1)
                .retry(flaky(3, new TransientException("timeout"), c1), 5, 100);
        System.out.println("1 transient x3 : " + result + " after " + c1.get() + " attempts, slept "
                + s1.delays + " ms (ceilings 100, 200, 400)");

        // 2. permanent error: fail fast
        RecordingSleeper s2 = new RecordingSleeper();
        AtomicInteger c2 = new AtomicInteger();
        try {
            new Retry(2_000, transientOnly, new Random(42), s2)
                    .retry(flaky(99, new PermanentException("invalid account number"), c2), 5, 100);
        } catch (PermanentException e) {
            System.out.println("2 permanent    : failed fast after " + c2.get() + " attempt, slept "
                    + s2.delays + " (" + e.getMessage() + ")");
        }

        // 3. the naive loop on the same permanent error
        RecordingSleeper s3 = new RecordingSleeper();
        AtomicInteger c3 = new AtomicInteger();
        try {
            naiveRetry(flaky(99, new PermanentException("invalid account number"), c3), 5, 100, s3);
        } catch (PermanentException e) {
            System.out.println("3 naive retry  : " + c3.get() + " attempts, slept " + s3.delays
                    + "  <-- can never pass");
        }

        // 4. downstream stays down: give up after maxAttempts, delays capped at 1000 ms
        RecordingSleeper s4 = new RecordingSleeper();
        AtomicInteger c4 = new AtomicInteger();
        try {
            new Retry(1_000, transientOnly, new Random(42), s4)
                    .retry(flaky(99, new TransientException("503"), c4), 7, 100);
        } catch (TransientException e) {
            System.out.println("4 always down  : gave up after " + c4.get() + " attempts, slept "
                    + s4.delays);
            System.out.println("4 always down  : ceilings 100, 200, 400, 800, then capped at 1000");
        }

        // 5. thundering herd: five clients fail at the same instant, first retry at what time?
        List<Long> noJitter = new ArrayList<>(), fullJitter = new ArrayList<>();
        Retry herd = new Retry(10_000, transientOnly, new Random(7), new RecordingSleeper());
        for (int client = 0; client < 5; client++) {
            noJitter.add(1_000L);
            fullJitter.add(herd.delayFor(1, 1_000));
        }
        System.out.println("5 first retry  : no jitter   " + noJitter + " ms  <-- all at once");
        System.out.println("5 first retry  : full jitter " + fullJitter + " ms");

        // 6. idempotency: the same message delivered twice
        Message m = new Message("MSG-1001", "ACC-1", 100);
        NaiveConsumer naive = new NaiveConsumer();
        naive.onMessage(m);
        naive.onMessage(m);                                // broker redelivers
        System.out.println("6 no idempotency   : delivered 2x, balance = "
                + naive.ledger.balance("ACC-1") + "  <-- credited twice");

        IdempotentConsumer once = new IdempotentConsumer();
        once.onMessage(m);
        once.onMessage(m);
        System.out.println("6 putIfAbsent      : delivered 2x, balance = "
                + once.ledger.balance("ACC-1") + ", processed = " + once.processed.get()
                + ", skipped = " + once.skipped.get());

        // 7. eight threads deliver the same message 1,000 times in total
        IdempotentConsumer racing = new IdempotentConsumer();
        CountDownLatch go = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 1_000; i++) {
                pool.submit(() -> {
                    go.await();
                    racing.onMessage(m);
                    return null;
                });
            }
            go.countDown();
        }
        System.out.println("7 8 threads, 1000x : processed = " + racing.processed.get()
                + ", balance = " + racing.ledger.balance("ACC-1"));

        // 8. first attempt fails mid-processing: the mark is removed, the redelivery succeeds
        IdempotentConsumer flakyDb = new IdempotentConsumer();
        flakyDb.ledger.down = true;
        try {
            flakyDb.onMessage(m);
        } catch (TransientException e) {
            flakyDb.ledger.down = false;                   // broker redelivers after a while
            flakyDb.onMessage(m);
        }
        System.out.println("8 fail, redeliver  : processed = " + flakyDb.processed.get()
                + ", balance = " + flakyDb.ledger.balance("ACC-1") + "  (not lost, not doubled)");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 1 transient x3 : SETTLED after 4 attempts, slept [15, 120, 368] ms (ceilings 100, 200, 400)
 * 2 permanent    : failed fast after 1 attempt, slept [] (invalid account number)
 * 3 naive retry  : 5 attempts, slept [100, 100, 100, 100]  <-- can never pass
 * 4 always down  : gave up after 7 attempts, slept [15, 120, 368, 359, 663, 459]
 * 4 always down  : ceilings 100, 200, 400, 800, then capped at 1000
 * 5 first retry  : no jitter   [1000, 1000, 1000, 1000, 1000] ms  <-- all at once
 * 5 first retry  : full jitter [640, 285, 263, 387, 138] ms
 * 6 no idempotency   : delivered 2x, balance = 200  <-- credited twice
 * 6 putIfAbsent      : delivered 2x, balance = 100, processed = 1, skipped = 1
 * 7 8 threads, 1000x : processed = 1, balance = 100
 * 8 fail, redeliver  : processed = 1, balance = 100  (not lost, not doubled)
 * ============================================================================================== */
