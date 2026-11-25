/* ==============================================================================================
 * Q29  .  Linked list: reverse, cycle, middle
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Linked List, Stack & Tree
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Three-pointer reversal; Floyd slow/fast traversal
 *
 * PROBLEM
 *   Reverse a singly linked list. Detect a cycle and find where it starts. Then: is the
 *   list a palindrome? Remove the nth node from the end.
 *
 * ALGORITHM
 *   1. Reverse: keep prev, cur and next; point cur.next at prev, then shift both forward.
 *   2. When cur becomes null, prev is the new head.
 *   3. Cycle: advance slow by one and fast by two; they meet inside the loop if one exists.
 *   4. Restart a pointer at the head and advance both one step - they collide at the cycle
 *      entry.
 *   5. Palindrome: find the middle with slow/fast, reverse the back half, then compare the
 *      two halves.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Copy values into a list or stack, rebuild      O(n)          O(n)      avoid
 *       > Works, and misses the entire point of the question.
 *   Recursion                                      O(n)          O(n)      acceptable
 *       > Elegant; stack depth n is a real risk on long lists.
 *   Three pointers (prev, cur, next)               O(n)          O(1)      WRITE THIS
 *       > The one you must type without thinking.
 *   Slow / fast pointers                           O(n)          O(1)      WRITE THIS
 *       > Cycle detection, middle, and nth-from-end all come from this.
 *
 * WHY THIS ONE
 *   Two primitives cover nearly every linked-list question: the three-pointer reverse and
 *   the slow/fast walk. Palindrome-check is literally "find middle with slow/fast, reverse
 *   the second half, compare" -- composed from both. For Floyd, be ready to say why
 *   restarting one pointer at the head finds the cycle entry; that proof is a common
 *   follow-up and a one-line answer if you have seen it.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Prove why the second phase of Floyd's algorithm lands on the cycle entry.
 *   ? Reverse only the first k nodes. Then in groups of k.
 *   ? Does your palindrome check restore the list? Should it?
 *   ? Find the intersection node of two lists without extra space.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Reverse in groups of K
 *   - Palindrome linked list
 *   - Middle of the list
 *   - Remove Nth from end
 *   - Intersection of two lists
 *   - Reorder list
 *   - Detect and remove a loop
 * ============================================================================================== */

import java.util.*;

public class Q29LinkedListToolkit {

    static final class ListNode {
        int val; ListNode next;
        ListNode(int v) { val = v; }
        static ListNode of(int... xs) {
            ListNode d = new ListNode(0), t = d;
            for (int x : xs) { t.next = new ListNode(x); t = t.next; }
            return d.next;
        }
        static String show(ListNode h) {
            StringBuilder sb = new StringBuilder();
            for (ListNode n = h; n != null; n = n.next)
                sb.append(n.val).append(n.next == null ? "" : "->");
            return sb.length() == 0 ? "(empty)" : sb.toString();
        }
    }

    /* ---- REVERSE : three pointers. Type this blind. ---- */
    static ListNode reverse(ListNode head) {
        ListNode prev = null, cur = head;
        while (cur != null) {
            ListNode next = cur.next;   // 1 remember
            cur.next = prev;            // 2 flip
            prev = cur;                 // 3 advance both
            cur  = next;
        }
        return prev;
    }

    /* ---- FLOYD : does a cycle exist, and where does it start? ---- */
    static ListNode cycleStart(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {                       // met inside the loop
                ListNode p = head;
                while (p != slow) { p = p.next; slow = slow.next; }
                return p;                              // the entry node
            }
        }
        return null;
    }

    /* ---- MIDDLE + REVERSE composed : is the list a palindrome? ---- */
    static boolean isPalindrome(ListNode head) {
        if (head == null || head.next == null) return true;
        ListNode slow = head, fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next; fast = fast.next.next;
        }
        ListNode second = reverse(slow.next);
        ListNode p = head, q = second;
        boolean ok = true;
        while (q != null) {
            if (p.val != q.val) { ok = false; break; }
            p = p.next; q = q.next;
        }
        slow.next = reverse(second);      // restore the list - good manners
        return ok;
    }

    /* ---- Nth FROM THE END : a gap of n between two pointers, one pass ---- */
    static ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode fast = dummy, slow = dummy;
        for (int i = 0; i <= n; i++) fast = fast.next;
        while (fast != null) { fast = fast.next; slow = slow.next; }
        slow.next = slow.next.next;
        return dummy.next;
    }

    public static void main(String[] args) {
        System.out.println("reverse          : " + ListNode.show(reverse(ListNode.of(1, 2, 3, 4, 5))));

        ListNode cyc = ListNode.of(3, 2, 0, -4);
        ListNode tail = cyc; while (tail.next != null) tail = tail.next;
        tail.next = cyc.next;                               // make a cycle at index 1
        ListNode start = cycleStart(cyc);
        System.out.println("cycle start val  : " + (start == null ? "none" : start.val));
        System.out.println("no cycle         : "
                + (cycleStart(ListNode.of(1, 2, 3)) == null ? "none" : "found"));

        System.out.println("palindrome 1221  : " + isPalindrome(ListNode.of(1, 2, 2, 1)));
        System.out.println("palindrome 12321 : " + isPalindrome(ListNode.of(1, 2, 3, 2, 1)));
        System.out.println("palindrome 123   : " + isPalindrome(ListNode.of(1, 2, 3)));

        System.out.println("remove 2nd-last  : "
                + ListNode.show(removeNthFromEnd(ListNode.of(1, 2, 3, 4, 5), 2)));
        System.out.println("remove only node : "
                + ListNode.show(removeNthFromEnd(ListNode.of(1), 1)));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * reverse          : 5->4->3->2->1
 * cycle start val  : 2
 * no cycle         : none
 * palindrome 1221  : true
 * palindrome 12321 : true
 * palindrome 123   : false
 * remove 2nd-last  : 1->2->3->5
 * remove only node : (empty)
 * ============================================================================================== */
