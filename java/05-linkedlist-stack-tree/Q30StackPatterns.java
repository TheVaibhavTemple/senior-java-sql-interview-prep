/* ==============================================================================================
 * Q30  .  Stacks: parentheses, min stack, monotonic stack
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Linked List, Stack & Tree
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
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
 *       > The only way to keep getMin O(1) through pops.
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
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why ArrayDeque over java.util.Stack?
 *   ? getMin in O(1) with O(1) EXTRA space -- is it possible?
 *   ? Daily temperatures -- same monotonic stack, restate it.
 *   ? Largest rectangle in a histogram -- what does the sentinel at the end do?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Min stack
 *   - Next greater element
 *   - Daily temperatures
 *   - Largest rectangle in histogram
 *   - Trapping rain water
 *   - Evaluate RPN
 *   - Remove K digits
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
