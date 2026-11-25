/* ==============================================================================================
 * Q28  .  Binary search and every variant they ask for
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Sorting, Searching & Heaps
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Half-open binary search [lo, hi); lower bound; search on the answer space
 *
 * PROBLEM
 *   Search a sorted array. Then first and last occurrence. Then a rotated sorted array.
 *   Then the smallest eating speed to finish in h hours.
 *
 * ALGORITHM
 *   1. Use one loop shape everywhere: lo = 0, hi = n, while (lo < hi).
 *   2. mid = lo + (hi - lo) / 2 so the midpoint cannot overflow.
 *   3. Lower bound: if a[mid] < target then lo = mid + 1 else hi = mid; lo is the answer.
 *   4. Rotated array: one half is always sorted - test which, then decide which half can
 *      hold the target.
 *   5. Search on the answer: binary search the candidate answers and use a feasibility
 *      check as the predicate.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Linear scan                                    O(n)          O(1)      avoid
 *   Recursive binary search                        O(log n)      O(log n)  acceptable
 *       > Correct, but the stack frames buy nothing.
 *   Iterative, half-open [lo, hi)                  O(log n)      O(1)      WRITE THIS
 *       > The form with no off-by-one ambiguity.
 *   Binary search on the ANSWER space              O(n log range O(1)      WRITE THIS
 *       > The senior variant: search over possible answers, not over the array.
 *
 * WHY THIS ONE
 *   Pick one form and never deviate: half-open [lo, hi), loop while lo < hi, mid = lo +
 *   (hi - lo) / 2. Every variant then changes the comparison, not the loop. The overflow-
 *   safe mid is something interviewers actively watch for. And "binary search on the
 *   answer" is the variant that separates mid from senior, because the array is not what
 *   you are searching at all.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why lo + (hi - lo) / 2 and not (lo + hi) / 2?
 *   ? Derive first-occurrence and last-occurrence from lowerBound.
 *   ? Rotated array WITH duplicates -- what is the worst case now, and why?
 *   ? How do you recognise a "binary search on the answer" problem?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Search insert position
 *   - First and last position of element
 *   - Search in rotated sorted array
 *   - Find peak element
 *   - Koko eating bananas
 *   - Split array largest sum
 *   - Search a 2D matrix
 * ============================================================================================== */

import java.util.*;

public class Q28BinarySearchVariants {

    /* ---- CANONICAL FORM : half-open [lo, hi). One shape, no off-by-one. ---- */
    static int binarySearch(int[] a, int target) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;         // never (lo + hi) / 2 -- that overflows
            if (a[mid] == target) return mid;
            if (a[mid] < target) lo = mid + 1; else hi = mid;
        }
        return -1;
    }

    /* ---- LOWER BOUND : first index with a[i] >= target. Everything builds on this. ---- */
    static int lowerBound(int[] a, int target) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] < target) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    static int firstOccurrence(int[] a, int t) {
        int i = lowerBound(a, t);
        return (i < a.length && a[i] == t) ? i : -1;
    }

    static int lastOccurrence(int[] a, int t) {
        int i = lowerBound(a, t + 1) - 1;
        return (i >= 0 && a[i] == t) ? i : -1;
    }

    /* ---- ROTATED ARRAY : one half is ALWAYS sorted; search that half ---- */
    static int searchRotated(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[lo] <= a[mid]) {                                     // left half sorted
                if (target >= a[lo] && target < a[mid]) hi = mid - 1; else lo = mid + 1;
            } else {                                                   // right half sorted
                if (target > a[mid] && target <= a[hi]) lo = mid + 1; else hi = mid - 1;
            }
        }
        return -1;
    }

    /* ---- SEARCH ON THE ANSWER : the senior variant. The array is not the search space. ---- */
    static int minEatingSpeed(int[] piles, int h) {
        int lo = 1, hi = Arrays.stream(piles).max().getAsInt();
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            long hours = 0;
            for (int p : piles) hours += (p + mid - 1) / mid;          // ceil division
            if (hours > h) lo = mid + 1; else hi = mid;                // too slow -> go faster
        }
        return lo;
    }

    public static void main(String[] args) {
        int[] a = {1, 3, 3, 3, 5, 7, 9};
        System.out.println("binarySearch(5)      : " + binarySearch(a, 5));
        System.out.println("binarySearch(4)      : " + binarySearch(a, 4));
        System.out.println("lowerBound(3)        : " + lowerBound(a, 3));
        System.out.println("lowerBound(4)        : " + lowerBound(a, 4) + "   (insertion point)");
        System.out.println("first/last of 3      : " + firstOccurrence(a, 3) + " / " + lastOccurrence(a, 3));

        int[] rot = {4, 5, 6, 7, 0, 1, 2};
        System.out.println("searchRotated(0)     : " + searchRotated(rot, 0));
        System.out.println("searchRotated(3)     : " + searchRotated(rot, 3));

        System.out.println("minEatingSpeed h=8   : " + minEatingSpeed(new int[]{3, 6, 7, 11}, 8));
        System.out.println("minEatingSpeed h=5   : "
                + minEatingSpeed(new int[]{30, 11, 23, 4, 20}, 5));

        // the overflow the mid formula avoids
        int lo = Integer.MAX_VALUE - 2, hi = Integer.MAX_VALUE;
        System.out.println("(lo+hi)/2 overflow   : " + ((lo + hi) / 2)
                + "   vs  lo+(hi-lo)/2 = " + (lo + (hi - lo) / 2));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * binarySearch(5)      : 4
 * binarySearch(4)      : -1
 * lowerBound(3)        : 1
 * lowerBound(4)        : 4   (insertion point)
 * first/last of 3      : 1 / 3
 * searchRotated(0)     : 4
 * searchRotated(3)     : -1
 * minEatingSpeed h=8   : 4
 * minEatingSpeed h=5   : 30
 * (lo+hi)/2 overflow   : -2   vs  lo+(hi-lo)/2 = 2147483646
 * ============================================================================================== */
