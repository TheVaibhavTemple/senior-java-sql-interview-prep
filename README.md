# Senior Java + SQL Interview Prep

**84 interview questions, every one of them runnable**: 56 Java files and 28 SQL files
(170 SQL queries), for the **8-10 year Java backend** round in India: service companies, product
and fintech companies, GCCs and banks. At this level the brute force is assumed; the interviewer is
listening for the trade-off, the trap and the follow-up.

Ye repo meri padhne ki style mein hai: samjho Hinglish mein, bolo English mein, trap pehle se
pata ho, aur har follow-up ka jawab ready ho.

| Start here | What it is |
|---|---|
| [docs/STUDY_PLAN.md](docs/STUDY_PLAN.md) | 10 din ka plan, roz ka loop, interview wale din kya karna hai |
| [docs/INDEX.md](docs/INDEX.md) | Saare 84 sawaal ek table mein, Priority ke hisaab se |
| [docs/AUDIT.md](docs/AUDIT.md) | Kaunsa sawaal kyun hai, kya galat tha aur theek kiya, kya joda |
| [docs/STYLE.md](docs/STYLE.md) | Har file ka format, aur "apni story" ke rules |

---

## Har file mein kya hai

| Block | Kya batata hai |
|---|---|
| **DIFFICULTY / WEIGHTAGE** | Kitna mushkil, aur kitni baar poocha jaata hai |
| **PRIORITY** | P1 pehle (almost har round), P2 baad mein, P3 time bache to |
| **TECHNIQUE / ALGORITHM** | Pattern ka naam, aur syntax se pehle ke 4-6 steps |
| **APPROACHES** | Har approach ka time / space / verdict: code se pehle yahi bolna hai |
| **WHY THIS ONE** | Interview mein ye approach kyun jeet-ta hai |
| **SAMJHO** | Idea Hinglish mein, 2-4 lines |
| **AISE BOLO** | 30-40 second ka English jawab, first person |
| **TRAP** | Woh galti jo marks kaat-ti hai |
| **FOLLOW-UPS KE JAWAB** | Code ke baad aane wale sawaal, har ek ke jawab ke saath |
| **JAVA 21 MEIN / ORACLE / POSTGRES MEIN** | Jahan naya API ya DB syntax jawab badalta hai |
| **APNI STORY SE JODO** | Jahan meri asli kaam ki baat fit hoti hai (company naam ke bina) |
| **EXPECTED OUTPUT / RESULTS** | File chala kar capture kiya asli output, haath se likha nahi |

Har folder ke README mein **Mock round** (interviewer ka drill-down) aur **Rapid fire**
(ek-line jawab) bhi hai. Jahan possible hai, file **galat version** bhi chala kar dikhati hai:
woh HashMap jo order kho deta hai, `volatile int++` jo updates gira deta hai, `NOT IN` jo NULL par
khaali aata hai, double jo paise mein paise kho deta hai.

---

## Quick start

```bash
git clone https://github.com/TheVaibhavTemple/senior-java-sql-interview-prep.git
cd senior-java-sql-interview-prep

# Java: every file is single-file runnable, no build tool, no dependencies
java java/03-two-pointers-and-sliding-window/Q21SlidingWindowTemplate.java

# run all Java files and diff each against the EXPECTED OUTPUT recorded inside it
./run-java.sh

# SQL: builds a seeded SQLite database and runs every query
python3 sql/run_all.py --dry      # run only, report failures
python3 sql/run_all.py            # run and re-stamp the RESULTS block in every file
```

**Requirements:** JDK 21 (folder 08 uses Java 21 features; the rest also runs on 11+) and
Python 3.8+ with SQLite 3.39+ (S24 and S28 use FULL / RIGHT JOIN; window functions need 3.25+).
Nothing else: no Maven, no Gradle, no server.

**Windows:** run `./run-java.sh` from Git Bash, and use `python` instead of `python3` if that is
what your install has. Both scripts handle CRLF line endings.

`run-java.sh` checks every Java file against its recorded output. Four files (`Q14`, `Q36`, `Q44`,
`Q45`) are marked `NON-DETERMINISTIC` because they show data races, lost updates and
scheduler-dependent pool numbers that change every run by design; for those the script only checks
that they run clean.

---

## Pattern, count nahi

Ye 84 alag-alag sawaal nahi hain. Har file ek **pattern** hai: ek baar solve karo, uski
*ALSO UNLOCKS* list ke sawaal wahi hain, bas naam badle hue.

**Java ke paanch template jo DSA round ka zyada hissa cover karte hain:**

| Template | File |
|---|---|
| Sliding window: expand right, shrink while invalid, record | `Q21SlidingWindowTemplate` |
| Prefix sum + HashMap: running sum, look up `sum - k` | `Q23SubarraySumEqualsK` |
| Bounded heap / two heaps: Kth largest, running median | `Q27KthLargestAndMedian` |
| Grid BFS/DFS: bounds check, mark, four directions | `Q33NumberOfIslands` |
| `groupingBy` + downstream collector: har "per department" sawaal | `Q06GroupByAndAggregate` |

**SQL ke chaar idea jo SQL round ka zyada hissa cover karte hain:**

| Idea | File |
|---|---|
| `ROW_NUMBER() = 1` over `PARTITION BY`: latest row per key, top N per group | `S19`, `S09` |
| Running total over a window frame: cumulative, balance, budget cutoffs | `S10`, `S25`, `S14` |
| `LAG` + running `SUM` of a flag: sessions, streaks, islands | `S15`, `S16` |
| Anti-join (`NOT EXISTS`) and the `NOT IN` + `NULL` trap | `S01`, `S21` |

**Fintech / backend wale sawaal** jo is profile mein zaroor aate hain: BigDecimal money (`Q52`),
retry + idempotency (`Q50`), synchronized HashMap vs ConcurrentHashMap (`Q51`), virtual threads
(`Q49`), FX rate at a timestamp (`Q55`), reconciliation (`S24`), running balance (`S25`), keyset
pagination (`S26`), idempotent upsert (`S27`), duplicate payments (`S17`).

---

## Repository map

```
java/
  01-streams-and-collectors/             16 files   Java 8 Streams & Collectors
  02-arrays-and-hashing/                  5 files   Arrays & Hashing
  03-two-pointers-and-sliding-window/     5 files   Two Pointers & Sliding Window
  04-sorting-searching-heaps/             5 files   Sorting, Searching & Heaps
  05-linkedlist-stack-tree/               3 files   Linked List, Stack & Tree
  06-graphs-and-design/                   4 files   Graphs & Design Data Structures
  07-concurrency/                        12 files   Concurrency & Multithreading
  08-core-java-and-java21/                6 files   Core Java & Java 21

sql/
  00-schema/                             schema + seed; every SQL file runs against it
  01-joins-and-anti-joins/                6 files   SQL — Joins And Anti Joins
  02-aggregation-and-grouping/            4 files   SQL — Aggregation And Grouping
  03-window-functions/                    5 files   SQL — Window Functions
  04-cte-and-recursive/                   3 files   SQL — Cte And Recursive
  05-gaps-islands-and-sessions/           3 files   SQL — Gaps Islands And Sessions
  06-dedup-and-data-quality/              2 files   SQL — Dedup And Data Quality
  07-performance-and-concepts/            5 files   SQL — Performance And Concepts
  run_all.py                             rebuilds the DB and re-stamps every RESULTS block

docs/
  STUDY_PLAN.md  INDEX.md  AUDIT.md  STYLE.md
```

---

## Coverage

| | P1 | P2 | P3 | Total | Easy | Easy-Medium | Medium | Medium-Hard | Hard |
|---|---|---|---|---|---|---|---|---|---|
| **Java** | 34 | 20 | 2 | **56** | 10 | 2 | 34 | 1 | 9 |
| **SQL** | 19 | 5 | 4 | **28** | 6 | 2 | 16 | 0 | 4 |

Full list with techniques: **[docs/INDEX.md](docs/INDEX.md)**

---

## A note on dialects and versions

- **Java:** folders 01-07 are written to Java 8 semantics, because that is how many interviewers
  still expect the answer; each file's `JAVA 21 MEIN` block says what changes on newer Java.
  Folder 08 is Java 21 on purpose. Everything compiles and runs on OpenJDK 21.
- **SQL:** ANSI SQL that runs on SQLite, PostgreSQL and MySQL 8+. Where Oracle or PostgreSQL
  differ (`FETCH FIRST`, `ROWNUM`, `MERGE`, `CONNECT BY`, date arithmetic, `EXPLAIN`), the file's
  `ORACLE / POSTGRES MEIN` block says so.

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
