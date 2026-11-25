# Senior Java + SQL Interview Prep

**67 interview questions, every one of them runnable.**
45 Java files and 22 SQL files, written for the **8–10 year** backend/cloud band —
the round where the brute force is assumed and the interviewer is listening for the
trade-off instead.

Every file carries, in a header comment:

| Field | What it tells you |
|---|---|
| **DIFFICULTY** | Easy / Medium / Hard (1–3) |
| **WEIGHTAGE** | HIGH = asked in almost every round · Medium = common · Low = occasional |
| **TECHNIQUE** | The named pattern, so you can file it in memory |
| **ALGORITHM** | The 4–6 steps, in order, before any syntax |
| **APPROACHES** | Every approach with time / space / verdict — what to say *out loud* first |
| **WHY THIS ONE** | Why the chosen approach wins in an interview, not just in a benchmark |
| **FOLLOW-UPS** | What lands 30 seconds after you stop typing |
| **ALSO UNLOCKS** | The other problems this one solution answers |
| **EXPECTED OUTPUT** | Real output, captured by running the file — not hand-written |

---

## Quick start

```bash
git clone https://github.com/TheVaibhavTemple/senior-java-sql-interview-prep.git
cd senior-java-sql-interview-prep

# --- Java: every file is single-file runnable, no build tool, no dependencies ---
java java/03-two-pointers-and-sliding-window/Q21SlidingWindowTemplate.java

# run all 45 and diff against the recorded output
./run-java.sh

# --- SQL: builds a seeded SQLite database and runs all 101 queries ---
python3 sql/run_all.py
```

`run-java.sh` compiles all 45 files, runs them, and diffs each one against the
`EXPECTED OUTPUT` block recorded inside it — **41 match byte-for-byte**. The other four
(`Q14`, `Q36`, `Q44`, `Q45`) are marked `NON-DETERMINISTIC` because they demonstrate data
races, lost updates, identity hashes and scheduler-dependent pool metrics; their numbers
change every run *by design*, so the script only checks that they still run clean.

Requirements: **JDK 11+** (tested on OpenJDK 21) and **Python 3.8+** with stdlib `sqlite3`
(3.25+ for window functions). Nothing else. No Maven, no Gradle, no server.

---

## The idea: patterns, not problem counts

These are not 67 unrelated problems. Each one is a **pattern representative** — solve it once
and everything in its *ALSO UNLOCKS* list becomes the same problem with different nouns.

**Five Java templates carry most of the DSA round:**

| Template | File |
|---|---|
| Sliding window — expand right, shrink while invalid, record | `Q21SlidingWindowTemplate` |
| Prefix sum + HashMap — running sum, look up `sum - k` | `Q23SubarraySumEqualsK` |
| Bounded heap / two heaps — Kth largest, running median | `Q27KthLargestAndMedian` |
| Grid BFS/DFS — bounds check, mark, four directions | `Q33NumberOfIslands` |
| `groupingBy` + downstream collector — every "per department" question | `Q06GroupByAndAggregate` |

**Four SQL ideas carry most of the SQL round:**

| Idea | File |
|---|---|
| `ROW_NUMBER() = 1` over `PARTITION BY` — latest row per key, top N per group | `S19`, `S09` |
| Running total over a window frame — cumulative, moving average, budget cutoffs | `S10`, `S14` |
| `LAG` + running `SUM` of a flag — sessions, streaks, islands | `S15`, `S16` |
| Anti-join (`NOT EXISTS`) and the `NOT IN` + `NULL` trap | `S01`, `S21` |

---

## Repository map

```
java/
  01-streams-and-collectors/          14 files   Collectors, groupingBy, flatMap, parallel streams
  02-arrays-and-hashing/               5 files   Two Sum, anagrams, XOR, prefix sums, top-K
  03-two-pointers-and-sliding-window/  5 files   reversal, palindromes, the window template, Kadane
  04-sorting-searching-heaps/          4 files   merging, intervals, heaps, binary search variants
  05-linkedlist-stack-tree/            3 files   reverse/Floyd, monotonic stack, BFS + BST bounds
  06-graphs-and-design/                3 files   LRU cache, grid flood fill, topological sort
  07-concurrency/                     11 files   executors, locks, CompletableFuture, deadlock, pools

sql/
  00-schema/                          schema + seed; load once, everything else runs against it
  01-joins-and-anti-joins/             4 files   anti-joins, self-joins, join grain, CROSS JOIN grids
  02-aggregation-and-grouping/         4 files   Nth highest, duplicates, conditional pivot, NULLs
  03-window-functions/                 3 files   ranking, running totals, LAG/LEAD
  04-cte-and-recursive/                3 files   hierarchies, date spines, budget allocation
  05-gaps-islands-and-sessions/        3 files   sessionisation, streaks, duplicate detection
  06-dedup-and-data-quality/           2 files   safe de-duplication, latest row per key
  07-performance-and-concepts/         3 files   sargability, NOT IN + NULL, quick-fire concepts
  run_all.py                          rebuilds the DB and re-stamps every expected output

docs/INDEX.md                         all 67 questions in one sortable table
```

---

## Coverage

| | Easy | Medium | Hard | Total | HIGH weightage |
|---|---|---|---|---|---|
| **Java** | 10 | 26 | 9 | **45** | 27 |
| **SQL** | 6 | 12 | 4 | **22** | 14 |

Java 45 files include ~15 secondary implementations (the "and now do it without sorting"
answer), so there is more code than there are headline questions.
SQL 22 files contain **101 runnable queries** — each file walks the working, the bug and the fix.

👉 **[Full index of all 67 questions →](docs/INDEX.md)**

---

## How to actually use this

1. **Read the header, close the file, type the solution.** Reading code builds recognition;
   typing it builds recall. The interviewer tests recall.
2. **Say the APPROACHES table out loud before you write.** At this level, naming the brute
   force and why you are rejecting it is worth as much as the working code.
3. **Check the EXPECTED OUTPUT only after you run yours.** If they differ, your edge cases
   differ — that is the lesson, not the typo.
4. **Steal the FOLLOW-UPS as flashcards.** They are what lands immediately after the code.

Every Java file also deliberately includes the **wrong** version where one exists —
the `HashMap` that loses sort order, the `volatile int++` that drops updates, the
per-node BST check that passes an invalid tree, the `mid++` that breaks Dutch National Flag.
Seeing the bug run is faster than being told about it.

---

## A note on dialects and versions

- **Java** — written to Java 8 semantics so it reads the way an interviewer expects.
  Two places use later APIs and say so inline: `Collectors.teeing` (Java 12+) in `Q07`.
  Everything compiles and runs on OpenJDK 21.
- **SQL** — ANSI SQL that runs unchanged on SQLite 3.25+, PostgreSQL and MySQL 8+.
  Where a function is dialect-specific (`julianday`, `strftime`, `EXPLAIN QUERY PLAN`)
  the file says so and names the PostgreSQL/MySQL equivalent.

---

## Sources

Question selection was drawn from interview reports and question banks rather than invented:

- [Glassdoor — Senior Java Developer interview reports](https://www.glassdoor.com/Interview/senior-java-developer-interview-questions-SRCH_KO0,21.htm)
- [Java2Blog — 100+ Java coding interview questions](https://java2blog.com/java-coding-interview-questions/)
- [Ketan Patel — Java 8 Stream coding questions](https://medium.com/@ketan.patel_46870/mastering-java8-stream-programming-questions-8e410760ca60)
- [Javarevisited — multithreading questions from investment banks](https://javarevisited.blogspot.com/2018/07/java-multi-threading-interview-questions-answers-from-investment-banks.html)
- [Java67 — thread and concurrency questions](https://www.java67.com/2012/08/5-thread-interview-questions-answers-in.html)
- [InterviewKickstart — advanced SQL interview questions](https://interviewkickstart.com/blogs/interview-questions/advanced-sql-interview-questions)
- [GoLinuxCloud — SQL technical interview questions](https://www.golinuxcloud.com/sql-technical-interview-questions/)
- [Window functions & CTEs — 12 real data-engineering questions](https://medium.com/@aicoders/master-sql-window-functions-and-ctes-12-real-data-engineering-interview-questions-with-code-9b42f37c1db1)
- [Machine Coding Round question bank](https://github.com/lavakumarThatisetti/Machine-Coding-Round)
- [Naukri Code360 — candidate interview experiences](https://www.naukri.com/code360/interview-experiences)
- LinkedIn interview-prep posts (Nov 2026) for the recurring SQL set and the budget-allocation
  question in `S14`.

## Licence

MIT — see [LICENSE](LICENSE). Use it, fork it, send a PR if you find a bug in a solution.
