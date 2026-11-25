# Full index — 67 questions

Sorted easy → hard inside each section. **Weightage** is how often the question actually
shows up in a senior loop, not how hard it is: HIGH means it appears in almost every round.

## Java — 45 problems

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| Q01 | Character and word frequency count | Easy | HIGH | Hash-map counting + Collectors.groupingBy with a map supplier and a downstream collector | [`Q01CharacterAndWordFrequency.java`](java/01-streams-and-collectors/Q01CharacterAndWordFrequency.java) |
| Q02 | First non-repeating character | Easy | HIGH | Two-pass frequency count; order preserved by LinkedHashMap | [`Q02FirstNonRepeatingCharacter.java`](java/01-streams-and-collectors/Q02FirstNonRepeatingCharacter.java) |
| Q03 | Find duplicates in a collection | Easy | HIGH | Set membership as a predicate (Set.add returns false on a repeat) | [`Q03FindDuplicates.java`](java/01-streams-and-collectors/Q03FindDuplicates.java) |
| Q05 | Sort a Map by its values | Easy | HIGH | Entry stream + comparator chain + LinkedHashMap collector | [`Q05SortMapByValue.java`](java/01-streams-and-collectors/Q05SortMapByValue.java) |
| Q15 | Two Sum — the HashMap complement pattern | Easy | HIGH | Hash map complement lookup (one pass); two pointers when the input is sorted | [`Q15TwoSum.java`](java/02-arrays-and-hashing/Q15TwoSum.java) |
| Q16 | Valid anagram and character counting | Easy | HIGH | Fixed-alphabet frequency counting with a single +1/-1 array | [`Q16ValidAnagram.java`](java/02-arrays-and-hashing/Q16ValidAnagram.java) |
| Q17 | Reverse a string, reverse words, rotate an array | Easy | HIGH | Two-pointer in-place reversal; rotation by three reversals | [`Q17ReverseAndRotate.java`](java/03-two-pointers-and-sliding-window/Q17ReverseAndRotate.java) |
| Q18 | Palindrome with two pointers | Easy | HIGH | Converging two pointers; expand-around-centre for the longest substring | [`Q18PalindromeFamily.java`](java/03-two-pointers-and-sliding-window/Q18PalindromeFamily.java) |
| Q35 | Threads, Callable, Future and clean shutdown | Easy | HIGH | ExecutorService with Callable/Future, timed get, and the two-phase shutdown | [`Q35CallableFutureAndShutdown.java`](java/07-concurrency/Q35CallableFutureAndShutdown.java) |
| Q36 | Thread-safe counter | Easy | HIGH | CAS via AtomicLong; striped cells via LongAdder; atomic map operations | [`Q36ThreadSafeCounter.java`](java/07-concurrency/Q36ThreadSafeCounter.java) |
| Q04 | Distinct by a field | Medium | Medium | toMap with a merge function (duplicate policy made explicit) | [`Q04DistinctByField.java`](java/01-streams-and-collectors/Q04DistinctByField.java) |
| Q06 | Group by department and aggregate | Medium | HIGH | Collectors.groupingBy with downstream collectors (counting, averaging, mapping, summarizing, nested groupingBy) | [`Q06GroupByAndAggregate.java`](java/01-streams-and-collectors/Q06GroupByAndAggregate.java) |
| Q07 | Highest-paid employee in each department | Medium | HIGH | groupingBy + maxBy wrapped in collectingAndThen to unwrap the Optional | [`Q07MaxPerGroup.java`](java/01-streams-and-collectors/Q07MaxPerGroup.java) |
| Q08 | Second highest salary / second largest number | Medium | HIGH | distinct + sort + skip (readable), then a single-pass two-variable scan (optimal) | [`Q08SecondHighest.java`](java/01-streams-and-collectors/Q08SecondHighest.java) |
| Q09 | Top N employees per department | Medium | Medium | groupingBy + collectingAndThen post-processing; bounded min-heap for large groups | [`Q09TopNPerGroup.java`](java/01-streams-and-collectors/Q09TopNPerGroup.java) |
| Q10 | map vs flatMap | Medium | HIGH | flatMap - one-to-many mapping followed by concatenation | [`Q10MapVsFlatMap.java`](java/01-streams-and-collectors/Q10MapVsFlatMap.java) |
| Q11 | Group anagrams | Medium | Medium | Canonical signature as a hash key (sorted characters, or a 26-slot count signature) | [`Q11GroupAnagrams.java`](java/01-streams-and-collectors/Q11GroupAnagrams.java) |
| Q12 | Partition, and summary statistics in one pass | Medium | Medium | partitioningBy (always both keys) and summarizing collectors (one traversal, five aggregates) | [`Q12PartitionAndStatistics.java`](java/01-streams-and-collectors/Q12PartitionAndStatistics.java) |
| Q13 | Merge two maps, summing values by key | Medium | Medium | toMap with a merge function; Map.merge for the in-place form | [`Q13MergeMaps.java`](java/01-streams-and-collectors/Q13MergeMaps.java) |
| Q19 | Missing number, single number, duplicate — the XOR trio | Medium | Medium | XOR cancellation (x^x == 0); Floyd cycle detection over array values | [`Q19XorTrio.java`](java/02-arrays-and-hashing/Q19XorTrio.java) |
| Q20 | Merge sorted sequences | Medium | Medium | Backward in-place merge; min-heap of heads for the K-way merge | [`Q20MergeSortedSequences.java`](java/04-sorting-searching-heaps/Q20MergeSortedSequences.java) |
| Q21 | Sliding window — the template that covers a dozen problems | Medium | HIGH | Sliding window: expand right, shrink while invalid, record | [`Q21SlidingWindowTemplate.java`](java/03-two-pointers-and-sliding-window/Q21SlidingWindowTemplate.java) |
| Q22 | Fixed window and Kadane | Medium | HIGH | Fixed-size window slide; Kadane's extend-or-restart decision | [`Q22FixedWindowAndKadane.java`](java/03-two-pointers-and-sliding-window/Q22FixedWindowAndKadane.java) |
| Q23 | Subarray sum equals K — prefix sum plus HashMap | Medium | Medium | Prefix sum plus a hash map of prefix counts | [`Q23SubarraySumEqualsK.java`](java/02-arrays-and-hashing/Q23SubarraySumEqualsK.java) |
| Q24 | Dutch national flag — three-way partition | Medium | Medium | Three-way partition with low / mid / high pointers | [`Q24DutchNationalFlag.java`](java/03-two-pointers-and-sliding-window/Q24DutchNationalFlag.java) |
| Q25 | Merge intervals and meeting rooms | Medium | HIGH | Sort by start then sweep; independent start/end sweep for the room count | [`Q25MergeIntervals.java`](java/04-sorting-searching-heaps/Q25MergeIntervals.java) |
| Q26 | Top K frequent elements | Medium | Medium | Frequency map + bounded min-heap; bucket-by-frequency for linear time | [`Q26TopKFrequent.java`](java/02-arrays-and-hashing/Q26TopKFrequent.java) |
| Q27 | Kth largest, and the running median | Medium | Medium | Bounded min-heap of size k; two balanced heaps for a streaming median | [`Q27KthLargestAndMedian.java`](java/04-sorting-searching-heaps/Q27KthLargestAndMedian.java) |
| Q28 | Binary search and every variant they ask for | Medium | HIGH | Half-open binary search [lo, hi); lower bound; search on the answer space | [`Q28BinarySearchVariants.java`](java/04-sorting-searching-heaps/Q28BinarySearchVariants.java) |
| Q29 | Linked list: reverse, cycle, middle | Medium | HIGH | Three-pointer reversal; Floyd slow/fast traversal | [`Q29LinkedListToolkit.java`](java/05-linkedlist-stack-tree/Q29LinkedListToolkit.java) |
| Q30 | Stacks: parentheses, min stack, monotonic stack | Medium | Medium | Stack of expected closers; value+min pairs; monotonic stack | [`Q30StackPatterns.java`](java/05-linkedlist-stack-tree/Q30StackPatterns.java) |
| Q31 | Tree traversals, BST validation, LCA | Medium | HIGH | BFS with level-size freezing; iterative inorder with an explicit stack; bounds-based BST validation | [`Q31TreeToolkit.java`](java/05-linkedlist-stack-tree/Q31TreeToolkit.java) |
| Q37 | Thread-safe singleton | Medium | HIGH | Initialization-on-demand holder; enum singleton; double-checked locking with volatile | [`Q37ThreadSafeSingleton.java`](java/07-concurrency/Q37ThreadSafeSingleton.java) |
| Q38 | Print odd and even alternately with two threads | Medium | HIGH | Monitor coordination with wait/notifyAll; Semaphore turn-passing as the alternative | [`Q38PrintOddEvenTwoThreads.java`](java/07-concurrency/Q38PrintOddEvenTwoThreads.java) |
| Q39 | Producer–consumer with a bounded buffer | Medium | HIGH | Bounded buffer with wait/notifyAll; BlockingQueue with a poison pill for shutdown | [`Q39ProducerConsumer.java`](java/07-concurrency/Q39ProducerConsumer.java) |
| Q41 | CompletableFuture fan-out / fan-in | Medium | HIGH | Asynchronous fan-out / fan-in with allOf, per-call timeout and fallback | [`Q41CompletableFutureFanOut.java`](java/07-concurrency/Q41CompletableFutureFanOut.java) |
| Q14 | Parallel streams done safely, and a custom Collector | Hard | Medium | Mutable reduction via collect (thread-confined accumulate + combine); Collector.of for custom collectors | [`Q14ParallelStreamsAndCustomCollector.java`](java/01-streams-and-collectors/Q14ParallelStreamsAndCustomCollector.java) |
| Q32 | LRU cache | Hard | HIGH | Hash map for lookup + doubly linked list for recency, with head/tail sentinels | [`Q32LRUCache.java`](java/06-graphs-and-design/Q32LRUCache.java) |
| Q33 | Number of islands — grid traversal | Hard | Medium | Grid DFS/BFS flood fill with in-place visited marking | [`Q33NumberOfIslands.java`](java/06-graphs-and-design/Q33NumberOfIslands.java) |
| Q34 | Course schedule — topological sort and cycle detection | Hard | Medium | Kahn's algorithm - topological sort by in-degree, with free cycle detection | [`Q34CourseSchedule.java`](java/06-graphs-and-design/Q34CourseSchedule.java) |
| Q40 | Implement a bounded BlockingQueue from scratch | Hard | Medium | ReentrantLock with two Conditions over a circular array buffer | [`Q40CustomBlockingQueue.java`](java/07-concurrency/Q40CustomBlockingQueue.java) |
| Q42 | Thread-safe LRU cache | Hard | Medium | Single-lock guard over an access-ordered LinkedHashMap; approximate LRU for scale | [`Q42ThreadSafeLruCache.java`](java/07-concurrency/Q42ThreadSafeLruCache.java) |
| Q43 | Rate limiter | Hard | Medium | Token bucket backed by a Semaphore; sliding-window log per key | [`Q43RateLimiter.java`](java/07-concurrency/Q43RateLimiter.java) |
| Q44 | Deadlock: create it, fix it, detect it | Hard | HIGH | Global lock ordering; tryLock with timeout and jittered backoff; ThreadMXBean detection | [`Q44DeadlockCreateFixDetect.java`](java/07-concurrency/Q44DeadlockCreateFixDetect.java) |
| Q45 | ThreadPoolExecutor tuning and graceful shutdown | Hard | HIGH | Explicit ThreadPoolExecutor with a bounded queue and CallerRunsPolicy backpressure | [`Q45ThreadPoolTuning.java`](java/07-concurrency/Q45ThreadPoolTuning.java) |

## SQL — 22 problem sets, 101 runnable queries

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| S01 | Customers who have never placed an order   (the anti-join) | Easy | High | Anti-join -- LEFT JOIN ... WHERE right IS NULL, or NOT EXISTS | [`S01_customers_with_no_orders.sql`](sql/01-joins-and-anti-joins/S01_customers_with_no_orders.sql) |
| S02 | Employees with their manager's name   (self-join) | Easy | High | Self-join on manager_id = emp_id, with LEFT JOIN to keep the root | [`S02_self_join_manager.sql`](sql/01-joins-and-anti-joins/S02_self_join_manager.sql) |
| S03 | Revenue is double-counted after a join -- find and fix it | Medium | High | Aggregate to a single grain BEFORE joining (pre-aggregation / derived table) | [`S03_join_grain_double_counting.sql`](sql/01-joins-and-anti-joins/S03_join_grain_double_counting.sql) |
| S04 | A zero-filled grid of every category x every month   (CROSS JOIN) | Medium | Medium | CROSS JOIN to build a complete grid, LEFT JOIN the facts onto it | [`S04_cross_join_zero_filled_grid.sql`](sql/01-joins-and-anti-joins/S04_cross_join_zero_filled_grid.sql) |
| S05 | Second and Nth highest salary | Easy | High | DENSE_RANK over the distinct salary ladder; MAX of a bounded subquery | [`S05_nth_highest_salary.sql`](sql/02-aggregation-and-grouping/S05_nth_highest_salary.sql) |
| S06 | Find duplicates -- the values, and then every offending row | Easy | High | GROUP BY ... HAVING COUNT(*) > 1, then join back for the full rows | [`S06_duplicate_detection.sql`](sql/02-aggregation-and-grouping/S06_duplicate_detection.sql) |
| S07 | Conditional aggregation -- rows into columns without PIVOT | Medium | Medium | SUM(CASE WHEN ... THEN ... ELSE 0 END) -- portable pivoting | [`S07_conditional_aggregation_pivot.sql`](sql/02-aggregation-and-grouping/S07_conditional_aggregation_pivot.sql) |
| S08 | WHERE vs HAVING, and how NULL behaves in aggregates | Easy | High | Logical clause order; NULL semantics in comparisons and aggregates | [`S08_having_vs_where_and_nulls.sql`](sql/02-aggregation-and-grouping/S08_having_vs_where_and_nulls.sql) |
| S09 | Top N highest-paid employees in each department | Medium | High | ROW_NUMBER / RANK / DENSE_RANK over PARTITION BY, filtered in an outer query | [`S09_top_n_per_group.sql`](sql/03-window-functions/S09_top_n_per_group.sql) |
| S10 | Running total, moving average, and percent of total | Medium | High | Aggregate OVER a window frame (ROWS BETWEEN ... AND CURRENT ROW) | [`S10_running_total_and_moving_average.sql`](sql/03-window-functions/S10_running_total_and_moving_average.sql) |
| S11 | LAG and LEAD -- month-over-month change and first salary increase | Medium | High | LAG / LEAD to reach the previous or next row without a self-join | [`S11_lag_lead_period_over_period.sql`](sql/03-window-functions/S11_lag_lead_period_over_period.sql) |
| S12 | Employee hierarchy with depth   (recursive CTE) | Hard | Medium | WITH RECURSIVE -- anchor member UNION ALL recursive member | [`S12_recursive_employee_hierarchy.sql`](sql/04-cte-and-recursive/S12_recursive_employee_hierarchy.sql) |
| S13 | Fill missing months with a recursive date spine | Medium | Medium | Recursive CTE generating a calendar, LEFT JOIN the facts onto it | [`S13_date_spine_fill_gaps.sql`](sql/04-cte-and-recursive/S13_date_spine_fill_gaps.sql) |
| S14 | Hire within a budget   (running total + CTE + UNION ALL) | Hard | Medium | Greedy selection via a running total, chained across two priority tiers | [`S14_running_total_budget_hiring.sql`](sql/04-cte-and-recursive/S14_running_total_budget_hiring.sql) |
| S15 | Group events into sessions when the gap exceeds 30 minutes | Hard | Medium | LAG to find gaps, then a running SUM of the gap flag as a session id | [`S15_sessionise_logins.sql`](sql/05-gaps-islands-and-sessions/S15_sessionise_logins.sql) |
| S16 | Consecutive login days   (the date-minus-row-number trick) | Hard | Medium | date - ROW_NUMBER() is constant inside a consecutive run | [`S16_consecutive_days_streak.sql`](sql/05-gaps-islands-and-sessions/S16_consecutive_days_streak.sql) |
| S17 | Duplicate payments by the same customer within 10 minutes | Medium | Medium | LAG within a partition plus a time-difference predicate (or a bounded self-join) | [`S17_duplicate_payments_time_window.sql`](sql/05-gaps-islands-and-sessions/S17_duplicate_payments_time_window.sql) |
| S18 | Delete duplicate rows, keeping exactly one | Medium | High | ROW_NUMBER() over the duplicate key, then delete everything with rn > 1 | [`S18_delete_duplicates_keep_one.sql`](sql/06-dedup-and-data-quality/S18_delete_duplicates_keep_one.sql) |
| S19 | The latest row per key   (the most reused query in any codebase) | Medium | High | ROW_NUMBER() = 1 over PARTITION BY key ORDER BY timestamp DESC | [`S19_latest_row_per_key.sql`](sql/06-dedup-and-data-quality/S19_latest_row_per_key.sql) |
| S20 | Make a WHERE clause index-usable   (sargability) | Medium | High | Keep the indexed column bare on the left of the predicate; rewrite to a range | [`S20_sargability_and_indexes.sql`](sql/07-performance-and-concepts/S20_sargability_and_indexes.sql) |
| S21 | NOT IN with a NULL returns nothing   (the three-valued-logic trap) | Medium | High | NOT EXISTS instead of NOT IN; understand UNKNOWN in three-valued logic | [`S21_not_in_null_trap.sql`](sql/07-performance-and-concepts/S21_not_in_null_trap.sql) |
| S22 | Quick-fire concepts -- the ones asked in every single round | Easy | High | Definitions you must be able to give in one sentence, each with a live example | [`S22_concepts_quickfire.sql`](sql/07-performance-and-concepts/S22_concepts_quickfire.sql) |
