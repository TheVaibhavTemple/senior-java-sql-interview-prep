/* ==============================================================================================
 * Q02  .  First non-repeating character
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Two-pass frequency count; order preserved by LinkedHashMap
 *
 * PROBLEM
 *   Return the first character that occurs exactly once. "swiss" -> 'w'.
 *
 * ALGORITHM
 *   1. Pass 1: build character -> count in an order-preserving map.
 *   2. Pass 2: stream the entries in insertion order.
 *   3. Filter to entries whose count is exactly 1.
 *   4. findFirst() short-circuits on the first match.
 *   5. Constant-space variant: int[256] counts, then rescan the original string.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   For each character, rescan the rest            O(n^2)        O(1)      avoid
 *       > The opener you immediately reject.
 *   HashMap counts, then a second pass over the st O(n)          O(k)      acceptable
 *       > Correct, and it handles Unicode.
 *   LinkedHashMap groupingBy + findFirst           O(n)          O(k)      WRITE THIS
 *       > One expression, order preserved by the map supplier.
 *   int[256] counts + second pass                  O(n)          O(1)      acceptable
 *       > Offer this when they ask for constant space.
 *
 * WHY THIS ONE
 *   Insertion order is the entire problem. Answer with a plain HashMap and the result is
 *   non-deterministic -- that is exactly the bug being probed. LinkedHashMap::new as the
 *   second argument of groupingBy is the fix, and naming why you put it there is the
 *   answer.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? What breaks if you use HashMap instead of LinkedHashMap?
 *   ? The input is now an infinite stream -- return the first unique so far, at any
 *     moment.
 *   ? Return the first REPEATING character instead.
 *   ? What about characters outside the BMP (surrogate pairs)?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - First repeating character
 *   - First unique word
 *   - First unique element in a List
 *   - Character appearing an odd number of times
 * ============================================================================================== */

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Q02FirstNonRepeatingCharacter {

    /* ---- BEST FOR INTERVIEW : order-preserving collector, then findFirst ---- */
    static Optional<Character> firstUnique(String s) {
        return s.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(Function.identity(),
                        LinkedHashMap::new,                 // WITHOUT this the answer is random
                        Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() == 1L)
                .map(Map.Entry::getKey)
                .findFirst();                               // short-circuits
    }

    /* ---- THE CONSTANT-SPACE FOLLOW-UP ---- */
    static char firstUniqueFast(String s) {
        int[] count = new int[256];
        for (int i = 0; i < s.length(); i++) count[s.charAt(i)]++;
        for (int i = 0; i < s.length(); i++)
            if (count[s.charAt(i)] == 1) return s.charAt(i);
        return 0;                                           // sentinel: none
    }

    /* ---- WHAT THE BUG LOOKS LIKE : plain HashMap loses the order ---- */
    static Optional<Character> buggyWithHashMap(String s) {
        return s.chars().mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() == 1L)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public static void main(String[] args) {
        for (String s : new String[]{"swiss", "aabbcc", "racecars", "leetcode"}) {
            char fast = firstUniqueFast(s);
            System.out.printf("%-10s stream=%-7s array=%s%n",
                    s, firstUnique(s).map(String::valueOf).orElse("none"),
                    fast == 0 ? "none" : String.valueOf(fast));
        }
        // HashMap version happens to agree here, but the order is an accident of hashing
        System.out.println("hashmap version on \"swiss\" -> " + buggyWithHashMap("swiss").orElse(null)
                + "   (correct by luck, not by contract)");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * swiss      stream=w       array=w
 * aabbcc     stream=none    array=none
 * racecars   stream=e       array=e
 * leetcode   stream=l       array=l
 * hashmap version on "swiss" -> w   (correct by luck, not by contract)
 * ============================================================================================== */
