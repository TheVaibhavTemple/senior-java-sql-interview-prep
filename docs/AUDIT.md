# Audit: kya relevant hai, kya galat tha, kya joda

Target: **8 saal ka Senior Java backend role in India** (service companies, product / fintech,
GCCs, banks), Java 21 + Spring Boot + Oracle / PostgreSQL. Har file ko chala kar check kiya (Java
JDK 21 par, SQL SQLite 3.50 par), aur har sawaal ko is nazar se dekha ki ye round mein aata hai ya nahi.

## Short mein

- **Purane 67 sawaal: sab relevant, koi delete nahi kiya.** Sab sahi chalte the, lekin 25+ jagah
  galat claim, kati hui lines ya demo bug the; sab theek kiye (neeche list).
- **6 sawaal P3 kiye** (Q33, Q34, S04, S13, S14, S15): graph / data-engineering flavour, backend
  rounds mein kam aate hain. Delete nahi kiya, kyunki sahi hain aur product rounds mein kabhi aa
  jaate hain. Time kam ho to skip.
- **17 naye sawaal** joda jo is profile ke liye zaroori the aur repo mein nahi the (neeche).
- Har file ab mere format mein hai: PRIORITY, SAMJHO, AISE BOLO, TRAP, FOLLOW-UPS KE JAWAB
  ([STYLE.md](STYLE.md)). Har folder README mein Mock round aur Rapid fire.

## Repo ke bugs (tooling)

| Kahan | Kya galat tha | Fix |
|---|---|---|
| `run-java.sh` | Windows par git CRLF se checkout karta hai, isliye 45 mein se 41 file "DIFF" dikhati thi jabki output sahi tha | Dono taraf se `\r` hata kar compare; `.gitattributes` se `.sh` hamesha LF |
| `sql/run_all.py` | Windows par crash: purana connection band kiye bina `prep.db` delete karta tha | Har file ke baad `con.close()` |
| Folder READMEs | File links repo-root path se the, folder ke andar se GitHub par 404 | Links ab same-folder |
| `docs/INDEX.md` | Links `docs/` ke relative toot rahe the | Index regenerate, `../` links |

## Content ke bugs jo theek kiye

| ID | Kya galat tha |
|---|---|
| Q02, Q07, Q20 | APPROACHES table ki lines beech mein kati hui thi |
| Q03 | Parallel demo do alag cheezein compare kar raha tha (count vs set size); code aur output theek |
| Q13 | Comment "allocation-free" galat tha |
| Q16 | `isAnagramGeneral` case / space handle karne ka daawa karta tha, karta nahi tha; code theek |
| Q24 | "Buggy" version sample par sorted output de raha tha, bug dikhta hi nahi tha; naya input jo bug dikhata hai |
| Q26 | Bucket top-K ties par k se zyada items de sakta tha |
| Q27 | Running median mein int overflow |
| Q28 | `lastOccurrence(Integer.MAX_VALUE)` overflow se -1 deta tha |
| Q30, Q33, Q34 | Complexity / "only way" claims galat (Q34 space O(V) nahi, O(V+E)) |
| Q36 | Header 16 threads bolta tha, code 8 chalata hai |
| Q38 | "Do threads mein notify fail hota hai" galat; teen ya zyada mein hota hai |
| Q42 | Option B ko "approximate LRU" kaha tha, asal mein FIFO hai |
| Q43 | Fixed-window ki kamzori wala demo window boundary cross hi nahi karta tha; code aur output theek |
| S01, S02, S04, S06, S08, S11 | Doosri files ke galat ID (cross-references) |
| S03, S05, S08 | `rows`, `rank`, `row_number` jaise alias Oracle / MySQL mein reserved hain; rename |
| S05 | "Asli lesson" wali ranking query `DISTINCT` ki wajah se tie dikhati hi nahi thi |
| S07 | COUNT/COUNT integer division ka claim Oracle / MySQL par galat tha |
| S08 | "NULL in aggregates" demo mein koi NULL hi nahi tha; proof query joda |
| S09, S10, S13 | WHY / dialect claims galat ya adhure |
| S15, S17 | `julianday` float maths: theek 30 / 10 minute ka gap galat side par chala jaata tha; exact `datetime(..., '+30 minutes')` |
| S19 | "Pehli row per key" query mein tiebreaker nahi tha |
| S20 | Index wala comparison fair nahi tha (dono queries alag predicates par) |
| S22 | "TRUNCATE identity reset karta hai" har DB par sach nahi |

## Har sawaal ka faisla

| ID | Sawaal | Priority | Faisla |
|---|---|---|---|
| [`Q01`](../java/01-streams-and-collectors/Q01CharacterAndWordFrequency.java) | Character and word frequency count | P1 | Keep: three-argument groupingBy har round mein |
| [`Q02`](../java/01-streams-and-collectors/Q02FirstNonRepeatingCharacter.java) | First non-repeating character | P1 | Keep: classic, asal mein order ka sawaal |
| [`Q03`](../java/01-streams-and-collectors/Q03FindDuplicates.java) | Find duplicates in a collection | P1 | Keep: streams duplicates sabse common |
| [`Q04`](../java/01-streams-and-collectors/Q04DistinctByField.java) | Distinct by a field | P2 | Keep: key se dedupe, fintech ka roz ka kaam |
| [`Q05`](../java/01-streams-and-collectors/Q05SortMapByValue.java) | Sort a Map by its values | P1 | Keep: sort karke HashMap mein daalna, classic bug |
| [`Q06`](../java/01-streams-and-collectors/Q06GroupByAndAggregate.java) | Group by department and aggregate | P1 | Keep: poori Collectors class ek sawaal mein |
| [`Q07`](../java/01-streams-and-collectors/Q07MaxPerGroup.java) | Highest-paid employee in each department | P1 | Keep: "max per department" favourite |
| [`Q08`](../java/01-streams-and-collectors/Q08SecondHighest.java) | Second highest salary / second largest number | P1 | Keep: stream aur one-pass dono jawab chahiye |
| [`Q09`](../java/01-streams-and-collectors/Q09TopNPerGroup.java) | Top N employees per department | P2 | Keep: collectingAndThen; product / GCC rounds |
| [`Q10`](../java/01-streams-and-collectors/Q10MapVsFlatMap.java) | map vs flatMap | P1 | Keep: Indian Java rounds ka core |
| [`Q11`](../java/01-streams-and-collectors/Q11GroupAnagrams.java) | Group anagrams | P2 | Keep: LC 49, groupingBy practice |
| [`Q12`](../java/01-streams-and-collectors/Q12PartitionAndStatistics.java) | Partition, and summary statistics in one pass | P2 | Keep: partitioningBy vs groupingBy gotcha |
| [`Q13`](../java/01-streams-and-collectors/Q13MergeMaps.java) | Merge two maps, summing values by key | P2 | Keep: merge function se ConcurrentHashMap.merge tak |
| [`Q14`](../java/01-streams-and-collectors/Q14ParallelStreamsAndCustomCollector.java) | Parallel streams done safely, and a custom Collector | P2 | Keep: "parallel kab nahi" wala sawaal |
| [`Q15`](../java/02-arrays-and-hashing/Q15TwoSum.java) | Two Sum -- the HashMap complement pattern | P1 | Keep: HashMap complement habit, har jagah |
| [`Q16`](../java/02-arrays-and-hashing/Q16ValidAnagram.java) | Valid anagram and character counting | P1 | Keep: character counting ka base |
| [`Q17`](../java/03-two-pointers-and-sliding-window/Q17ReverseAndRotate.java) | Reverse a string, reverse words, rotate an array | P1 | Keep: service round warm-up |
| [`Q18`](../java/03-two-pointers-and-sliding-window/Q18PalindromeFamily.java) | Palindrome with two pointers | P1 | Keep: palindrome family, warm-up |
| [`Q19`](../java/02-arrays-and-hashing/Q19XorTrio.java) | Missing number, single number, duplicate -- the XOR trio | P2 | Keep: missing number service rounds mein, Floyd product mein |
| [`Q20`](../java/04-sorting-searching-heaps/Q20MergeSortedSequences.java) | Merge sorted sequences | P2 | Keep: merge sorted / K lists, product rounds |
| [`Q21`](../java/03-two-pointers-and-sliding-window/Q21SlidingWindowTemplate.java) | Sliding window -- the template that covers a dozen problems | P1 | Keep: sabse zyada leverage wala template |
| [`Q22`](../java/03-two-pointers-and-sliding-window/Q22FixedWindowAndKadane.java) | Fixed window and Kadane | P1 | Keep: fixed window + Kadane |
| [`Q23`](../java/02-arrays-and-hashing/Q23SubarraySumEqualsK.java) | Subarray sum equals K -- prefix sum plus HashMap | P2 | Keep: prefix-sum template |
| [`Q24`](../java/03-two-pointers-and-sliding-window/Q24DutchNationalFlag.java) | Dutch national flag -- three-way partition | P2 | Keep: "sort 0s 1s 2s" service rounds mein bhi |
| [`Q25`](../java/04-sorting-searching-heaps/Q25MergeIntervals.java) | Merge intervals and meeting rooms | P2 | Keep: product / fintech ka common medium |
| [`Q26`](../java/02-arrays-and-hashing/Q26TopKFrequent.java) | Top K frequent elements | P2 | Keep: heap direction wala sawaal |
| [`Q27`](../java/04-sorting-searching-heaps/Q27KthLargestAndMedian.java) | Kth largest, and the running median | P2 | Keep: Kth largest common; median P3 ke kareeb |
| [`Q28`](../java/04-sorting-searching-heaps/Q28BinarySearchVariants.java) | Binary search and every variant they ask for | P1 | Keep: binary search ke saare variants |
| [`Q29`](../java/05-linkedlist-stack-tree/Q29LinkedListToolkit.java) | Linked list: reverse, cycle, middle | P1 | Keep: linked list basics |
| [`Q30`](../java/05-linkedlist-stack-tree/Q30StackPatterns.java) | Stacks: parentheses, min stack, monotonic stack | P2 | Keep: balanced brackets warm-up |
| [`Q31`](../java/05-linkedlist-stack-tree/Q31TreeToolkit.java) | Tree traversals, BST validation, LCA | P2 | Keep: tree basics, kabhi-kabhi |
| [`Q32`](../java/06-graphs-and-design/Q32LRUCache.java) | LRU cache | P1 | Keep: LRU, sabse important design sawaal |
| [`Q33`](../java/06-graphs-and-design/Q33NumberOfIslands.java) | Number of islands -- grid traversal | P3 | P3: graphs Java backend rounds mein kam |
| [`Q34`](../java/06-graphs-and-design/Q34CourseSchedule.java) | Course schedule -- topological sort and cycle detection | P3 | P3: repo chhota karna ho to pehle ye |
| [`Q35`](../java/07-concurrency/Q35CallableFutureAndShutdown.java) | Threads, Callable, Future and clean shutdown | P1 | Keep: executor + clean shutdown |
| [`Q36`](../java/07-concurrency/Q36ThreadSafeCounter.java) | Thread-safe counter | P1 | Keep: "volatile kaafi kyun nahi" |
| [`Q37`](../java/07-concurrency/Q37ThreadSafeSingleton.java) | Thread-safe singleton | P1 | Keep: singleton, India mein bahut common |
| [`Q38`](../java/07-concurrency/Q38PrintOddEvenTwoThreads.java) | Print odd and even alternately with two threads | P1 | Keep: sabse common multithreading coding sawaal |
| [`Q39`](../java/07-concurrency/Q39ProducerConsumer.java) | Producer-consumer with a bounded buffer | P1 | Keep: producer-consumer |
| [`Q40`](../java/07-concurrency/Q40CustomBlockingQueue.java) | Implement a bounded BlockingQueue from scratch | P2 | Keep: Condition ke saath apni queue |
| [`Q41`](../java/07-concurrency/Q41CompletableFutureFanOut.java) | CompletableFuture fan-out / fan-in | P1 | Keep: CompletableFuture fan-out / fan-in |
| [`Q42`](../java/07-concurrency/Q42ThreadSafeLruCache.java) | Thread-safe LRU cache | P2 | Keep: thread-safe cache |
| [`Q43`](../java/07-concurrency/Q43RateLimiter.java) | Rate limiter | P2 | Keep: fintech aur LLD rounds |
| [`Q44`](../java/07-concurrency/Q44DeadlockCreateFixDetect.java) | Deadlock: create it, fix it, detect it | P1 | Keep: deadlock create / fix / detect |
| [`Q45`](../java/07-concurrency/Q45ThreadPoolTuning.java) | ThreadPoolExecutor tuning and graceful shutdown | P1 | Keep: pool sizing aur execution order |
| [`Q46`](../java/08-core-java-and-java21/Q46ImmutableClassAndRecord.java) | Immutable class, defensive copies, and the record gotcha | P1 | Naya: immutable class har senior round mein |
| [`Q47`](../java/08-core-java-and-java21/Q47EqualsHashCodeContract.java) | The equals / hashCode contract: three ways a key goes missing | P1 | Naya: equals / hashCode contract |
| [`Q48`](../java/08-core-java-and-java21/Q48Java21FeaturesIUse.java) | Java 8 to 21: the features a backend developer actually uses | P1 | Naya: Java 8 se 21 kya badla; migration story |
| [`Q49`](../java/08-core-java-and-java21/Q49VirtualThreads.java) | Virtual threads: 10,000 blocking calls, virtual vs a platform pool | P1 | Naya: virtual threads, kaam par use kiye |
| [`Q50`](../java/08-core-java-and-java21/Q50RetryWithBackoffAndIdempotency.java) | Retry with exponential backoff and jitter, and an idempotent consumer | P2 | Naya: retry + idempotency; platform story |
| [`Q51`](../java/08-core-java-and-java21/Q51SynchronizedMapVsConcurrentHashMap.java) | A synchronized shared HashMap vs ConcurrentHashMap in a batch job | P1 | Naya: batch story, 45 min se 8 min |
| [`Q52`](../java/01-streams-and-collectors/Q52MoneyWithBigDecimal.java) | Money with BigDecimal: total per currency, rounding, equals, and an exact split | P1 | Naya: paisa kabhi double mein nahi |
| [`Q53`](../java/01-streams-and-collectors/Q53SortByMultipleFieldsWithNulls.java) | Sort by several fields, one of them descending, with nulls | P1 | Naya: multi-field sort, bahut common |
| [`Q54`](../java/06-graphs-and-design/Q54HashMapFromScratch.java) | HashMap from scratch: buckets, hash spreading, resize and treeify | P1 | Naya: HashMap internals, senior staple |
| [`Q55`](../java/04-sorting-searching-heaps/Q55RateAtTimestampWithTreeMap.java) | Which FX rate applied at time t? TreeMap.floorEntry vs binary search | P2 | Naya: kis time ka FX rate, remittance |
| [`Q56`](../java/07-concurrency/Q56LatchBarrierSemaphore.java) | CountDownLatch, CyclicBarrier, Semaphore -- and ReentrantLock vs synchronized | P1 | Naya: latch vs barrier vs semaphore |
| [`S01`](../sql/01-joins-and-anti-joins/S01_customers_with_no_orders.sql) | Customers who have never placed an order   (the anti-join) | P1 | Keep: anti-join + NOT IN trap |
| [`S02`](../sql/01-joins-and-anti-joins/S02_self_join_manager.sql) | Employees with their manager's name   (self-join) | P1 | Keep: classic self-join |
| [`S03`](../sql/01-joins-and-anti-joins/S03_join_grain_double_counting.sql) | Revenue is double-counted after a join -- find and fix it | P2 | Keep: join fan-out, sabse common reporting bug |
| [`S04`](../sql/01-joins-and-anti-joins/S04_cross_join_zero_filled_grid.sql) | A zero-filled grid of every category x every month   (CROSS JOIN) | P3 | P3: reporting flavour |
| [`S05`](../sql/02-aggregation-and-grouping/S05_nth_highest_salary.sql) | Second and Nth highest salary | P1 | Keep: sabse zyada poocha jaane wala SQL |
| [`S06`](../sql/02-aggregation-and-grouping/S06_duplicate_detection.sql) | Find duplicates -- the values, and then every offending row | P1 | Keep: duplicates dhoondhna |
| [`S07`](../sql/02-aggregation-and-grouping/S07_conditional_aggregation_pivot.sql) | Conditional aggregation -- rows into columns without PIVOT | P2 | Keep: PIVOT ke bina pivot |
| [`S08`](../sql/02-aggregation-and-grouping/S08_having_vs_where_and_nulls.sql) | WHERE vs HAVING, and how NULL behaves in aggregates | P1 | Keep: WHERE vs HAVING, NULL |
| [`S09`](../sql/03-window-functions/S09_top_n_per_group.sql) | Top N highest-paid employees in each department | P1 | Keep: top N per group, ties ke saath |
| [`S10`](../sql/03-window-functions/S10_running_total_and_moving_average.sql) | Running total, moving average, and percent of total | P1 | Keep: running total, ROWS vs RANGE |
| [`S11`](../sql/03-window-functions/S11_lag_lead_period_over_period.sql) | LAG and LEAD -- month-over-month change and first salary increase | P2 | Keep: pichhli row se compare |
| [`S12`](../sql/04-cte-and-recursive/S12_recursive_employee_hierarchy.sql) | Employee hierarchy with depth   (recursive CTE) | P2 | Keep: hierarchy; Oracle CONNECT BY |
| [`S13`](../sql/04-cte-and-recursive/S13_date_spine_fill_gaps.sql) | Fill missing months with a recursive date spine | P3 | P3: reporting / data role |
| [`S14`](../sql/04-cte-and-recursive/S14_running_total_budget_hiring.sql) | Hire within a budget   (running total + CTE + UNION ALL) | P3 | P3: LC hard puzzle, rare |
| [`S15`](../sql/05-gaps-islands-and-sessions/S15_sessionise_logins.sql) | Group events into sessions when the gap exceeds 30 minutes | P3 | P3: product analytics flavour |
| [`S16`](../sql/05-gaps-islands-and-sessions/S16_consecutive_days_streak.sql) | Consecutive login days   (the date-minus-row-number trick) | P2 | Keep: consecutive days (LC 180 type) |
| [`S17`](../sql/05-gaps-islands-and-sessions/S17_duplicate_payments_time_window.sql) | Duplicate payments by the same customer within 10 minutes | P1 | Keep: fintech ke liye top |
| [`S18`](../sql/06-dedup-and-data-quality/S18_delete_duplicates_keep_one.sql) | Delete duplicate rows, keeping exactly one | P1 | Keep: duplicates delete karna |
| [`S19`](../sql/06-dedup-and-data-quality/S19_latest_row_per_key.sql) | The latest row per key   (the most reused query in any codebase) | P1 | Keep: latest row per key |
| [`S20`](../sql/07-performance-and-concepts/S20_sargability_and_indexes.sql) | Make a WHERE clause index-usable   (sargability) | P1 | Keep: slow query + apni index story |
| [`S21`](../sql/07-performance-and-concepts/S21_not_in_null_trap.sql) | NOT IN with a NULL returns nothing   (the three-valued-logic trap) | P1 | Keep: NOT IN + NULL |
| [`S22`](../sql/07-performance-and-concepts/S22_concepts_quickfire.sql) | Quick-fire concepts -- the ones asked in every single round | P1 | Keep: quick-fire definitions |
| [`S23`](../sql/03-window-functions/S23_salary_above_department_average.sql) | Employees earning more than their department average | P1 | Naya: dept average se zyada, most Indian rounds |
| [`S24`](../sql/01-joins-and-anti-joins/S24_reconciliation_bank_vs_ledger.sql) | Reconcile the bank statement against the ledger   (FULL OUTER JOIN + status) | P1 | Naya: reconciliation; recon story |
| [`S25`](../sql/03-window-functions/S25_running_balance_per_account.sql) | Running balance per account   (the balance after every transaction) | P1 | Naya: running balance per account |
| [`S26`](../sql/07-performance-and-concepts/S26_keyset_vs_offset_pagination.sql) | Keyset vs OFFSET pagination   (page by the last row seen, not by a row count) | P1 | Naya: keyset pagination, Pageable follow-up |
| [`S27`](../sql/07-performance-and-concepts/S27_idempotent_upsert.sql) | Idempotent insert and upsert   (the same message arrives twice) | P1 | Naya: idempotent upsert |
| [`S28`](../sql/01-joins-and-anti-joins/S28_join_row_count_puzzle.sql) | Join row-count puzzle, and ON vs WHERE on a LEFT JOIN | P1 | Naya: join row-count puzzle, kagaz par |

## Jo joda, aur kyun

| ID | Sawaal | Priority | Kyun joda |
|---|---|---|---|
| [`Q46`](../java/08-core-java-and-java21/Q46ImmutableClassAndRecord.java) | Immutable class, defensive copies, and the record gotcha | P1 | immutable class har senior round mein |
| [`Q47`](../java/08-core-java-and-java21/Q47EqualsHashCodeContract.java) | The equals / hashCode contract: three ways a key goes missing | P1 | equals / hashCode contract |
| [`Q48`](../java/08-core-java-and-java21/Q48Java21FeaturesIUse.java) | Java 8 to 21: the features a backend developer actually uses | P1 | Java 8 se 21 kya badla; migration story |
| [`Q49`](../java/08-core-java-and-java21/Q49VirtualThreads.java) | Virtual threads: 10,000 blocking calls, virtual vs a platform pool | P1 | virtual threads, kaam par use kiye |
| [`Q50`](../java/08-core-java-and-java21/Q50RetryWithBackoffAndIdempotency.java) | Retry with exponential backoff and jitter, and an idempotent consumer | P2 | retry + idempotency; platform story |
| [`Q51`](../java/08-core-java-and-java21/Q51SynchronizedMapVsConcurrentHashMap.java) | A synchronized shared HashMap vs ConcurrentHashMap in a batch job | P1 | batch story, 45 min se 8 min |
| [`Q52`](../java/01-streams-and-collectors/Q52MoneyWithBigDecimal.java) | Money with BigDecimal: total per currency, rounding, equals, and an exact split | P1 | paisa kabhi double mein nahi |
| [`Q53`](../java/01-streams-and-collectors/Q53SortByMultipleFieldsWithNulls.java) | Sort by several fields, one of them descending, with nulls | P1 | multi-field sort, bahut common |
| [`Q54`](../java/06-graphs-and-design/Q54HashMapFromScratch.java) | HashMap from scratch: buckets, hash spreading, resize and treeify | P1 | HashMap internals, senior staple |
| [`Q55`](../java/04-sorting-searching-heaps/Q55RateAtTimestampWithTreeMap.java) | Which FX rate applied at time t? TreeMap.floorEntry vs binary search | P2 | kis time ka FX rate, remittance |
| [`Q56`](../java/07-concurrency/Q56LatchBarrierSemaphore.java) | CountDownLatch, CyclicBarrier, Semaphore -- and ReentrantLock vs synchronized | P1 | latch vs barrier vs semaphore |
| [`S23`](../sql/03-window-functions/S23_salary_above_department_average.sql) | Employees earning more than their department average | P1 | dept average se zyada, most Indian rounds |
| [`S24`](../sql/01-joins-and-anti-joins/S24_reconciliation_bank_vs_ledger.sql) | Reconcile the bank statement against the ledger   (FULL OUTER JOIN + status) | P1 | reconciliation; recon story |
| [`S25`](../sql/03-window-functions/S25_running_balance_per_account.sql) | Running balance per account   (the balance after every transaction) | P1 | running balance per account |
| [`S26`](../sql/07-performance-and-concepts/S26_keyset_vs_offset_pagination.sql) | Keyset vs OFFSET pagination   (page by the last row seen, not by a row count) | P1 | keyset pagination, Pageable follow-up |
| [`S27`](../sql/07-performance-and-concepts/S27_idempotent_upsert.sql) | Idempotent insert and upsert   (the same message arrives twice) | P1 | idempotent upsert |
| [`S28`](../sql/01-joins-and-anti-joins/S28_join_row_count_puzzle.sql) | Join row-count puzzle, and ON vs WHERE on a LEFT JOIN | P1 | join row-count puzzle, kagaz par |

## Agents ne aur kya suggest kiya (abhi nahi joda)

- Stream laziness aur reuse (`peek`, "stream has already been operated upon") - Rapid fire mein hai.
- Longest consecutive sequence (LC 128), product of array except self (LC 238).
- FIRST_VALUE / LAST_VALUE ka default-frame trap; manager chain upar ki taraf (recursive CTE).
- DB mein concurrent balance update (`@Version` vs `SELECT ... FOR UPDATE`) - ye Day 7 notes mein
  hai; is repo mein bina DB ke runnable nahi banta.

## Tumhe bharna hai

`APNI STORY SE JODO` mein jahan `[bracket]` hai, wahan sirf sach likho; pata nahi to line hata do:

- [`Q13`](../java/01-streams-and-collectors/Q13MergeMaps.java): jo sach ho
- [`Q14`](../java/01-streams-and-collectors/Q14ParallelStreamsAndCustomCollector.java): jo sach ho
- [`Q21`](../java/03-two-pointers-and-sliding-window/Q21SlidingWindowTemplate.java): fill: which algorithm ours used, token bucket or sliding window
- [`Q32`](../java/06-graphs-and-design/Q32LRUCache.java): what it cached
- [`Q35`](../java/07-concurrency/Q35CallableFutureAndShutdown.java): bhar do: hamare consumers aur @Scheduled jobs shutdown par kaise rukte the
- [`Q36`](../java/07-concurrency/Q36ThreadSafeCounter.java): bhar do: merge / compute / jo sach ho
- [`Q42`](../java/07-concurrency/Q42ThreadSafeLruCache.java): bhar do: kis cheez ke liye
- [`Q43`](../java/07-concurrency/Q43RateLimiter.java): bhar do: limiter kahan laga tha aur kaunsa algorithm tha
- [`Q45`](../java/07-concurrency/Q45ThreadPoolTuning.java): bhar do: kaunsa I/O-bound kaam
- [`Q48`](../java/08-core-java-and-java21/Q48Java21FeaturesIUse.java): bhar do: jo sach mein code mein the; bhar do
- [`Q49`](../java/08-core-java-and-java21/Q49VirtualThreads.java): which service and which blocking calls; bhar do: jo sach ho
- [`Q50`](../java/08-core-java-and-java21/Q50RetryWithBackoffAndIdempotency.java): bhar do: jo sach ho
- [`Q51`](../java/08-core-java-and-java21/Q51SynchronizedMapVsConcurrentHashMap.java): bhar do: kya dekha, aur merge / compute / jo sach ho
- [`Q52`](../java/01-streams-and-collectors/Q52MoneyWithBigDecimal.java): jo sach ho
- [`Q55`](../java/04-sorting-searching-heaps/Q55RateAtTimestampWithTreeMap.java): bhar do: DB query / cache / jo sach ho
- [`Q56`](../java/07-concurrency/Q56LatchBarrierSemaphore.java): where; bhar do
- [`S17`](../sql/05-gaps-islands-and-sessions/S17_duplicate_payments_time_window.sql): sirf sach ho to
- [`S20`](../sql/07-performance-and-concepts/S20_sargability_and_indexes.sql): column + predicate, jo sach hai
- [`S24`](../sql/01-joins-and-anti-joins/S24_reconciliation_bank_vs_ledger.sql): yahan sach bharo
- [`S25`](../sql/03-window-functions/S25_running_balance_per_account.sql): yahan sach bharo
- [`S27`](../sql/07-performance-and-concepts/S27_idempotent_upsert.sql): sirf sach ho to
