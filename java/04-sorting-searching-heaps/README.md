# Sorting, Searching & Heaps

5 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| Q20 | Merge sorted sequences | Medium | Medium | P2 | Backward in-place merge; min-heap of heads for the K-way merge | [`Q20MergeSortedSequences.java`](Q20MergeSortedSequences.java) |
| Q25 | Merge intervals and meeting rooms | Medium | HIGH | P2 | Sort by start then sweep; independent start/end sweep for the room count | [`Q25MergeIntervals.java`](Q25MergeIntervals.java) |
| Q27 | Kth largest, and the running median | Medium | Medium | P2 | Bounded min-heap of size k; two balanced heaps for a streaming median | [`Q27KthLargestAndMedian.java`](Q27KthLargestAndMedian.java) |
| Q28 | Binary search and every variant they ask for | Medium | HIGH | P1 | Half-open binary search [lo, hi); lower bound; search on the answer space | [`Q28BinarySearchVariants.java`](Q28BinarySearchVariants.java) |
| Q55 | Which FX rate applied at time t? TreeMap.floorEntry vs binary search | Medium | Medium | P2 | One TreeMap per currency pair with floorEntry; upperBound - 1 on a sorted array; BigDecimal with setScale(2, HALF_EVEN) | [`Q55RateAtTimestampWithTreeMap.java`](Q55RateAtTimestampWithTreeMap.java) |

## Is folder ko kaise padhein

Pehle Q28 karo: lowerBound wala ek hi loop shape yaad karo, baaki variants usi se nikalte hain.
Phir Q25 aur Q20, dono "sorted order ka fayda uthao" wale sawaal hain. Q27 mein kth largest
type karo; running median ka sirf SAMJHO aur TRAP padh lo.

## Mock round

| Interviewer | Aap |
|---|---|
| Find the first and last position of a target in a sorted array. | I write one half-open binary search. First is lowerBound(t) if a[i] equals t; last is upperBound(t) minus one. Each is O(log n). |
| Why lo + (hi - lo) / 2? | lo + hi can overflow int and go negative; hi - lo cannot. The JDK uses (low + high) >>> 1 for the same reason. |
| Now the array is rotated. | One half around mid is always sorted. I check which half it is and whether the target lies in it, then drop the other half. Still O(log n) with distinct values. |
| Different problem: merge overlapping intervals. | I sort by start and sweep once. If the next start is <= the current end I extend the end with max, otherwise I close it. O(n log n) for the sort. |
| How many meeting rooms do these meetings need? | I sort starts and ends separately and sweep. Each start takes a room, each end that is not after it frees one, and the peak is the answer. |
| Find the kth largest amount in a stream of transactions. | A min-heap capped at k. I push each amount and poll when the size goes over k, so the head is the kth largest. O(log k) per element, O(k) memory. |
| Why not just sort? | Sorting is O(n log n) and needs all the data up front. The heap works on a stream with bounded memory. |
| Merge k sorted files that do not fit in memory. | I read each file with a buffered reader and keep a min-heap of the current head of each file. Poll the smallest, write it, push the next line from that file. O(N log k), and disk I/O dominates. |
| A transfer was booked at 10:45. Which FX rate applies? | The latest rate whose effective time is at or before 10:45. I keep a TreeMap per currency pair and call floorEntry, which is O(log n). If 10:45 is before the first rate there is no answer, so I reject the transfer instead of pricing it at zero. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Kth largest ke liye kaunsa heap? | Size k ka MIN-heap; head hi jawab hai. |
| Java PriorityQueue default kya hai? | Min-heap. Max-heap ke liye Comparator.reverseOrder(). |
| (a, b) -> b - a comparator mein kya problem? | Bade ya negative numbers par int overflow; Integer.compare ya reverseOrder() lo. |
| Sorted array in place merge kis side se? | Peeche se; free space wahi hai, shifting nahi lagti. |
| Merge K lists ki complexity? | Heap ke saath O(N log k) time, O(k) space. |
| Merge intervals mein sort kis par? | Start par. End se sort karne par galat merge hota hai. |
| Meeting rooms ka jawab asal mein kya hai? | Ek saath chal rahi meetings ka maximum count. |
| Binary search mein safe mid? | lo + (hi - lo) / 2. |
| lowerBound kya return karta hai? | Pehla index jahan a[i] >= target; yahi insert position hai. |
| Rotated array with duplicates ka worst case? | O(n), kyunki a[lo] == a[mid] == a[hi] par half decide nahi hota. |
| Binary search on answer kab lagta hai? | "Minimum ya maximum X" aur check monotonic ho, jaise Koko bananas. |
| Quickselect ka risk? | Average O(n), worst O(n^2), aur input mutate karta hai. |
| "Rate at time t" ke liye TreeMap ka kaunsa method? | floorEntry(t): sabse bada key <= t. lowerEntry strict < hai, boundary miss karta hai. |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
