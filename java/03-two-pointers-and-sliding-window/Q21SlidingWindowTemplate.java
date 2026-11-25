/* ==============================================================================================
 * Q21  .  Sliding window -- the template that covers a dozen problems
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Two Pointers & Sliding Window
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- "longest substring without repeating" sabse zyada poocha jata hai
 * TECHNIQUE    : Sliding window: expand right, shrink while invalid, record
 *
 * PROBLEM
 *   Longest substring without repeating characters. Then at most K distinct characters.
 *   Then minimum window substring containing all of t.
 *
 * ALGORITHM
 *   1. Expand: take s[right] into the window and update the window state.
 *   2. Shrink: WHILE the window violates the constraint, remove s[left] and advance left.
 *   3. Record: the window is now valid, so update the best answer.
 *   4. Every window problem reuses this skeleton - only the definition of 'invalid'
 *      changes.
 *   5. For distinct characters, left can jump to lastSeen[c] + 1 instead of crawling.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Generate all substrings and check each         O(n^3)        O(n)      avoid
 *   Every start, extend while valid                O(n^2)        O(n)      avoid
 *       > The honest brute force. Name it, then improve.
 *   Sliding window with a HashMap                  O(n)          O(k)      WRITE THIS
 *       > The template: expand right, shrink while invalid, record.
 *   Window with a lastSeen index jump              O(n)          O(1)      WRITE THIS
 *       > For unique-characters, left jumps rather than crawls.
 *
 * WHY THIS ONE
 *   This is the highest-leverage template in the entire coding round. Write the three-step
 *   skeleton once, out loud -- expand, shrink-while-invalid, record -- and every window
 *   question becomes a different definition of "invalid". size > k, missing > 0, sum >
 *   target: same code, different condition.
 *
 * SAMJHO
 *   Teen step yaad rakho: right se andar lo, jab tak window invalid hai left se nikalo,
 *   phir answer update karo. Har problem mein sirf "invalid" ki definition badalti hai.
 *   Minimum window mein ulta hai: jab tak window VALID hai, record karo aur shrink karo.
 *
 * AISE BOLO  (30-40 sec)
 *   "The brute force tries every start and extends while the substring stays valid, which
 *   is O(n^2). I use a sliding window instead: expand the right end, shrink from the left
 *   while the window breaks the rule, then record the answer. Each character enters and
 *   leaves the window at most once, so it is O(n) time and O(k) space for the counts. For
 *   unique characters I store the last index of each character so the left pointer jumps
 *   instead of crawling. The catch is that this only works when shrinking always moves
 *   the window back towards valid."
 *
 * TRAP
 *   ! Jump wale version mein lastSeen[c] >= left check zaroori hai; warna left peeche chala
 *     jata hai ("abba" pe 2 ki jagah 3). int[128] sirf ASCII input ke liye hai.
 *   ! Maximum wale problem mein record shrink ke BAAD; minimum window mein shrink loop ke
 *     ANDAR. Jagah badli to answer galat.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Write the generic template on the board, then adapt it to at-most-K-distinct in 30
 *     seconds.
 *     -> Map of counts; invalid = map.size() > k. Shrink mein count 0 ho jaye to key remove.
 *        Code: longestWithAtMostKDistinct().
 *   ? Exactly K distinct instead of at most K -- what is the trick?
 *     -> Ginti ke liye: exactly(k) = atMost(k) - atMost(k - 1). Longest ke liye at-most-k
 *        window hi chalao, aur record sirf tab karo jab map.size() == k.
 *   ? Why can't you use a sliding window for "subarray sums to K" with negative numbers?
 *     -> Negative se sum monotonic nahi rehta; shrink karne se sum badh bhi sakta hai. Wahan
 *        prefix sum + HashMap lagta hai (Q23SubarraySumEqualsK).
 *   ? Minimum window substring -- what is the shrink condition?
 *     -> Jab tak missing == 0 (t ke saare chars window mein), record karo aur left badhao.
 *        Kisi needed char ka need 0 se 1 hua to missing++, aur loop ruk jata hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - At most K distinct characters
 *   - Minimum window substring
 *   - Longest repeating character replacement
 *   - Fruit into baskets
 *   - Permutation in string
 *   - Max consecutive ones III
 *
 * APNI STORY SE JODO
 *   "I have used a rate limiter at work; a sliding-window limiter is this same idea over
 *   request timestamps. [fill: which algorithm ours used, token bucket or sliding window]"
 *   Note: window = last N seconds ke requests; purane evict karo, phir size ko limit se
 *   compare karo. Code: Q43RateLimiter.
 *
 * VARIANTS     : LC 3, LC 159, LC 340, LC 76, LC 424, LC 904, LC 567, LC 1004, LC 992
 * ============================================================================================== */

import java.util.*;

public class Q21SlidingWindowTemplate {

    /* ======================================================================
       THE TEMPLATE.  Memorise these three steps and every window problem
       becomes a different definition of "invalid".
         1. EXPAND  - take s[right] into the window
         2. SHRINK  - WHILE the window is invalid, drop s[left++]
         3. RECORD  - the window is valid now, update the answer
       ====================================================================== */
    static int longestWithAtMostKDistinct(String s, int k) {
        Map<Character, Integer> win = new HashMap<>();
        int best = 0, left = 0;
        for (int right = 0; right < s.length(); right++) {
            win.merge(s.charAt(right), 1, Integer::sum);                  // 1 EXPAND
            while (win.size() > k) {                                      // 2 SHRINK
                char lc = s.charAt(left++);
                if (win.merge(lc, -1, Integer::sum) == 0) win.remove(lc);
            }
            best = Math.max(best, right - left + 1);                      // 3 RECORD
        }
        return best;
    }

    /* ---- same template, optimised: left JUMPS past the previous occurrence ---- */
    static int longestUnique(String s) {
        int[] lastSeen = new int[128];
        Arrays.fill(lastSeen, -1);
        int best = 0, left = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastSeen[c] >= left) left = lastSeen[c] + 1;
            lastSeen[c] = right;
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    /* ---- same template, "how many still needed" counter ---- */
    static String minWindow(String s, String t) {
        int[] need = new int[128];
        for (char c : t.toCharArray()) need[c]++;
        int missing = t.length(), left = 0, bestLen = Integer.MAX_VALUE, bestStart = 0;

        for (int right = 0; right < s.length(); right++) {
            if (need[s.charAt(right)]-- > 0) missing--;
            while (missing == 0) {
                if (right - left + 1 < bestLen) {
                    bestLen = right - left + 1; bestStart = left;
                }
                if (need[s.charAt(left++)]++ == 0) missing++;
            }
        }
        return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
    }

    /* ---- same template again: longest repeating-character replacement ---- */
    static int characterReplacement(String s, int k) {
        int[] count = new int[26];
        int left = 0, maxCount = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            maxCount = Math.max(maxCount, ++count[s.charAt(right) - 'A']);
            while (right - left + 1 - maxCount > k) count[s.charAt(left++) - 'A']--;
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println("longestUnique abcabcbb      : " + longestUnique("abcabcbb"));
        System.out.println("longestUnique bbbbb         : " + longestUnique("bbbbb"));
        System.out.println("longestUnique pwwkew        : " + longestUnique("pwwkew"));
        System.out.println("atMost2Distinct eceba       : " + longestWithAtMostKDistinct("eceba", 2));
        System.out.println("atMost1Distinct eceba       : " + longestWithAtMostKDistinct("eceba", 1));
        System.out.println("minWindow ADOBECODEBANC/ABC : \"" + minWindow("ADOBECODEBANC", "ABC") + "\"");
        System.out.println("minWindow a/aa              : \"" + minWindow("a", "aa") + "\"  (impossible)");
        System.out.println("charReplacement AABABBA k=1 : " + characterReplacement("AABABBA", 1));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * longestUnique abcabcbb      : 3
 * longestUnique bbbbb         : 1
 * longestUnique pwwkew        : 3
 * atMost2Distinct eceba       : 3
 * atMost1Distinct eceba       : 1
 * minWindow ADOBECODEBANC/ABC : "BANC"
 * minWindow a/aa              : ""  (impossible)
 * charReplacement AABABBA k=1 : 4
 * ============================================================================================== */
