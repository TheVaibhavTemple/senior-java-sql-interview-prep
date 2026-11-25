/* ==============================================================================================
 * Q38  .  Print odd and even alternately with two threads
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- India mein sabse common multithreading coding sawaal
 * TECHNIQUE    : Monitor coordination with wait/notifyAll; Semaphore turn-passing as the alternative
 *
 * PROBLEM
 *   Two threads: one prints odd numbers, one prints even, alternating, from 1 to N in
 *   order.
 *
 * ALGORITHM
 *   1. Both threads share one lock object and one counter.
 *   2. Each thread waits WHILE it is not its turn - a while loop, never an if.
 *   3. The while loop is required because of spurious wakeups and because the condition can
 *      change again.
 *   4. After printing and incrementing, call notifyAll to release the partner.
 *   5. The wait condition must also check number <= max, or the partner waits forever at
 *      the end. The notifyAll on the exit path is a safety net on top of that.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Busy-wait on a shared flag                     --            --        avoid
 *       > Burns a core. Correct output, wrong answer.
 *   AtomicInteger + spin                           --            --        avoid
 *       > Busy-waiting again.
 *   wait / notifyAll on a shared monitor           --            --        WRITE THIS
 *       > What the question is actually testing.
 *   Two Semaphores passing the turn                --            --        WRITE THIS
 *       > Shorter, and it extends to N threads with one semaphore per thread.
 *   ReentrantLock with two Conditions              --            --        acceptable
 *       > Equivalent to the semaphore version, more ceremony.
 *
 * WHY THIS ONE
 *   Two things are graded: wait() inside a WHILE loop and not an if -- because of spurious
 *   wakeups and because the condition can be falsified again between the notify and the
 *   reacquire -- and notifyAll rather than notify. With exactly two threads notify happens to
 *   work, because the only waiter is the partner; with three or more it can wake the wrong
 *   thread and stall everybody, so notifyAll is the safe default. Also put number <= max in
 *   the wait condition, or the partner waits forever at the end.
 *
 * SAMJHO
 *   Dono threads ek lock aur ek counter share karte hain. Jiski baari nahi hai wo while loop
 *   mein wait() karta hai. Print karo, counter badhao, notifyAll() karo. Wait condition mein
 *   number <= max bhi rakho, taaki end mein partner atke nahi.
 *
 * AISE BOLO  (30-40 sec)
 *   "Busy-waiting on a flag gives the right output but burns a core, so I use one shared lock
 *   with wait and notifyAll. Each thread waits in a while loop until the counter's parity is
 *   its turn, prints, increments and calls notifyAll, so it is O(N) handoffs with constant
 *   space. The while loop covers spurious wakeups, and the wait condition also checks
 *   number <= max so both threads exit cleanly at the end. A two-semaphore version is
 *   shorter and extends to N threads with one semaphore per thread."
 *
 * TRAP
 *   ! wait() if mein rakha to spurious wakeup ya galat notify ke baad thread bina check kiye
 *     print kar dega. Hamesha while.
 *   ! Wait condition sirf parity dekhe aur number <= max na dekhe, to aakhri number ke baad
 *     partner hamesha wait karega: output sahi, program hang.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why while and not if? Give a concrete interleaving that breaks the if version.
 *     -> 3 threads: A ka notifyAll B aur C dono ko jagata hai, C pehle lock le leta hai. if
 *        dobara check nahi karta, to C B ki baari mein print kar deta hai.
 *   ? Why notifyAll and not notify here?
 *     -> Sirf do threads mein notify bhi chal jata hai, wait set mein sirf partner hota hai.
 *        Teen ya zyada mein notify galat thread jaga sakta hai aur sab atak jaate hain.
 *   ? Extend to three threads printing 1, 2, 3, 1, 2, 3 in order.
 *     -> Turn = number % 3. Har thread apne slot ka wait kare, print ke baad notifyAll. Code:
 *        ThreeWayPrinter section.
 *   ? What happens at the end if you forget the final notifyAll?
 *     -> Is code mein kuch nahi: aakhri print ka notifyAll partner ko jagata hai aur wo
 *        number > max dekh ke nikal jata hai. Hang tab hota hai jab condition mein max na ho.
 *   ? Can you do it with a single Semaphore?
 *     -> Saaf tareeke se nahi: ek semaphore ko pata nahi baari kiski hai, dono same permit pe
 *        race karenge. Turn variable jodo to retry loop (spin) ban jata hai. Do semaphores lo.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Print 1..N with three threads
 *   - Zero-even-odd printer
 *   - Alternate printing of characters
 *   - Turn-taking between threads
 *   - FooBar printing
 *
 * VARIANTS     : LC 1114, LC 1115, LC 1116, LC 1195
 * ============================================================================================== */

public class Q38PrintOddEvenTwoThreads {

    /* ======================================================================
       ANSWER 1 - wait / notifyAll.  This is what the question actually tests:
         * wait() inside a WHILE loop, never an if
         * notifyAll(), not notify(), because there are two distinct waiters
         * a final notifyAll on the exit path, or the partner waits forever
       ====================================================================== */
    static final class OddEvenPrinter {
        private final int max;
        private int number = 1;
        private final Object lock = new Object();
        private final StringBuilder log = new StringBuilder();

        OddEvenPrinter(int max) { this.max = max; }

        void print(boolean wantOdd, String tag) {
            while (true) {
                synchronized (lock) {
                    while (number <= max && (number % 2 == 0) == wantOdd) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    if (number > max) {
                        lock.notifyAll();            // release the partner before leaving
                        return;
                    }
                    log.append(tag).append(number++).append(' ');
                    lock.notifyAll();
                }
            }
        }

        String run() throws InterruptedException {
            Thread odd  = new Thread(() -> print(true,  "O"), "odd");
            Thread even = new Thread(() -> print(false, "E"), "even");
            odd.start(); even.start();
            odd.join(); even.join();
            return log.toString().trim();
        }
    }

    /* ======================================================================
       ANSWER 2 - Semaphores.  Shorter, and it generalises to N threads.
       ====================================================================== */
    static final class OddEvenSemaphore {
        private final java.util.concurrent.Semaphore oddTurn  = new java.util.concurrent.Semaphore(1);
        private final java.util.concurrent.Semaphore evenTurn = new java.util.concurrent.Semaphore(0);
        private final int max;
        private final StringBuilder log = new StringBuilder();

        OddEvenSemaphore(int max) { this.max = max; }

        private void loop(int start, java.util.concurrent.Semaphore mine,
                          java.util.concurrent.Semaphore other, String tag) {
            for (int i = start; i <= max; i += 2) {
                try { mine.acquire(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                synchronized (log) { log.append(tag).append(i).append(' '); }
                other.release();
            }
            other.release();        // unblock the partner so it can finish
        }

        String run() throws InterruptedException {
            Thread o = new Thread(() -> loop(1, oddTurn,  evenTurn, "O"), "odd");
            Thread e = new Thread(() -> loop(2, evenTurn, oddTurn,  "E"), "even");
            o.start(); e.start(); o.join(); e.join();
            return log.toString().trim();
        }
    }

    /* ======================================================================
       THE THREE-THREAD EXTENSION the interviewer asks next.
       ====================================================================== */
    static final class ThreeWayPrinter {
        private final int max;
        private int number = 1;
        private final Object lock = new Object();
        private final StringBuilder log = new StringBuilder();

        ThreeWayPrinter(int max) { this.max = max; }

        void print(int slot, String tag) {               // slot is 0, 1 or 2
            while (true) {
                synchronized (lock) {
                    while (number <= max && number % 3 != slot) {
                        try { lock.wait(); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                    }
                    if (number > max) { lock.notifyAll(); return; }
                    log.append(tag).append(number++).append(' ');
                    lock.notifyAll();
                }
            }
        }

        String run() throws InterruptedException {
            Thread a = new Thread(() -> print(1, "A"));
            Thread b = new Thread(() -> print(2, "B"));
            Thread c = new Thread(() -> print(0, "C"));
            a.start(); b.start(); c.start();
            a.join(); b.join(); c.join();
            return log.toString().trim();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("wait/notifyAll   : " + new OddEvenPrinter(10).run());
        System.out.println("semaphore        : " + new OddEvenSemaphore(10).run());
        System.out.println("three threads    : " + new ThreeWayPrinter(9).run());
        System.out.println("note             : the sequence is strictly ordered every run - "
                + "that is the proof the handoff is correct");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * wait/notifyAll   : O1 E2 O3 E4 O5 E6 O7 E8 O9 E10
 * semaphore        : O1 E2 O3 E4 O5 E6 O7 E8 O9 E10
 * three threads    : A1 B2 C3 A4 B5 C6 A7 B8 C9
 * note             : the sequence is strictly ordered every run - that is the proof the handoff is correct
 * ============================================================================================== */
