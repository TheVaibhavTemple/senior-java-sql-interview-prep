/* ==============================================================================================
 * Q22  .  Fixed window and Kadane
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Two Pointers & Sliding Window
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- Kadane aur fixed window dono service aur product rounds mein common
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
 * SAMJHO
 *   Fixed window: pehle k ka sum, phir har step naya add aur purana minus. Dobara mat jodo.
 *   Kadane: har index pe ek hi sawaal: purane run mein judun ya yahin se naya shuru karun?
 *   cur = max(a[i], cur + a[i]); best alag rakho. Restart index note karo to range milti hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "For a fixed window of size k, recomputing every window is O(n*k), so I sum the first
 *   window once and then add the new element and drop the old one: O(n) time, O(1) space.
 *   For the maximum subarray of any length, checking all subarrays is O(n^2); Kadane
 *   decides at each index whether to extend the current run or restart there, in one pass
 *   with two variables. I seed best with the first element, not zero, so an all-negative
 *   array returns its largest element instead of a wrong zero."
 *
 * TRAP
 *   ! best = 0 se seed kiya to all-negative array pe 0 aata hai, jo galat hai. a[0] se seed.
 *   ! Empty array (a[0]) aur k > n pe ArrayIndexOutOfBounds; input pehle validate karo.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Return the subarray, not the sum.
 *     -> Restart pe start index s = i note karo; best update ho to (s, i) save karo.
 *        Code: maxSubArrayRange().
 *   ? All numbers are negative -- what does your code return, and is that right?
 *     -> Sabse bada (least negative) element, jaise [-5,-2,-9] pe -2. Sahi hai jab subarray
 *        non-empty hona chahiye; empty allowed ho to answer 0.
 *   ? Maximum PRODUCT subarray -- why can't you reuse Kadane directly?
 *     -> Negative x negative = positive, to aaj ka min kal ka max ban sakta hai. Har step pe
 *        max aur min dono track karo; a[i] negative ho to dono swap. O(n).
 *   ? Circular subarray, where the array wraps around.
 *     -> Answer = max(normal Kadane, total - minimum subarray). Sab negative hon to total -
 *        min = 0 (khali) aata hai; tab normal Kadane hi lautao.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Maximum subarray
 *   - Best time to buy and sell stock
 *   - Maximum average subarray
 *   - Max product subarray
 *   - Circular maximum subarray
 *
 * VARIANTS     : LC 53, LC 121, LC 643, LC 152, LC 918
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
