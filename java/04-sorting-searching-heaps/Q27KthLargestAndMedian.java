/* ==============================================================================================
 * Q27  .  Kth largest, and the running median
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Sorting, Searching & Heaps
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- kth largest PriorityQueue ke saath common hai; running median kam aata hai
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
 *   Max-heap of everything, poll k times           O(n+k log n)  O(n)      acceptable
 *   Min-heap capped at size k                      O(n log k)    O(k)      WRITE THIS
 *       > Works on a stream, bounded memory, the head is the answer.
 *   Quickselect                                    O(n) avg      O(1)      acceptable
 *       > Faster on average, O(n^2) worst, and it mutates the input.
 *   Two heaps (median)                             O(log n) add  O(n)      WRITE THIS
 *       > Max-heap for the low half, min-heap for the high half. median() is O(1).
 *
 * WHY THIS ONE
 *   Both answers are the same idea: keep only the part of the data that can still change
 *   the answer. For kth largest that is k elements; for the median it is the two halves,
 *   kept balanced so the middle sits at the heap tops. The funnel-through insert -- always
 *   add to low, move low's top to high, rebalance -- makes the ordering invariant
 *   automatic rather than something you reason about each time.
 *
 * SAMJHO
 *   Kth largest ke liye sirf top k elements chahiye, to size k ka MIN-heap rakho. Heap ka head
 *   in k mein sabse chhota hai, wahi kth largest hai. Median ke liye do heaps: chhota half
 *   max-heap mein, bada half min-heap mein. Middle dono ke top par baitha hota hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "Sorting and picking index n-k is O(n log n) and does not work on a stream. I keep a
 *   min-heap capped at k and poll whenever the size goes over k, so the head is the kth
 *   largest in O(n log k) time and O(k) space. Quickselect is O(n) on average, but it is
 *   O(n^2) in the worst case and it mutates the input, so I mention it as the trade-off. For
 *   the running median I use a max-heap for the low half and a min-heap for the high half,
 *   so add is O(log n) and the median is O(1)."
 *
 * TRAP
 *   ! Kth LARGEST ke liye MIN-heap lagta hai, max-heap nahi. Ulta yaad mat karna.
 *   ! Max-heap ke liye Comparator.reverseOrder() do; (a, b) -> b - a bade numbers par overflow
 *     karta hai. Median mein do ints jodte waqt bhi long lo, isliye code mein (long) cast hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Kth SMALLEST -- which heap now?
 *     -> Ulta: size k ka MAX-heap. Head in k chhote elements mein sabse bada hai, wahi kth
 *        smallest. Code: findKthSmallest.
 *   ? Why does the median insert push through low first?
 *     -> Naya number low se guzarta hai aur low ka sabse bada high mein chala jaata hai. Isse
 *        low ka har element high ke har element se chhota ya barabar rehta hai, bina if-else.
 *   ? The stream is infinite -- does memory grow?
 *     -> Haan, dono heaps saare n elements rakhte hain, O(n). Value range chhoti ho to count
 *        array (buckets) rakho; warna sliding window ya approximate sketch (jaise t-digest).
 *   ? Kth largest in a matrix sorted by row and column.
 *     -> Max-heap mein har row ka last element daalo; k-1 baar poll karo aur usi row ka left
 *        neighbour daalo, phir head jawab hai: O(k log n). Ya value range par binary search.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Kth smallest element
 *   - Median from data stream
 *   - K closest points
 *   - Sliding window median
 *   - Top K frequent
 *
 * VARIANTS     : LC 215, LC 703, LC 295, LC 347, LC 973, LC 480, LC 378
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
                    : ((long) low.peek() + high.peek()) / 2.0;   // long: int + int can overflow
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
