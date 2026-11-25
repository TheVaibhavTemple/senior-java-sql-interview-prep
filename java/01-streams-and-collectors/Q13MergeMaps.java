/* ==============================================================================================
 * Q13  .  Merge two maps, summing values by key
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- common hai, aur ConcurrentHashMap.merge se concurrency round khulta hai
 * TECHNIQUE    : toMap with a merge function; Map.merge for the in-place form
 *
 * PROBLEM
 *   Two maps of product -> units sold. Produce one map with the totals.
 *
 * ALGORITHM
 *   1. Stream.concat the two entry streams into one.
 *   2. Collect with toMap(key, value, Integer::sum) - the third argument resolves duplicate
 *      keys.
 *   3. Without the merge function, a duplicate key throws IllegalStateException.
 *   4. In-place alternative: copy map one, then map2.forEach((k,v) -> result.merge(k, v,
 *      Integer::sum)).
 *   5. putAll is the trap answer: it overwrites rather than combining.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   putAll                                         O(n)          O(n)      avoid
 *       > Silently overwrites instead of summing. The trap answer.
 *   Loop with containsKey / get / put              O(n)          O(n)      acceptable
 *       > Correct but verbose, and that shape races in a concurrent version.
 *   Stream.concat + toMap with a merge function    O(n)          O(n)      WRITE THIS
 *       > The merge function is the point; without it you get IllegalStateException.
 *   Map.merge in a forEach                         O(n)          O(n)      WRITE THIS
 *       > Shortest correct version, and the right shape for ConcurrentHashMap.
 *
 * WHY THIS ONE
 *   Two things are being tested: that you know toMap has a third argument, and that you
 *   know putAll overwrites. Map.merge(key, value, remappingFn) is the same idea in the Map
 *   API and carries straight over to ConcurrentHashMap.merge, which is atomic -- mention
 *   that link, it is the bridge to the concurrency round.
 *
 * SAMJHO
 *   Dono maps ki entries ek stream mein jodo (Stream.concat) aur toMap ke teesre argument
 *   Integer::sum se same key ki values add karo. Ya pehla map copy karo aur doosre pe forEach
 *   se merge(k, v, Integer::sum). putAll mat likho, woh value overwrite karta hai, jodta nahi.
 *
 * AISE BOLO  (30-40 sec)
 *   "putAll is the tempting answer, but it overwrites the value for a repeated key instead of
 *   adding it. I concat both entry streams and collect with toMap, using Integer::sum as the
 *   merge function, which is O(n) time and O(n) space; without that third argument a duplicate
 *   key throws IllegalStateException. The shorter version copies the first map and calls
 *   merge(k, v, Integer::sum) for each entry of the second. If it has to be thread-safe, the
 *   same merge call on a ConcurrentHashMap is atomic per key."
 *
 * TRAP
 *   ! putAll chupchap overwrite karta hai: mouse 450 ki jagah 150 aata hai.
 *   ! Map.merge mein function null return kare to key hi hat jati hai, aur value null di to
 *     NullPointerException.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? What exception does toMap throw without a merge function, and why?
 *     -> IllegalStateException ("Duplicate key mouse ..."). toMap(key, value) unique keys maan
 *        ke chalta hai; duplicate pe rakhe, jode ya overwrite kare, woh guess nahi karta.
 *   ? One map has a null value -- which version breaks?
 *     -> A (toMap) aur C (summingInt) NullPointerException dete hain. B mein doosre map ka null
 *        NPE deta hai, pehle map ka null chupchap output mein reh jata hai. Pehle null filter karo.
 *   ? Merge N maps, not two.
 *     -> mergeAll() dekho: saare entrySets flatMap karo, phir groupingBy(key, summingInt(value)).
 *   ? Now do it thread-safely across 8 threads.
 *     -> Shared ConcurrentHashMap pe merge(k, v, Integer::sum): per key atomic, poore map pe
 *        lock nahi. Contention bahut ho to computeIfAbsent(k, x -> new LongAdder()).add(v).
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Combine counters from multiple sources
 *   - Sum inventory across warehouses
 *   - Reduce a list of maps
 *   - Word count across files
 *
 * APNI STORY SE JODO
 *   "In a batch job I replaced a synchronized shared HashMap with ConcurrentHashMap to remove
 *   lock contention, and the batch window went from 45 minutes to 8 minutes."
 *   Note: thread-safe wale follow-up ke baad bolo. Wahan merge ya compute use hua tha ya nahi,
 *   [jo sach ho] bharo; guess mat karo.
 *
 * VARIANTS     : classic -- word count across files, sum stock across warehouses
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Q13MergeMaps {

    /* ---- A : Stream.concat + toMap with a merge function ---- */
    static Map<String, Integer> mergeByConcat(Map<String, Integer> a, Map<String, Integer> b) {
        return Stream.concat(a.entrySet().stream(), b.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        Integer::sum, TreeMap::new));
    }

    /* ---- B : Map.merge, shortest, no intermediate stream ---- */
    static Map<String, Integer> mergeInPlace(Map<String, Integer> a, Map<String, Integer> b) {
        Map<String, Integer> out = new TreeMap<>(a);
        b.forEach((k, v) -> out.merge(k, v, Integer::sum));
        return out;
    }

    /* ---- C : N maps at once ---- */
    static Map<String, Integer> mergeAll(List<Map<String, Integer>> maps) {
        return maps.stream()
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, TreeMap::new,
                        Collectors.summingInt(Map.Entry::getValue)));
    }

    public static void main(String[] args) {
        Map<String, Integer> q1 = new LinkedHashMap<>();
        q1.put("laptop", 120); q1.put("mouse", 300);
        Map<String, Integer> q2 = new LinkedHashMap<>();
        q2.put("mouse", 150); q2.put("monitor", 60);

        System.out.println("q1             : " + q1);
        System.out.println("q2             : " + q2);
        System.out.println("A concat+toMap : " + mergeByConcat(q1, q2));
        System.out.println("B Map.merge    : " + mergeInPlace(q1, q2));
        System.out.println("C N maps       : " + mergeAll(Arrays.asList(q1, q2, q1)));

        /* the trap answer */
        Map<String, Integer> overwritten = new TreeMap<>(q1);
        overwritten.putAll(q2);
        System.out.println("putAll (TRAP)  : " + overwritten + "   <-- mouse overwritten, not summed");

        /* what toMap does without a merge function */
        try {
            Stream.concat(q1.entrySet().stream(), q2.entrySet().stream())
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IllegalStateException ex) {
            System.out.println("no merge fn    : IllegalStateException -> " + ex.getMessage());
        }
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * q1             : {laptop=120, mouse=300}
 * q2             : {mouse=150, monitor=60}
 * A concat+toMap : {laptop=120, monitor=60, mouse=450}
 * B Map.merge    : {laptop=120, monitor=60, mouse=450}
 * C N maps       : {laptop=240, monitor=60, mouse=750}
 * putAll (TRAP)  : {laptop=120, monitor=60, mouse=150}   <-- mouse overwritten, not summed
 * no merge fn    : IllegalStateException -> Duplicate key mouse (attempted merging values 300 and 150)
 * ============================================================================================== */
