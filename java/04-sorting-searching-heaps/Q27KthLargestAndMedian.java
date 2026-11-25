/* ==============================================================================================
 * Q27  .  Kth largest, and the running median
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Sorting, Searching & Heaps
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : Bounded min-heap of size k; two balanced heaps for a streaming median
 *
 * PROBLEM
 *   Find the kth largest element. Then maintain the median of an unbounded stream of
 *   numbers.
 *
 * ALGORITHM
 *   1. Kth largest: keep a min-heap capped at k - the head is the answer.
 *   2. Push every element and poll whenever the size exceeds k.
 *   3. Median: a max-heap for the lower half and a min-heap for the upper half.
 *   4. Insert through the low heap, then move its top into the high heap - this keeps low
 *      <= high automatically.
 *   5. Rebalance so low is never smaller than high; the median is low's top, or the mean of
 *      both tops.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Sort, index n-k                                O(n log n)    O(1)      acceptable
 *       > Fine for a one-off query; hopeless for a stream.
 *   Max-heap of everything, poll k times           O(n + k log n O(n)      acceptable
 *   Min-heap capped at size k                      O(n log k)    O(k)      WRITE THIS
 *       > Works on a stream, bounded memory, the head is the answer.
 *   Quickselect                                    O(n) avg      O(1)      acceptable
 *       > Faster on average, O(n^2) worst, and it mutates the input.
 *   Two heaps (median)                             O(log n) / in O(n)      WRITE THIS
 *       > Max-heap for the low half, min-heap for the high half.
 *
 * WHY THIS ONE
 *   Both answers are the same idea: keep only the part of the data that can still change
 *   the answer. For kth largest that is k elements; for the median it is the two halves,
 *   kept balanced so the middle sits at the heap tops. The funnel-through insert -- always
 *   add to low, move low's top to high, rebalance -- makes the ordering invariant
 *   automatic rather than something you reason about each time.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Kth SMALLEST -- which heap now?
 *   ? Why does the median insert push through low first?
 *   ? The stream is infinite -- does memory grow?
 *   ? Kth largest in a matrix sorted by row and column.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Kth smallest element
 *   - Median from data stream
 *   - K closest points
 *   - Sliding window median
 *   - Top K frequent
 * ============================================================================================== */

import java.util.*;

public class Q27KthLargestAndMedian {

    /* ---- Kth LARGEST : a MIN-heap capped at k. Its head is the answer. ---- */
    static int findKthLargest(int[] a, int k) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);
        for (int v : a) {
            minHeap.offer(v);
            if (minHeap.size() > k) minHeap.poll();
        }
        return minHeap.peek();
    }

    /* ---- Kth SMALLEST : mirror image, a MAX-heap capped at k ---- */
    static int findKthSmallest(int[] a, int k) {
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(k, Comparator.reverseOrder());
        for (int v : a) {
            maxHeap.offer(v);
            if (maxHeap.size() > k) maxHeap.poll();
        }
        return maxHeap.peek();
    }

    /* ---- STREAMING MEDIAN : two heaps kept within one element of each other ---- */
    static final class MedianFinder {
        private final PriorityQueue<Integer> low  = new PriorityQueue<>(Comparator.reverseOrder());
        private final PriorityQueue<Integer> high = new PriorityQueue<>();

        void add(int num) {
            low.offer(num);                 // always enter through low ...
            high.offer(low.poll());         // ... then funnel the biggest across
            if (high.size() > low.size()) low.offer(high.poll());   // rebalance
        }

        double median() {
            if (low.isEmpty()) throw new NoSuchElementException("no elements yet");
            return low.size() > high.size()
                    ? low.peek()
                    : (low.peek() + high.peek()) / 2.0;
        }
    }

    public static void main(String[] args) {
        int[] a = {3, 2, 1, 5, 6, 4};
        System.out.println("2nd largest      : " + findKthLargest(a, 2));
        System.out.println("2nd smallest     : " + findKthSmallest(a, 2));
        System.out.println("1st largest      : " + findKthLargest(a, 1));

        MedianFinder mf = new MedianFinder();
        StringBuilder sb = new StringBuilder();
        for (int v : new int[]{5, 15, 1, 3, 8, 7, 9, 10, 6, 11, 4}) {
            mf.add(v);
            sb.append(String.format("%.1f ", mf.median()));
        }
        System.out.println("running median   : " + sb.toString().trim());
        System.out.println("  heaps balanced : low and high differ by at most 1 element");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 2nd largest      : 5
 * 2nd smallest     : 2
 * 1st largest      : 6
 * running median   : 5.0 10.0 5.0 4.0 5.0 6.0 7.0 7.5 7.0 7.5 7.0
 *   heaps balanced : low and high differ by at most 1 element
 * ============================================================================================== */
