# Graphs & Design Data Structures

3 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| Q32 | LRU cache | Hard | HIGH | Hash map for lookup + doubly linked list for recency, with head/tail sentinels | [`Q32LRUCache.java`](java/06-graphs-and-design/Q32LRUCache.java) |
| Q33 | Number of islands — grid traversal | Hard | Medium | Grid DFS/BFS flood fill with in-place visited marking | [`Q33NumberOfIslands.java`](java/06-graphs-and-design/Q33NumberOfIslands.java) |
| Q34 | Course schedule — topological sort and cycle detection | Hard | Medium | Kahn's algorithm - topological sort by in-degree, with free cycle detection | [`Q34CourseSchedule.java`](java/06-graphs-and-design/Q34CourseSchedule.java) |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
