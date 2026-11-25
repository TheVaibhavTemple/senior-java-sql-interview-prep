/* ==============================================================================================
 * Q36  .  Thread-safe counter
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : CAS via AtomicLong; striped cells via LongAdder; atomic map operations
 *
 * PROBLEM
 *   Make a counter correct under 16 threads. Then a per-key counter, for example hits per
 *   endpoint.
 *
 * ALGORITHM
 *   1. count++ is read-modify-write: three steps, not one.
 *   2. volatile makes each step visible but does not make the trio atomic - updates are
 *      still lost.
 *   3. synchronized is correct but serialises every thread on one monitor.
 *   4. AtomicLong uses compare-and-swap: read, compute, swap only if unchanged, retry
 *      otherwise.
 *   5. For per-key counting use ConcurrentHashMap.merge or computeIfAbsent - get-then-put
 *      races.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   volatile int with ++                           --            --        avoid
 *       > The trap. volatile gives visibility, not atomicity; ++ is read-modify-write.
 *   synchronized methods                           --            --        acceptable
 *       > Correct. Every thread serialises on one monitor.
 *   AtomicLong (CAS)                               --            --        WRITE THIS
 *       > Lock-free and correct. The default choice.
 *   LongAdder                                      --            --        WRITE THIS
 *       > Much faster under contention; sum() is a snapshot, so metrics only.
 *   ConcurrentHashMap.merge / computeIfAbsent      --            --        WRITE THIS
 *       > For per-key counting. get-then-put races.
 *
 * WHY THIS ONE
 *   "Why isn't volatile enough for count++?" comes up in almost every concurrency round.
 *   The answer is one sentence: increment is read, modify, write -- volatile makes each
 *   visible but does not make the three atomic together. From there, AtomicLong via CAS,
 *   and LongAdder when writes are hot and an approximate total is acceptable.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Draw the interleaving where two threads both read 5 and the counter ends at 6.
 *   ? When is LongAdder worse than AtomicLong?
 *   ? Why can't you build a monotonic counter from get-then-put on a ConcurrentHashMap?
 *   ? Is computeIfAbsent atomic? What deadlock can it cause?
 *   ? What DOES volatile solve correctly?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - volatile vs atomic vs synchronized
 *   - CAS and the ABA problem
 *   - ConcurrentHashMap atomic operations
 *   - Safe publication
 *   - Metrics counters
 * ============================================================================================== */

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Q36ThreadSafeCounter {

    static final int THREADS = 8, PER_THREAD = 50_000;
    static final int EXPECTED = THREADS * PER_THREAD;

    /* ---- THE TRAP : volatile gives VISIBILITY, not ATOMICITY ---- */
    static volatile int broken = 0;

    /* ---- A : synchronized. Correct, but one monitor for everybody. ---- */
    static final class SyncCounter {
        private long value;
        synchronized void inc()  { value++; }
        synchronized long get()  { return value; }
    }

    static long race(Runnable increment) throws InterruptedException {
        Thread[] ts = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> { for (int j = 0; j < PER_THREAD; j++) increment.run(); });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        return 0;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("expected total   : " + EXPECTED);

        race(() -> broken++);                              // read-modify-write, unprotected
        System.out.println("volatile int ++  : " + broken
                + (broken == EXPECTED ? "  (got lucky this run)" : "  <-- UPDATES LOST"));

        SyncCounter sc = new SyncCounter();
        race(sc::inc);
        System.out.println("synchronized     : " + sc.get());

        AtomicLong atomic = new AtomicLong();
        race(atomic::incrementAndGet);                      // compare-and-swap
        System.out.println("AtomicLong (CAS) : " + atomic.get());

        LongAdder adder = new LongAdder();
        race(adder::increment);                             // striped cells
        System.out.println("LongAdder        : " + adder.sum());

        /* ---- per-key counting: get-then-put RACES, merge does not ---- */
        ConcurrentHashMap<String, Integer> racy = new ConcurrentHashMap<>();
        race(() -> { Integer v = racy.get("/login"); racy.put("/login", v == null ? 1 : v + 1); });
        System.out.println("get-then-put     : " + racy.get("/login") + "  <-- RACY");

        ConcurrentHashMap<String, Integer> safe = new ConcurrentHashMap<>();
        race(() -> safe.merge("/login", 1, Integer::sum));
        System.out.println("CHM.merge        : " + safe.get("/login"));

        ConcurrentHashMap<String, LongAdder> adders = new ConcurrentHashMap<>();
        race(() -> adders.computeIfAbsent("/login", k -> new LongAdder()).increment());
        System.out.println("CHM+LongAdder    : " + adders.get("/login").sum());

        /* ---- what volatile DOES solve: whole-object reference swap ---- */
        System.out.println("volatile is right for: one writer swapping an immutable "
                + "reference that many readers see fully constructed");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured on OpenJDK 21)
 * NON-DETERMINISTIC: the unsynchronised counters lose a different number of updates every run --
 *                    that variation IS the lesson. Shape is stable, numbers are not.
 * ----------------------------------------------------------------------------------------------
 * expected total   : 400000
 * volatile int ++  : 391330  <-- UPDATES LOST
 * synchronized     : 400000
 * AtomicLong (CAS) : 400000
 * LongAdder        : 400000
 * get-then-put     : 73187  <-- RACY
 * CHM.merge        : 400000
 * CHM+LongAdder    : 400000
 * volatile is right for: one writer swapping an immutable reference that many readers see fully constructed
 * ============================================================================================== */
