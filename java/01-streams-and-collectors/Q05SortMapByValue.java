/* ==============================================================================================
 * Q05  .  Sort a Map by its values
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Entry stream + comparator chain + LinkedHashMap collector
 *
 * PROBLEM
 *   Sort a Map<String, Integer> by value descending and return a Map whose iteration order
 *   is that order.
 *
 * ALGORITHM
 *   1. Stream map.entrySet().
 *   2. Sort with Map.Entry.comparingByValue().reversed().
 *   3. Add .thenComparing(Map.Entry.comparingByKey()) so ties are deterministic.
 *   4. Collect with toMap(key, value, mergeFn, LinkedHashMap::new).
 *   5. The map supplier is mandatory: a HashMap would discard the sort order.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   To List, sort, rebuild into a HashMap          O(n log n)    O(n)      avoid
 *       > The bug they are hunting: HashMap throws the order away again.
 *   TreeMap with a comparator on values            O(n log n)    O(n)      avoid
 *       > A TreeMap comparator orders KEYS; equal values collapse entries.
 *   Stream sorted + toMap with LinkedHashMap::new  O(n log n)    O(n)      WRITE THIS
 *       > Ordered, explicit, and the tie-break is visible in the code.
 *
 * WHY THIS ONE
 *   Almost everyone sorts correctly and then collects into a HashMap. The most important
 *   token in this answer is LinkedHashMap::new; the second is the thenComparing tie-break,
 *   because without it the order of equal values depends on the source map's internal
 *   layout.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why does toMap need a merge function here even though no keys collide?
 *   ? Sort by value descending, then key ascending -- explain your comparator chain.
 *   ? Why is a TreeMap with a value comparator wrong?
 *   ? Return only the top 3 by value.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Top K frequent words
 *   - Leaderboard from a score map
 *   - Sort employees by salary then name
 *   - Most frequent element
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;

public class Q05SortMapByValue {

    /* ---- BEST FOR INTERVIEW : note BOTH the tie-break and the map supplier ---- */
    static Map<String, Integer> byValueDesc(Map<String, Integer> in) {
        return in.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,            // unreachable here, but toMap demands it
                        LinkedHashMap::new));   // WITHOUT this the order is discarded
    }

    static Map<String, Integer> topN(Map<String, Integer> in, int n) {
        return in.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(n)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /* ---- THE BUG : collecting into a HashMap throws the sort away ---- */
    static Map<String, Integer> broken(Map<String, Integer> in) {
        return in.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static void main(String[] args) {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("amit", 82); scores.put("neha", 95);
        scores.put("ravi", 71); scores.put("zoya", 95);
        scores.put("kabir", 60);

        System.out.println("source map : " + scores);
        System.out.println("sorted desc: " + byValueDesc(scores));
        System.out.println("top 3      : " + topN(scores, 3));
        System.out.println("broken ver : " + broken(scores) + "   <-- sort lost, HashMap re-hashed");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * source map : {neha=95, zoya=95, kabir=60, ravi=71, amit=82}
 * sorted desc: {neha=95, zoya=95, amit=82, ravi=71, kabir=60}
 * top 3      : {neha=95, zoya=95, amit=82}
 * broken ver : {neha=95, zoya=95, kabir=60, ravi=71, amit=82}   <-- sort lost, HashMap re-hashed
 * ============================================================================================== */
