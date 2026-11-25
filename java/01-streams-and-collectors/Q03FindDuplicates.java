/* ==============================================================================================
 * Q03  .  Find duplicates in a collection
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- "duplicates nikalo" streams ka sabse common sawal hai
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
 * SAMJHO
 *   Set.add() false deta hai jab element pehle se ho. Isi ko filter mein lagao to sirf repeat
 *   wale bachte hain. Result Set mein collect karo, taaki 3 baar wala ek hi baar dikhe. Count
 *   bhi chahiye to groupingBy + counting aur value > 1 filter.
 *
 * AISE BOLO  (30-40 sec)
 *   "A nested loop is O(n^2), and sorting costs O(n log n) and loses the input order. I keep a
 *   HashSet and filter with not seen.add, because add returns false for an element that is
 *   already there, then collect into a LinkedHashSet so each duplicate shows once. That is
 *   O(n) time and O(n) space. The lambda mutates shared state, so I would not run it on a
 *   parallel stream; for counts or parallel work I use groupingBy with counting."
 *
 * TRAP
 *   ! filter(n -> !seen.add(n)) stateful lambda hai; parallelStream pe HashSet race karta hai.
 *     Interviewer ke puchhne se pehle khud bolo.
 *   ! count() lagaya to repeat occurrences ginta hai (yahan 3), distinct duplicates nahi (2).
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why is filter(n -> !seen.add(n)) unsafe in a parallel stream?
 *     -> HashSet thread-safe nahi: do thread same value ek saath add karein to dono ko true mil
 *        sakta hai, ya map corrupt. Stream ka rule bhi hai ki lambda stateless ho.
 *   ? Find elements appearing exactly twice.
 *     -> Set wali trick count nahi deti. duplicateCounts() jaisa groupingBy + counting lo aur
 *        filter(e -> e.getValue() == 2).
 *   ? The list has 10^8 elements and will not fit in memory -- now what?
 *     -> hash(value) % N se N files mein baanto; same value hamesha same file mein jati hai,
 *        phir har file pe HashSet. Ya DB mein GROUP BY value HAVING COUNT(*) > 1.
 *   ? Find the duplicate without extra space, given values in 1..n.
 *     -> Index ko marker banao: nums[|x|-1] negative karo; pehle se negative mile to x
 *        duplicate. Array badal nahi sakte aur ek hi duplicate ho to Floyd cycle (LC 287).
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Duplicate characters in a string
 *   - Contains duplicate within distance K
 *   - Find all duplicates in 1..n
 *   - Intersection of two arrays
 *
 * VARIANTS     : LC 217, LC 219, LC 287, LC 349, LC 442
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
        // Compare the SAME pipeline both ways: count() counts repeat occurrences (1 once,
        // 2 twice = 3), not distinct duplicates (2). A match on 8 elements is luck.
        Set<Integer> seen = Collections.newSetFromMap(new HashMap<>());
        long parallelSize = nums.parallelStream().filter(n -> !seen.add(n)).count();
        Set<Integer> seqSeen = new HashSet<>();
        long sequentialSize = nums.stream().filter(n -> !seqSeen.add(n)).count();
        System.out.println("parallel count (UNRELIABLE by design): " + parallelSize
                + "   -- sequential count is " + sequentialSize + " (a match here is luck)");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * input      : [1, 2, 3, 2, 5, 1, 7, 2]
 * duplicates : [2, 1]
 * with counts: {1=2, 2=3}
 * dup chars  : [r, m, g]
 * parallel count (UNRELIABLE by design): 3   -- sequential count is 3 (a match here is luck)
 * ============================================================================================== */
