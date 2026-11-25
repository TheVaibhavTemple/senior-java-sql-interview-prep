/* ==============================================================================================
 * Q41  .  CompletableFuture fan-out / fan-in
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Asynchronous fan-out / fan-in with allOf, per-call timeout and fallback
 *
 * PROBLEM
 *   Call three downstream services in parallel, combine the results, with a per-call
 *   timeout and fallback, without blocking a request thread.
 *
 * ALGORITHM
 *   1. Launch each downstream call with supplyAsync on YOUR OWN executor, not the common
 *      pool.
 *   2. Attach a timeout: schedule a completeExceptionally and cancel it when the call
 *      completes.
 *   3. Attach exceptionally(...) so a failure degrades to a fallback instead of failing the
 *      request.
 *   4. allOf(...) completes when all of them do - it returns Void, not the results.
 *   5. In the thenApply after allOf, join() each future; it no longer blocks because all
 *      are complete.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Sequential blocking calls                      --            --        avoid
 *       > Latency is the sum, not the max.
 *   List of Futures, then get() each               --            --        acceptable
 *       > Parallel, but every get() blocks and you cannot compose.
 *   Parallel stream over the calls                 --            --        avoid
 *       > Blocking I/O on the shared common ForkJoinPool. Do not.
 *   CompletableFuture.allOf + join                 --            --        WRITE THIS
 *       > Non-blocking composition; allOf signals completion, join collects.
 *
 * WHY THIS ONE
 *   Four things get graded: allOf returns Void so you still join each future (which no
 *   longer blocks, because allOf guaranteed completion); thenCompose flattens when your
 *   function itself returns a future and thenApply does not; the Async suffix decides
 *   which thread runs the callback; and you must pass your own executor because the common
 *   pool is CPU-sized and JVM-wide. The per-call timeout and fallback is what makes it a
 *   production answer.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why does allOf return CompletableFuture<Void>?
 *   ? thenApply vs thenCompose -- show the type that goes wrong.
 *   ? Where does the callback run without the Async suffix?
 *   ? exceptionally vs handle vs whenComplete -- which one swallows the exception?
 *   ? get() throws ExecutionException, join() throws CompletionException -- why the
 *     difference?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Async orchestration
 *   - Timeout and fallback patterns
 *   - thenCombine / thenCompose
 *   - Fan-out fan-in
 *   - Bulkhead isolation
 * ============================================================================================== */

import java.util.concurrent.*;
import java.util.function.Supplier;

public class Q41CompletableFutureFanOut {

    /* ---- never run blocking I/O on the common pool: it is CPU-sized and JVM-wide ---- */
    static final ExecutorService IO = Executors.newFixedThreadPool(16, r -> {
        Thread t = new Thread(r, "io-worker");
        t.setDaemon(true);
        return t;
    });
    static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "cf-timer");
        t.setDaemon(true);
        return t;
    });

    /* ---- per-call timeout + fallback (the Java 8 form of orTimeout) ---- */
    static <T> CompletableFuture<T> call(String name, Supplier<T> supplier, T fallback, long timeoutMs) {
        CompletableFuture<T> f = CompletableFuture.supplyAsync(supplier, IO);
        ScheduledFuture<?> killer = TIMER.schedule(
                () -> f.completeExceptionally(new TimeoutException(name + " timed out")),
                timeoutMs, TimeUnit.MILLISECONDS);
        f.whenComplete((r, e) -> killer.cancel(false));      // stop the timer on success
        return f.exceptionally(e -> {
            System.out.println("   fallback for " + name + " -> "
                    + rootCause(e).getClass().getSimpleName());
            return fallback;
        });
    }

    static Throwable rootCause(Throwable t) {
        return (t instanceof CompletionException || t instanceof ExecutionException)
                && t.getCause() != null ? t.getCause() : t;
    }

    static String remote(String name, long delayMs, String value) {
        try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return value;
    }

    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();

        CompletableFuture<String> profile = call("profile", () -> remote("profile", 200, "Asha"), "anon", 800);
        CompletableFuture<String> orders  = call("orders",  () -> remote("orders", 250, "7 orders"), "0 orders", 800);
        CompletableFuture<String> rewards = call("rewards", () -> remote("rewards", 2000, "gold"), "none", 400);

        /* allOf signals COMPLETION, not results. join() afterwards no longer blocks. */
        CompletableFuture<String> view = CompletableFuture
                .allOf(profile, orders, rewards)
                .thenApply(ignored -> profile.join() + " | " + orders.join() + " | " + rewards.join());

        String result = view.get(5, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - t0;

        System.out.println("combined view    : " + result);
        System.out.println("elapsed          : ~" + (elapsed < 900 ? "max(call), not sum" : elapsed + "ms")
                + "  (3 calls in parallel; slowest one fell back at its 400ms deadline)");

        /* ---- thenApply vs thenCompose vs thenCombine ---- */
        CompletableFuture<String>  a = CompletableFuture.supplyAsync(() -> "id-1", IO);
        CompletableFuture<Integer> b = a.thenApply(String::length);                 // T -> U
        CompletableFuture<String>  c = a.thenCompose(id ->                          // T -> CF<U>
                CompletableFuture.supplyAsync(() -> "user(" + id + ")", IO));
        CompletableFuture<String>  d = a.thenCombine(b, (s, n) -> s + ":" + n);     // join two
        System.out.println("thenApply        : " + b.get());
        System.out.println("thenCompose      : " + c.get() + "   (flattened, not CF<CF<String>>)");
        System.out.println("thenCombine      : " + d.get());

        /* ---- exception wrapper differs between get() and join() ---- */
        CompletableFuture<String> boom = CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("downstream 500");
        }, IO);
        try { boom.get(); }
        catch (ExecutionException e) { System.out.println("get()  throws    : ExecutionException -> " + e.getCause()); }
        try { boom.join(); }
        catch (CompletionException e) { System.out.println("join() throws    : CompletionException -> " + e.getCause()); }

        /* ---- anyOf : first one home wins ---- */
        Object fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> remote("slow", 300, "slow"), IO),
                CompletableFuture.supplyAsync(() -> remote("fast", 50, "fast"), IO)).get();
        System.out.println("anyOf winner     : " + fastest);

        IO.shutdownNow();
        TIMER.shutdownNow();
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 *    fallback for rewards -> TimeoutException
 * combined view    : Asha | 7 orders | none
 * elapsed          : ~max(call), not sum  (3 calls in parallel; slowest one fell back at its 400ms deadline)
 * thenApply        : 4
 * thenCompose      : user(id-1)   (flattened, not CF<CF<String>>)
 * thenCombine      : id-1:4
 * get()  throws    : ExecutionException -> java.lang.IllegalStateException: downstream 500
 * join() throws    : CompletionException -> java.lang.IllegalStateException: downstream 500
 * anyOf winner     : fast
 * ============================================================================================== */
