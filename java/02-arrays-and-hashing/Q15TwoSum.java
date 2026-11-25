/* ==============================================================================================
 * Q15  .  Two Sum -- the HashMap complement pattern
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Arrays & Hashing
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Hash map complement lookup (one pass); two pointers when the input is sorted
 *
 * PROBLEM
 *   Return the indices of the two numbers adding to target. Then the values from a sorted
 *   array. Then 3Sum.
 *
 * ALGORITHM
 *   1. Walk the array once keeping a map of value -> index.
 *   2. At index i, look up target - nums[i] in the map.
 *   3. A hit means the pair is found; return both indices.
 *   4. Insert nums[i] AFTER the lookup, otherwise an element can pair with itself.
 *   5. Sorted input variant: lo/hi pointers, move lo up when the sum is too small, hi down
 *      when too large.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Nested loops over every pair                   O(n^2)        O(1)      avoid
 *       > Name it, then discard it.
 *   Sort, then two pointers                        O(n log n)    O(1)      acceptable
 *       > Best when already sorted, or when you need values not indices.
 *   HashMap of value -> index, one pass            O(n)          O(n)      WRITE THIS
 *       > The pattern: as you scan, ask whether what completes the answer was already
 *       > seen.
 *
 * WHY THIS ONE
 *   This is not a problem, it is a habit: "have I already seen what I need?" stored in a
 *   HashMap. Own that habit and subarray-sum, longest-consecutive-sequence and first-
 *   unique-character collapse into the same three lines. The only trap is putting the
 *   value into the map before the lookup, which lets an element pair with itself.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why put AFTER the lookup and not before?
 *   ? Multiple valid pairs -- return all of them without duplicates.
 *   ? The array is sorted -- what beats the map?
 *   ? Extend to 3Sum, then 4Sum. What is the general shape?
 *   ? Two Sum on a stream you can read only once.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Pair with given sum
 *   - 3Sum and 4Sum
 *   - Subarray sum equals K
 *   - Longest consecutive sequence
 *   - Two Sum II (sorted)
 * ============================================================================================== */

import java.util.*;

public class Q15TwoSum {

    /* ---- BEST FOR INTERVIEW : one pass, map of value -> index ---- */
    static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            Integer j = seen.get(target - nums[i]);     // complement already seen?
            if (j != null) return new int[]{j, i};
            seen.put(nums[i], i);                        // put AFTER the lookup
        }
        return new int[]{-1, -1};
    }

    /* ---- SORTED INPUT, VALUES WANTED : two pointers, O(1) space ---- */
    static int[] twoSumSorted(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            int sum = a[lo] + a[hi];
            if (sum == target) return new int[]{a[lo], a[hi]};
            if (sum < target) lo++; else hi--;
        }
        return null;
    }

    /* ---- 3SUM : sort, fix one anchor, two-pointer the rest ---- */
    static List<List<Integer>> threeSum(int[] input) {
        int[] a = input.clone();
        Arrays.sort(a);
        List<List<Integer>> out = new ArrayList<>();
        for (int i = 0; i < a.length - 2; i++) {
            if (i > 0 && a[i] == a[i - 1]) continue;            // skip duplicate anchors
            int lo = i + 1, hi = a.length - 1;
            while (lo < hi) {
                int sum = a[i] + a[lo] + a[hi];
                if (sum == 0) {
                    out.add(Arrays.asList(a[i], a[lo], a[hi]));
                    while (lo < hi && a[lo] == a[lo + 1]) lo++;
                    while (lo < hi && a[hi] == a[hi - 1]) hi--;
                    lo++; hi--;
                } else if (sum < 0) lo++;
                else hi--;
            }
        }
        return out;
    }

    public static void main(String[] args) {
        int[] nums = {2, 7, 11, 15, 3};
        System.out.println("twoSum(9)        : " + Arrays.toString(twoSum(nums, 9)));
        System.out.println("twoSum(18)       : " + Arrays.toString(twoSum(nums, 18)));
        System.out.println("twoSum(100)      : " + Arrays.toString(twoSum(nums, 100)));

        // why the insert must come AFTER the lookup: 3+3 would self-match
        System.out.println("self-match guard : " + Arrays.toString(twoSum(new int[]{3, 2, 4}, 6)));

        System.out.println("sorted values(26): "
                + Arrays.toString(twoSumSorted(new int[]{2, 3, 7, 11, 15}, 26)));
        System.out.println("threeSum         : " + threeSum(new int[]{-1, 0, 1, 2, -1, -4}));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * twoSum(9)        : [0, 1]
 * twoSum(18)       : [1, 2]
 * twoSum(100)      : [-1, -1]
 * self-match guard : [1, 2]
 * sorted values(26): [11, 15]
 * threeSum         : [[-1, -1, 2], [-1, 0, 1]]
 * ============================================================================================== */
