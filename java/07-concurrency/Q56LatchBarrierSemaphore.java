/* ==============================================================================================
 * Q56  .  CountDownLatch, CyclicBarrier, Semaphore -- and ReentrantLock vs synchronized
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- "latch vs barrier vs semaphore" lagbhag har concurrency round mein
 * TECHNIQUE    : CountDownLatch to wait for N, CyclicBarrier to meet in rounds, Semaphore for at
 *                most K at once; ReentrantLock when synchronized is not enough
 *
 * PROBLEM
 *   Show the three classic synchronizers with one small demo each: main waits for N workers
 *   (CountDownLatch), N threads meet at the end of every round (CyclicBarrier), and at most K
 *   threads use a resource at once (Semaphore). Then compare ReentrantLock with synchronized,
 *   and say when a ReadWriteLock helps.
 *
 * ALGORITHM
 *   1. CountDownLatch(N): every worker calls countDown() in a finally block, main calls
 *      await(). One-shot: once the count is 0 it stays 0.
 *   2. CyclicBarrier(N, action): every party calls await() at the end of a round. The last
 *      one to arrive runs the action, then all N continue. The same barrier serves the next
 *      round.
 *   3. If a party is interrupted or times out, the barrier breaks and every party still
 *      waiting gets BrokenBarrierException instead of hanging.
 *   4. Semaphore(K): acquire() BEFORE the try, release() in finally. At most K inside.
 *   5. ReentrantLock: lock() before the try, unlock() in finally. It adds tryLock(timeout),
 *      lockInterruptibly() and fairness, which synchronized does not have.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Thread.sleep and hope the workers are done     --            --        avoid
 *       > Too short on a slow day, wasted time on a fast one.
 *   join() on every thread                         --            --        acceptable
 *       > Only for threads you started. Pool threads never end, so join() cannot help.
 *   A shared counter with wait / notifyAll         --            --        avoid
 *       > Correct only with a while loop and notifyAll. The JDK has already written it.
 *   CountDownLatch / CyclicBarrier / Semaphore     --            --        WRITE THIS
 *       > One class per coordination shape. Latch and Semaphore sit on AQS, like ReentrantLock.
 *   invokeAll / CompletableFuture.allOf            --            --        acceptable
 *       > Simpler than a latch when the N tasks return values (Q35, Q41).
 *
 * COMPARISON  (one line per tool)
 *   TOOL            WHO WAITS, FOR WHAT                      REUSABLE  REACH FOR IT WHEN
 *   ----------------------------------------------------------------------------------------
 *   CountDownLatch  waiters, until N countDown() calls       no        main waits for N workers
 *   CyclicBarrier   the N parties, for each other            yes       N threads work in rounds
 *   Semaphore       any thread, for one of K permits         yes       at most K at once
 *   ReentrantLock   one thread; only the owner unlocks       yes       mutex with tryLock/timeout
 *   ReadWriteLock   readers share, a writer is alone         yes       many long reads, few writes
 *   Phaser          registered parties, phase by phase       yes       barrier, parties come and go
 *   Thread.join()   the caller, until one thread ends        --        a thread you started
 *
 * WHY THIS ONE
 *   "CountDownLatch vs CyclicBarrier" checks whether you know the shape of each problem, not
 *   the method names. Latch: one side waits for N events, once. Barrier: N threads wait for
 *   each other, round after round. Semaphore: at most K at a time, and a permit belongs to
 *   nobody. Each demo below prints the same result on every run, and two of them show the
 *   production traps: countDown outside finally, and release after a failed acquire.
 *
 * SAMJHO
 *   Latch: ek ya zyada thread N kaam khatam hone ka wait karte hain; count 0 hua to bas, dobara
 *   use nahi. Barrier: N thread ek doosre ka wait karte hain, har round mein, reusable.
 *   Semaphore: K permits, ek waqt par sirf K andar. Lock: ek waqt par ek; ReentrantLock mein
 *   tryLock aur interrupt ka option bhi.
 *
 * AISE BOLO  (30-40 sec)
 *   "For 'main waits until N workers finish' I use a CountDownLatch, with countDown in a
 *   finally block so a failing worker cannot leave main waiting forever; it is one-shot. When
 *   N threads must meet at the end of every round I use a CyclicBarrier, which is reusable,
 *   runs a barrier action once per round, and breaks with BrokenBarrierException if one party
 *   times out or is interrupted. To cap concurrent calls to something slow at K, I use a
 *   Semaphore, with acquire before the try and release in finally. I move from synchronized
 *   to ReentrantLock only when I need tryLock with a timeout, an interruptible wait or
 *   fairness."
 *
 * TRAP
 *   ! countDown() finally mein nahi rakha to ek worker ka exception main ko hamesha ke liye
 *     await() mein atka deta hai. Demo 1: count 1 par atka, sirf timed await se pakda.
 *   ! acquire() try ke andar aur release() finally mein: acquire fail hua to bhi release chala
 *     aur permits badh gaye (demo 3: 1 se 2). acquire hamesha try se pehle.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? CountDownLatch vs CyclicBarrier vs join()?
 *     -> join: ek thread ke khatam hone ka wait, sirf apne start kiye threads par; pool ke
 *        threads khatam nahi hote. Latch: N events ka wait, countDown koi bhi thread kare,
 *        one-shot. Barrier: N threads ek doosre ka wait karte hain, har round ke baad phir
 *        se taiyaar, barrier action ke saath.
 *   ? What is a Phaser, in one line?
 *     -> Reusable barrier jismein parties beech mein register() aur arriveAndDeregister() kar
 *        sakti hain, aur har phase ka ek number hota hai.
 *   ? Is a semaphore permit tied to the thread that acquired it?
 *     -> Nahi. Koi bhi thread release kar sakta hai, bina acquire kiye bhi (demo 3:
 *        Semaphore(0) ko doosre thread ne release kiya). Lock owner se bandha hai: doosra
 *        thread unlock kare to IllegalMonitorStateException.
 *   ? One party at a CyclicBarrier times out or is interrupted. What happens to the rest?
 *     -> Barrier broken ho jaata hai. Timeout wale ko TimeoutException, interrupted ko
 *        InterruptedException, baaki waiting parties ko BrokenBarrierException (demo 2).
 *        Dobara use ke liye reset(), ya naya barrier banao.
 *   ? ReentrantLock or synchronized?
 *     -> Default synchronized: chhota, aur unlock bhoolne ka darr nahi. ReentrantLock tab jab
 *        tryLock(timeout), lockInterruptibly, fairness ya kai Conditions chahiye. synchronized
 *        par wait karta thread interrupt se nahi nikalta (demo 4: BLOCKED hi raha).
 *   ? When does a ReadWriteLock help?
 *     -> Reads bahut, writes kam, aur reads lambe hon: tab kai readers ek saath chalte hain.
 *        Bahut chhote reads par iska overhead simple lock se zyada pad sakta hai (javadoc).
 *        Read se write upgrade nahi hota, downgrade hota hai (demo 5).
 *   ? Do these primitives work with virtual threads?
 *     -> Haan. Latch await, semaphore acquire, barrier aur ReentrantLock par block hone wala
 *        virtual thread carrier ko chhod deta hai. Java 21 mein synchronized ke andar block
 *        karna carrier ko pin karta hai (JDK 24 mein theek hua). Virtual threads ka pool nahi
 *        banate, isliye limit Semaphore se lagao: demo 3 virtual threads par chalta hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Start gate: CountDownLatch(1) so all threads begin together
 *   - Wait for N partitions of a batch before the merge step
 *   - Bulkhead: cap calls to one slow downstream with a Semaphore
 *   - Resource pool with K connections or licences
 *   - Timed and interruptible locking to avoid deadlock (Q44)
 *
 * JAVA 21 MEIN
 *   Demo 3 mein try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) hai:
 *   har task ka apna virtual thread, aur close() saare tasks ka wait karta hai (ExecutorService
 *   Java 19 se AutoCloseable hai). Yahan limit pool size se nahi, Semaphore(3) se aati hai.
 *
 * APNI STORY SE JODO
 *   "I have used Java 21 virtual threads at work [where]. With virtual threads the pool size
 *   no longer limits concurrency, so when a downstream can take only K calls at once, a
 *   Semaphore is how I would cap it."
 *   Note: Semaphore kaam par sach mein use kiya ya nahi, wo [bhar do]; nahi kiya to ise design
 *   answer ki tarah bolo, apna experience mat batao.
 *
 * VARIANTS     : LC 1114, LC 1117, classic - start gate, bulkhead with a Semaphore
 * ============================================================================================== */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class Q56LatchBarrierSemaphore {

    /* run one task on a fresh platform thread and wait for its result */
    static <T> T onOtherThread(Callable<T> task) throws Exception {
        FutureTask<T> f = new FutureTask<>(task);
        new Thread(f).start();
        return f.get();
    }

    /* ======================================================================
       1. CountDownLatch : main waits for N workers. One-shot.
       ====================================================================== */
    static void latch() throws InterruptedException {
        int[][] files = {{100, 250}, {400}, {50, 50, 75}};     // amounts in three batch files
        long[] totals = new long[files.length];
        CountDownLatch done = new CountDownLatch(files.length);
        for (int i = 0; i < files.length; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    totals[id] = Arrays.stream(files[id]).sum();
                } finally {
                    done.countDown();                   // in finally: a failure still counts
                }
            }).start();
        }
        done.await();                                   // countDown happens-before this return
        System.out.println("3 workers done        : total = " + Arrays.stream(totals).sum()
                + ", count = " + done.getCount());
        done.countDown();                               // no effect at 0
        System.out.println("one-shot              : extra countDown -> count " + done.getCount()
                + ", await returns at once = " + done.await(0, TimeUnit.MILLISECONDS));

        /* ---- THE WRONG VERSION : countDown outside finally ---- */
        CountDownLatch broken = new CountDownLatch(3);
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    if (id == 1) throw new IllegalStateException("bad file");
                    broken.countDown();                 // WRONG: skipped when the work throws
                } catch (IllegalStateException e) {
                    // logged and swallowed -- and the latch never hears about it
                }
            });
            workers.add(t);
            t.start();
        }
        boolean finished = broken.await(300, TimeUnit.MILLISECONDS);
        for (Thread t : workers) t.join();
        System.out.println("WRONG, not in finally : await(300 ms) = " + finished
                + ", count stuck at " + broken.getCount() + "   <-- await() hangs forever");
    }

    /* ======================================================================
       2. CyclicBarrier : N threads meet at the end of every round. Reusable.
       ====================================================================== */
    interface Await { int run() throws Exception; }

    static String outcome(Await a) {
        try { a.run(); return "passed"; }
        catch (Exception e) { return e.getClass().getSimpleName(); }
    }

    static void barrier() throws Exception {
        final int PARTIES = 3, ROUNDS = 3;
        int[] partial = new int[PARTIES];
        int[] round = {0};
        List<String> log = new ArrayList<>();           // written only by the barrier action
        CyclicBarrier barrier = new CyclicBarrier(PARTIES, () -> {
            round[0]++;                                 // runs once, in the LAST thread to arrive
            log.add("round " + round[0] + "               : all " + PARTIES + " arrived, partials "
                    + Arrays.toString(partial) + " -> " + Arrays.stream(partial).sum());
        });
        Thread[] ts = new Thread[PARTIES];
        for (int p = 0; p < PARTIES; p++) {
            final int id = p;
            ts[p] = new Thread(() -> {
                try {
                    for (int r = 1; r <= ROUNDS; r++) {
                        partial[id] = (id + 1) * 10 * r;    // this round's work
                        barrier.await();                    // wait for the other two
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }
            });
            ts[p].start();
        }
        for (Thread t : ts) t.join();
        log.forEach(System.out::println);
        System.out.println("same barrier, 3 rounds: isBroken = " + barrier.isBroken()
                + ", waiting = " + barrier.getNumberWaiting());

        /* ---- one party gives up : the barrier breaks for everybody ---- */
        CyclicBarrier b = new CyclicBarrier(3);         // 3 parties, but only 2 will come
        FutureTask<String> patient = new FutureTask<>(() -> outcome(b::await));
        FutureTask<String> impatient =
                new FutureTask<>(() -> outcome(() -> b.await(200, TimeUnit.MILLISECONDS)));
        new Thread(patient).start();
        while (b.getNumberWaiting() < 1) Thread.sleep(1);   // patient is waiting first
        new Thread(impatient).start();
        System.out.println("party that timed out  : " + impatient.get());
        System.out.println("party still waiting   : " + patient.get());
        System.out.print("broken barrier        : isBroken = " + b.isBroken());
        b.reset();
        System.out.println(", after reset() = " + b.isBroken());
    }

    /* ======================================================================
       3. Semaphore : at most K inside at once. A permit belongs to nobody.
       ====================================================================== */
    static int maxInside(Semaphore permits, int tasks) throws InterruptedException {
        AtomicInteger inside = new AtomicInteger(), max = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                ex.submit(() -> {
                    startGate.await();                  // all tasks start together
                    if (permits != null) permits.acquire();     // BEFORE the try
                    try {
                        max.accumulateAndGet(inside.incrementAndGet(), Math::max);
                        Thread.sleep(200);              // the slow downstream call
                        inside.decrementAndGet();
                    } finally {
                        if (permits != null) permits.release();
                    }
                    return null;
                });
            }
            startGate.countDown();
        }                                               // close() waits for every task
        return max.get();
    }

    /* THE WRONG PATTERN : acquire inside the try, so a failed acquire still releases */
    static void wrongPattern(Semaphore s) throws InterruptedException {
        try {
            s.acquire();
            // ... use the resource ...
        } finally {
            s.release();                                // WRONG: gives back a permit it never got
        }
    }

    static void rightPattern(Semaphore s) throws InterruptedException {
        s.acquire();                                    // acquire BEFORE the try
        try {
            // ... use the resource ...
        } finally {
            s.release();
        }
    }

    /* the interrupt arrives before acquire(), so acquire() throws at once */
    static int permitsAfterFailedAcquire(boolean wrong) throws InterruptedException {
        Semaphore s = new Semaphore(1);
        Thread t = new Thread(() -> {
            Thread.currentThread().interrupt();
            try {
                if (wrong) wrongPattern(s); else rightPattern(s);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();     // restore the flag, as real code should
            }
        });
        t.start();
        t.join();
        return s.availablePermits();
    }

    static void semaphore() throws Exception {
        System.out.println("Semaphore(3), 10 tasks: max inside at once = "
                + maxInside(new Semaphore(3), 10));
        System.out.println("no semaphore          : more than 3 at once = "
                + (maxInside(null, 10) > 3) + "   <-- nothing limits them");

        Semaphore signal = new Semaphore(0);
        Thread other = new Thread(signal::release);
        other.start();
        signal.acquire();                               // released by ANOTHER thread
        other.join();
        System.out.println("Semaphore(0)          : main acquired a permit released by another"
                + " thread");

        ReentrantLock owned = new ReentrantLock();
        owned.lock();
        try {
            String r = onOtherThread(() -> {
                try { owned.unlock(); return "unlocked"; }
                catch (IllegalMonitorStateException e) { return "IllegalMonitorStateException"; }
            });
            System.out.println("lock, non-owner unlock: " + r + "   (a lock has an owner)");
        } finally {
            owned.unlock();
        }

        System.out.println("WRONG acquire in try  : permits 1 -> "
                + permitsAfterFailedAcquire(true) + " after a failed acquire   <-- permit leak");
        System.out.println("RIGHT acquire first   : permits 1 -> "
                + permitsAfterFailedAcquire(false));
    }

    /* ======================================================================
       4. ReentrantLock vs synchronized
       ====================================================================== */
    static boolean tryBriefly(Lock lock) throws InterruptedException {
        if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) return false;
        try { return true; } finally { lock.unlock(); }
    }

    static void locks() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        boolean whileHeld;
        lock.lock();
        try {
            whileHeld = onOtherThread(() -> tryBriefly(lock));
        } finally {
            lock.unlock();                              // ALWAYS in finally
        }
        System.out.println("tryLock(100 ms), held : " + whileHeld);
        System.out.println("tryLock(100 ms), free : " + onOtherThread(() -> tryBriefly(lock)));

        AtomicReference<String> waited = new AtomicReference<>();
        lock.lock();
        try {
            Thread waiter = new Thread(() -> {
                try {
                    lock.lockInterruptibly();
                    try { waited.set("got the lock"); } finally { lock.unlock(); }
                } catch (InterruptedException e) {
                    waited.set("InterruptedException, stopped waiting");
                }
            });
            waiter.start();
            while (!lock.hasQueuedThread(waiter)) Thread.sleep(1);
            waiter.interrupt();
            waiter.join();
        } finally {
            lock.unlock();
        }
        System.out.println("lockInterruptibly     : " + waited.get());

        Object monitor = new Object();
        AtomicBoolean flagInside = new AtomicBoolean();
        Thread blocked = new Thread(() -> {
            synchronized (monitor) { flagInside.set(Thread.currentThread().isInterrupted()); }
        });
        Thread.State afterInterrupt;
        synchronized (monitor) {
            blocked.start();
            while (blocked.getState() != Thread.State.BLOCKED) Thread.sleep(1);
            blocked.interrupt();
            Thread.sleep(100);
            afterInterrupt = blocked.getState();
        }
        blocked.join();
        System.out.println("synchronized+interrupt: still " + afterInterrupt
                + ", entered later with the flag set = " + flagInside.get());

        lock.lock();
        lock.lock();
        System.out.println("lock() twice          : hold count = " + lock.getHoldCount()
                + "   (reentrant: unlock twice)");
        lock.unlock();
        lock.unlock();
        System.out.println("ReentrantLock(true)   : isFair = " + new ReentrantLock(true).isFair()
                + "   (FIFO waiters, lower throughput)");
    }

    /* ======================================================================
       5. ReadWriteLock : readers share, a writer is alone
       ====================================================================== */
    static void readWrite() throws Exception {
        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        rw.readLock().lock();                           // main is reading
        try {
            System.out.println("second reader         : "
                    + onOtherThread(() -> tryBriefly(rw.readLock())));
            System.out.println("writer                : "
                    + onOtherThread(() -> tryBriefly(rw.writeLock())));
            boolean upgraded = rw.writeLock().tryLock(); // same thread already holds the read lock
            if (upgraded) rw.writeLock().unlock();
            System.out.println("upgrade read -> write : " + upgraded
                    + "   <-- writeLock().lock() here would wait forever");
        } finally {
            rw.readLock().unlock();
        }

        rw.writeLock().lock();
        try {
            rw.readLock().lock();                       // allowed while holding the write lock
        } finally {
            rw.writeLock().unlock();                    // now only the read lock is held
        }
        try {
            System.out.println("downgrade write->read : read holds = " + rw.getReadHoldCount()
                    + ", write locked = " + rw.isWriteLocked());
        } finally {
            rw.readLock().unlock();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== 1. CountDownLatch");
        latch();
        System.out.println("== 2. CyclicBarrier");
        barrier();
        System.out.println("== 3. Semaphore (virtual threads)");
        semaphore();
        System.out.println("== 4. ReentrantLock vs synchronized");
        locks();
        System.out.println("== 5. ReadWriteLock");
        readWrite();
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * == 1. CountDownLatch
 * 3 workers done        : total = 925, count = 0
 * one-shot              : extra countDown -> count 0, await returns at once = true
 * WRONG, not in finally : await(300 ms) = false, count stuck at 1   <-- await() hangs forever
 * == 2. CyclicBarrier
 * round 1               : all 3 arrived, partials [10, 20, 30] -> 60
 * round 2               : all 3 arrived, partials [20, 40, 60] -> 120
 * round 3               : all 3 arrived, partials [30, 60, 90] -> 180
 * same barrier, 3 rounds: isBroken = false, waiting = 0
 * party that timed out  : TimeoutException
 * party still waiting   : BrokenBarrierException
 * broken barrier        : isBroken = true, after reset() = false
 * == 3. Semaphore (virtual threads)
 * Semaphore(3), 10 tasks: max inside at once = 3
 * no semaphore          : more than 3 at once = true   <-- nothing limits them
 * Semaphore(0)          : main acquired a permit released by another thread
 * lock, non-owner unlock: IllegalMonitorStateException   (a lock has an owner)
 * WRONG acquire in try  : permits 1 -> 2 after a failed acquire   <-- permit leak
 * RIGHT acquire first   : permits 1 -> 1
 * == 4. ReentrantLock vs synchronized
 * tryLock(100 ms), held : false
 * tryLock(100 ms), free : true
 * lockInterruptibly     : InterruptedException, stopped waiting
 * synchronized+interrupt: still BLOCKED, entered later with the flag set = true
 * lock() twice          : hold count = 2   (reentrant: unlock twice)
 * ReentrantLock(true)   : isFair = true   (FIFO waiters, lower throughput)
 * == 5. ReadWriteLock
 * second reader         : true
 * writer                : false
 * upgrade read -> write : false   <-- writeLock().lock() here would wait forever
 * downgrade write->read : read holds = 1, write locked = false
 * ============================================================================================== */
