/* ==============================================================================================
 * Q24  .  Dutch national flag -- three-way partition
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Two Pointers & Sliding Window
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- "0s, 1s, 2s sort karo" aur move zeroes service rounds mein bhi aate
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
 * SAMJHO
 *   Teen pointer: low ke pehle sab 0, low se mid tak 1, high ke baad sab 2.
 *   a[mid] 0 hai to low se swap, dono aage. 1 hai to sirf mid aage.
 *   2 hai to high se swap aur high peeche, par mid wahin; jo aaya wo abhi dekha nahi.
 *
 * AISE BOLO  (30-40 sec)
 *   "Arrays.sort works but is O(n log n) and ignores that there are only three values.
 *   Counting the 0s, 1s and 2s and overwriting is O(n) but takes two passes. I do it in one
 *   pass with three pointers, low, mid and high, keeping the invariant that everything
 *   before low is 0, between low and mid is 1, and after high is 2: O(n) time and O(1)
 *   space. The edge case is a 2: after swapping it with high I do not advance mid, because
 *   the value that came in from the right has not been checked yet."
 *
 * TRAP
 *   ! 2 ko high se swap karke mid++ kiya to bug. Sample [2,0,2,1,1,0] pe yeh bug chhup jata
 *     hai; [1,2,0] pe dry run karo. main() dono dikhata hai.
 *   ! Loop mid <= high chahiye; mid < high se aakhri element dekha hi nahi jata ([1,0]).
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why does mid advance on a 0 but not on a 2?
 *     -> low se jo aata hai wo pehle se dekha hua 1 hai (ya mid == low), isliye safe. high
 *        se jo aata hai wo unexplored hai, use abhi check karna hai.
 *   ? Four colours instead of three -- does this generalise?
 *     -> Seedha nahi. Counting sort O(n + k) sabse simple; ya pehle {0,1} vs {2,3} partition,
 *        phir har half pe two-way partition. Dono O(n).
 *   ? Is this stable? Does it matter here?
 *     -> Stable nahi, swaps se equal values ka order badalta hai. Plain ints mein fark nahi;
 *        records ko kisi key se partition karo to farak padta hai.
 *   ? Write quicksort's partition step with the same idea.
 *     -> Pivot ke against 3-way partition: < pivot low ki taraf, == beech mein, > high ki
 *        taraf; wahi low/mid/high loop. Bahut duplicates pe quicksort O(n^2) se bachta hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Sort colors
 *   - Move zeroes
 *   - Segregate even and odd
 *   - Quicksort partition
 *   - Partition around a pivot
 *
 * JAVA 21 MEIN
 *   Arrow switch (Java 14 se standard): case 0 -> swap(a, low++, mid++); case 1 -> mid++;
 *   default -> swap(a, mid, high--); Fall-through nahi hota, break bhoolne ka bug khatam.
 *
 * VARIANTS     : LC 75, LC 283, LC 905
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

        // the sample input hides the bug: it happens to come out sorted
        int[] bug = {2, 0, 2, 1, 1, 0};
        buggySortColors(bug);
        System.out.println("buggy on sample   : " + Arrays.toString(bug)
                + (Arrays.equals(bug, a) ? "   (sorted by luck)" : "   <-- NOT SORTED"));

        // a 0 arrives from the right and mid skips past it
        int[] bug2 = {1, 2, 0};
        buggySortColors(bug2);
        System.out.println("buggy on [1,2,0]  : " + Arrays.toString(bug2)
                + (Arrays.equals(bug2, new int[]{0, 1, 2})
                        ? "" : "   <-- NOT SORTED (mid advanced too early)"));

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
 * buggy on sample   : [0, 0, 1, 1, 2, 2]   (sorted by luck)
 * buggy on [1,2,0]  : [1, 0, 2]   <-- NOT SORTED (mid advanced too early)
 * moveZeroes        : [1, 3, 12, 0, 0]
 * segregateEvenOdd  : [12, 34, 90, 8, 9, 45, 3]
 * ============================================================================================== */
