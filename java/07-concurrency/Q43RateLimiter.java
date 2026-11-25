/* ==============================================================================================
 * Q43  .  Rate limiter
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : Token bucket backed by a Semaphore; sliding-window log per key
 *
 * PROBLEM
 *   Limit callers to N requests per second with a small burst allowance. Then a per-user
 *   limit over a sliding window.
 *
 * ALGORITHM
 *   1. Fixed-window counters allow 2N requests across a boundary - that flaw is why token
 *      buckets exist.
 *   2. Token bucket: a Semaphore holds the tokens, a scheduled task releases one per
 *      period.
 *   3. Cap the refill at the burst size, or an idle service banks unlimited burst capacity.
 *   4. tryAcquire gives a non-blocking reject; the timed variant gives a bounded wait.
 *   5. Sliding window: per key keep a deque of timestamps, evict ones older than the
 *      window, then compare size to the limit.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Counter reset by a scheduled task (fixed windo --            --        avoid
 *       > Allows 2N requests across a window boundary. The classic flaw.
 *   Token bucket with a Semaphore + refill task    --            --        WRITE THIS
 *       > Smooth rate, controlled burst, non-blocking tryAcquire.
 *   Sliding window log of timestamps per key       --            --        WRITE THIS
 *       > Exact, no boundary flaw; memory grows with the limit.
 *   Sliding window counter (weighted buckets)      --            --        acceptable
 *       > Approximate but constant memory -- what large systems actually use.
 *   Guava RateLimiter / Resilience4j               --            --        WRITE THIS
 *       > The library answer. Name it.
 *
 * WHY THIS ONE
 *   Start by naming the fixed-window boundary problem -- N requests at 0.999s and N more
 *   at 1.001s is 2N in two milliseconds -- because that is why the other algorithms exist.
 *   Then token bucket for a global limit and sliding window per user. The cap in
 *   addToken() matters: without it an idle service banks unlimited burst. Volunteer the
 *   distributed version before they ask, because in a microservice this lives in Redis,
 *   not in a JVM.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? What is wrong with a fixed-window counter?
 *   ? Why cap the bucket in the refill task?
 *   ? Make it work across 10 instances -- what changes?
 *   ? Implement it in Redis. Why does it need a Lua script?
 *   ? Rate limiter vs bulkhead vs circuit breaker -- when each?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Throttling and backpressure
 *   - Bulkhead pattern
 *   - Semaphore use cases
 *   - Distributed rate limiting
 *   - API gateway design
 * ============================================================================================== */

import java.util.*;
import java.util.concurrent.*;

public class Q43RateLimiter {

    /* ======================================================================
       TOKEN BUCKET - steady refill rate, burst capped at the bucket size.
       Name the fixed-window flaw first: N requests at 0.999s plus N at
       1.001s is 2N inside two milliseconds. That is why this exists.
       ====================================================================== */
    static final class TokenBucketRateLimiter {
        private final Semaphore tokens;
        private final int capacity;
        private final ScheduledExecutorService refiller =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "rate-limiter-refill");
                    t.setDaemon(true);
                    return t;
                });

        TokenBucketRateLimiter(int permitsPerSecond, int burst) {
            this.capacity = burst;
            this.tokens = new Semaphore(burst);
            long periodNanos = TimeUnit.SECONDS.toNanos(1) / permitsPerSecond;
            refiller.scheduleAtFixedRate(this::addToken, periodNanos, periodNanos,
                                         TimeUnit.NANOSECONDS);
        }

        private void addToken() {
            // CAP the bucket, or an idle service banks unlimited burst capacity
            if (tokens.availablePermits() < capacity) tokens.release();
        }

        boolean tryAcquire() { return tokens.tryAcquire(); }

        boolean acquire(long timeout, TimeUnit unit) throws InterruptedException {
            return tokens.tryAcquire(timeout, unit);
        }

        void shutdown() { refiller.shutdownNow(); }
    }

    /* ======================================================================
       SLIDING WINDOW LOG - exact, per key, no background thread.
       ====================================================================== */
    static final class SlidingWindowLimiter {
        private final int limit;
        private final long windowMillis;
        private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

        SlidingWindowLimiter(int limit, long windowMillis) {
            this.limit = limit; this.windowMillis = windowMillis;
        }

        boolean allow(String key, long now) {
            Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (q) {                 // lock the per-key deque, not the whole map
                while (!q.isEmpty() && now - q.peekFirst() >= windowMillis) q.pollFirst();
                if (q.size() >= limit) return false;
                q.addLast(now);
                return true;
            }
        }
        boolean allow(String key) { return allow(key, System.currentTimeMillis()); }
    }

    /* ---- THE FLAW to name out loud : fixed window ---- */
    static final class FixedWindowLimiter {
        private final int limit; private final long windowMs;
        private long windowStart = 0; private int count = 0;
        FixedWindowLimiter(int limit, long windowMs) { this.limit = limit; this.windowMs = windowMs; }
        synchronized boolean allow(long now) {
            if (now - windowStart >= windowMs) { windowStart = now; count = 0; }
            return count++ < limit;
        }
    }

    public static void main(String[] args) throws Exception {
        /* token bucket: the burst is available immediately, then it is empty */
        TokenBucketRateLimiter tb = new TokenBucketRateLimiter(5, 5);
        StringBuilder burst = new StringBuilder();
        for (int i = 0; i < 7; i++) burst.append(tb.tryAcquire() ? "Y" : "n");
        System.out.println("burst of 5       : " + burst + "   (5 allowed, then rejected)");

        Thread.sleep(500);                       // ~2 tokens refill at 5/sec
        int after = 0;
        for (int i = 0; i < 5; i++) if (tb.tryAcquire()) after++;
        System.out.println("after 500ms      : " + after + " token(s) refilled  (rate 5/sec)");
        System.out.println("blocking acquire : " + tb.acquire(500, TimeUnit.MILLISECONDS));
        tb.shutdown();

        /* sliding window, driven by an injected clock so the demo is deterministic */
        SlidingWindowLimiter sw = new SlidingWindowLimiter(3, 1000);
        long t = 10_000;
        StringBuilder log = new StringBuilder();
        for (long offset : new long[]{0, 100, 200, 300, 1100, 1200}) {
            log.append(sw.allow("user-7", t + offset) ? "Y" : "n");
        }
        System.out.println("sliding window   : " + log
                + "   (3 per 1000ms; the 4th at +300 is rejected, +1100 is a fresh window)");
        System.out.println("per-key isolated : " + (sw.allow("user-8", t + 300) ? "Y" : "n")
                + "   (a different user is unaffected)");

        /* the boundary flaw of a fixed window: 2N requests in 2ms */
        FixedWindowLimiter fw = new FixedWindowLimiter(3, 1000);
        int allowed = 0;
        for (int i = 0; i < 3; i++) if (fw.allow(1_999)) allowed++;   // end of window 1
        for (int i = 0; i < 3; i++) if (fw.allow(2_001)) allowed++;   // start of window 2
        System.out.println("fixed window FLAW: " + allowed
                + " requests allowed across a 2ms boundary, limit was 3/sec");

        System.out.println("DISTRIBUTED      : across N instances this must move to Redis "
                + "(INCR + EXPIRE, or a Lua script for atomicity)");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * burst of 5       : YYYYYnn   (5 allowed, then rejected)
 * after 500ms      : 2 token(s) refilled  (rate 5/sec)
 * blocking acquire : true
 * sliding window   : YYYnYY   (3 per 1000ms; the 4th at +300 is rejected, +1100 is a fresh window)
 * per-key isolated : Y   (a different user is unaffected)
 * fixed window FLAW: 3 requests allowed across a 2ms boundary, limit was 3/sec
 * DISTRIBUTED      : across N instances this must move to Redis (INCR + EXPIRE, or a Lua script for atomicity)
 * ============================================================================================== */
