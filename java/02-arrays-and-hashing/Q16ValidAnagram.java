/* ==============================================================================================
 * Q16  .  Valid anagram and character counting
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Arrays & Hashing
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Fixed-alphabet frequency counting with a single +1/-1 array
 *
 * PROBLEM
 *   Are two strings anagrams? Then: can the letters of one build the other (ransom note)?
 *
 * ALGORITHM
 *   1. Reject immediately if the lengths differ.
 *   2. Walk both strings together with one index.
 *   3. Increment the slot for a.charAt(i) and decrement the slot for b.charAt(i).
 *   4. If the strings are anagrams every slot ends at zero.
 *   5. For Unicode or mixed case, swap int[26] for a HashMap of counts.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Sort both and compare                          O(n log n)    O(n)      acceptable
 *       > One line, always correct, any alphabet. Say it first.
 *   HashMap of counts                              O(n)          O(k)      acceptable
 *       > The general answer for Unicode or mixed case.
 *   int[26], +1 for a and -1 for b                 O(n)          O(1)      WRITE THIS
 *       > One pass over both strings, one array, no boxing.
 *
 * WHY THIS ONE
 *   The length check first is free and rejects most inputs. The +1/-1 single-array trick
 *   is what makes this "best" rather than "two arrays and compare" -- it shows you are
 *   thinking about the work, not just correctness. Always ask whether the alphabet is a-z
 *   before committing to int[26].
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Unicode input -- what breaks in the int[26] version?
 *   ? Ignore case and spaces -- where does that change go?
 *   ? Find all anagrams of p inside s (this becomes a sliding window).
 *   ? Group anagrams instead of comparing two.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Group anagrams
 *   - Ransom note
 *   - Permutation in string
 *   - Isomorphic strings
 * ============================================================================================== */

import java.util.*;

public class Q16ValidAnagram {

    /* ---- BEST FOR INTERVIEW : one array, one pass over both strings ---- */
    static boolean isAnagram(String a, String b) {
        if (a.length() != b.length()) return false;      // cheapest rejection first
        int[] count = new int[26];
        for (int i = 0; i < a.length(); i++) {
            count[a.charAt(i) - 'a']++;
            count[b.charAt(i) - 'a']--;
        }
        for (int c : count) if (c != 0) return false;
        return true;
    }

    /* ---- Unicode / mixed case / spaces : a map instead of int[26] ---- */
    static boolean isAnagramGeneral(String a, String b) {
        if (a.length() != b.length()) return false;
        Map<Character, Integer> m = new HashMap<>();
        for (char c : a.toCharArray()) m.merge(c, 1, Integer::sum);
        for (char c : b.toCharArray())
            if (m.merge(c, -1, Integer::sum) < 0) return false;
        return true;
    }

    /* ---- SAME COUNTING SKELETON : can the magazine build the note? ---- */
    static boolean canConstruct(String note, String magazine) {
        int[] have = new int[26];
        for (char c : magazine.toCharArray()) have[c - 'a']++;
        for (char c : note.toCharArray()) if (--have[c - 'a'] < 0) return false;
        return true;
    }

    public static void main(String[] args) {
        System.out.println("listen / silent  : " + isAnagram("listen", "silent"));
        System.out.println("rat    / car     : " + isAnagram("rat", "car"));
        System.out.println("ab     / abb     : " + isAnagram("ab", "abb") + "   (length guard)");
        System.out.println("general Dormitory/DirtyRoom : "
                + isAnagramGeneral("dormitory", "dirtyroom"));
        System.out.println("ransom aa / aab  : " + canConstruct("aa", "aab"));
        System.out.println("ransom aa / ab   : " + canConstruct("aa", "ab"));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * listen / silent  : true
 * rat    / car     : false
 * ab     / abb     : false   (length guard)
 * general Dormitory/DirtyRoom : true
 * ransom aa / aab  : true
 * ransom aa / ab   : false
 * ============================================================================================== */
