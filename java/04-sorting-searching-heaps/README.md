# Sorting, Searching & Heaps

4 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| Q20 | Merge sorted sequences | Medium | Medium | Backward in-place merge; min-heap of heads for the K-way merge | [`Q20MergeSortedSequences.java`](java/04-sorting-searching-heaps/Q20MergeSortedSequences.java) |
| Q25 | Merge intervals and meeting rooms | Medium | HIGH | Sort by start then sweep; independent start/end sweep for the room count | [`Q25MergeIntervals.java`](java/04-sorting-searching-heaps/Q25MergeIntervals.java) |
| Q27 | Kth largest, and the running median | Medium | Medium | Bounded min-heap of size k; two balanced heaps for a streaming median | [`Q27KthLargestAndMedian.java`](java/04-sorting-searching-heaps/Q27KthLargestAndMedian.java) |
| Q28 | Binary search and every variant they ask for | Medium | HIGH | Half-open binary search [lo, hi); lower bound; search on the answer space | [`Q28BinarySearchVariants.java`](java/04-sorting-searching-heaps/Q28BinarySearchVariants.java) |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
