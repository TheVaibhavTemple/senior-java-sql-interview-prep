/* ==============================================================================================
 * Q19  .  Missing number, single number, duplicate -- the XOR trio
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Arrays & Hashing
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : XOR cancellation (x^x == 0); Floyd cycle detection over array values
 *
 * PROBLEM
 *   One number is missing from 0..n. Then: every element appears twice except one. Then:
 *   n+1 values in 1..n, find the duplicate without extra space and without modifying the
 *   array.
 *
 * ALGORITHM
 *   1. XOR is associative and commutative, and any value XORed with itself cancels to zero.
 *   2. Missing number: XOR together n, every index and every value - the survivor is the
 *      gap.
 *   3. Single number: XOR every element; the pairs cancel and the loner remains.
 *   4. Duplicate in a read-only array: treat a[i] as a next pointer, so a duplicate creates
 *      a cycle.
 *   5. Run Floyd's tortoise and hare, then restart one pointer at the head to find the
 *      cycle entry.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Sort and scan                                  O(n log n)    O(1)      acceptable
 *   HashSet of seen values                         O(n)          O(n)      acceptable
 *       > Obvious and fine unless extra space is forbidden.
 *   Sum formula                                    O(n)          O(1)      acceptable
 *       > Simplest to explain, but overflows for large n. Say so.
 *   XOR of indices and values                      O(n)          O(1)      WRITE THIS
 *       > No overflow, no extra space, one pass.
 *   Floyd cycle detection on values                O(n)          O(1)      WRITE THIS
 *       > The only answer for read-only plus O(1) space.
 *
 * WHY THIS ONE
 *   Two properties carry all of these: x ^ x == 0 and 0 ^ y == y, so everything appearing
 *   an even number of times vanishes. The duplicate variant is the one nobody sees coming:
 *   treating a[i] as a pointer turns an array problem into the linked-list cycle problem
 *   you already know.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Two numbers are missing instead of one -- how does XOR extend?
 *   ? Every element appears three times except one -- does XOR still work?
 *   ? Why does the sum formula overflow, and at what n?
 *   ? Explain why Floyd's algorithm applies to an array.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Single number I / II / III
 *   - Find the missing number
 *   - Find the duplicate number
 *   - Detect a cycle in a linked list
 *   - Odd occurrence element
 * ============================================================================================== */

import java.util.*;

public class Q19XorTrio {

    /* ---- 1. exactly one number missing from 0..n ---- */
    static int missingNumber(int[] a) {
        int xor = a.length;                                   // seed with n
        for (int i = 0; i < a.length; i++) xor ^= i ^ a[i];
        return xor;
    }

    /* ---- 2. every element appears twice except one ---- */
    static int singleNumber(int[] a) {
        int x = 0;
        for (int v : a) x ^= v;                               // pairs cancel
        return x;
    }

    /* ---- 3. n+1 values in 1..n, read-only, O(1) space : Floyd on values ---- */
    static int findDuplicate(int[] a) {
        int slow = a[0], fast = a[0];
        do {
            slow = a[slow];
            fast = a[a[fast]];
        } while (slow != fast);

        slow = a[0];
        while (slow != fast) { slow = a[slow]; fast = a[fast]; }
        return slow;
    }

    /* ---- 4. the sum formula : simpler to explain, overflows on large n ---- */
    static int missingBySum(int[] a) {
        long n = a.length;
        long expected = n * (n + 1) / 2, actual = 0;
        for (int v : a) actual += v;
        return (int) (expected - actual);
    }

    public static void main(String[] args) {
        int[] missing = {3, 0, 1};
        System.out.println("missing (xor)    : " + missingNumber(missing));
        System.out.println("missing (sum)    : " + missingBySum(missing));

        System.out.println("single number    : " + singleNumber(new int[]{4, 1, 2, 1, 2}));
        System.out.println("duplicate (Floyd): " + findDuplicate(new int[]{1, 3, 4, 2, 2}));
        System.out.println("duplicate (Floyd): " + findDuplicate(new int[]{3, 1, 3, 4, 2}));

        // why the sum formula is a liability: n near Integer.MAX_VALUE overflows an int accumulator
        System.out.println("overflow note    : expected sum for n=100000 is "
                + (100000L * 100001L / 2) + " -- fits in long, NOT in int");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * missing (xor)    : 2
 * missing (sum)    : 2
 * single number    : 4
 * duplicate (Floyd): 2
 * duplicate (Floyd): 3
 * overflow note    : expected sum for n=100000 is 5000050000 -- fits in long, NOT in int
 * ============================================================================================== */
