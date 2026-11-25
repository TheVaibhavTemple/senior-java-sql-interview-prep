/* ==============================================================================================
 * Q03  .  Find duplicates in a collection
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Set membership as a predicate (Set.add returns false on a repeat)
 *
 * PROBLEM
 *   Return every element that appears more than once. Then a version that also reports how
 *   many times.
 *
 * ALGORITHM
 *   1. Create an empty HashSet outside the stream.
 *   2. filter(n -> !seen.add(n)) keeps only elements already present.
 *   3. Collect to a Set so an element appearing 3 times is reported once.
 *   4. If counts are needed, groupingBy + counting and filter value > 1 instead.
 *   5. Note: the predicate is stateful, so it is not safe for parallel streams.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Nested loop                                    O(n^2)        O(1)      avoid
 *   Sort, then scan neighbours                     O(n log n)    O(1)      acceptable
 *       > Destroys input order. Fine if it is already sorted.
 *   HashSet.add() returns false                    O(n)          O(n)      WRITE THIS
 *       > One pass, one structure, and it reads as intent.
 *   groupingBy + counting, filter > 1              O(n)          O(n)      acceptable
 *       > Use when you need the counts, not just the keys.
 *
 * WHY THIS ONE
 *   Set.add() returning false when the element is already present is the shortest correct
 *   answer and shows you know the contract. Then flag out loud that the lambda holds
 *   state, so it is not parallel-safe. That one sentence is often the gap between "right
 *   answer" and "senior answer".
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why is filter(n -> !seen.add(n)) unsafe in a parallel stream?
 *   ? Find elements appearing exactly twice.
 *   ? The list has 10^8 elements and will not fit in memory -- now what?
 *   ? Find the duplicate without extra space, given values in 1..n.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Duplicate characters in a string
 *   - Contains duplicate within distance K
 *   - Find all duplicates in 1..n
 *   - Intersection of two arrays
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;

public class Q03FindDuplicates {

    /* ---- BEST FOR INTERVIEW : Set.add() returns false on a repeat ---- */
    static Set<Integer> duplicates(List<Integer> nums) {
        Set<Integer> seen = new HashSet<>();
        return nums.stream()
                .filter(n -> !seen.add(n))      // stateful lambda: sequential streams only
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /* ---- WHEN YOU ALSO NEED THE COUNTS ---- */
    static Map<Integer, Long> duplicateCounts(List<Integer> nums) {
        return nums.stream()
                .collect(Collectors.groupingBy(n -> n, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /* ---- SAME IDEA ON CHARACTERS, WITHOUT STREAMS ---- */
    static Set<Character> duplicateChars(String s) {
        Set<Character> seen = new HashSet<>(), dup = new LinkedHashSet<>();
        for (char c : s.toCharArray()) if (!seen.add(c)) dup.add(c);
        return dup;
    }

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 2, 3, 2, 5, 1, 7, 2);
        System.out.println("input      : " + nums);
        System.out.println("duplicates : " + duplicates(nums));
        System.out.println("with counts: " + duplicateCounts(nums));
        System.out.println("dup chars  : " + duplicateChars("programming"));

        // The parallel trap the interviewer asks about - shared mutable Set, lost updates.
        Set<Integer> seen = Collections.newSetFromMap(new HashMap<>());
        long parallelSize = nums.parallelStream().filter(n -> !seen.add(n)).count();
        System.out.println("parallel count (UNRELIABLE by design): " + parallelSize
                + "   -- sequential answer is " + duplicates(nums).size());
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * input      : [1, 2, 3, 2, 5, 1, 7, 2]
 * duplicates : [2, 1]
 * with counts: {1=2, 2=3}
 * dup chars  : [r, m, g]
 * parallel count (UNRELIABLE by design): 3   -- sequential answer is 2
 * ============================================================================================== */
