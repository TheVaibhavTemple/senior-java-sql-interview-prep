/* ==============================================================================================
 * Q24  .  Dutch national flag -- three-way partition
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Two Pointers & Sliding Window
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : Three-way partition with low / mid / high pointers
 *
 * PROBLEM
 *   Sort an array of only 0, 1 and 2 in one pass with constant space. Then move all zeroes
 *   to the end keeping order.
 *
 * ALGORITHM
 *   1. Invariant: [0, low) are 0s, [low, mid) are 1s, (high, end] are 2s, [mid, high]
 *      unexplored.
 *   2. If a[mid] is 0, swap with low and advance BOTH low and mid.
 *   3. If a[mid] is 1, it is already in place - advance mid only.
 *   4. If a[mid] is 2, swap with high and decrement high but do NOT advance mid.
 *   5. mid must stay because the value swapped in from the right has not been examined yet.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Arrays.sort                                    O(n log n)    O(1)      avoid
 *       > Ignores the point of the question.
 *   Count 0s, 1s, 2s, then overwrite               O(n)          O(1)      acceptable
 *       > Two passes. Perfectly valid -- say it, then offer one pass.
 *   Three pointers: low, mid, high                 O(n)          O(1)      WRITE THIS
 *       > One pass. The invariant is the answer.
 *
 * WHY THIS ONE
 *   State the invariant first: everything before low is 0, between low and mid is 1, after
 *   high is 2, and [mid, high] is unexplored. The one bug everyone hits is advancing mid
 *   after swapping with high -- you have not looked at the value that just arrived from
 *   the right, so mid must stay put.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why does mid advance on a 0 but not on a 2?
 *   ? Four colours instead of three -- does this generalise?
 *   ? Is this stable? Does it matter here?
 *   ? Write quicksort's partition step with the same idea.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Sort colors
 *   - Move zeroes
 *   - Segregate even and odd
 *   - Quicksort partition
 *   - Partition around a pivot
 * ============================================================================================== */

import java.util.*;

public class Q24DutchNationalFlag {

    /* ======================================================================
       INVARIANT - state this before writing a line:
         [0, low)      are 0s
         [low, mid)    are 1s
         (high, end]   are 2s
         [mid, high]   unexplored
       ====================================================================== */
    static void sortColors(int[] a) {
        int low = 0, mid = 0, high = a.length - 1;
        while (mid <= high) {
            switch (a[mid]) {
                case 0:  swap(a, low++, mid++); break;   // 0 belongs left
                case 1:  mid++;                 break;   // already in place
                default: swap(a, mid, high--);           // 2 belongs right
                         break;                          // mid does NOT advance:
            }                                            // the incoming value is unseen
        }
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    /* ---- same partition idea, stable : push zeroes to the end ---- */
    static void moveZeroes(int[] a) {
        int write = 0;
        for (int read = 0; read < a.length; read++)
            if (a[read] != 0) a[write++] = a[read];
        while (write < a.length) a[write++] = 0;
    }

    /* ---- and again : segregate even and odd ---- */
    static void segregateEvenOdd(int[] a) {
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            while (lo < hi && a[lo] % 2 == 0) lo++;
            while (lo < hi && a[hi] % 2 != 0) hi--;
            if (lo < hi) swap(a, lo++, hi--);
        }
    }

    /* ---- the bug : advancing mid after swapping with high ---- */
    static void buggySortColors(int[] a) {
        int low = 0, mid = 0, high = a.length - 1;
        while (mid <= high) {
            if (a[mid] == 0)      swap(a, low++, mid++);
            else if (a[mid] == 1) mid++;
            else                  { swap(a, mid++, high--); }   // mid++ is the bug
        }
    }

    public static void main(String[] args) {
        int[] a = {2, 0, 2, 1, 1, 0};
        sortColors(a);
        System.out.println("sortColors        : " + Arrays.toString(a));

        int[] bug = {2, 0, 2, 1, 1, 0};
        buggySortColors(bug);
        System.out.println("buggy version     : " + Arrays.toString(bug)
                + (Arrays.equals(bug, a) ? "" : "   <-- NOT SORTED (mid advanced too early)"));

        int[] z = {0, 1, 0, 3, 12};
        moveZeroes(z);
        System.out.println("moveZeroes        : " + Arrays.toString(z));

        int[] e = {12, 34, 45, 9, 8, 90, 3};
        segregateEvenOdd(e);
        System.out.println("segregateEvenOdd  : " + Arrays.toString(e));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * sortColors        : [0, 0, 1, 1, 2, 2]
 * buggy version     : [0, 0, 1, 1, 2, 2]
 * moveZeroes        : [1, 3, 12, 0, 0]
 * segregateEvenOdd  : [12, 34, 90, 8, 9, 45, 3]
 * ============================================================================================== */
