/* ==============================================================================================
 * Q44  .  Deadlock: create it, fix it, detect it
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Global lock ordering; tryLock with timeout and jittered backoff; ThreadMXBean detection
 *
 * PROBLEM
 *   Write code that deadlocks. Then fix it two different ways. Then show how you would
 *   find it in production.
 *
 * ALGORITHM
 *   1. Deadlock needs four conditions: mutual exclusion, hold-and-wait, no preemption,
 *      circular wait.
 *   2. Circular wait is the practical one to break - impose a total order on lock
 *      acquisition.
 *   3. Order by identity hash code, with a tiebreaker lock for the rare equal-hash case.
 *   4. Alternative: tryLock with a timeout, release everything on failure, back off with
 *      random jitter.
 *   5. Detect at runtime with ThreadMXBean.findDeadlockedThreads, or offline with jcmd
 *      Thread.print.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Hope it does not happen                        --            --        avoid
 *   One coarse global lock                         --            --        acceptable
 *       > Deadlock-free by construction; throughput collapses.
 *   Global lock ordering                           --            --        WRITE THIS
 *       > Removes circular wait -- the condition that is actually breakable.
 *   tryLock with timeout + jittered backoff        --            --        WRITE THIS
 *       > Breaks hold-and-wait. Needs random backoff or you get livelock.
 *   Lock-free / immutable design                   --            --        WRITE THIS
 *       > No locks, no deadlock. The real senior answer where it applies.
 *
 * WHY THIS ONE
 *   Name the four Coffman conditions and then say which one you are breaking -- that
 *   framing distinguishes a rehearsed answer from an understood one. Circular wait is the
 *   practical target, via a total order on locks. The tiebreaker object for equal identity
 *   hash codes is the detail almost nobody includes and interviewers love. For detection:
 *   jstack or jcmd Thread.print prints "Found one Java-level deadlock" directly, and
 *   ThreadMXBean.findDeadlockedThreads does it from inside the JVM.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Name the four conditions and say which one each fix removes.
 *   ? Why is the tiebreaker lock needed at all?
 *   ? Without jitter, tryLock-and-retry can livelock -- explain how.
 *   ? Read a thread dump: what exactly tells you it is a deadlock?
 *   ? Can a deadlock happen between database transactions? How is that different?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Dining philosophers
 *   - Livelock and starvation
 *   - Thread dump analysis
 *   - Lock ordering
 *   - tryLock patterns
 *   - Account transfer problem
 * ============================================================================================== */

import java.lang.management.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Q44DeadlockCreateFixDetect {

    static final class Account {
        final String id; BigDecimal balance;
        final ReentrantLock lock = new ReentrantLock();
        Account(String id, long balance) { this.id = id; this.balance = BigDecimal.valueOf(balance); }
    }

    static void doTransfer(Account from, Account to, BigDecimal amt) {
        from.balance = from.balance.subtract(amt);
        to.balance   = to.balance.add(amt);
    }

    /* ======================================================================
       1. HOW TO CREATE ONE - two locks taken in opposite orders.
          The threads are DAEMONS, so a stuck pair cannot keep the JVM alive.
       ====================================================================== */
    static final Object LOCK_A = new Object(), LOCK_B = new Object();

    static void createDeadlock() {
        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {
                sleep(100);
                synchronized (LOCK_B) { System.out.println("t1 finished"); }
            }
        }, "deadlock-t1");
        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {                 // OPPOSITE ORDER -> circular wait
                sleep(100);
                synchronized (LOCK_A) { System.out.println("t2 finished"); }
            }
        }, "deadlock-t2");
        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
    }

    /* ======================================================================
       2. FIX A - GLOBAL LOCK ORDERING. Breaks the circular-wait condition.
          The tiebreaker handles the rare equal-identity-hash case.
       ====================================================================== */
    static final Object TIE_BREAK = new Object();

    static void transferOrdered(Account from, Account to, BigDecimal amount) {
        int c = Integer.compare(System.identityHashCode(from), System.identityHashCode(to));
        if (c < 0)       { synchronized (from) { synchronized (to)   { doTransfer(from, to, amount); } } }
        else if (c > 0)  { synchronized (to)   { synchronized (from) { doTransfer(from, to, amount); } } }
        else             { synchronized (TIE_BREAK) {
                             synchronized (from) { synchronized (to) { doTransfer(from, to, amount); } } } }
    }

    /* ======================================================================
       3. FIX B - tryLock with a timeout. Breaks hold-and-wait.
          The random backoff is what stops this degenerating into livelock.
       ====================================================================== */
    static boolean transferWithTimeout(Account from, Account to, BigDecimal amt, int attempts)
            throws InterruptedException {
        for (int i = 0; i < attempts; i++) {
            if (from.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try { doTransfer(from, to, amt); return true; }
                        finally { to.lock.unlock(); }
                    }
                } finally { from.lock.unlock(); }
            }
            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 25));   // JITTER
        }
        return false;
    }

    /* ======================================================================
       4. DETECTION from inside the JVM.
          Offline equivalent: jcmd <pid> Thread.print  /  jstack <pid>
       ====================================================================== */
    static String detectDeadlocks() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = bean.findDeadlockedThreads();
        if (ids == null) return "none";
        StringBuilder sb = new StringBuilder();
        for (ThreadInfo info : bean.getThreadInfo(ids, false, false)) {
            sb.append(info.getThreadName())
              .append(" BLOCKED on ").append(info.getLockName())
              .append(" held by ").append(info.getLockOwnerName()).append("; ");
        }
        return sb.toString().trim();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("COFFMAN conditions: mutual exclusion, hold-and-wait, "
                + "no preemption, circular wait");
        System.out.println("   lock ordering breaks CIRCULAR WAIT; tryLock breaks HOLD-AND-WAIT");

        createDeadlock();
        Thread.sleep(600);
        System.out.println("1 detected       : " + detectDeadlocks());

        /* FIX A under contention: both directions at once, 2000 transfers each way */
        Account x = new Account("X", 1_000_000), y = new Account("Y", 1_000_000);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch done = new CountDownLatch(2);
        pool.execute(() -> { for (int i = 0; i < 2000; i++) transferOrdered(x, y, BigDecimal.ONE); done.countDown(); });
        pool.execute(() -> { for (int i = 0; i < 2000; i++) transferOrdered(y, x, BigDecimal.ONE); done.countDown(); });
        boolean finished = done.await(10, TimeUnit.SECONDS);
        System.out.println("2 lock ordering  : completed=" + finished
                + "  X=" + x.balance + " Y=" + y.balance
                + "  total preserved=" + x.balance.add(y.balance).equals(BigDecimal.valueOf(2_000_000)));

        /* FIX B */
        Account p = new Account("P", 500), q = new Account("Q", 500);
        CountDownLatch done2 = new CountDownLatch(2);
        pool.execute(() -> { try { for (int i = 0; i < 200; i++) transferWithTimeout(p, q, BigDecimal.ONE, 20); }
                             catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                             finally { done2.countDown(); } });
        pool.execute(() -> { try { for (int i = 0; i < 200; i++) transferWithTimeout(q, p, BigDecimal.ONE, 20); }
                             catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                             finally { done2.countDown(); } });
        boolean finished2 = done2.await(20, TimeUnit.SECONDS);
        System.out.println("3 tryLock+jitter : completed=" + finished2
                + "  total preserved=" + p.balance.add(q.balance).equals(BigDecimal.valueOf(1000)));

        pool.shutdownNow();
        System.out.println("4 in production  : jcmd <pid> Thread.print -> "
                + "look for \"Found one Java-level deadlock:\"");
        System.out.println("   (the two deadlocked daemon threads are still stuck; "
                + "they die with the JVM)");
        System.exit(0);
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured on OpenJDK 21)
 * NON-DETERMINISTIC: Object identity hashes differ every JVM start --
 *                    that variation IS the lesson. Shape is stable, numbers are not.
 * ----------------------------------------------------------------------------------------------
 * COFFMAN conditions: mutual exclusion, hold-and-wait, no preemption, circular wait
 *    lock ordering breaks CIRCULAR WAIT; tryLock breaks HOLD-AND-WAIT
 * 1 detected       : deadlock-t1 BLOCKED on java.lang.Object@4eec7777 held by deadlock-t2; deadlock-t2 BLOCKED on java.lang.Object@27d6c5e0 held by deadlock-t1;
 * 2 lock ordering  : completed=true  X=1000000 Y=1000000  total preserved=true
 * 3 tryLock+jitter : completed=true  total preserved=true
 * 4 in production  : jcmd <pid> Thread.print -> look for "Found one Java-level deadlock:"
 *    (the two deadlocked daemon threads are still stuck; they die with the JVM)
 * ============================================================================================== */
