/* ==============================================================================================
 * Q45  .  ThreadPoolExecutor tuning and graceful shutdown
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- ThreadPoolExecutor ke parameters aur task order senior rounds ka standard
 * TECHNIQUE    : Explicit ThreadPoolExecutor with a bounded queue and CallerRunsPolicy backpressure
 *
 * PROBLEM
 *   Size and configure a thread pool for a service. Explain why
 *   Executors.newFixedThreadPool is dangerous, and what you monitor.
 *
 * ALGORITHM
 *   1. Tasks go to core threads first, then the QUEUE, then extra threads up to max, then
 *      rejection.
 *   2. With an unbounded queue step three never happens, so maximumPoolSize is decoration.
 *   3. Executors.newFixedThreadPool uses exactly that unbounded queue - hence the OOM risk.
 *   4. Use a bounded ArrayBlockingQueue so overload becomes backpressure instead of heap
 *      growth.
 *   5. CallerRunsPolicy makes the submitting thread run the task, which throttles the
 *      producer naturally.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Executors.newFixedThreadPool                   --            --        avoid
 *       > Unbounded queue: it grows until OOM, and max pool size is unreachable.
 *   Executors.newCachedThreadPool                  --            --        avoid
 *       > Unbounded thread creation. One spike, thousands of threads.
 *   new ThreadPoolExecutor with a BOUNDED queue    --            --        WRITE THIS
 *       > Backpressure by construction; every parameter is a deliberate choice.
 *   CallerRunsPolicy as the rejection handler      --            --        WRITE THIS
 *       > The caller executes the task, which naturally slows the producer.
 *   Virtual threads (Java 21)                      --            --        acceptable
 *       > Changes the calculus for I/O-bound work. Not a CPU-bound fix.
 *
 * WHY THIS ONE
 *   The execution order is counter-intuitive and is the single most asked detail: core
 *   threads first, then the QUEUE, then extra threads up to max, then rejection. With an
 *   unbounded queue step three never happens, so maximumPoolSize is decoration. After
 *   that: sizing (cores + 1 for CPU-bound, cores x (1 + wait/service) for I/O-bound), and
 *   the fact that submit() buries exceptions inside the Future while execute() surfaces
 *   them.
 *
 * SAMJHO
 *   Naya task pehle core threads ko, phir queue mein, queue full ho tab max tak naye threads,
 *   phir reject. Unbounded queue kabhi full nahi hoti, to max kabhi nahi pahunchta aur heap
 *   bharta rehta hai. Bounded queue + CallerRunsPolicy se overload backpressure ban jata hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "Executors.newFixedThreadPool hides an unbounded LinkedBlockingQueue, so under overload it
 *   queues until the heap runs out, and newCachedThreadPool can create thousands of threads.
 *   I build the ThreadPoolExecutor explicitly: named threads, a bounded ArrayBlockingQueue,
 *   and CallerRunsPolicy so the submitter slows down when the pool is saturated. I start at
 *   cores plus one for CPU-bound work and cores times one plus wait over compute for I/O,
 *   then tune from queue depth and p99 latency. The catch is that CallerRuns executes on the
 *   caller, so the caller must never be an event-loop thread."
 *
 * TRAP
 *   ! "maxPoolSize 50 hai to load par 50 threads banenge" - galat. Core se upar thread tabhi
 *     banta hai jab queue full ho.
 *   ! Pool ka task usi pool mein child task submit karke get() pe ruke, to saare threads wait
 *     mein aur child queue mein: thread starvation deadlock.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why is maximumPoolSize never reached with an unbounded queue?
 *     -> Extra thread tabhi banta hai jab queue ka offer() false de. Unbounded queue kabhi
 *        false nahi deti, to pool core size par hi ruka rehta hai.
 *   ? Take the four rejection policies apart -- when is each right?
 *     -> Abort (default): exception, caller ko pata chale. CallerRuns: backpressure. Discard:
 *        chupchap drop, sirf lossy metrics ke liye. DiscardOldest: sabse purana drop, jab
 *        sirf latest value maayne rakhe.
 *   ? Size a pool for 8 cores, 100 ms of I/O wait and 10 ms of CPU per task.
 *     -> 8 x (1 + 100/10) = 88 threads. Phir downstream dekho: DB connection pool chhota hai
 *        to 88 threads bhi wahin line mein khade honge.
 *   ? A task in the pool submits another task to the same pool and waits for it -- what
 *     happens?
 *     -> Saare threads parent tasks mein wait karein to child queue mein hi pada rahega:
 *        thread starvation deadlock. Alag pool lo, ya CompletableFuture se bina block kiye jodo.
 *   ? What do you alert on: CPU, queue depth, or latency?
 *     -> Queue depth, p99 latency aur rejection count. I/O-bound pool mein CPU kam dikhta hai
 *        jab service already doob rahi hoti hai.
 *   ? How do virtual threads change this configuration?
 *     -> I/O-bound kaam mein pool sizing khatam: har task ka naya virtual thread, limit ke
 *        liye Semaphore. CPU-bound kaam ke liye sized platform pool hi rahega.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Executor sizing
 *   - Rejection policies and backpressure
 *   - Graceful shutdown
 *   - Thread pool starvation
 *   - Virtual threads vs pools
 *
 * JAVA 21 MEIN
 *   - Executors.newVirtualThreadPerTaskExecutor(): virtual threads ko pool mat karo, wo saste
 *     hain. DB jaisi limit ke liye Semaphore lagao (Q49).
 *   - Java 21 mein synchronized ke andar blocking call carrier thread ko pin karta hai; hot
 *     path par ReentrantLock lo. JDK 24 mein ye pinning hata di gayi.
 *
 * APNI STORY SE JODO
 *   "On Java 21 I have used virtual threads at work for [bhar do: kaunsa I/O-bound kaam]; for
 *   CPU-bound work I would still keep a sized platform pool."
 *   Note: Koi throughput ya latency number nahi hai, mat banao. Detail ke liye Q49 dekho.
 *
 * VARIANTS     : classic - size an I/O vs CPU pool, custom RejectedExecutionHandler with a
 *                metric, thread starvation deadlock
 * ============================================================================================== */

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Q45ThreadPoolTuning {

    /* ---- named threads, or your thread dump is useless ---- */
    static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger n = new AtomicInteger(1);
        NamedThreadFactory(String prefix) { this.prefix = prefix; }
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + n.getAndIncrement());
            t.setUncaughtExceptionHandler((th, e) ->
                    System.out.println("   uncaught in " + th.getName() + ": " + e.getMessage()));
            return t;
        }
    }

    static void metrics(String label, ThreadPoolExecutor p) {
        System.out.printf("%-17s: active=%d queued=%d completed=%d poolSize=%d largest=%d%n",
                label, p.getActiveCount(), p.getQueue().size(),
                p.getCompletedTaskCount(), p.getPoolSize(), p.getLargestPoolSize());
    }

    public static void main(String[] args) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("cores            : " + cores);
        System.out.println("CPU-bound size   : " + (cores + 1));
        System.out.println("I/O-bound size   : " + (cores * (1 + 100 / 10))
                + "   (cores * (1 + wait/service), 100ms wait / 10ms CPU)");

        /* ======================================================================
           THE ORDER THAT SURPRISES PEOPLE:
             core threads -> QUEUE -> extra threads up to max -> rejection
           With an UNBOUNDED queue, step three never happens, so
           maximumPoolSize is pure decoration. That is the newFixedThreadPool trap.
           ====================================================================== */
        ThreadPoolExecutor unbounded = new ThreadPoolExecutor(
                2, 8, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("unbounded"));
        CountDownLatch hold = new CountDownLatch(1);
        for (int i = 0; i < 50; i++)
            unbounded.execute(() -> { try { hold.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        Thread.sleep(200);
        metrics("UNBOUNDED queue", unbounded);
        System.out.println("                   max was 8 but poolSize stayed at core=2; "
                + "the queue absorbed everything");
        hold.countDown();
        unbounded.shutdownNow();

        /* ---- BOUNDED queue: overload becomes backpressure, not heap growth ---- */
        AtomicInteger ranOnCaller = new AtomicInteger();
        final Thread mainThread = Thread.currentThread();
        ThreadPoolExecutor bounded = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4),
                new NamedThreadFactory("order-worker"),
                new ThreadPoolExecutor.CallerRunsPolicy());

        CountDownLatch gate = new CountDownLatch(1);
        for (int i = 0; i < 20; i++) {
            bounded.execute(() -> {
                if (Thread.currentThread() == mainThread) ranOnCaller.incrementAndGet();
                try { gate.await(300, TimeUnit.MILLISECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        metrics("BOUNDED queue", bounded);
        System.out.println("CallerRuns       : " + ranOnCaller.get()
                + " task(s) executed on the SUBMITTING thread -> the producer is throttled");
        gate.countDown();
        bounded.shutdown();
        bounded.awaitTermination(10, TimeUnit.SECONDS);
        metrics("after drain", bounded);

        /* ---- AbortPolicy: the default, and what rejection actually looks like ---- */
        ThreadPoolExecutor strict = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1),
                new NamedThreadFactory("strict"), new ThreadPoolExecutor.AbortPolicy());
        CountDownLatch block = new CountDownLatch(1);
        strict.execute(() -> { try { block.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        strict.execute(() -> { });                       // fills the queue
        try {
            strict.execute(() -> { });                   // nowhere left to go
        } catch (RejectedExecutionException e) {
            System.out.println("AbortPolicy      : RejectedExecutionException as designed");
        }
        block.countDown();
        strict.shutdownNow();

        /* ---- submit() BURIES the exception; execute() surfaces it ---- */
        ThreadPoolExecutor p = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new NamedThreadFactory("exc"));
        Future<?> f = p.submit(() -> { throw new IllegalStateException("task blew up"); });
        try { f.get(); }
        catch (ExecutionException e) { System.out.println("submit()         : silent until Future.get() -> " + e.getCause()); }
        p.execute(() -> { throw new IllegalStateException("surfaced immediately"); });
        Thread.sleep(200);
        p.shutdownNow();

        System.out.println("ALERT ON         : queue depth and p99 latency, not CPU");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured on OpenJDK 21)
 * NON-DETERMINISTIC: queue depth and completed counts depend on scheduling --
 *                    the shape is stable, the exact numbers are not.
 * ----------------------------------------------------------------------------------------------
 * cores            : 2
 * CPU-bound size   : 3
 * I/O-bound size   : 22   (cores * (1 + wait/service), 100ms wait / 10ms CPU)
 * UNBOUNDED queue  : active=2 queued=48 completed=0 poolSize=2 largest=2
 *                    max was 8 but poolSize stayed at core=2; the queue absorbed everything
 * BOUNDED queue    : active=4 queued=1 completed=12 poolSize=4 largest=4
 * CallerRuns       : 3 task(s) executed on the SUBMITTING thread -> the producer is throttled
 * after drain      : active=0 queued=0 completed=17 poolSize=0 largest=4
 * AbortPolicy      : RejectedExecutionException as designed
 * submit()         : silent until Future.get() -> java.lang.IllegalStateException: task blew up
 *    uncaught in exc-1: surfaced immediately
 * ALERT ON         : queue depth and p99 latency, not CPU
 * ============================================================================================== */
