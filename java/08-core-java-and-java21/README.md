# Core Java & Java 21

6 problems. Part of [senior-java-sql-interview-prep](../../README.md).
This folder needs **JDK 21** (records, sealed types, pattern-matching switch, virtual threads).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| Q46 | Immutable class, defensive copies, and the record gotcha | Easy-Medium | HIGH | P1 | final class + private final fields + defensive copy in and out; record with a copying compact constructor | [`Q46ImmutableClassAndRecord.java`](Q46ImmutableClassAndRecord.java) |
| Q47 | The equals / hashCode contract: three ways a key goes missing | Medium | HIGH | P1 | equals and hashCode from the same immutable fields (or a record); a comparator consistent with equals | [`Q47EqualsHashCodeContract.java`](Q47EqualsHashCodeContract.java) |
| Q48 | Java 8 to 21: the features a backend developer actually uses | Medium | HIGH | P1 | sealed interface + records + exhaustive pattern-matching switch; text blocks; var; sequenced collections; Stream.toList() | [`Q48Java21FeaturesIUse.java`](Q48Java21FeaturesIUse.java) |
| Q49 | Virtual threads: 10,000 blocking calls, virtual vs a platform pool | Medium | HIGH | P1 | One virtual thread per blocking task; a Semaphore, not a pool, to cap a downstream | [`Q49VirtualThreads.java`](Q49VirtualThreads.java) |
| Q50 | Retry with exponential backoff and jitter, and an idempotent consumer | Medium | Medium | P2 | Retry only transient errors; capped exponential backoff with full jitter; putIfAbsent on the message id | [`Q50RetryWithBackoffAndIdempotency.java`](Q50RetryWithBackoffAndIdempotency.java) |
| Q51 | A synchronized shared HashMap vs ConcurrentHashMap in a batch job | Medium | HIGH | P1 | Per-key atomic merge on ConcurrentHashMap (CAS on an empty bin, one-bin lock, lock-free reads); LongAdder for hot keys | [`Q51SynchronizedMapVsConcurrentHashMap.java`](Q51SynchronizedMapVsConcurrentHashMap.java) |

## Is folder ko kaise padhein

Pehle Q46 aur Q47 karo: ye har round mein aate hain, aur Q51 ki map wali baat inhi par khadi hai.
Phir Q51 aur Q49, kyunki dono mein apni kaam ki story judti hai. Q48 resume ke "Java 21" ka jawab
hai, aur Q50 fintech aur messaging rounds ke liye. Har file chalao aur WRONG version ka output khud
dekho, sirf padhne se yaad nahi rehta.

## Mock round

| Interviewer | Aap |
|---|---|
| How do you make a class immutable? | I make it final, keep every field private final, drop the setters, and copy any mutable field such as a List or a Date in the constructor and again in the getter. With a record I copy the list with List.copyOf in the compact constructor, otherwise it is only shallowly immutable. |
| You overrode equals but not hashCode. What happens? | HashMap uses hashCode to pick the bucket before it ever calls equals, so two equal keys usually never meet and get returns null. I build both from the same immutable fields, or I use a record as the key. |
| What did you actually use from Java 21? | I have used virtual threads at work for blocking I/O, and [add the other features you really used]. The ones that change backend code most are records and sealed interfaces with pattern-matching switch, where a new type breaks the build instead of falling into a default. I also moved seven services, in a team of three, from Java 1.7 to 21 and Spring Boot 3, and javax to jakarta was the biggest break. |
| When would you not use virtual threads? | For CPU-bound work, because there are still only as many carriers as cores. I never pool them; to protect a downstream I put a Semaphore in front of it. In Java 21 I also avoid blocking inside synchronized on hot paths, because that pins the carrier. |
| A downstream payment API times out now and then. How do you retry? | Only on transient errors, with exponential backoff, a cap, full jitter and a small number of attempts; permanent errors fail fast. A retry can duplicate the call, so the operation needs an idempotency key. In real code I would take the retry policy from a library such as Spring Retry or Resilience4j instead of a hand-written loop. |
| How do you stop a redelivered message from being processed twice? | The consumer is idempotent: it records the message id atomically, with putIfAbsent in memory or better a unique key in the database in the same transaction as the business write, and skips ids it has seen. Broker duplicate detection only drops duplicate sends, not redeliveries. |
| Your batch job is slow and the threads are queued on one shared map. What do you do? | That is lock contention on one shared lock. The fix is ConcurrentHashMap with merge or compute for each update: atomic per key, reads take no lock, and writers only meet when they hit the same bin. In a batch job I replaced a synchronized shared HashMap with ConcurrentHashMap, and the batch window went from 45 minutes to 8 minutes. |
| Is ConcurrentHashMap.size() accurate? | Under concurrent updates it is an estimate, a sum of striped counters; for a big map I use mappingCount(). If I need the exact figure I read it after the writers have finished. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| final field matlab immutable? | Nahi, sirf reference fix hai; andar ki List ya Date badal sakti hai. |
| unmodifiableList vs List.copyOf? | unmodifiableList sirf view hai, original badla to dikhega; List.copyOf asli copy hai. |
| Record kab final hua? | Java 16 mein; sealed Java 17, pattern switch Java 21. |
| equals override, hashCode nahi? | HashMap mein equal key nahi milti, HashSet mein duplicate aa jaata hai. |
| BigDecimal 1.0 vs 1.00? | equals false (scale alag), compareTo 0; TreeSet ek rakhega, HashSet do. |
| Stream.toList() par add()? | UnsupportedOperationException, kyunki list unmodifiable hai. |
| var field par chalta hai? | Nahi, sirf local variable, loop aur lambda params par. |
| Virtual threads ka pool banayein? | Kabhi nahi; har task ka naya thread, limit ke liye Semaphore. |
| Java 21 mein pinning kab hoti hai? | synchronized ya native call ke andar block hone par carrier pinned rehta hai. |
| Spring Boot mein virtual threads kaise? | Boot 3.2+ aur Java 21 par spring.threads.virtual.enabled=true. |
| Retry mein jitter kyun? | Taaki saare clients ek hi pal mein wapas na aayein (thundering herd). |
| ConcurrentHashMap ka get() lock leta hai? | Nahi; sirf write us ek bin ko lock karta hai, khali bin par CAS. |

## Run any file

```bash
# from the repo root, on JDK 21
java java/08-core-java-and-java21/Q46ImmutableClassAndRecord.java

# all files, diffed against the EXPECTED OUTPUT recorded in each one
bash run-java.sh
```

Every file is single-file runnable and ends with the output it actually produced on OpenJDK 21.
Q49 takes about 10 seconds: the platform-thread pool is slow on purpose.
