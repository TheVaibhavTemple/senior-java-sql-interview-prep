/* ==============================================================================================
 * Q30  .  Stacks: parentheses, min stack, monotonic stack
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Linked List, Stack & Tree
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- balanced brackets bahut common; min stack, monotonic stack product mein
 * TECHNIQUE    : Stack of expected closers; value+min pairs; monotonic stack
 *
 * PROBLEM
 *   Validate balanced brackets. Then a stack with O(1) getMin. Then the next greater
 *   element for every index.
 *
 * ALGORITHM
 *   1. Brackets: push the EXPECTED closing character when an opener is seen.
 *   2. On a closing character, pop and compare - a mismatch or empty stack means invalid.
 *   3. Min stack: push {value, min-so-far} pairs so getMin is O(1) through pops.
 *   4. Monotonic stack: hold indices whose answer is still unknown, values decreasing.
 *   5. A new larger value resolves and pops every smaller index below it - amortised O(n).
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   A counter per bracket type                     O(n)          O(1)      avoid
 *       > Fails on "([)]" -- order matters and a counter loses it.
 *   Stack of opening brackets + a matching map     O(n)          O(n)      acceptable
 *       > Correct and conventional.
 *   Stack of EXPECTED closers                      O(n)          O(n)      WRITE THIS
 *       > Removes the matching table entirely.
 *   Value + running-min pairs                      O(n)          O(n)      WRITE THIS
 *       > The simplest way to keep getMin O(1) through pops.
 *   Monotonic stack                                O(n)          O(n)      WRITE THIS
 *       > Each index is pushed and popped once -- amortised O(n).
 *
 * WHY THIS ONE
 *   Use ArrayDeque, never java.util.Stack -- Stack extends Vector and synchronises every
 *   call, and saying that unprompted is free credit. The monotonic stack is the real
 *   content: the stack holds indices whose answer is still unknown, and a new larger value
 *   resolves all of them at once. That sentence unlocks histogram, daily temperatures and
 *   trapping rain water.
 *
 * SAMJHO
 *   Brackets: opener dikhe to uska EXPECTED closer push karo; closer aaye to pop karke match
 *   karo. Min stack: har value ke saath "ab tak ka min" bhi rakho. Monotonic stack mein wo
 *   indices rakho jinka jawab abhi pata nahi; bada number aaya to sabka jawab ek saath mila.
 *
 * AISE BOLO  (30-40 sec)
 *   "A counter per bracket type fails on input like ([)], because order matters, so I use a
 *   stack. When I see an opener I push the closer I expect, and on a closer I pop and compare,
 *   which is O(n) time and O(n) space. I use ArrayDeque, not java.util.Stack, because Stack
 *   extends Vector and synchronises every call. The edge cases are an empty string, which is
 *   valid, and leftover openers like (((, so I check that the stack is empty at the end."
 *
 * TRAP
 *   ! End mein stack.isEmpty() check bhoole to "(((" true aayega. Aur pop se pehle empty
 *     check, warna ")" par ArrayDeque NoSuchElementException deta hai.
 *   ! Monotonic stack mein value nahi, INDEX push karo; width aur "kitne din baad" index se
 *     hi nikalta hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why ArrayDeque over java.util.Stack?
 *     -> Stack Vector extend karta hai, har method synchronized, single thread mein bhi lock ka
 *        kharcha. Stack ka Javadoc khud Deque suggest karta hai. ArrayDeque null nahi leta.
 *   ? getMin in O(1) with O(1) EXTRA space -- is it possible?
 *     -> Haan, encoding: v < min ho to 2*v - min push karo aur min = v. Pop par top < min mile
 *        to purana min = 2*min - top. long use karo, warna overflow.
 *   ? Daily temperatures -- same monotonic stack, restate it.
 *     -> Wahi nextGreater loop, bas pop par value ki jagah i - index store karo: kitne din baad
 *        garam din aaya. Jinka koi nahi, unka 0.
 *   ? Largest rectangle in a histogram -- what does the sentinel at the end do?
 *     -> i == n par height 0 maante hain, jo har bar se chhoti hai. Isse bache hue saare bars
 *        pop hokar apna area de dete hain; alag cleanup loop nahi chahiye.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Min stack
 *   - Next greater element
 *   - Daily temperatures
 *   - Largest rectangle in histogram
 *   - Trapping rain water
 *   - Evaluate RPN
 *   - Remove K digits
 *
 * VARIANTS     : LC 20, LC 155, LC 496, LC 503, LC 739, LC 84, LC 42, LC 150, LC 402
 * ============================================================================================== */

import java.util.*;

public class Q30StackPatterns {

    /* ---- BRACKETS : push the EXPECTED closer. No matching table needed. ---- */
    static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();   // ArrayDeque, never java.util.Stack
        for (char c : s.toCharArray()) {
            switch (c) {
                case '(': stack.push(')'); break;
                case '[': stack.push(']'); break;
                case '{': stack.push('}'); break;
                default:
                    if (stack.isEmpty() || stack.pop() != c) return false;
            }
        }
        return stack.isEmpty();
    }

    /* ---- MIN STACK : carry the running minimum beside every value ---- */
    static final class MinStack {
        private final Deque<int[]> s = new ArrayDeque<>();      // {value, minSoFar}
        void push(int v) { s.push(new int[]{v, s.isEmpty() ? v : Math.min(v, s.peek()[1])}); }
        int  pop()       { return s.pop()[0]; }
        int  top()       { return s.peek()[0]; }
        int  getMin()    { return s.peek()[1]; }
    }

    /* ---- MONOTONIC STACK : indices whose answer is still unknown ---- */
    static int[] nextGreater(int[] a) {
        int[] out = new int[a.length];
        Arrays.fill(out, -1);
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < a.length; i++) {
            while (!stack.isEmpty() && a[stack.peek()] < a[i]) out[stack.pop()] = a[i];
            stack.push(i);
        }
        return out;
    }

    /* ---- the same stack, one step further ---- */
    static int largestRectangle(int[] h) {
        Deque<Integer> stack = new ArrayDeque<>();
        int best = 0;
        for (int i = 0; i <= h.length; i++) {
            int cur = (i == h.length) ? 0 : h[i];              // sentinel flushes the stack
            while (!stack.isEmpty() && h[stack.peek()] >= cur) {
                int height = h[stack.pop()];
                int left = stack.isEmpty() ? -1 : stack.peek();
                best = Math.max(best, height * (i - left - 1));
            }
            stack.push(i);
        }
        return best;
    }

    public static void main(String[] args) {
        for (String s : new String[]{"()[]{}", "([)]", "(]", "", "((("})
            System.out.printf("isValid %-8s : %s%n", "\"" + s + "\"", isValid(s));

        MinStack ms = new MinStack();
        ms.push(-2); ms.push(0); ms.push(-3);
        System.out.println("minStack getMin  : " + ms.getMin());
        ms.pop();
        System.out.println("after pop getMin : " + ms.getMin() + "  top=" + ms.top());

        System.out.println("nextGreater      : "
                + Arrays.toString(nextGreater(new int[]{2, 1, 2, 4, 3})));
        System.out.println("largestRectangle : "
                + largestRectangle(new int[]{2, 1, 5, 6, 2, 3}));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * isValid "()[]{}" : true
 * isValid "([)]"   : false
 * isValid "(]"     : false
 * isValid ""       : true
 * isValid "((("    : false
 * minStack getMin  : -3
 * after pop getMin : -2  top=0
 * nextGreater      : [4, 2, 4, -1, -1]
 * largestRectangle : 10
 * ============================================================================================== */
