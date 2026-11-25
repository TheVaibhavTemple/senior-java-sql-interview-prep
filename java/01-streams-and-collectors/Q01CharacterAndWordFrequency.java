/* ==============================================================================================
 * Q01  .  Character and word frequency count
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- lagbhag har round mein aata hai; groupingBy yahin se test hota hai
 * TECHNIQUE    : Hash-map counting + Collectors.groupingBy with a map supplier and a downstream collector
 *
 * PROBLEM
 *   Given a string, return how many times each character occurs. Then the same for words
 *   in a paragraph, and the top 3 by frequency.
 *
 * ALGORITHM
 *   1. Stream the characters (String.chars() gives an IntStream, so mapToObj back to
 *      Character).
 *   2. Classify by identity so each distinct character becomes a key.
 *   3. Pass LinkedHashMap::new as the map supplier so first-seen order survives.
 *   4. Use Collectors.counting() as the downstream collector to fold each group to a count.
 *   5. For top-N, stream the entrySet, sort by value descending, limit(n), map to key.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Nested loop, rescan for each character         O(n^2)        O(1)      avoid
 *       > Name it and discard it. Writing this at 8 years is a red flag.
 *   int[256] counts                                O(n)          O(1)      acceptable
 *       > Fastest, no boxing -- but ASCII only.
 *   HashMap with merge / getOrDefault              O(n)          O(k)      acceptable
 *       > The plain-Java answer. Works for any key type.
 *   Stream groupingBy + counting                   O(n)          O(k)      WRITE THIS
 *       > The three-argument groupingBy is literally what is being tested.
 *
 * WHY THIS ONE
 *   The interviewer is checking whether you know the three-argument groupingBy:
 *   classifier, map supplier, downstream collector. Reach for the int[] version only when
 *   they ask you to optimise, and say out loud why you are switching.
 *
 * SAMJHO
 *   Har character ko key banao aur uska count value. groupingBy(key, LinkedHashMap::new,
 *   counting()) ek hi line mein yahi karta hai. LinkedHashMap isliye, taaki pehli baar aane
 *   wala order bana rahe. Top N ke liye entries ko value se ulta sort karo aur limit(n).
 *
 * AISE BOLO  (30-40 sec)
 *   "The brute force rescans the string for every character, which is O(n^2), so I skip it.
 *   I stream the characters and use the three-argument groupingBy: identity as the key,
 *   LinkedHashMap::new to keep first-seen order, and counting() as the downstream. That is
 *   O(n) time and O(k) space for k distinct characters. If they want raw speed on ASCII, an
 *   int[256] avoids boxing, and for emoji I switch chars() to codePoints()."
 *
 * TRAP
 *   ! chars() UTF-16 units deta hai, emoji do units mein toot jata hai; Unicode ho to
 *     codePoints() lo. int[256] wala version char > 255 pe exception deta hai.
 *   ! Words mein split("\\s+") se pehle trim() aur toLowerCase() nahi kiya to leading space
 *     se "" word aata hai, aur "The" aur "the" alag gine jaate hain.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why does counting() return Long and not Integer?
 *     -> Stream.count() bhi long deta hai; elements Integer.MAX_VALUE se zyada ho sakte hain.
 *        Integer hi chahiye to Collectors.summingInt(c -> 1) lagao.
 *   ? How do you get a TreeMap sorted by character instead?
 *     -> Sirf map supplier badlo: groupingBy(identity(), TreeMap::new, counting()).
 *   ? Same problem on a 10 GB file that does not fit in memory -- what changes?
 *     -> Files.lines() se lazy padho (try-with-resources); map mein sirf distinct keys rehti
 *        hain. Words bhi fit na hon to hash(word) % N se N files banao, har file alag gino.
 *   ? Is this correct on a parallelStream? Why?
 *     -> Haan, order bhi. Har thread apna alag map banata hai aur combiner merge karta hai,
 *        shared state nahi. Race tab hota jab forEach se ek shared HashMap update karte.
 *   ? Now do it without Collectors.groupingBy.
 *     -> Loop mein map.merge(c, 1, Integer::sum), ya toMap(identity(), c -> 1L, Long::sum,
 *        LinkedHashMap::new). Dono O(n).
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Word frequency in a paragraph
 *   - Find duplicate characters
 *   - Top K frequent words
 *   - Anagram check
 *   - Count by any object field
 *
 * VARIANTS     : LC 242, LC 347, LC 451, LC 692
 * ============================================================================================== */

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Q01CharacterAndWordFrequency {

    /* ---- BEST FOR INTERVIEW : groupingBy + map supplier + counting ---- */
    static Map<Character, Long> charFrequency(String input) {
        return input.chars()                                 // IntStream
                .mapToObj(c -> (char) c)                     // Stream<Character>
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        LinkedHashMap::new,                  // keeps first-seen order
                        Collectors.counting()));
    }

    static Map<String, Long> wordFrequency(String text) {
        return Arrays.stream(text.split("\\s+"))
                .collect(Collectors.groupingBy(w -> w, LinkedHashMap::new, Collectors.counting()));
    }

    static List<String> topN(Map<String, Long> freq, int n) {
        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))   // deterministic ties
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /* ---- OPTIMISATION TO OFFER : no streams, no boxing, O(1) space for ASCII ---- */
    static int[] frequencyArray(String s) {
        int[] count = new int[256];
        for (int i = 0; i < s.length(); i++) count[s.charAt(i)]++;
        return count;
    }

    public static void main(String[] args) {
        System.out.println("char freq  : " + charFrequency("programming"));

        String text = "the quick brown fox jumps over the lazy dog the end";
        Map<String, Long> words = wordFrequency(text);
        System.out.println("word freq  : " + words);
        System.out.println("top 3 words: " + topN(words, 3));

        int[] c = frequencyArray("programming");
        System.out.println("array form : g=" + c['g'] + " m=" + c['m'] + " r=" + c['r'] + " z=" + c['z']);
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * char freq  : {p=1, r=2, o=1, g=2, a=1, m=2, i=1, n=1}
 * word freq  : {the=3, quick=1, brown=1, fox=1, jumps=1, over=1, lazy=1, dog=1, end=1}
 * top 3 words: [the, brown, dog]
 * array form : g=2 m=2 r=2 z=0
 * ============================================================================================== */
