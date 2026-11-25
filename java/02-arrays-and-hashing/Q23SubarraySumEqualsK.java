/* ==============================================================================================
 * Q23  .  Subarray sum equals K -- prefix sum plus HashMap
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Arrays & Hashing
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- product aur fintech rounds mein common; prefix sum ka core template
 * TECHNIQUE    : Prefix sum plus a hash map of prefix counts
 *
 * PROBLEM
 *   Count the subarrays summing to k, with negative numbers allowed. Then the longest
 *   subarray with equal 0s and 1s.
 *
 * ALGORITHM
 *   1. Maintain a running prefix sum while scanning.
 *   2. If prefix[j] - prefix[i] == k then the subarray after i up to j sums to k.
 *   3. So at each j, look up how many earlier prefixes equal sum - k.
 *   4. Seed the map with (0 -> 1) so subarrays starting at index 0 are counted.
 *   5. Record the current prefix and continue. Works with negatives; a sliding window does
 *      not.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   All subarrays, sum each                        O(n^3)        O(1)      avoid
 *   All subarrays with a running sum               O(n^2)        O(1)      acceptable
 *       > Acceptable for small n. Name it, then improve.
 *   Sliding window                                 O(n)          O(1)      avoid
 *       > WRONG with negatives -- the window cannot decide when to shrink. This is the
 *       > trap.
 *   Prefix sum + HashMap of counts                 O(n)          O(n)      WRITE THIS
 *       > If prefix[j] - prefix[i] == k then that subarray sums to k.
 *
 * WHY THIS ONE
 *   Seeding the map with (0 -> 1) is the line people forget, and it is what lets subarrays
 *   starting at index 0 be counted. The second thing to know cold is why a sliding window
 *   fails: with negatives, growing the window can decrease the sum, so "shrink while too
 *   big" is not a valid rule. Saying that unprompted is a strong signal.
 *
 * SAMJHO
 *   Running sum rakho. Agar (sum - k) pehle kabhi prefix tha, to beech ka subarray k hai.
 *   Map mein "yeh prefix sum kitni baar aaya" rakho; har step pe count += map[sum - k].
 *   Map ko (0 -> 1) se seed karo, warna index 0 se shuru hone wale subarray chhoot jaate.
 *   Negative numbers hon to sliding window nahi chalta; yahi prefix sum ka kaam hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "Brute force fixes every start and extends with a running sum, which is O(n^2). A
 *   sliding window looks tempting, but with negative numbers the sum is not monotonic, so
 *   there is no safe rule for when to shrink. I keep a running prefix sum and a HashMap of
 *   how many times each prefix has appeared, and at each index I add the count of sum
 *   minus k. That is O(n) time and O(n) space, and I seed the map with zero mapped to one
 *   so that subarrays starting at index 0 are counted."
 *
 * TRAP
 *   ! put(0, 1) bhoole to shuru wale subarray miss: [1,1,1], k = 2 pe 2 ki jagah 1 aata hai.
 *   ! Current sum ko lookup se pehle map mein daala to k = 0 pe khali subarray bhi gin loge.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why prefixCount.put(0, 1) before the loop?
 *     -> Khali prefix ka sum 0 hai. Jab sum khud k ho (subarray index 0 se), tab sum - k = 0
 *        dhundha jata hai; seed na ho to wo subarray count nahi hota.
 *   ? Why does a sliding window work for positive-only arrays but not here?
 *     -> Sirf positive mein window badhao to sum badhta, chhota karo to ghatta: monotonic.
 *        Negative aate hi yeh tootta hai, aur "sum > k pe shrink" galat ho jata hai.
 *   ? Longest subarray summing to k instead of the count -- what changes in the map?
 *     -> Count ki jagah prefix ka PEHLA index rakho (putIfAbsent), seed (0 -> -1). Har i pe
 *        best = max(best, i - first[sum - k]). Same shape: findMaxLength().
 *   ? Subarrays divisible by k -- what is the key now?
 *     -> Key = prefix % k: same remainder wale do prefix ke beech ka subarray divisible hai.
 *        Java mein -7 % 5 = -2, isliye ((sum % k) + k) % k. Code: subarraysDivByK().
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Contiguous array (equal 0s and 1s)
 *   - Subarray sums divisible by K
 *   - Longest subarray with sum K
 *   - Count nice subarrays
 *   - Binary subarrays with sum
 *
 * VARIANTS     : LC 560, LC 525, LC 974, LC 523, LC 325, LC 930, LC 1248
 * ============================================================================================== */

import java.util.*;

public class Q23SubarraySumEqualsK {

    /* ---- PREFIX SUM + HASHMAP. Works with negatives; a window does not. ---- */
    static int subarraySum(int[] a, int k) {
        Map<Integer, Integer> prefixCount = new HashMap<>();
        prefixCount.put(0, 1);          // THE line people forget: the empty prefix
        int sum = 0, count = 0;
        for (int v : a) {
            sum += v;
            count += prefixCount.getOrDefault(sum - k, 0);
            prefixCount.merge(sum, 1, Integer::sum);
        }
        return count;
    }

    /* ---- same trick, different key : longest subarray with equal 0s and 1s ---- */
    static int findMaxLength(int[] a) {
        Map<Integer, Integer> firstIndex = new HashMap<>();
        firstIndex.put(0, -1);
        int sum = 0, best = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] == 0 ? -1 : 1);                 // treat 0 as -1
            Integer seenAt = firstIndex.get(sum);
            if (seenAt != null) best = Math.max(best, i - seenAt);
            else firstIndex.put(sum, i);                  // keep the EARLIEST index only
        }
        return best;
    }

    /* ---- same trick, modular key : subarrays divisible by k ---- */
    static int subarraysDivByK(int[] a, int k) {
        int[] count = new int[k];
        count[0] = 1;
        int sum = 0, total = 0;
        for (int v : a) {
            sum = ((sum + v) % k + k) % k;                // normalise negative remainders
            total += count[sum]++;
        }
        return total;
    }

    /* ---- WHY A SLIDING WINDOW FAILS HERE ---- */
    static int brokenSlidingWindow(int[] a, int k) {
        int left = 0, sum = 0, count = 0;
        for (int right = 0; right < a.length; right++) {
            sum += a[right];
            while (sum > k && left <= right) sum -= a[left++];  // invalid rule with negatives
            if (sum == k) count++;
        }
        return count;
    }

    public static void main(String[] args) {
        int[] a = {1, 1, 1};
        System.out.println("subarraySum([1,1,1],2)         : " + subarraySum(a, 2));

        int[] neg = {1, -1, 0};
        System.out.println("subarraySum([1,-1,0],0)        : " + subarraySum(neg, 0));
        System.out.println("  sliding window on same input : " + brokenSlidingWindow(neg, 0)
                + "   <-- WRONG, window cannot handle negatives");

        System.out.println("findMaxLength([0,1,0,1,1,0])   : "
                + findMaxLength(new int[]{0, 1, 0, 1, 1, 0}));
        System.out.println("subarraysDivByK([4,5,0,-2,-3,1],5) : "
                + subarraysDivByK(new int[]{4, 5, 0, -2, -3, 1}, 5));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * subarraySum([1,1,1],2)         : 2
 * subarraySum([1,-1,0],0)        : 3
 *   sliding window on same input : 1   <-- WRONG, window cannot handle negatives
 * findMaxLength([0,1,0,1,1,0])   : 6
 * subarraysDivByK([4,5,0,-2,-3,1],5) : 7
 * ============================================================================================== */
