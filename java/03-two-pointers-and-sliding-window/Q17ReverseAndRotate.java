/* ==============================================================================================
 * Q17  .  Reverse a string, reverse words, rotate an array
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Two Pointers & Sliding Window
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Two-pointer in-place reversal; rotation by three reversals
 *
 * PROBLEM
 *   Reverse a string in place. Reverse the words of a sentence, collapsing spaces. Rotate
 *   an array right by k in O(1) space.
 *
 * ALGORITHM
 *   1. Reverse: swap a[i] and a[j] moving i up and j down until they cross.
 *   2. Reverse words: trim, split on whitespace runs, append in reverse order.
 *   3. Rotate right by k: normalise k with ((k % n) + n) % n to handle k > n and negatives.
 *   4. Reverse the whole array, then reverse [0, k-1], then reverse [k, n-1].
 *   5. The result is a right rotation in O(n) time and O(1) extra space.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   StringBuilder.reverse()                        O(n)          O(n)      avoid
 *       > Correct -- and they asked precisely because they want the manual version.
 *   Recursion                                      O(n)          O(n)      avoid
 *       > Stack depth n for no benefit.
 *   Two pointers on a char array                   O(n)          O(1)*     WRITE THIS
 *       > The building block for everything else here.
 *   Reverse whole, then reverse parts              O(n)          O(1)      WRITE THIS
 *       > The trick they are fishing for in the rotate follow-up.
 *
 * WHY THIS ONE
 *   The reversal trick is one idea reused: reverse the whole thing, then reverse the
 *   pieces, and you have rotated an array or reversed a sentence's words in place. Handle
 *   k > n and negative k with ((k % n) + n) % n -- forgetting that is the single most
 *   common bug on this question.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Rotate LEFT by k instead -- what changes?
 *   ? Reverse the words in place with a char[] and no split().
 *   ? What is the O(1)-space claim actually measuring, given Java Strings are immutable?
 *   ? Reverse every word but keep word order.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Rotate array by K
 *   - Reverse words in a sentence
 *   - Palindrome check
 *   - Reverse a linked list (same three-pointer idea)
 *   - String rotation check
 * ============================================================================================== */

import java.util.*;

public class Q17ReverseAndRotate {

    /* ---- 1. reverse a string in place : two pointers on a char array ---- */
    static String reverse(String s) {
        char[] c = s.toCharArray();
        for (int i = 0, j = c.length - 1; i < j; i++, j--) {
            char t = c[i]; c[i] = c[j]; c[j] = t;
        }
        return new String(c);
    }

    /* ---- 2. reverse the WORDS, collapsing extra whitespace ---- */
    static String reverseWords(String s) {
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = parts.length - 1; i >= 0; i--) {
            sb.append(parts[i]);
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }

    /* ---- 3. rotate right by k : THREE REVERSALS, O(n) time O(1) space ---- */
    static void rotate(int[] a, int k) {
        int n = a.length;
        if (n == 0) return;
        k = ((k % n) + n) % n;              // handles k > n AND negative k
        reverse(a, 0, n - 1);
        reverse(a, 0, k - 1);
        reverse(a, k, n - 1);
    }

    static void reverse(int[] a, int i, int j) {
        while (i < j) { int t = a[i]; a[i++] = a[j]; a[j--] = t; }
    }

    public static void main(String[] args) {
        System.out.println("reverse          : " + reverse("interview"));
        System.out.println("reverseWords     : \"" + reverseWords("  the   sky is  blue ") + "\"");

        int[] a = {1, 2, 3, 4, 5, 6, 7};
        rotate(a, 3);
        System.out.println("rotate right 3   : " + Arrays.toString(a));

        int[] b = {1, 2, 3, 4, 5};
        rotate(b, 12);                       // k > n
        System.out.println("rotate right 12  : " + Arrays.toString(b) + "   (12 % 5 == 2)");

        int[] c = {1, 2, 3, 4, 5};
        rotate(c, -1);                       // negative k == rotate left
        System.out.println("rotate right -1  : " + Arrays.toString(c) + "   (== rotate left 1)");

        int[] empty = {};
        rotate(empty, 3);
        System.out.println("rotate empty     : " + Arrays.toString(empty) + "   (no div-by-zero)");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * reverse          : weivretni
 * reverseWords     : "blue is sky the"
 * rotate right 3   : [5, 6, 7, 1, 2, 3, 4]
 * rotate right 12  : [4, 5, 1, 2, 3]   (12 % 5 == 2)
 * rotate right -1  : [2, 3, 4, 5, 1]   (== rotate left 1)
 * rotate empty     : []   (no div-by-zero)
 * ============================================================================================== */
