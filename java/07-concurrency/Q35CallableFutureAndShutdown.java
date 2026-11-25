/* ==============================================================================================
 * Q35  .  Threads, Callable, Future and clean shutdown
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
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
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? What is in e.getCause(), and why is the wrapper there?
 *   ? Why restore the interrupt flag instead of swallowing it?
 *   ? shutdown vs shutdownNow -- what happens to queued tasks in each?
 *   ? Your task ignores interrupts -- what does shutdownNow achieve?
 *   ? Where does an exception go if you use execute() instead of submit()?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Runnable vs Callable
 *   - Future vs CompletableFuture
 *   - Graceful shutdown
 *   - Task cancellation
 *   - InterruptedException handling
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
