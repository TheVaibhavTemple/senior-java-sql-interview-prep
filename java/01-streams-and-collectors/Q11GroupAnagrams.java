/* ==============================================================================================
 * Q11  .  Group anagrams
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- product rounds mein LeetCode style aata hai; groupingBy ki achhi practice
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
 * SAMJHO
 *   Saare anagrams ka ek common "signature" hota hai. Characters sort karo ("eat" -> "aet") ya
 *   26 counts ka string banao, aur usi ko map ki key banao. groupingBy(signature) se saare
 *   anagram ek bucket mein aa jaate hain; map.values() hi jawab hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "Comparing every pair with an anagram check is O(n squared times k), so I avoid it.
 *   Instead I give every word a signature, its characters sorted, and group by that key with
 *   groupingBy, which is O(n k log k) time and O(n k) space. If words are long, I switch to a
 *   26-slot count signature, which makes it O(n k). The count version only works for a to z,
 *   so for uppercase or Unicode I would sort code points or keep a map of counts instead."
 *
 * TRAP
 *   ! char[] ya int[] ko seedha key mat banao: array ka equals/hashCode identity pe chalta hai,
 *     har word alag group banega. Pehle String banao.
 *   ! Count signature mein separator ('#') zaroori hai, warna counts [1, 11] aur [11, 1] dono
 *     "111" ban ke ek hi key de dete hain.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Words contain Unicode, not just a-z -- does your signature still work?
 *     -> int[26] wala nahi: uppercase ya Unicode pe ArrayIndexOutOfBounds. codePoints() sort
 *        karke signature banao ya Map<Integer, Long> counts lo; case ignore karna ho to lowercase.
 *   ? Just detect whether two strings are anagrams -- what is the cheapest check first?
 *     -> Pehle length compare, O(1) mein reject. Phir ek int[26] mein ek string se ++ aur
 *        doosri se --; sab zero to anagram, O(k). isAnagram() dekho.
 *   ? Find all anagrams of a pattern inside a long string (this becomes a sliding window).
 *     -> Pattern length ki fixed window aur do count arrays; har step ek char add, ek remove,
 *        counts match hon to index note karo. O(n) time, O(1) space (26 slots).
 *   ? Group 100 million words that do not fit in memory.
 *     -> Signature ke hash se words ko N files mein partition karo (ek signature hamesha ek hi
 *        file mein), phir har file ko memory mein group karo. Map-reduce wala idea hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Valid anagram check
 *   - Find all anagram substrings
 *   - Group by canonical form
 *   - Group files by content hash
 *   - Isomorphic strings
 *
 * VARIANTS     : LC 49 (group anagrams), LC 242 (valid anagram), LC 438, LC 567, LC 205
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
