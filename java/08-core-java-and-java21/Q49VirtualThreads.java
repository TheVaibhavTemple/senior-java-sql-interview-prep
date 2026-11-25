/* ==============================================================================================
 * Q49  .  Virtual threads: 10,000 blocking calls, virtual vs a platform pool
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Core Java & Java 21
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- resume par Java 21 aur kaam par virtual threads, to ye sawaal pakka hai
 * TECHNIQUE    : One virtual thread per blocking task (newVirtualThreadPerTaskExecutor); a
 *                Semaphore, not a pool, to cap a downstream
 *
 * PROBLEM
 *   Run 10,000 tasks that each block for 50 ms (a stand-in for an HTTP or JDBC call): first
 *   on a fixed pool of 100 platform threads, then with one virtual thread per task. Explain
 *   the difference, when virtual threads do NOT help, and what to watch in Java 21: pooling,
 *   pinning on synchronized, ThreadLocal, and how to switch them on in Spring Boot.
 *
 * ALGORITHM
 *   1. A platform thread wraps an OS thread. A pool of 100 means at most 100 blocking calls
 *      in flight, so 10,000 x 50 ms needs at least 100 waves = 5 s.
 *   2. A virtual thread is scheduled by the JVM onto a few carrier threads (a ForkJoinPool,
 *      one carrier per core by default). When it blocks on sleep, a socket or a
 *      java.util.concurrent lock, it unmounts and the carrier runs another virtual thread.
 *   3. So create one virtual thread per task: Executors.newVirtualThreadPerTaskExecutor().
 *      Never pool them; a pool only brings the cap back.
 *   4. To protect a downstream that takes only N calls at once, put a Semaphore(N) in front.
 *   5. Watch for pinning (blocking inside synchronized, in Java 21) and heavy ThreadLocal use.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       WALL TIME     MEMORY    VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Fixed pool of 100 platform threads             >= 5 s        low       avoid for this
 *       > Only 100 calls in flight. A bigger pool costs OS memory and context switches.
 *   new platform Thread per task                   --            high      avoid
 *       > 10,000 OS threads: memory, scheduling cost, and OS limits on thread count.
 *   CompletableFuture / reactive (WebFlux)         < 1 s         low       acceptable
 *       > Scales, but callback style; stack traces and debugging get harder.
 *   One virtual thread per task                    < 1 s         low       WRITE THIS
 *       > Plain blocking code that scales like async code. ~0.2 s here once warmed up.
 *   Virtual threads inside a fixed pool            same as pool  low       avoid
 *       > The cap of 100 is back. Section 3 of main.
 *
 * WHY THIS ONE
 *   Virtual threads are the headline Java 21 feature, and the question has moved from "what
 *   are they" to "when do they NOT help". Show the 10,000-task result, then give the four
 *   cautions in one breath: never pool them, no gain for CPU-bound work, pinning inside
 *   synchronized in Java 21, and the ThreadLocal cost when there are millions of threads.
 *
 * SAMJHO
 *   Platform thread = OS thread, mehenga hai, isliye pool mein 100 hi rakhte hain; 100 se zyada
 *   call ek saath wait nahi kar sakti. Virtual thread JVM ka sasta thread hai: block hote hi
 *   carrier chhod deta hai, to 10,000 ek saath wait kar sakte hain. Har task ka naya virtual
 *   thread banao, pool mat karo. CPU wale kaam mein koi fayda nahi.
 *
 * AISE BOLO  (30-40 sec)
 *   "A platform thread is an OS thread, so a pool of a hundred has at most a hundred blocking
 *   calls in flight, and ten thousand calls of fifty milliseconds take at least five seconds.
 *   A virtual thread is a cheap JVM thread that unmounts from its carrier when it blocks, so
 *   with one virtual thread per task the same work finishes in a fraction of a second. I never
 *   pool them; if the database can take only twenty calls, I put a Semaphore in front of it.
 *   They help blocking I/O, not CPU-bound work, and in Java 21 I avoid blocking inside
 *   synchronized on hot paths, because that pins the carrier; ReentrantLock does not."
 *
 * TRAP
 *   ! Virtual threads ko pool mat karo (newFixedThreadPool(100, Thread.ofVirtual().factory())):
 *     100 ki limit wapas aa jaati hai (section 3). Limit chahiye to Semaphore (section 4).
 *   ! Java 21 mein synchronized ke andar blocking call = carrier pinned. Hot path par
 *     ReentrantLock lo. JDK 24 (JEP 491) ne synchronized wali pinning zyaadatar hata di.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why does the pool of 100 platform threads take at least 5 seconds here?
 *     -> 10,000 / 100 = 100 waves, har wave 50 ms block: 100 x 50 ms = 5 s. Threads CPU
 *        nahi khaa rahe, bas wait kar rahe hain.
 *   ? Why not pool virtual threads? How do you limit concurrency then?
 *     -> Virtual thread banana sasta hai, reuse ka koi fayda nahi; pool sirf limit lagata hai.
 *        Downstream ki limit ke liye Semaphore(N), ya DB ke liye connection pool ka size.
 *   ? Do virtual threads make CPU-bound work faster?
 *     -> Nahi. Carrier threads utne hi hain jitne cores. CPU wale kaam ke liye cores ke
 *        barabar fixed pool ya parallel stream.
 *   ? What is pinning and how do you deal with it in Java 21?
 *     -> synchronized ya native call ke andar block hone par virtual thread carrier se chipka
 *        rehta hai. Java 21 mein hot path par ReentrantLock lo; JFR event
 *        jdk.VirtualThreadPinned ya -Djdk.tracePinnedThreads=full se pinning dhundho.
 *   ? What is the problem with ThreadLocal and virtual threads?
 *     -> ThreadLocal chalta hai, par har virtual thread ki apni copy. Lakhon threads = lakhon
 *        copies, aur thread reuse nahi hota to ThreadLocal mein mehngi cheez cache karna
 *        bekaar. ScopedValue Java 21 mein sirf preview hai.
 *   ? How do you turn them on in Spring Boot?
 *     -> Spring Boot 3.2+ aur Java 21 par spring.threads.virtual.enabled=true. Tomcat ke
 *        request threads aur auto-configured task executor virtual ho jaate hain. DB calls
 *        phir bhi Hikari pool size se limited rahengi.
 *   ? Virtual threads or reactive (WebFlux)?
 *     -> Dono I/O wait mein thread free karte hain. Virtual threads mein simple blocking code,
 *        normal stack trace aur debugging. Streaming aur backpressure mein reactive ab bhi
 *        strong hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Thread-per-request vs async / reactive
 *   - Semaphore as a bulkhead (concurrency limiter)
 *   - Carrier threads and the ForkJoinPool scheduler
 *   - ExecutorService in try-with-resources (Java 19+)
 *   - Sizing a thread pool vs a connection pool
 *
 * JAVA 21 MEIN
 *   Virtual threads Java 21 mein final hue (JEP 444); 19 aur 20 mein preview the.
 *   ExecutorService Java 19 se AutoCloseable hai, isliye try-with-resources close() par saare
 *   task ka wait karta hai. Structured concurrency aur ScopedValue Java 21 mein preview hain.
 *
 * APNI STORY SE JODO
 *   "I have used Java 21 virtual threads at work, for [which service and which blocking
 *   calls]."
 *   Note: Pakka fact sirf itna hai ki kaam par virtual threads use kiye. Kahan, kyun aur kya
 *   fark dikha, wo [bhar do: jo sach ho]; koi number mat banao. Pinning ya ThreadLocal ka
 *   issue aaya ho to wahi bolo, warna upar wala design answer do.
 *
 * VARIANTS     : "platform thread vs virtual thread", how many threads for an I/O-bound pool,
 *                Spring Boot with virtual threads
 * ============================================================================================== */

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Q49VirtualThreads {

    static final int TASKS = 10_000;
    static final int SLEEP_MS = 50;              // stands in for a blocking HTTP or JDBC call
    static final int POOL = 100;

    static void blockingCall(int ms) {
        try {
            Thread.sleep(ms);                    // a virtual thread UNMOUNTS here
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    record Result(int done, long millis) { }

    /** Submits n blocking tasks, closes the executor (which waits for all), and times it. */
    static Result run(ExecutorService executor, int n) {
        AtomicInteger done = new AtomicInteger();
        long start = System.nanoTime();
        try (executor) {                         // ExecutorService is AutoCloseable (Java 19+)
            for (int i = 0; i < n; i++) {
                executor.submit(() -> {
                    blockingCall(SLEEP_MS);
                    done.incrementAndGet();
                });
            }
        }                                        // close() = shutdown + wait for every task
        return new Result(done.get(), (System.nanoTime() - start) / 1_000_000);
    }

    public static void main(String[] args) throws Exception {
        // 0. what kind of thread runs the task
        try (var virtualExec = Executors.newVirtualThreadPerTaskExecutor();
             var platformExec = Executors.newFixedThreadPool(1)) {
            System.out.println("isVirtual, newVirtualThreadPerTaskExecutor : "
                    + virtualExec.submit(() -> Thread.currentThread().isVirtual()).get());
            System.out.println("isVirtual, newFixedThreadPool              : "
                    + platformExec.submit(() -> Thread.currentThread().isVirtual()).get());
        }
        Thread named = Thread.ofVirtual().name("transfer-", 0).unstarted(() -> { });
        System.out.println("Thread.ofVirtual().name(...) : " + named.getName()
                + ", daemon = " + named.isDaemon() + "  (virtual threads are always daemon)");

        // Only booleans are printed, never the timings, so the output is the same on every run.
        // The platform pool cannot beat 100 waves x 50 ms = 5 s (sleep never returns early).
        // The warmed-up virtual run takes ~0.1-0.2 s, far inside its 3 s budget. Warm-up first,
        // like any benchmark, so JIT and scheduler start-up are not measured.
        run(Executors.newVirtualThreadPerTaskExecutor(), 1_000);

        // 1. virtual: one new virtual thread per task, no pool
        Result v = run(Executors.newVirtualThreadPerTaskExecutor(), TASKS);
        System.out.println("virtual  : " + v.done() + " tasks done, under 3 s = "
                + (v.millis() < 3_000));

        // 2. platform: a fixed pool of 100 OS threads, so only 100 tasks in flight at a time
        Result p = run(Executors.newFixedThreadPool(POOL), TASKS);
        long floorMs = (long) TASKS / POOL * SLEEP_MS;                 // 100 waves x 50 ms
        System.out.println("platform : " + p.done() + " tasks done, slower than virtual = "
                + (p.millis() > v.millis()));
        System.out.println("platform : needs " + TASKS / POOL + " waves of " + SLEEP_MS
                + " ms, at least " + floorMs * 9 / 10 + " ms = "
                + (p.millis() >= floorMs * 9 / 10));

        // 3. WRONG: virtual threads inside a fixed pool, so the cap of 100 is back
        int pooledTasks = 2_000;
        Result pooled = run(Executors.newFixedThreadPool(POOL, Thread.ofVirtual().factory()),
                pooledTasks);
        long pooledFloorMs = (long) pooledTasks / POOL * SLEEP_MS;     // 20 waves x 50 ms
        System.out.println("pooled virtual : " + pooled.done() + " tasks, still "
                + pooledTasks / POOL + " waves, at least " + pooledFloorMs * 9 / 10 + " ms = "
                + (pooled.millis() >= pooledFloorMs * 9 / 10) + "  <-- no gain");

        // 4. RIGHT way to protect a downstream: a Semaphore in front of it, not a pool
        Semaphore dbPermits = new Semaphore(20);
        AtomicInteger inFlight = new AtomicInteger(), maxInFlight = new AtomicInteger();
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 500; i++) {
                exec.submit(() -> {
                    dbPermits.acquireUninterruptibly();
                    try {
                        maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                        blockingCall(10);
                        inFlight.decrementAndGet();
                    } finally {
                        dbPermits.release();
                    }
                });
            }
        }
        System.out.println("semaphore(20), 500 virtual tasks : max db calls in flight <= 20 = "
                + (maxInFlight.get() <= 20));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * isVirtual, newVirtualThreadPerTaskExecutor : true
 * isVirtual, newFixedThreadPool              : false
 * Thread.ofVirtual().name(...) : transfer-0, daemon = true  (virtual threads are always daemon)
 * virtual  : 10000 tasks done, under 3 s = true
 * platform : 10000 tasks done, slower than virtual = true
 * platform : needs 100 waves of 50 ms, at least 4500 ms = true
 * pooled virtual : 2000 tasks, still 20 waves, at least 900 ms = true  <-- no gain
 * semaphore(20), 500 virtual tasks : max db calls in flight <= 20 = true
 * ============================================================================================== */
