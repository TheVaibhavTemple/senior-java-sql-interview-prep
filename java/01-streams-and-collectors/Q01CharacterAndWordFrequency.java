/* ==============================================================================================
 * Q01  .  Character and word frequency count
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
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
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why does counting() return Long and not Integer?
 *   ? How do you get a TreeMap sorted by character instead?
 *   ? Same problem on a 10 GB file that does not fit in memory -- what changes?
 *   ? Is this correct on a parallelStream? Why?
 *   ? Now do it without Collectors.groupingBy.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Word frequency in a paragraph
 *   - Find duplicate characters
 *   - Top K frequent words
 *   - Anagram check
 *   - Count by any object field
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
