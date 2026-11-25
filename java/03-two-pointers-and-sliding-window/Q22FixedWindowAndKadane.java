/* ==============================================================================================
 * Q22  .  Fixed window and Kadane
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Two Pointers & Sliding Window
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Fixed-size window slide; Kadane's extend-or-restart decision
 *
 * PROBLEM
 *   Maximum sum of exactly k consecutive elements. Then maximum subarray sum of any
 *   length. Then return the indices too.
 *
 * ALGORITHM
 *   1. Fixed window: sum the first k elements, then for each step add a[i] and subtract
 *      a[i-k].
 *   2. Kadane: at index i decide whether a[i] alone beats the running sum plus a[i].
 *   3. If a[i] alone is larger, the best subarray restarts at i.
 *   4. Track the global best separately from the running best.
 *   5. Record the restart index to report the subarray bounds, not just the sum.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Recompute the sum for every window             O(nk)         O(1)      avoid
 *   Prefix sums, then differences                  O(n)          O(n)      acceptable
 *       > Works; unnecessary memory for a fixed window.
 *   Slide: add the new, drop the old               O(n)          O(1)      WRITE THIS
 *       > The fixed-window idiom.
 *   Kadane: extend or restart                      O(n)          O(1)      WRITE THIS
 *       > The variable-length answer. Two variables, one pass.
 *
 * WHY THIS ONE
 *   Kadane is one decision repeated n times: is a[i] alone better than the run so far plus
 *   a[i]? If yes, the run restarts at i. Tracking the restart index turns it into the
 *   "return the subarray" version, which is almost always the follow-up. Best-time-to-buy-
 *   and-sell is the same loop with min instead of a running sum.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Return the subarray, not the sum.
 *   ? All numbers are negative -- what does your code return, and is that right?
 *   ? Maximum PRODUCT subarray -- why can't you reuse Kadane directly?
 *   ? Circular subarray, where the array wraps around.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Maximum subarray
 *   - Best time to buy and sell stock
 *   - Maximum average subarray
 *   - Max product subarray
 *   - Circular maximum subarray
 * ============================================================================================== */

import java.util.*;

public class Q22FixedWindowAndKadane {

    /* ---- FIXED window : slide, never re-add ---- */
    static int maxSumOfSizeK(int[] a, int k) {
        int sum = 0;
        for (int i = 0; i < k; i++) sum += a[i];
        int best = sum;
        for (int i = k; i < a.length; i++) {
            sum += a[i] - a[i - k];          // add the new, drop the old
            best = Math.max(best, sum);
        }
        return best;
    }

    /* ---- VARIABLE window : Kadane. Extend the run, or restart at i? ---- */
    static int maxSubArray(int[] a) {
        int best = a[0], cur = a[0];
        for (int i = 1; i < a.length; i++) {
            cur  = Math.max(a[i], cur + a[i]);
            best = Math.max(best, cur);
        }
        return best;
    }

    /* ---- Kadane that also reports WHERE : the usual follow-up ---- */
    static int[] maxSubArrayRange(int[] a) {
        int best = a[0], cur = a[0], s = 0, bs = 0, be = 0;
        for (int i = 1; i < a.length; i++) {
            if (cur + a[i] < a[i]) { cur = a[i]; s = i; }   // restart here
            else cur += a[i];
            if (cur > best) { best = cur; bs = s; be = i; }
        }
        return new int[]{best, bs, be};
    }

    /* ---- same skeleton, different state : best single buy/sell ---- */
    static int maxProfit(int[] price) {
        int minSoFar = Integer.MAX_VALUE, best = 0;
        for (int p : price) {
            minSoFar = Math.min(minSoFar, p);
            best = Math.max(best, p - minSoFar);
        }
        return best;
    }

    public static void main(String[] args) {
        int[] a = {2, 1, 5, 1, 3, 2};
        System.out.println("maxSumOfSizeK(3)      : " + maxSumOfSizeK(a, 3));

        int[] k = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        System.out.println("maxSubArray           : " + maxSubArray(k));
        int[] r = maxSubArrayRange(k);
        System.out.println("  with indices        : sum=" + r[0] + " from " + r[1] + " to " + r[2]
                + " -> " + Arrays.toString(Arrays.copyOfRange(k, r[1], r[2] + 1)));

        int[] allNeg = {-5, -2, -9};
        System.out.println("all negative          : " + maxSubArray(allNeg)
                + "   (the least-bad single element, which is correct)");

        System.out.println("maxProfit             : " + maxProfit(new int[]{7, 1, 5, 3, 6, 4}));
        System.out.println("maxProfit (declining) : " + maxProfit(new int[]{7, 6, 4, 3, 1}));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * maxSumOfSizeK(3)      : 9
 * maxSubArray           : 6
 *   with indices        : sum=6 from 3 to 6 -> [4, -1, 2, 1]
 * all negative          : -2   (the least-bad single element, which is correct)
 * maxProfit             : 5
 * maxProfit (declining) : 0
 * ============================================================================================== */
