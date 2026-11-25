/* ==============================================================================================
 * Q23  .  Subarray sum equals K -- prefix sum plus HashMap
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Arrays & Hashing
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
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
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why prefixCount.put(0, 1) before the loop?
 *   ? Why does a sliding window work for positive-only arrays but not here?
 *   ? Longest subarray summing to k instead of the count -- what changes in the map?
 *   ? Subarrays divisible by k -- what is the key now?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Contiguous array (equal 0s and 1s)
 *   - Subarray sums divisible by K
 *   - Longest subarray with sum K
 *   - Count nice subarrays
 *   - Binary subarrays with sum
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
