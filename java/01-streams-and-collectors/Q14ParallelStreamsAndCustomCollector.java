/* ==============================================================================================
 * Q14  .  Parallel streams done safely, and a custom Collector
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- "parallel stream kab nahi" product/GCC rounds mein aata hai
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
 * SAMJHO
 *   Parallel stream ke forEach se shared ArrayList mein add karna data race hai: elements gum
 *   hote hain ya ArrayList andar se exception deta hai. collect() har split ko apna container
 *   deta hai aur end mein combiner se jodta hai, isliye lock nahi chahiye. parallelStream
 *   JVM-wide common ForkJoinPool pe chalta hai, us pe blocking I/O mat daalo.
 *
 * AISE BOLO  (30-40 sec)
 *   "The bug is forEach adding to a plain ArrayList from many threads; that is a data race, so
 *   elements get lost or ArrayList throws. A synchronized list is correct, but every add
 *   fights for one lock, so it can be slower than sequential. I use collect(Collectors.toList()),
 *   which fills a separate list per split and merges them in the combiner: O(n), no locking.
 *   I also keep blocking I/O off parallel streams, because they run on the shared common
 *   ForkJoinPool, and for small inputs the split and merge cost makes parallel slower."
 *
 * TRAP
 *   ! Ek run mein count sahi aa gaya to "safe hai" mat bolo; data race har run mein nahi
 *     dikhta (neeche expected output mein bhi survive hua).
 *   ! parallelStream mein blocking I/O ya DB call daali to poore JVM ka common pool atak jata
 *     hai, baaki parallel streams bhi slow ho jaati hain.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? When is the combiner of a Collector invoked? Prove it.
 *     -> Sirf parallel mein, jab do splits ke partial containers jodne hon; sequential mein
 *        kabhi nahi. Proof: combiner mein print daalo, stream() aur parallelStream() chalao.
 *   ? Why is parallel slower here for n = 1000?
 *     -> Split, thread scheduling aur combine ka kharcha 1000 chhote operations se zyada hai,
 *        upar se Integer boxing. Parallel tab jab elements x per-element kaam bada ho.
 *   ? How do you run a parallel stream on your own pool, and why is that a workaround
 *     rather than a feature?
 *     -> pool.submit(() -> list.parallelStream()...).get(), "isolated pool" section dekho. Ye
 *        implementation detail pe tika hai, spec guarantee nahi; pool shutdown bhi khud karo.
 *   ? Is Collectors.toList() thread-safe? Is the list it returns?
 *     -> Parallel collect safe hai kyunki har split apni list bharta hai, phir combine. Return
 *        list ke type ya thread-safety ki koi guarantee nahi (abhi ArrayList); share mat karo.
 *   ? What does the CONCURRENT characteristic change?
 *     -> Sab threads ek hi shared thread-safe container (jaise ConcurrentHashMap) mein add
 *        karte hain, combiner nahi chalta; tabhi jab collector UNORDERED ho ya stream unordered.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Why parallel streams hurt
 *   - Writing a custom Collector
 *   - Thread-safe accumulation
 *   - ForkJoinPool common pool problems
 *   - Reduce vs collect
 *
 * JAVA 21 MEIN
 *   - Blocking I/O ke liye parallel stream nahi, Executors.newVirtualThreadPerTaskExecutor()
 *     (virtual threads, Java 21). Parallel stream sirf CPU-bound, in-memory kaam ke liye.
 *
 * APNI STORY SE JODO
 *   "I have used Java 21 virtual threads at work, so for blocking calls I reach for them, and I
 *   keep parallel streams for CPU-bound work on data already in memory."
 *   Note: virtual threads kis kaam mein use kiye, [jo sach ho] bharo; koi number mat jodo.
 *
 * VARIANTS     : classic -- custom Collector for joining or top-K, reduce vs collect for a sum
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
