/* ==============================================================================================
 * Q14  .  Parallel streams done safely, and a custom Collector
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : Mutable reduction via collect (thread-confined accumulate + combine); Collector.of for custom collectors
 *
 * PROBLEM
 *   Here is a parallel stream collecting into a shared ArrayList. Explain what is wrong
 *   and fix it. Then write a custom Collector.
 *
 * ALGORITHM
 *   1. forEach into a shared ArrayList is a data race: lost elements and intermittent
 *      exceptions.
 *   2. collect() accumulates into a separate container per split, then merges with the
 *      combiner - no locking.
 *   3. Reduction functions must be associative and stateless to be correct in parallel.
 *   4. parallelStream uses the shared ForkJoinPool.commonPool - never run blocking I/O on
 *      it.
 *   5. A custom Collector is four parts: supplier, accumulator, combiner (parallel only),
 *      finisher.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   forEach into a shared ArrayList                --            --        avoid
 *       > Data race. Lost elements, and occasionally an exception from inside ArrayList.
 *   forEach into a synchronized list               O(n)          O(n)      acceptable
 *       > Correct, but every element contends on one lock -- often slower than sequential.
 *   collect(Collectors.toList())                   O(n)          O(n)      WRITE THIS
 *       > Thread-confined accumulation per split, then combine. No locking at all.
 *   Custom Collector.of(...)                       O(n)          O(n)      WRITE THIS
 *       > When no built-in fits. The combiner is the parallel-only piece.
 *
 * WHY THIS ONE
 *   This question tells the interviewer whether you understand the Stream contract or only
 *   the syntax. Two sentences win it: "collect does a thread-confined mutable reduction
 *   and then combines, so it needs no synchronisation", and "parallelStream uses the
 *   shared common ForkJoinPool, so I would never put blocking I/O on it".
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? When is the combiner of a Collector invoked? Prove it.
 *   ? Why is parallel slower here for n = 1000?
 *   ? How do you run a parallel stream on your own pool, and why is that a workaround
 *     rather than a feature?
 *   ? Is Collectors.toList() thread-safe? Is the list it returns?
 *   ? What does the CONCURRENT characteristic change?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Why parallel streams hurt
 *   - Writing a custom Collector
 *   - Thread-safe accumulation
 *   - ForkJoinPool common pool problems
 *   - Reduce vs collect
 * ============================================================================================== */

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Q14ParallelStreamsAndCustomCollector {

    static final class Employee {
        private final String name;
        Employee(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static void main(String[] args) throws Exception {
        List<Integer> data = IntStream.range(0, 200_000).boxed().collect(Collectors.toList());
        int expected = (int) data.stream().filter(x -> x % 3 == 0).count();

        /* ---- WRONG : shared mutable state across threads ---- */
        List<Integer> bad = new ArrayList<>();
        String badResult;
        try {
            data.parallelStream().filter(x -> x % 3 == 0).forEach(bad::add);
            badResult = bad.size() == expected
                    ? bad.size() + " (happened to survive this run - still a data race)"
                    : bad.size() + " of " + expected + " - ELEMENTS LOST";
        } catch (RuntimeException ex) {
            badResult = "threw " + ex.getClass().getSimpleName() + " from inside ArrayList";
        }
        System.out.println("WRONG forEach   : " + badResult);

        /* ---- RIGHT : collect() accumulates per split, then combines ---- */
        List<Integer> good = data.parallelStream().filter(x -> x % 3 == 0)
                .collect(Collectors.toList());
        System.out.println("RIGHT collect   : " + good.size() + " of " + expected);

        /* ---- reductions must be associative and stateless ---- */
        long sum = data.parallelStream().mapToLong(Integer::longValue).sum();
        System.out.println("parallel sum    : " + sum);

        /* ---- the common pool is JVM-wide; isolate blocking or heavy work ---- */
        System.out.println("common pool size: " + ForkJoinPool.commonPool().getParallelism());
        ForkJoinPool pool = new ForkJoinPool(4);
        try {
            int isolated = pool.submit(() ->
                    data.parallelStream().filter(x -> x % 3 == 0).collect(Collectors.toList())
            ).get().size();
            System.out.println("isolated pool   : " + isolated);
        } finally {
            pool.shutdown();
        }

        /* ---- a custom Collector: supplier, accumulator, combiner, finisher ---- */
        Collector<Employee, StringJoiner, String> joinNames = Collector.of(
                () -> new StringJoiner(", ", "[", "]"),   // supplier
                (j, e) -> j.add(e.getName()),             // accumulator
                StringJoiner::merge,                      // combiner - PARALLEL ONLY
                StringJoiner::toString);                  // finisher

        List<Employee> emps = Arrays.asList(
                new Employee("Asha"), new Employee("Bilal"), new Employee("Chen"));
        System.out.println("custom collector: " + emps.stream().collect(joinNames));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured on OpenJDK 21)
 * NON-DETERMINISTIC: the parallel data race loses a different number of elements every run --
 *                    that variation IS the lesson. Shape is stable, numbers are not.
 * ----------------------------------------------------------------------------------------------
 * WRONG forEach   : 66667 (happened to survive this run - still a data race)
 * RIGHT collect   : 66667 of 66667
 * parallel sum    : 19999900000
 * common pool size: 1
 * isolated pool   : 66667
 * custom collector: [Asha, Bilal, Chen]
 * ============================================================================================== */
