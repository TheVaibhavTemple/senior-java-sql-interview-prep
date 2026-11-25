/* ==============================================================================================
 * Q51  .  A synchronized shared HashMap vs ConcurrentHashMap in a batch job
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Core Java & Java 21
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- HashMap vs ConcurrentHashMap har round mein; 45 se 8 min story yahi hai
 * TECHNIQUE    : Per-key atomic merge on ConcurrentHashMap (CAS on an empty bin, a lock on one
 *                bin otherwise, lock-free reads); LongAdder values for very hot keys
 *
 * PROBLEM
 *   Eight worker threads aggregate a record count per currency, like a batch job. Compare
 *   (A) a plain HashMap, (B) one synchronized shared HashMap, and (C) ConcurrentHashMap.merge.
 *   Show which lose updates, why Collections.synchronizedMap with get-then-put still loses
 *   them, and why one lock does not scale: every thread, readers included, waits on it.
 *
 * ALGORITHM
 *   1. Plain HashMap: get + put is read-modify-write with no lock. Two threads read the same
 *      old value and one update is lost.
 *   2. synchronized(map) around get + put: correct, but one monitor for every reader and
 *      every writer, so adding threads adds waiting.
 *   3. Collections.synchronizedMap locks each call, not the pair: get + put still races.
 *   4. ConcurrentHashMap.merge(key, 1L, Long::sum) is atomic per key. In Java 8+ a write
 *      uses CAS on an empty bin, otherwise synchronized on that bin's first node; get()
 *      takes no lock at all.
 *   5. For a few very hot keys: computeIfAbsent(key, k -> new LongAdder()).increment().
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Plain HashMap from many threads                --            --        avoid
 *       > Lost updates; while resizing it can lose whole entries.
 *   synchronized(map) / one shared lock            --            --        acceptable
 *       > Correct. Every thread queues on one monitor; it does not scale.
 *   Collections.synchronizedMap, get then put      --            --        avoid
 *       > Each call is locked, the compound action is not.
 *   ConcurrentHashMap.merge / compute              --            --        WRITE THIS
 *       > Correct, locks one bin at a time, reads never block.
 *   ConcurrentHashMap<K, LongAdder>                --            --        WRITE THIS
 *       > Best when many threads hit the same few keys.
 *
 * WHY THIS ONE
 *   "HashMap vs Hashtable vs ConcurrentHashMap" is the most common collections question in
 *   Indian Java rounds. At senior level the interviewer wants the mechanism, not the list:
 *   what exactly is locked, why reads are free, why get-then-put is still wrong, and what you
 *   must never do inside compute. A batch-job aggregation also lets you tell a real
 *   performance story.
 *
 * SAMJHO
 *   Plain HashMap multi-thread mein updates kho deta hai. Ek synchronized shared map sahi
 *   jawab deta hai, par saare threads, padhne wale bhi, ek hi lock ki line mein lagte hain.
 *   ConcurrentHashMap sirf us bin ko lock karta hai jismein likh raha hai (khali bin par CAS),
 *   read par lock hi nahi, aur merge() ek key par atomic hai. Isliye wahi kaam bina queue ke.
 *
 * AISE BOLO  (30-40 sec)
 *   "If worker threads aggregate into a plain HashMap, get-then-put races and the totals come
 *   out short, and wrapping it in Collections.synchronizedMap does not fix the compound
 *   action. Putting the whole update under one lock is correct, but every thread, readers
 *   included, waits on that one monitor, so more threads only means more waiting.
 *   ConcurrentHashMap since Java 8 locks only the bin being written, with CAS for an empty
 *   bin, reads take no lock, and merge or compute is atomic per key. Inside compute I keep the
 *   function short and never do I/O, because the bin stays locked while it runs."
 *
 * TRAP
 *   ! synchronizedMap ya ConcurrentHashMap par get() phir put() = compound action, atomic nahi
 *     (output line B'). merge() ya compute() use karo.
 *   ! compute/merge ke function mein DB ya HTTP call mat karo: bin locked rehta hai, us bin ke
 *     saare writers ruk jaate hain (output ki last line). Function ke andar usi map ki doosri
 *     key bhi update mat karo.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? How does ConcurrentHashMap lock in Java 8+, compared with Java 7?
 *     -> Java 7: segments (default 16), har segment ek ReentrantLock. Java 8+: segments khatam;
 *        khali bin mein CAS se insert, bhari bin par uske first node par synchronized. Lock
 *        sirf ek bin ka.
 *   ? Does get() ever lock?
 *     -> Nahi. Node ka value aur next volatile hain, get() bina lock ke padhta hai. Isliye slow
 *        compute ke beech bhi reader nahi ruka (output line).
 *   ? Is size() exact?
 *     -> Concurrent updates ke beech size() ek estimate hai (counter cells ka sum). Bade map ke
 *        liye mappingCount() long deta hai. Exact chahiye to writers ruk jaane ke baad padho.
 *   ? Why no null keys or values in ConcurrentHashMap?
 *     -> get() null de to "key nahi hai" aur "value null hai" alag nahi kar sakte; check karke
 *        dekhne tak doosra thread map badal dega. Isliye null allowed hi nahi.
 *   ? Are compute and merge atomic? What must you never do inside them?
 *     -> Haan, per key atomic. Function bin lock ke andar chalta hai: blocking I/O, lamba kaam,
 *        ya usi map ki doosri key ka update nahi (Java 9+ kuch cases mein IllegalStateException
 *        "Recursive update" deta hai).
 *   ? LongAdder or merge(k, 1L, Long::sum)?
 *     -> Kuch hi hot keys aur bahut threads: LongAdder, contention cells mein bat jaata hai. Kam
 *        contention par merge simple aur kaafi hai.
 *   ? Can I iterate a ConcurrentHashMap while others write?
 *     -> Haan. Iterator weakly consistent hai, ConcurrentModificationException nahi deta, beech
 *        ke updates dikhein ya na dikhein. synchronizedMap par iterate karne ke liye
 *        synchronized(map) block zaroori hai.
 *   ? Why did the plain HashMap lose updates even with no resize here?
 *     -> get aur put alag steps hain; do threads ek hi purani value padh ke dono +1 likhte hain.
 *        Resize hota to entries bhi kho sakti thi; Java 7 mein concurrent resize se infinite
 *        loop tak ho jaata tha.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Lock contention and lock granularity
 *   - Atomic per-key updates: compute, merge, computeIfAbsent
 *   - LongAdder for hot counters (Q36)
 *   - Hashtable vs synchronizedMap vs ConcurrentHashMap
 *   - Thread-safe aggregation in batch jobs
 *
 * APNI STORY SE JODO
 *   "In a batch job, the threads were contending on one lock: a synchronized shared HashMap.
 *   I replaced it with ConcurrentHashMap to remove the lock contention, and the batch window
 *   went from 45 minutes to 8 minutes."
 *   Note: Agla sawaal: "kaise pata chala ki lock hi problem tha?" aur "update kaise kiya?".
 *   Dono ka jawab [bhar do: kya dekha, aur merge / compute / jo sach ho]. Thread count ya
 *   record count mat banao; get-then-put bola to interviewer wahi pakdega.
 *
 * VARIANTS     : multi-threaded word count, hits per endpoint (Q36), Hashtable vs
 *                ConcurrentHashMap
 * ============================================================================================== */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.*;

public class Q51SynchronizedMapVsConcurrentHashMap {

    static final String[] CURRENCIES = {"INR", "USD", "EUR", "GBP", "AED"};
    static final int THREADS = 8, RECORDS_PER_THREAD = 100_000, RUNS = 5;
    static final long EXPECTED = (long) THREADS * RECORDS_PER_THREAD;

    /** One way of aggregating: what a worker calls per record, and how to read the total. */
    record Target(Consumer<String> add, LongSupplier total) { }

    static Map<String, Long> seeded(Map<String, Long> m) {
        for (String c : CURRENCIES) m.put(c, 0L);        // keys known up front: no resize later
        return m;
    }

    static long sum(Map<String, Long> m) {               // called after every worker has joined
        long s = 0;
        for (long v : m.values()) s += v;
        return s;
    }

    /* ---- (A) THE TRAP : plain HashMap, get then put, no lock ---- */
    static Target plainHashMap() {
        Map<String, Long> m = seeded(new HashMap<>());
        return new Target(k -> m.put(k, m.get(k) + 1L), () -> sum(m));
    }

    /* ---- (B) one synchronized shared HashMap: correct, one monitor for everybody ---- */
    static Target oneLock() {
        Map<String, Long> m = seeded(new HashMap<>());
        return new Target(k -> { synchronized (m) { m.put(k, m.get(k) + 1L); } }, () -> sum(m));
    }

    /* ---- (B') Collections.synchronizedMap: each call locked, the get + put pair is not ---- */
    static Target synchronizedMapGetPut() {
        Map<String, Long> m = Collections.synchronizedMap(seeded(new HashMap<>()));
        return new Target(k -> m.put(k, m.get(k) + 1L), () -> sum(m));
    }

    /* ---- (C) THE FIX : ConcurrentHashMap.merge, atomic per key ---- */
    static Target concurrentMerge() {
        ConcurrentHashMap<String, Long> m = new ConcurrentHashMap<>();
        return new Target(k -> m.merge(k, 1L, Long::sum), () -> sum(m));
    }

    /* ---- (C') ConcurrentHashMap of LongAdder: spreads a hot key over cells ---- */
    static Target concurrentLongAdder() {
        ConcurrentHashMap<String, LongAdder> m = new ConcurrentHashMap<>();
        return new Target(k -> m.computeIfAbsent(k, x -> new LongAdder()).increment(),
                () -> m.values().stream().mapToLong(LongAdder::sum).sum());
    }

    /** Runs the batch RUNS times on a fresh target; true only if every run hit EXPECTED. */
    static boolean matchedOnEveryRun(Supplier<Target> fresh) throws InterruptedException {
        boolean everyRun = true;
        for (int run = 0; run < RUNS; run++) {
            Target target = fresh.get();
            CountDownLatch go = new CountDownLatch(1);
            Thread[] workers = new Thread[THREADS];
            for (int t = 0; t < THREADS; t++) {
                int offset = t;
                workers[t] = new Thread(() -> {
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    for (int i = 0; i < RECORDS_PER_THREAD; i++) {
                        target.add().accept(CURRENCIES[(i + offset) % CURRENCIES.length]);
                    }
                });
                workers[t].start();
            }
            go.countDown();                                // all workers start together
            for (Thread w : workers) w.join();
            everyRun &= target.total().getAsLong() == EXPECTED;
        }
        return everyRun;
    }

    static void report(String label, boolean matched) {
        System.out.println(label + ": matched on every run = " + matched
                + (matched ? "    lost updates: no" : "   lost updates: yes"));
    }

    static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static long timeMs(Runnable r) {
        long start = System.nanoTime();
        r.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    public static void main(String[] args) throws Exception {
        // Lost updates are random in size, so only "did EVERY run match?" is printed. With
        // 800,000 racy updates per run, a run with zero lost updates does not happen in practice.
        System.out.println("batch: " + THREADS + " threads x " + RECORDS_PER_THREAD
                + " records over 5 keys, expected total " + EXPECTED + ", " + RUNS + " runs each");
        report("(A)  plain HashMap, get + put      ", matchedOnEveryRun(
                Q51SynchronizedMapVsConcurrentHashMap::plainHashMap));
        report("(B)  synchronized(shared HashMap)  ", matchedOnEveryRun(
                Q51SynchronizedMapVsConcurrentHashMap::oneLock));
        report("(B') synchronizedMap, get + put    ", matchedOnEveryRun(
                Q51SynchronizedMapVsConcurrentHashMap::synchronizedMapGetPut));
        report("(C)  ConcurrentHashMap.merge       ", matchedOnEveryRun(
                Q51SynchronizedMapVsConcurrentHashMap::concurrentMerge));
        report("(C') ConcurrentHashMap + LongAdder ", matchedOnEveryRun(
                Q51SynchronizedMapVsConcurrentHashMap::concurrentLongAdder));

        // Why (B) does not scale: ONE lock, and even a reader has to wait for it.
        // Thresholds are wide (a 300 ms hold vs 200 ms / 100 ms), so these booleans are stable.
        Map<String, Long> sync = Collections.synchronizedMap(new HashMap<>(Map.of("INR", 0L)));
        CountDownLatch holding = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            synchronized (sync) {                          // a writer holds the map's one lock
                holding.countDown();
                sleepMs(300);
            }
        });
        writer.start();
        holding.await();
        long syncReadWait = timeMs(() -> sync.get("INR"));
        writer.join();
        System.out.println("reader on synchronizedMap waits for the writer's lock (>= 200 ms) = "
                + (syncReadWait >= 200));

        // ConcurrentHashMap: a slow compute locks one bin; readers do not wait at all
        ConcurrentHashMap<String, Long> chm = new ConcurrentHashMap<>(Map.of("INR", 0L));
        CountDownLatch inCompute = new CountDownLatch(1);
        Thread slow = new Thread(() -> chm.compute("INR", (k, v) -> {
            inCompute.countDown();
            sleepMs(300);                                  // never do this in real code
            return v + 1;
        }));
        slow.start();
        inCompute.await();
        long[] seen = new long[1];
        long chmReadWait = timeMs(() -> seen[0] = chm.get("INR"));
        long sameKeyWriteWait = timeMs(() -> chm.merge("INR", 1L, Long::sum));
        slow.join();
        System.out.println("reader on ConcurrentHashMap during a slow compute (< 100 ms) = "
                + (chmReadWait < 100) + ", saw " + seen[0]);
        System.out.println("writer on the SAME key waits for that slow compute (>= 200 ms) = "
                + (sameKeyWriteWait >= 200) + ", final " + chm.get("INR"));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * batch: 8 threads x 100000 records over 5 keys, expected total 800000, 5 runs each
 * (A)  plain HashMap, get + put      : matched on every run = false   lost updates: yes
 * (B)  synchronized(shared HashMap)  : matched on every run = true    lost updates: no
 * (B') synchronizedMap, get + put    : matched on every run = false   lost updates: yes
 * (C)  ConcurrentHashMap.merge       : matched on every run = true    lost updates: no
 * (C') ConcurrentHashMap + LongAdder : matched on every run = true    lost updates: no
 * reader on synchronizedMap waits for the writer's lock (>= 200 ms) = true
 * reader on ConcurrentHashMap during a slow compute (< 100 ms) = true, saw 0
 * writer on the SAME key waits for that slow compute (>= 200 ms) = true, final 2
 * ============================================================================================== */
