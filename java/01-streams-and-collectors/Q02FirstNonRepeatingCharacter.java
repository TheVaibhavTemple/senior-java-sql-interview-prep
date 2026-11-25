/* ==============================================================================================
 * Q02  .  First non-repeating character
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- service aur product dono mein bahut common; asli sawal order ka hai
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
 *   HashMap counts, then rescan the string         O(n)          O(k)      acceptable
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
 * SAMJHO
 *   Pehle har character ka count nikalo, order ke saath. Phir usi order mein chalo, jiska
 *   count 1 hai wahi jawab. Order LinkedHashMap se aata hai; HashMap hash ke hisaab se
 *   ghoomta hai, insertion ke hisaab se nahi.
 *
 * AISE BOLO  (30-40 sec)
 *   "Checking every character against the rest is O(n^2), so I use two passes instead. I
 *   count with groupingBy into a LinkedHashMap, then stream the entries in insertion order,
 *   keep the ones with count one, and call findFirst. That is O(n) time and O(k) space. A
 *   plain HashMap can return the wrong character because it iterates in hash order, and if
 *   they want constant space on ASCII, I use an int[256] and rescan the string."
 *
 * TRAP
 *   ! LinkedHashMap::new bhoole to "swiss" pe luck se 'w' aata hai, par "zab" pe 'a' aata
 *     hai jabki sahi 'z' hai. Insertion order ka contract sirf LinkedHashMap deta hai.
 *   ! Koi unique na ho ("aabbcc") to Optional.empty() do; int[256] ka 0 sentinel ek asli
 *     char bhi ho sakta hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? What breaks if you use HashMap instead of LinkedHashMap?
 *     -> Entries hash order mein aati hain, to findFirst galat char de sakta hai: "zab" pe
 *        'a', sahi 'z'. Code mein buggyWithHashMap() dekho.
 *   ? The input is now an infinite stream -- return the first unique so far, at any
 *     moment.
 *     -> LinkedHashSet unique aur HashSet repeated rakho. Naya char unique mein; dobara aaye
 *        to unique se hata ke repeated mein; repeated wala ignore. Jawab = unique ka pehla.
 *   ? Return the first REPEATING character instead.
 *     -> Ek pass, HashSet: jis char pe seen.add() false de, wahi jawab. "Pehla char jiska
 *        count > 1" chahiye to yahi map lo aur filter count > 1.
 *   ? What about characters outside the BMP (surrogate pairs)?
 *     -> chars() emoji ko do surrogate units mein todta hai, dono alag gine jaate hain.
 *        codePoints() aur Map<Integer, Long> lo; int[256] wala wahan chalega hi nahi.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - First repeating character
 *   - First unique word
 *   - First unique element in a List
 *   - Character appearing an odd number of times
 *
 * JAVA 21 MEIN
 *   LinkedHashSet ab SequencedSet hai: stream wale follow-up mein jawab unique.getFirst()
 *   se milta hai (pehle iterator().next()). Set khaali ho to yeh NoSuchElementException deta.
 *
 * VARIANTS     : LC 387; classic -- first repeating character, first unique in a stream
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
