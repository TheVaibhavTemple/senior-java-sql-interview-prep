/* ==============================================================================================
 * Q25  .  Merge intervals and meeting rooms
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Sorting, Searching & Heaps
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P2   -- product aur fintech rounds mein common; meeting rooms twist bhi wahi aata
 * TECHNIQUE    : Sort by start then sweep; independent start/end sweep for the room count
 *
 * PROBLEM
 *   Merge all overlapping intervals. Then find the minimum number of meeting rooms needed.
 *
 * ALGORITHM
 *   1. Sort the intervals by start time.
 *   2. After sorting, an interval can only overlap the one immediately before it.
 *   3. If the next start is <= the current end, extend the current end to the max of the
 *      two.
 *   4. Otherwise close the current interval and open a new one.
 *   5. Meeting rooms: sort starts and ends separately and sweep - each start opens a room,
 *      each passed end frees one.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Compare every pair and merge repeatedly        O(n^2)        O(n)      avoid
 *   Sort by start, sweep once                      O(n log n)    O(n)      WRITE THIS
 *       > Sorting makes "does it overlap the previous one" a local decision.
 *   Sort starts and ends separately                O(n log n)    O(n)      WRITE THIS
 *       > For the room count, you never need to know WHICH meeting is where.
 *   Min-heap of end times                          O(n log n)    O(n)      acceptable
 *       > Needed if you must report the rooms themselves.
 *
 * WHY THIS ONE
 *   Sorting by start is not a detail, it is the algorithm: once sorted, an interval can
 *   only overlap the one immediately before it. The meeting-rooms variant is the
 *   interesting jump -- same data, but the answer comes from decoupling starts and ends
 *   into two sorted timelines and sweeping them. Candidates who only memorised merge-
 *   intervals stall there.
 *
 * SAMJHO
 *   Pehle start se sort karo. Ab har interval sirf pichhle merged interval se hi overlap kar
 *   sakta hai, to ek pass kaafi hai: overlap hai to end badhao, warna naya interval shuru.
 *   Meeting rooms mein starts aur ends alag sort karo: har start par room +1, har khatam
 *   meeting par -1. Sabse bada count hi jawab hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "Comparing every pair and merging repeatedly is O(n^2), so I sort by start time first.
 *   After sorting, an interval can only overlap the last merged one, so one sweep that either
 *   extends the current end with max or closes it gives the answer in O(n log n) time and
 *   O(n) space. For meeting rooms I sort the starts and the ends separately and sweep: every
 *   start takes a room, and every end that is not after it frees one. The edge case I confirm
 *   first is whether touching intervals like [1,4] and [4,5] count as overlapping."
 *
 * TRAP
 *   ! End update mein Math.max lagao: [1,10] ke baad [2,3] aaye to end 10 hi rehna chahiye,
 *     3 nahi. Contained interval yahi pakadta hai.
 *   ! Boundary pehle confirm karo: merge mein <= se touching intervals jud jaate hain, rooms
 *     mein end <= start matlab room free ho gaya.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why sort by start and not by end for merging?
 *     -> Start se sort par agar next interval current se overlap nahi karta, to aage wala koi
 *        nahi karega. End se sort par [2,3],[4,5],[1,10] mein [1,10] pehle band hua [2,3] miss
 *        kar deta hai.
 *   ? Why can you sort starts and ends independently for the room count -- doesn't that
 *     lose the pairing?
 *     -> Kisi bhi time par rooms = ab tak shuru hui meetings - ab tak khatam hui. KAUN si
 *        khatam hui farak nahi padta, sirf KITNI. Isliye pairing ki zaroorat nahi.
 *   ? Insert a new interval into an already-merged list without re-sorting.
 *     -> Teen phase, O(n): jo new ke start se pehle khatam hote hain wo add karo, overlap walon
 *        ko new mein merge karo (min start, max end), phir baaki sab add karo.
 *   ? Minimum platforms at a railway station -- restate it.
 *     -> Train = meeting, arrival = start, departure = end, platform = room. Bas poochho ki
 *        same time par aana aur jaana ho to alag platform chahiye ya nahi.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Insert interval
 *   - Meeting rooms I and II
 *   - Minimum platforms
 *   - Interval list intersections
 *   - Non-overlapping intervals
 *   - Employee free time
 *
 * VARIANTS     : LC 56, LC 57, LC 252, LC 253, LC 435, LC 986
 * ============================================================================================== */

import java.util.*;

public class Q25MergeIntervals {

    /* ---- sort by START, then sweep. Sorting IS the algorithm. ---- */
    static int[][] merge(int[][] intervals) {
        if (intervals.length == 0) return intervals;
        int[][] in = Arrays.stream(intervals).map(int[]::clone).toArray(int[][]::new);
        Arrays.sort(in, Comparator.comparingInt(x -> x[0]));

        List<int[]> out = new ArrayList<>();
        int[] cur = in[0].clone();
        for (int i = 1; i < in.length; i++) {
            if (in[i][0] <= cur[1]) cur[1] = Math.max(cur[1], in[i][1]);   // overlap -> extend
            else { out.add(cur); cur = in[i].clone(); }                    // gap -> close & open
        }
        out.add(cur);
        return out.toArray(new int[0][]);
    }

    /* ---- MEETING ROOMS II : same data, sweep starts and ends independently ---- */
    static int minMeetingRooms(int[][] meetings) {
        int n = meetings.length;
        if (n == 0) return 0;
        int[] starts = new int[n], ends = new int[n];
        for (int i = 0; i < n; i++) { starts[i] = meetings[i][0]; ends[i] = meetings[i][1]; }
        Arrays.sort(starts);
        Arrays.sort(ends);

        int rooms = 0, best = 0, e = 0;
        for (int s = 0; s < n; s++) {
            while (e < n && ends[e] <= starts[s]) { rooms--; e++; }   // free finished rooms
            rooms++;
            best = Math.max(best, rooms);
        }
        return best;
    }

    /* ---- heap variant : needed if you must report the rooms themselves ---- */
    static int minRoomsHeap(int[][] meetings) {
        if (meetings.length == 0) return 0;
        int[][] m = Arrays.stream(meetings).map(int[]::clone).toArray(int[][]::new);
        Arrays.sort(m, Comparator.comparingInt(x -> x[0]));
        PriorityQueue<Integer> endTimes = new PriorityQueue<>();
        for (int[] mt : m) {
            if (!endTimes.isEmpty() && endTimes.peek() <= mt[0]) endTimes.poll();
            endTimes.offer(mt[1]);
        }
        return endTimes.size();
    }

    static String show(int[][] a) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < a.length; i++)
            sb.append(Arrays.toString(a[i])).append(i == a.length - 1 ? "" : ", ");
        return sb.append("]").toString();
    }

    public static void main(String[] args) {
        int[][] iv = {{1, 3}, {2, 6}, {8, 10}, {15, 18}};
        System.out.println("merge            : " + show(merge(iv)));
        System.out.println("touching [1,4][4,5] : " + show(merge(new int[][]{{1, 4}, {4, 5}})));
        System.out.println("contained[1,4][2,3] : " + show(merge(new int[][]{{1, 4}, {2, 3}})));
        System.out.println("unsorted input      : " + show(merge(new int[][]{{5, 6}, {1, 3}, {2, 4}})));

        int[][] meetings = {{0, 30}, {5, 10}, {15, 20}};
        System.out.println("minMeetingRooms  : " + minMeetingRooms(meetings)
                + "   (heap says " + minRoomsHeap(meetings) + ")");
        System.out.println("no overlap       : "
                + minMeetingRooms(new int[][]{{7, 10}, {2, 4}}));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * merge            : [[1, 6], [8, 10], [15, 18]]
 * touching [1,4][4,5] : [[1, 5]]
 * contained[1,4][2,3] : [[1, 4]]
 * unsorted input      : [[1, 4], [5, 6]]
 * minMeetingRooms  : 2   (heap says 2)
 * no overlap       : 1
 * ============================================================================================== */
