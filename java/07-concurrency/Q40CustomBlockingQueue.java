/* ==============================================================================================
 * Q40  .  Implement a bounded BlockingQueue from scratch
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- product aur fintech rounds mein "apna BlockingQueue likho" aata hai
 * TECHNIQUE    : ReentrantLock with two Conditions over a circular array buffer
 *
 * PROBLEM
 *   Write your own BlockingQueue: put() blocks when full, take() blocks when empty, plus a
 *   timed offer().
 *
 * ALGORITHM
 *   1. Back the queue with a fixed array plus head, tail and count.
 *   2. Guard all three with one ReentrantLock; unlock always inside finally.
 *   3. notFull.await() when the buffer is full, notEmpty.await() when it is empty.
 *   4. After a put, signal notEmpty; after a take, signal notFull - exactly one thread that
 *      can proceed.
 *   5. Timed offer: awaitNanos returns the REMAINING nanos, so feed it back rather than
 *      restarting the clock.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   synchronized + wait / notifyAll                --            --        acceptable
 *       > Correct, but every state change wakes every waiter.
 *   ReentrantLock + one Condition                  --            --        acceptable
 *       > Same wake-everyone problem as a single monitor.
 *   ReentrantLock + notFull and notEmpty           --            --        WRITE THIS
 *       > Signal exactly the threads that can now progress. This is the answer.
 *   Array-backed circular buffer                   --            --        WRITE THIS
 *       > Head, tail and count -- no shifting, no allocation per element.
 *
 * WHY THIS ONE
 *   Why two Conditions exist is the point of the question: with one wait set you must use
 *   notifyAll and wake producers to inform consumers. With notFull and notEmpty you call
 *   signal() and wake exactly one thread that can proceed. The other detail interviewers
 *   watch for is awaitNanos returning the REMAINING time -- restarting the clock each loop
 *   turns a timed offer into an unbounded wait.
 *
 * SAMJHO
 *   Ek array, head, tail aur count; sab ek ReentrantLock ke neeche. Full ho to notFull pe
 *   await, khali ho to notEmpty pe await. Do Conditions hain to signal() sirf sahi type ka ek
 *   thread jagata hai. Timed offer mein awaitNanos ka bacha hua time hi wapas do.
 *
 * AISE BOLO  (30-40 sec)
 *   "synchronized with wait and notifyAll works, but every state change wakes every waiter.
 *   I use one ReentrantLock with two Conditions, notFull and notEmpty, over a circular array,
 *   so put and take are O(1) with no shifting and memory is fixed at the capacity. Each
 *   Condition holds only one kind of waiter, so signal wakes exactly one thread that can
 *   proceed. For the timed offer I feed the remaining nanos from awaitNanos back into the
 *   loop, otherwise every wakeup restarts the clock."
 *
 * TRAP
 *   ! unlock() finally mein nahi rakha to ek exception aur lock hamesha ke liye pakda rahega.
 *   ! await() if mein rakha to spurious wakeup par full array mein likh doge. awaitNanos ka
 *     return ignore kiya to har wakeup par clock restart, timed offer unbounded ban jata hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why two Conditions instead of one? What does it save?
 *     -> Producers aur consumers alag wait set mein rehte hain, to signalAll ki jagah signal()
 *        kaafi hai. Faltu wake-ups aur context switch bachte hain.
 *   ? Why signal() here but notifyAll() in the wait/notify version?
 *     -> notEmpty pe sirf consumers wait karte hain, koi bhi jage to kaam kar sakta hai. Ek
 *        monitor mein notify galat type ko jaga sakta hai, isliye wahan notifyAll.
 *   ? Why does awaitNanos return a value, and what if you ignore it?
 *     -> Wo bacha hua time return karta hai. Ignore karke wahi timeout dobara diya to har
 *        wakeup par clock reset hoga, aur offer kabhi time out nahi bhi ho sakta.
 *   ? Where exactly must unlock() go, and why?
 *     -> lock() try ke bahar, unlock() finally mein. Exception aaye to bhi lock chhoote; aur
 *        lock lene mein hi interrupt aaya to finally galat unlock() na kare.
 *   ? How would the real ArrayBlockingQueue differ from what you wrote?
 *     -> null reject karta hai (NPE), fair lock ka option deta hai, aur poll, drainTo,
 *        remainingCapacity aur weakly consistent iterator ke saath poora BlockingQueue API.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - ArrayBlockingQueue internals
 *   - Condition vs wait/notify
 *   - Circular buffer
 *   - Bounded buffer
 *   - Custom thread-safe collections
 *
 * VARIANTS     : LC 1188 (Design Bounded Blocking Queue)
 * ============================================================================================== */

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Q40CustomBlockingQueue {

    /* ======================================================================
       ONE Lock, TWO Conditions.  With a single wait set you must notifyAll
       and wake producers in order to inform consumers.  With notFull and
       notEmpty you signal() exactly one thread that can actually proceed.
       ====================================================================== */
    static final class MyBlockingQueue<T> {
        private final Object[] items;
        private int head, tail, count;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        MyBlockingQueue(int capacity) { items = new Object[capacity]; }

        public void put(T x) throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (count == items.length) notFull.await();
                items[tail] = x;
                tail = (tail + 1) % items.length;      // circular: no shifting, ever
                count++;
                notEmpty.signal();                      // wake exactly ONE consumer
            } finally {
                lock.unlock();                          // ALWAYS in finally
            }
        }

        @SuppressWarnings("unchecked")
        public T take() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (count == 0) notEmpty.await();
                T x = (T) items[head];
                items[head] = null;                     // drop the reference for GC
                head = (head + 1) % items.length;
                count--;
                notFull.signal();
                return x;
            } finally {
                lock.unlock();
            }
        }

        /** awaitNanos returns the REMAINING time - feed it back, never restart the clock. */
        public boolean offer(T x, long timeout, TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            lock.lockInterruptibly();
            try {
                while (count == items.length) {
                    if (nanos <= 0) return false;
                    nanos = notFull.awaitNanos(nanos);
                }
                items[tail] = x;
                tail = (tail + 1) % items.length;
                count++;
                notEmpty.signal();
                return true;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try { return count; } finally { lock.unlock(); }
        }
    }

    public static void main(String[] args) throws Exception {
        MyBlockingQueue<Integer> q = new MyBlockingQueue<>(5);

        /* single-threaded behaviour including the circular wrap */
        for (int i = 1; i <= 5; i++) q.put(i);
        System.out.println("filled to cap    : size=" + q.size());
        System.out.println("take, take       : " + q.take() + ", " + q.take() + "  size=" + q.size());
        q.put(6); q.put(7);                       // wraps around the array
        System.out.println("after wrap       : size=" + q.size());

        /* the queue is full again (size 5 of 5) - the timed offer must TIME OUT,
           not block forever. put(99) here would hang the program. */
        long t0 = System.nanoTime();
        boolean accepted = q.offer(99, 150, TimeUnit.MILLISECONDS);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("offer on full    : accepted=" + accepted
                + "  waited~" + (ms >= 140 && ms <= 400 ? "150ms" : ms + "ms"));

        /* 4 producers, 4 consumers, nothing lost and nothing duplicated */
        final int N = 4, PER = 500;
        MyBlockingQueue<Integer> q2 = new MyBlockingQueue<>(16);
        AtomicInteger taken = new AtomicInteger();
        long expectedSum = (long) N * PER * (PER - 1) / 2;
        java.util.concurrent.atomic.AtomicLong sum = new java.util.concurrent.atomic.AtomicLong();

        ExecutorService pool = Executors.newFixedThreadPool(N * 2);
        CountDownLatch done = new CountDownLatch(N * 2);
        for (int p = 0; p < N; p++) {
            pool.execute(() -> {
                try { for (int i = 0; i < PER; i++) q2.put(i); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { done.countDown(); }
            });
            pool.execute(() -> {
                try { for (int i = 0; i < PER; i++) { sum.addAndGet(q2.take()); taken.incrementAndGet(); } }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { done.countDown(); }
            });
        }
        done.await(20, TimeUnit.SECONDS);
        pool.shutdownNow();
        System.out.println("stress 4x4x500   : taken=" + taken.get() + " of " + (N * PER)
                + "  checksum " + (sum.get() == expectedSum ? "matches" : "MISMATCH")
                + "  leftover=" + q2.size());
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * filled to cap    : size=5
 * take, take       : 1, 2  size=3
 * after wrap       : size=5
 * offer on full    : accepted=false  waited~150ms
 * stress 4x4x500   : taken=2000 of 2000  checksum matches  leftover=0
 * ============================================================================================== */
