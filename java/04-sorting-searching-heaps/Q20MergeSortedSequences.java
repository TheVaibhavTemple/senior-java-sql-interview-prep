/* ==============================================================================================
 * Q20  .  Merge sorted sequences
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Sorting, Searching & Heaps
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : Backward in-place merge; min-heap of heads for the K-way merge
 *
 * PROBLEM
 *   Merge array b into array a which has n free slots at the end. Then two sorted linked
 *   lists. Then K sorted lists.
 *
 * ALGORITHM
 *   1. Both inputs are already sorted, so never re-sort.
 *   2. Write from the back of the destination, where the free space is - nothing needs
 *      shifting.
 *   3. Compare the two current tails and copy the larger into the write position.
 *   4. For linked lists, a dummy head removes the first-node special case.
 *   5. For K lists, keep a min-heap of the current heads: poll the smallest, push its
 *      successor.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Concatenate and sort                           O((m+n) log(m O(m+n)    avoid
 *       > Throws away the fact that both inputs are already sorted.
 *   Merge forwards into a new array                O(m+n)        O(m+n)    acceptable
 *       > Correct, but allocates.
 *   Merge BACKWARDS in place                       O(m+n)        O(1)      WRITE THIS
 *       > No shifting, no allocation. The intended answer.
 *   Min-heap of K heads                            O(N log k)    O(k)      WRITE THIS
 *       > The K-way version. O(N.k) without the heap.
 *
 * WHY THIS ONE
 *   Filling from the back is the whole trick for the in-place version: the free space is
 *   at the end, so writing there never overwrites something still needed. For the K-way
 *   merge, the heap turns "pick the smallest of k" from O(k) into O(log k) -- that is the
 *   only difference between a good and a bad answer.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why fill from the back rather than the front?
 *   ? Merge K lists without a heap -- divide and conquer. What is the complexity?
 *   ? The lists are too large for memory -- external merge, what changes?
 *   ? Merge two sorted arrays removing duplicates.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Merge K sorted lists
 *   - Merge sort's merge step
 *   - Merge intervals (after sorting)
 *   - Intersection of sorted arrays
 *   - External sort
 * ============================================================================================== */

import java.util.*;

public class Q20MergeSortedSequences {

    static final class ListNode {
        int val; ListNode next;
        ListNode(int v) { val = v; }
        static ListNode of(int... xs) {
            ListNode dummy = new ListNode(0), t = dummy;
            for (int x : xs) { t.next = new ListNode(x); t = t.next; }
            return dummy.next;
        }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            for (ListNode n = this; n != null; n = n.next)
                sb.append(n.val).append(n.next == null ? "" : "->");
            return sb.toString();
        }
    }

    /* ---- 1. merge b into a IN PLACE : fill from the BACK ---- */
    static void merge(int[] a, int m, int[] b, int n) {
        int i = m - 1, j = n - 1, k = m + n - 1;
        while (j >= 0) {
            a[k--] = (i >= 0 && a[i] > b[j]) ? a[i--] : b[j--];
        }
    }

    /* ---- 2. two sorted linked lists : dummy head kills the edge case ---- */
    static ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0), tail = dummy;
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) { tail.next = l1; l1 = l1.next; }
            else                  { tail.next = l2; l2 = l2.next; }
            tail = tail.next;
        }
        tail.next = (l1 != null) ? l1 : l2;
        return dummy.next;
    }

    /* ---- 3. K sorted lists : min-heap of the current heads, O(N log k) ---- */
    static ListNode mergeKLists(List<ListNode> lists) {
        PriorityQueue<ListNode> pq =
                new PriorityQueue<>(Comparator.comparingInt(node -> node.val));
        for (ListNode n : lists) if (n != null) pq.add(n);

        ListNode dummy = new ListNode(0), tail = dummy;
        while (!pq.isEmpty()) {
            ListNode n = pq.poll();
            tail.next = n; tail = n;
            if (n.next != null) pq.add(n.next);
        }
        tail.next = null;
        return dummy.next;
    }

    public static void main(String[] args) {
        int[] a = {1, 3, 5, 0, 0, 0};
        merge(a, 3, new int[]{2, 4, 6}, 3);
        System.out.println("in-place merge   : " + Arrays.toString(a));

        System.out.println("two lists        : "
                + mergeTwoLists(ListNode.of(1, 3, 5), ListNode.of(2, 4, 6)));

        System.out.println("K lists          : " + mergeKLists(Arrays.asList(
                ListNode.of(1, 4, 7), ListNode.of(2, 5, 8), ListNode.of(3, 6, 9))));

        System.out.println("one side empty   : "
                + mergeTwoLists(ListNode.of(1, 2), null));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * in-place merge   : [1, 2, 3, 4, 5, 6]
 * two lists        : 1->2->3->4->5->6
 * K lists          : 1->2->3->4->5->6->7->8->9
 * one side empty   : 1->2
 * ============================================================================================== */
