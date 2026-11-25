/* ==============================================================================================
 * Q11  .  Group anagrams
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : Canonical signature as a hash key (sorted characters, or a 26-slot count signature)
 *
 * PROBLEM
 *   Group words so all anagrams land together. ["eat","tea","tan","ate","nat","bat"] ->
 *   [[eat,tea,ate],[tan,nat],[bat]].
 *
 * ALGORITHM
 *   1. Every anagram class shares one canonical form - its signature.
 *   2. Sorted-characters signature: toCharArray, Arrays.sort, new String - O(k log k) per
 *      word.
 *   3. Count signature: int[26] of character counts rendered as a string - O(k) per word.
 *   4. groupingBy(signature) buckets all words sharing a signature.
 *   5. Return map.values() for the groups.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Compare every pair with an isAnagram check     O(n^2k)       O(1)      avoid
 *   Sorted characters as the group key             O(nk log k)   O(nk)     WRITE THIS
 *       > Short, obviously correct, self-explaining key.
 *   26-slot count array as the group key           O(nk)         O(nk)     acceptable
 *       > Faster for long words. Offer it when they push.
 *
 * WHY THIS ONE
 *   The insight is that every anagram class needs a canonical form -- a signature. Once
 *   you name it "signature", the count-array optimisation is obvious and so are the
 *   variants: group by sorted digits, group files by content hash. Sorting is O(k log k)
 *   per word, counting is O(k).
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Words contain Unicode, not just a-z -- does your signature still work?
 *   ? Just detect whether two strings are anagrams -- what is the cheapest check first?
 *   ? Find all anagrams of a pattern inside a long string (this becomes a sliding window).
 *   ? Group 100 million words that do not fit in memory.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Valid anagram check
 *   - Find all anagram substrings
 *   - Group by canonical form
 *   - Group files by content hash
 *   - Isomorphic strings
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;

public class Q11GroupAnagrams {

    /* ---- BEST FOR INTERVIEW : sorted characters as the canonical signature ---- */
    static String sortedSignature(String w) {
        char[] c = w.toCharArray();
        Arrays.sort(c);
        return new String(c);                   // O(k log k)
    }

    /* ---- THE OPTIMISATION : count signature, O(k) ---- */
    static String countSignature(String w) {
        int[] count = new int[26];
        for (char c : w.toCharArray()) count[c - 'a']++;
        StringBuilder sb = new StringBuilder(52);
        for (int i = 0; i < 26; i++) sb.append('#').append(count[i]);
        return sb.toString();
    }

    static Collection<List<String>> group(List<String> words, boolean fast) {
        return words.stream()
                .collect(Collectors.groupingBy(
                        w -> fast ? countSignature(w) : sortedSignature(w),
                        LinkedHashMap::new, Collectors.toList()))
                .values();
    }

    /* ---- the cheap two-string check, for comparison ---- */
    static boolean isAnagram(String a, String b) {
        if (a.length() != b.length()) return false;         // free rejection
        int[] c = new int[26];
        for (int i = 0; i < a.length(); i++) { c[a.charAt(i) - 'a']++; c[b.charAt(i) - 'a']--; }
        for (int x : c) if (x != 0) return false;
        return true;
    }

    public static void main(String[] args) {
        List<String> words = Arrays.asList("eat", "tea", "tan", "ate", "nat", "bat");
        System.out.println("input            : " + words);
        System.out.println("sorted signature : " + group(words, false));
        System.out.println("count  signature : " + group(words, true));
        System.out.println("sig(\"eat\")       : " + sortedSignature("eat"));
        System.out.println("isAnagram(listen,silent) : " + isAnagram("listen", "silent"));
        System.out.println("isAnagram(hello,world)   : " + isAnagram("hello", "world"));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * input            : [eat, tea, tan, ate, nat, bat]
 * sorted signature : [[eat, tea, ate], [tan, nat], [bat]]
 * count  signature : [[eat, tea, ate], [tan, nat], [bat]]
 * sig("eat")       : aet
 * isAnagram(listen,silent) : true
 * isAnagram(hello,world)   : false
 * ============================================================================================== */
