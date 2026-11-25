# Java 8 Streams & Collectors

14 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| Q01 | Character and word frequency count | Easy | HIGH | Hash-map counting + Collectors.groupingBy with a map supplier and a downstream collector | [`Q01CharacterAndWordFrequency.java`](java/01-streams-and-collectors/Q01CharacterAndWordFrequency.java) |
| Q02 | First non-repeating character | Easy | HIGH | Two-pass frequency count; order preserved by LinkedHashMap | [`Q02FirstNonRepeatingCharacter.java`](java/01-streams-and-collectors/Q02FirstNonRepeatingCharacter.java) |
| Q03 | Find duplicates in a collection | Easy | HIGH | Set membership as a predicate (Set.add returns false on a repeat) | [`Q03FindDuplicates.java`](java/01-streams-and-collectors/Q03FindDuplicates.java) |
| Q05 | Sort a Map by its values | Easy | HIGH | Entry stream + comparator chain + LinkedHashMap collector | [`Q05SortMapByValue.java`](java/01-streams-and-collectors/Q05SortMapByValue.java) |
| Q04 | Distinct by a field | Medium | Medium | toMap with a merge function (duplicate policy made explicit) | [`Q04DistinctByField.java`](java/01-streams-and-collectors/Q04DistinctByField.java) |
| Q06 | Group by department and aggregate | Medium | HIGH | Collectors.groupingBy with downstream collectors (counting, averaging, mapping, summarizing, nested groupingBy) | [`Q06GroupByAndAggregate.java`](java/01-streams-and-collectors/Q06GroupByAndAggregate.java) |
| Q07 | Highest-paid employee in each department | Medium | HIGH | groupingBy + maxBy wrapped in collectingAndThen to unwrap the Optional | [`Q07MaxPerGroup.java`](java/01-streams-and-collectors/Q07MaxPerGroup.java) |
| Q08 | Second highest salary / second largest number | Medium | HIGH | distinct + sort + skip (readable), then a single-pass two-variable scan (optimal) | [`Q08SecondHighest.java`](java/01-streams-and-collectors/Q08SecondHighest.java) |
| Q09 | Top N employees per department | Medium | Medium | groupingBy + collectingAndThen post-processing; bounded min-heap for large groups | [`Q09TopNPerGroup.java`](java/01-streams-and-collectors/Q09TopNPerGroup.java) |
| Q10 | map vs flatMap | Medium | HIGH | flatMap - one-to-many mapping followed by concatenation | [`Q10MapVsFlatMap.java`](java/01-streams-and-collectors/Q10MapVsFlatMap.java) |
| Q11 | Group anagrams | Medium | Medium | Canonical signature as a hash key (sorted characters, or a 26-slot count signature) | [`Q11GroupAnagrams.java`](java/01-streams-and-collectors/Q11GroupAnagrams.java) |
| Q12 | Partition, and summary statistics in one pass | Medium | Medium | partitioningBy (always both keys) and summarizing collectors (one traversal, five aggregates) | [`Q12PartitionAndStatistics.java`](java/01-streams-and-collectors/Q12PartitionAndStatistics.java) |
| Q13 | Merge two maps, summing values by key | Medium | Medium | toMap with a merge function; Map.merge for the in-place form | [`Q13MergeMaps.java`](java/01-streams-and-collectors/Q13MergeMaps.java) |
| Q14 | Parallel streams done safely, and a custom Collector | Hard | Medium | Mutable reduction via collect (thread-confined accumulate + combine); Collector.of for custom collectors | [`Q14ParallelStreamsAndCustomCollector.java`](java/01-streams-and-collectors/Q14ParallelStreamsAndCustomCollector.java) |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
