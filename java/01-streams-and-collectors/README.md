# Java 8 Streams & Collectors

16 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| Q01 | Character and word frequency count | Easy | HIGH | P1 | Hash-map counting + Collectors.groupingBy with a map supplier and a downstream collector | [`Q01CharacterAndWordFrequency.java`](Q01CharacterAndWordFrequency.java) |
| Q02 | First non-repeating character | Easy | HIGH | P1 | Two-pass frequency count; order preserved by LinkedHashMap | [`Q02FirstNonRepeatingCharacter.java`](Q02FirstNonRepeatingCharacter.java) |
| Q03 | Find duplicates in a collection | Easy | HIGH | P1 | Set membership as a predicate (Set.add returns false on a repeat) | [`Q03FindDuplicates.java`](Q03FindDuplicates.java) |
| Q05 | Sort a Map by its values | Easy | HIGH | P1 | Entry stream + comparator chain + LinkedHashMap collector | [`Q05SortMapByValue.java`](Q05SortMapByValue.java) |
| Q53 | Sort by several fields, one of them descending, with nulls | Easy-Medium | HIGH | P1 | Comparator chain: comparing + thenComparing, reverse one key only, nullsLast / nullsFirst | [`Q53SortByMultipleFieldsWithNulls.java`](Q53SortByMultipleFieldsWithNulls.java) |
| Q04 | Distinct by a field | Medium | Medium | P2 | toMap with a merge function (duplicate policy made explicit) | [`Q04DistinctByField.java`](Q04DistinctByField.java) |
| Q06 | Group by department and aggregate | Medium | HIGH | P1 | Collectors.groupingBy with downstream collectors (counting, averaging, mapping, summarizing, nested groupingBy) | [`Q06GroupByAndAggregate.java`](Q06GroupByAndAggregate.java) |
| Q07 | Highest-paid employee in each department | Medium | HIGH | P1 | groupingBy + maxBy wrapped in collectingAndThen to unwrap the Optional | [`Q07MaxPerGroup.java`](Q07MaxPerGroup.java) |
| Q08 | Second highest salary / second largest number | Medium | HIGH | P1 | distinct + sort + skip (readable), then a single-pass two-variable scan (optimal) | [`Q08SecondHighest.java`](Q08SecondHighest.java) |
| Q10 | map vs flatMap | Medium | HIGH | P1 | flatMap - one-to-many mapping followed by concatenation | [`Q10MapVsFlatMap.java`](Q10MapVsFlatMap.java) |
| Q52 | Money with BigDecimal: total per currency, rounding, equals, and an exact split | Medium | HIGH | P1 | groupingBy + reducing(BigDecimal.ZERO, mapper, BigDecimal::add); explicit RoundingMode; split in minor units | [`Q52MoneyWithBigDecimal.java`](Q52MoneyWithBigDecimal.java) |
| Q09 | Top N employees per department | Medium | Medium | P2 | groupingBy + collectingAndThen post-processing; bounded min-heap for large groups | [`Q09TopNPerGroup.java`](Q09TopNPerGroup.java) |
| Q11 | Group anagrams | Medium | Medium | P2 | Canonical signature as a hash key (sorted characters, or a 26-slot count signature) | [`Q11GroupAnagrams.java`](Q11GroupAnagrams.java) |
| Q12 | Partition, and summary statistics in one pass | Medium | Medium | P2 | partitioningBy (always both keys) and summarizing collectors (one traversal, five aggregates) | [`Q12PartitionAndStatistics.java`](Q12PartitionAndStatistics.java) |
| Q13 | Merge two maps, summing values by key | Medium | Medium | P2 | toMap with a merge function; Map.merge for the in-place form | [`Q13MergeMaps.java`](Q13MergeMaps.java) |
| Q14 | Parallel streams done safely, and a custom Collector | Hard | Medium | P2 | Mutable reduction via collect (thread-confined accumulate + combine); Collector.of for custom collectors | [`Q14ParallelStreamsAndCustomCollector.java`](Q14ParallelStreamsAndCustomCollector.java) |

## Is folder ko kaise padhein

Ye folder har round mein aata hai, isliye P1 wale saare (Q01, Q02, Q03, Q05, Q53, Q06, Q07, Q08,
Q10, Q52) memory se do baar type karo. Q06 sabse zaroori hai: groupingBy ka doosra argument ek
aur Collector ho sakta hai, ye samajh aaya to har "per department" sawaal same lagta hai.
Q52 fintech ke liye hai: paisa kabhi double mein nahi. P2 wale ek baar type karke AISE BOLO bolo.

## Mock round

| Interviewer | Aap |
|---|---|
| From a list of employees, give me the average salary per department, sorted by department. | groupingBy(Employee::getDept, TreeMap::new, averagingDouble(Employee::getSalary)). The TreeMap supplier gives me the sorted keys in the same pass. |
| Now the highest-paid employee in each department, not an Optional. | groupingBy with maxBy(comparing(salary)) inside collectingAndThen(..., Optional::get). The Optional is never empty there, because groupingBy only creates a group when there is at least one element. |
| Two people share the top salary. Who comes back? | maxBy keeps the first one it saw in encounter order. If the business wants both, I group to a list and filter by the max, or define a tiebreaker on name or id. |
| These are payment amounts, not salaries. Anything you would change? | Yes, I would never sum money in double. I use BigDecimal and groupingBy(currency, reducing(BigDecimal.ZERO, Txn::amount, BigDecimal::add)), and round only at the end with an explicit RoundingMode. |
| Sort the employees by department, then salary descending, then name. | comparing(dept).thenComparing(salary, reverseOrder()).thenComparing(name). I put reverseOrder on the salary key only; reversed() at the end would flip the whole chain. |
| Some departments are null. | nullsLast(naturalOrder()) as the key comparator for department, otherwise the comparison throws a NullPointerException. |
| Would you make this a parallel stream for ten million records? | Only if it is CPU-bound, the source splits well, like an ArrayList, and I have measured it. It runs on the common ForkJoinPool, so on a server with blocking calls I would not. |
| Map vs flatMap in one line? | map gives one output per input; flatMap gives zero or more and flattens them, so a List of Lists becomes one Stream. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| groupingBy ka default map kaunsa? | HashMap; order chahiye to TreeMap::new ya LinkedHashMap::new supplier do. |
| partitioningBy vs groupingBy? | partitioningBy mein hamesha true aur false dono keys aati hain, khaali ho tab bhi. |
| toMap mein duplicate key aaye to? | IllegalStateException; teesra argument (merge function) do. |
| toMap mein null value? | NullPointerException, kyunki andar HashMap.merge use hota hai. |
| Sort map by value? | entrySet().stream().sorted(Map.Entry.comparingByValue()) phir LinkedHashMap mein collect. |
| First non-repeating char mein LinkedHashMap kyun? | Insertion order yaad rakhta hai; HashMap hash order deta hai. |
| Stream dobara use karo to? | IllegalStateException: stream has already been operated upon or closed. |
| Stream.toList() vs collect(toList())? | toList() (Java 16+) unmodifiable list deta hai; collect(toList()) ki guarantee nahi. |
| 0.1 + 0.2 double mein? | 0.30000000000000004; paise ke liye BigDecimal, aur new BigDecimal("0.1") ya valueOf. |
| BigDecimal("1.0").equals("1.00")? | false (scale alag); value compare karni ho to compareTo == 0. |
| reversed() chain ke end mein? | Poori chain ulti ho jaati hai; ek key ke liye reverseOrder() us key par lagao. |
| Parallel stream kis pool par? | Common ForkJoinPool (cores - 1 threads); blocking kaam yahan mat daalo. |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
