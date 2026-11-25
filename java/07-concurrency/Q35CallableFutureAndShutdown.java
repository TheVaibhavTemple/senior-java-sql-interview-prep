/* ==============================================================================================
 * Q35  .  Threads, Callable, Future and clean shutdown
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- ExecutorService, Future aur shutdown har round mein poochte hain
 * TECHNIQUE    : ExecutorService with Callable/Future, timed get, and the two-phase shutdown
 *
 * PROBLEM
 *   Run a task on a pool, get its result, handle its failure, apply a timeout, and shut
 *   the pool down properly.
 *
 * ALGORITHM
 *   1. Submit a Callable so the task can return a value and throw checked exceptions.
 *   2. Always use the timed get(timeout, unit) - the untimed get blocks forever.
 *   3. ExecutionException wraps the task's real exception: unwrap with getCause().
 *   4. On InterruptedException, restore the flag with Thread.currentThread().interrupt().
 *   5. Shutdown: shutdown(), awaitTermination, then shutdownNow() and await again if it did
 *      not finish.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   new Thread(...).start()                        --            --        avoid
 *       > No pooling, no result, no lifecycle. Unmanaged thread creation at scale is an
 *       > OOM.
 *   Runnable on an ExecutorService                 --            --        acceptable
 *       > Fine for fire-and-forget; cannot return a value or throw checked exceptions.
 *   Callable + Future with a TIMED get             --            --        WRITE THIS
 *       > Result, exception propagation and a deadline.
 *   CompletableFuture                              --            --        WRITE THIS
 *       > When you must compose several calls -- see Q41.
 *
 * WHY THIS ONE
 *   Three details are the whole answer: the untimed get() blocks forever and belongs
 *   nowhere near a request thread; ExecutionException wraps the real exception so you must
 *   unwrap getCause(); and catching InterruptedException without restoring the flag
 *   silently breaks every cancellation mechanism above you. The two-phase shutdown is
 *   asked verbatim -- memorise it.
 *
 * SAMJHO
 *   Callable value return karta hai aur checked exception throw kar sakta hai, Runnable nahi.
 *   Future.get() hamesha timeout ke saath, warna thread hamesha ke liye atak sakta hai.
 *   Task ka asli exception ExecutionException ke getCause() mein hota hai. Shutdown do step
 *   mein: shutdown() aur wait, phir shutdownNow() aur dobara wait.
 *
 * AISE BOLO  (30-40 sec)
 *   "The naive way is new Thread().start(), but that gives me no result, no pooling and no
 *   lifecycle. I submit a Callable to an ExecutorService and read the Future with a timed get,
 *   because an untimed get can block a request thread forever. A failure comes back wrapped
 *   in ExecutionException, so I unwrap getCause(), and on InterruptedException I restore the
 *   flag. To stop the pool I call shutdown, await with a deadline, then shutdownNow - which
 *   only interrupts, so my tasks must check the interrupt flag."
 *
 * TRAP
 *   ! catch (InterruptedException e) {} khaali chhod diya to cancel(true) aur shutdownNow dono
 *     bekaar ho jaate hain. Hamesha Thread.currentThread().interrupt() karo.
 *   ! submit() exception ko Future mein chhupa deta hai. get() kabhi call nahi kiya to error
 *     kahin print nahi hoga.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? What is in e.getCause(), and why is the wrapper there?
 *     -> getCause() mein task ka asli exception hai. Wrapper se pata chalta hai ki failure task
 *        ke andar hua, get() mein nahi; aur get() ko har checked type declare nahi karna padta.
 *   ? Why restore the interrupt flag instead of swallowing it?
 *     -> InterruptedException throw hote hi flag clear ho jata hai. Wapas set nahi kiya to upar
 *        ka code (pool, framework) kabhi jaan nahi payega ki cancel hua tha.
 *   ? shutdown vs shutdownNow -- what happens to queued tasks in each?
 *     -> shutdown(): naye task reject, queued aur running dono poore chalte hain. shutdownNow():
 *        running ko interrupt, queued tasks nahi chalte, unki List<Runnable> return hoti hai.
 *   ? Your task ignores interrupts -- what does shutdownNow achieve?
 *     -> Kuch nahi, interrupt sirf ek flag hai. Task chalta rahega, awaitTermination false
 *        dega, aur non-daemon thread JVM ko exit nahi karne dega.
 *   ? Where does an exception go if you use execute() instead of submit()?
 *     -> Worker thread mar jata hai aur exception UncaughtExceptionHandler tak jata hai
 *        (default: stderr pe stack trace). Pool uski jagah naya thread bana leta hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Runnable vs Callable
 *   - Future vs CompletableFuture
 *   - Graceful shutdown
 *   - Task cancellation
 *   - InterruptedException handling
 *
 * JAVA 21 MEIN
 *   - ExecutorService ab AutoCloseable hai (Java 19 se): try-with-resources ka close() shutdown
 *     karke saare tasks khatam hone tak wait karta hai. Uska koi timeout nahi hai, to deadline
 *     chahiye to upar wala two-phase shutdown hi likho.
 *   - Future.state(), resultNow() aur exceptionNow() (Java 19 se) complete future ka result
 *     bina checked exception ke dete hain.
 *
 * APNI STORY SE JODO
 *   "My services run on AKS, so a rolling deploy stops pods, and in-flight work has to finish
 *   or go back to the queue before the JVM exits."
 *   Note: [bhar do: hamare consumers aur @Scheduled jobs shutdown par kaise rukte the]. Koi
 *   number mat bolo.
 *
 * VARIANTS     : classic - invokeAll / invokeAny with a deadline, cancel a long-running task
 * ============================================================================================== */

import java.util.concurrent.*;

public class Q35CallableFutureAndShutdown {

    /* ---- THE SHUTDOWN SEQUENCE : asked verbatim. Write it from memory. ---- */
    static void shutdown(ExecutorService pool) {
        pool.shutdown();                                        // stop accepting new tasks
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();                             // interrupt what is running
                if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                    System.out.println("   pool did not terminate");
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();                 // restore, never swallow
        }
    }

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            /* 1. Callable returns a value and may throw */
            Future<Integer> ok = pool.submit(() -> {
                TimeUnit.MILLISECONDS.sleep(100);
                return 42;
            });
            System.out.println("1 timed get      : " + ok.get(2, TimeUnit.SECONDS));

            /* 2. the task's real exception arrives wrapped */
            Future<Integer> boom = pool.submit(() -> {
                throw new IllegalStateException("downstream refused");
            });
            try {
                boom.get(2, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                System.out.println("2 wrapper        : " + e.getClass().getSimpleName());
                System.out.println("  getCause()     : " + e.getCause());
            }

            /* 3. a task that outruns its deadline is cancelled */
            Future<String> slow = pool.submit(() -> {
                TimeUnit.SECONDS.sleep(30);
                return "never";
            });
            try {
                slow.get(200, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                boolean cancelled = slow.cancel(true);          // true = interrupt it
                System.out.println("3 timed out      : cancelled=" + cancelled
                        + " isCancelled=" + slow.isCancelled());
            }

            /* 4. submit() SWALLOWS the exception; execute() surfaces it */
            pool.submit(() -> { throw new RuntimeException("invisible"); });
            System.out.println("4 submit()       : exception buried in the Future, nothing printed");

            /* 5. cooperative cancellation - a task that ignores interrupts never stops */
            Future<?> cooperative = pool.submit(() -> {
                int spins = 0;
                while (!Thread.currentThread().isInterrupted() && spins < 1_000_000) spins++;
                return null;
            });
            cooperative.cancel(true);
            System.out.println("5 cooperative    : cancel delivered an interrupt, task checks the flag");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            System.out.println("unexpected: " + e);
        } finally {
            shutdown(pool);
            System.out.println("6 shutdown       : isShutdown=" + pool.isShutdown()
                    + " isTerminated=" + pool.isTerminated());
        }
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 1 timed get      : 42
 * 2 wrapper        : ExecutionException
 *   getCause()     : java.lang.IllegalStateException: downstream refused
 * 3 timed out      : cancelled=true isCancelled=true
 * 4 submit()       : exception buried in the Future, nothing printed
 * 5 cooperative    : cancel delivered an interrupt, task checks the flag
 * 6 shutdown       : isShutdown=true isTerminated=true
 * ============================================================================================== */
