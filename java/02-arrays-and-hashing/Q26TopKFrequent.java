/* ==============================================================================================
 * Q26  .  Top K frequent elements
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Arrays & Hashing
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- product rounds mein common; heap direction ka sawaal pakka aata hai
 * TECHNIQUE    : Frequency map + bounded min-heap; bucket-by-frequency for linear time
 *
 * PROBLEM
 *   Return the k most frequent elements of an array.
 *
 * ALGORITHM
 *   1. Count occurrences into a map in one pass.
 *   2. Push entries into a MIN-heap ordered by count.
 *   3. When the heap exceeds size k, poll - the weakest candidate is always on top.
 *   4. What survives is the top k; drain the heap in reverse for descending order.
 *   5. O(n) alternative: bucket keys by frequency (bounded by n) and read buckets from the
 *      top down.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Count, then sort all entries by frequency      O(n log n)    O(n)      acceptable
 *       > Correct. Name it, then beat it.
 *   Count + min-heap of size k                     O(n log k)    O(n)      WRITE THIS
 *       > Keep only k candidates and evict the weakest.
 *   Count + bucket by frequency                    O(n)          O(n)      WRITE THIS
 *       > Linear. Frequencies are bounded by n, so an array of buckets works.
 *   Quickselect on frequencies                     O(n) avg      O(n)      acceptable
 *       > Average linear, worst quadratic. Mention, don't write.
 *
 * WHY THIS ONE
 *   The trap is heap direction. For top-k LARGEST you keep a MIN-heap, so the weakest
 *   candidate sits on top and is cheap to evict. Say that sentence before you write,
 *   because reaching for a max-heap is the most common wrong turn. If they push for
 *   strictly linear, bucket sort by frequency is the clean answer.
 *
 * SAMJHO
 *   Pehle HashMap mein har number ki frequency gino.
 *   Phir size k ka MIN-heap: top pe sabse kamzor; size k se upar jaye to poll karke nikalo.
 *   End mein heap mein top k bachte hain. Linear chahiye to frequency ke buckets banao.
 *
 * AISE BOLO  (30-40 sec)
 *   "First I count frequencies in a HashMap. Sorting all entries by count works but is
 *   O(n log n). Instead I keep a min-heap of size k: the weakest candidate sits on top, so
 *   when the heap grows past k I evict it, which gives O(n log k) time and O(k) heap space.
 *   If they want strictly linear, I bucket the keys by frequency, because a count can never
 *   exceed n, and read the buckets from the top down: O(n) time and O(n) space."
 *
 * TRAP
 *   ! Top-k LARGEST ke liye MIN-heap. Max-heap mein saare entries rakhne padte hain: O(n).
 *   ! Bucket mein ties ho to ek bucket k se zyada de sakta hai; k pe ruko (topKBucket()).
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why a min-heap and not a max-heap?
 *     -> Min-heap size k: top pe sabse kamzor, evict O(log k); total O(n log k), heap O(k).
 *        Max-heap galat nahi, par saare entries rakhta hai: heap O(n), poll k baar.
 *   ? Can you do it in O(n)? Show the bucket version.
 *     -> Haan: frequency <= n, to n+1 buckets; key ko bucket[freq] mein daalo aur upar se
 *        neeche padho jab tak k na ho jaye. Code: topKBucket().
 *   ? k equals n -- which approach wins now?
 *     -> Heap ka log k ab log n hai, yani sort jaisa. Bucket O(n) hi rehta hai; aur order
 *        nahi chahiye to saare distinct keys seedha return kar do.
 *   ? Top K frequent WORDS with ties broken alphabetically.
 *     -> Comparator: count ascending, tie pe word REVERSE order, taaki bada word pehle evict
 *        ho; phir drain karke ulta karo. Code: topKWords().
 *
 * LEARN THIS AND YOU ALSO GET
 *   - K closest points to origin
 *   - Top K frequent words
 *   - Sort characters by frequency
 *   - K largest elements
 *
 * JAVA 21 MEIN
 *   LinkedList.addFirst ki jagah ArrayList mein poll karo aur out.reversed() lautao.
 *   List.reversed() Java 21 (SequencedCollection) ka hai: copy nahi, ulta view deta hai.
 *
 * APNI STORY SE JODO
 *   "In a batch job I replaced a synchronized shared HashMap with ConcurrentHashMap to
 *   remove lock contention, and the batch window went from 45 minutes to 8 minutes."
 *   Note: agar pooche "counts kai threads se banao", yahan jodo. ConcurrentHashMap.merge
 *   atomic hai aur poore map pe lock nahi lagata.
 *
 * VARIANTS     : LC 347, LC 692, LC 451, LC 973, LC 215
 * ============================================================================================== */

import java.util.*;

public class Q26TopKFrequent {

    /* ---- BEST FOR INTERVIEW : MIN-heap of size k, O(n log k) ----
       Min-heap, not max-heap: the weakest candidate sits on top and is
       cheap to evict. Reaching for a max-heap is the common wrong turn. */
    static List<Integer> topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int n : nums) freq.merge(n, 1, Integer::sum);

        PriorityQueue<Map.Entry<Integer, Integer>> minHeap =
                new PriorityQueue<>(Map.Entry.comparingByValue());
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            minHeap.offer(e);
            if (minHeap.size() > k) minHeap.poll();
        }
        LinkedList<Integer> out = new LinkedList<>();
        while (!minHeap.isEmpty()) out.addFirst(minHeap.poll().getKey());
        return out;
    }

    /* ---- O(n) : bucket by frequency. A frequency can never exceed n. ---- */
    static List<Integer> topKBucket(int[] nums, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int n : nums) freq.merge(n, 1, Integer::sum);

        List<List<Integer>> bucket = new ArrayList<>(nums.length + 1);
        for (int i = 0; i <= nums.length; i++) bucket.add(null);
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            int c = e.getValue();
            if (bucket.get(c) == null) bucket.set(c, new ArrayList<>());
            bucket.get(c).add(e.getKey());
        }
        List<Integer> out = new ArrayList<>();
        for (int c = bucket.size() - 1; c >= 0 && out.size() < k; c--)
            if (bucket.get(c) != null) out.addAll(bucket.get(c));
        return out.subList(0, Math.min(k, out.size()));   // a tied bucket can overshoot k
    }

    /* ---- the same shape for words, with an alphabetical tie-break ---- */
    static List<String> topKWords(String[] words, int k) {
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) freq.merge(w, 1, Integer::sum);
        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>(
                Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()));
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            minHeap.offer(e);
            if (minHeap.size() > k) minHeap.poll();
        }
        LinkedList<String> out = new LinkedList<>();
        while (!minHeap.isEmpty()) out.addFirst(minHeap.poll().getKey());
        return out;
    }

    public static void main(String[] args) {
        int[] nums = {1, 1, 1, 2, 2, 3, 4, 4, 4, 4};
        System.out.println("heap   top 2     : " + topKFrequent(nums, 2));
        System.out.println("bucket top 2     : " + topKBucket(nums, 2));
        System.out.println("heap   top 3     : " + topKFrequent(nums, 3));
        System.out.println("k == n distinct  : " + topKFrequent(nums, 4));
        System.out.println("top 2 words      : " + topKWords(
                new String[]{"i", "love", "code", "i", "love", "java"}, 2));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * heap   top 2     : [4, 1]
 * bucket top 2     : [4, 1]
 * heap   top 3     : [4, 1, 2]
 * k == n distinct  : [4, 1, 2, 3]
 * top 2 words      : [i, love]
 * ============================================================================================== */
