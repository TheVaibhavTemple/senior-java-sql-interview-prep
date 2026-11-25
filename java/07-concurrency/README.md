# Concurrency & Multithreading

12 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| Q35 | Threads, Callable, Future and clean shutdown | Easy | HIGH | P1 | ExecutorService with Callable/Future, timed get, and the two-phase shutdown | [`Q35CallableFutureAndShutdown.java`](Q35CallableFutureAndShutdown.java) |
| Q36 | Thread-safe counter | Easy | HIGH | P1 | CAS via AtomicLong; striped cells via LongAdder; atomic map operations | [`Q36ThreadSafeCounter.java`](Q36ThreadSafeCounter.java) |
| Q37 | Thread-safe singleton | Medium | HIGH | P1 | Initialization-on-demand holder; enum singleton; double-checked locking with volatile | [`Q37ThreadSafeSingleton.java`](Q37ThreadSafeSingleton.java) |
| Q38 | Print odd and even alternately with two threads | Medium | HIGH | P1 | Monitor coordination with wait/notifyAll; Semaphore turn-passing as the alternative | [`Q38PrintOddEvenTwoThreads.java`](Q38PrintOddEvenTwoThreads.java) |
| Q39 | Producer–consumer with a bounded buffer | Medium | HIGH | P1 | Bounded buffer with wait/notifyAll; BlockingQueue with a poison pill for shutdown | [`Q39ProducerConsumer.java`](Q39ProducerConsumer.java) |
| Q41 | CompletableFuture fan-out / fan-in | Medium | HIGH | P1 | Asynchronous fan-out / fan-in with allOf, per-call timeout and fallback | [`Q41CompletableFutureFanOut.java`](Q41CompletableFutureFanOut.java) |
| Q56 | CountDownLatch, CyclicBarrier, Semaphore — and ReentrantLock vs synchronized | Medium | HIGH | P1 | CountDownLatch to wait for N, CyclicBarrier to meet in rounds, Semaphore for at most K at once; ReentrantLock when synchronized is not enough | [`Q56LatchBarrierSemaphore.java`](Q56LatchBarrierSemaphore.java) |
| Q40 | Implement a bounded BlockingQueue from scratch | Hard | Medium | P2 | ReentrantLock with two Conditions over a circular array buffer | [`Q40CustomBlockingQueue.java`](Q40CustomBlockingQueue.java) |
| Q42 | Thread-safe LRU cache | Hard | Medium | P2 | Single-lock guard over an access-ordered LinkedHashMap; approximate LRU for scale | [`Q42ThreadSafeLruCache.java`](Q42ThreadSafeLruCache.java) |
| Q43 | Rate limiter | Hard | Medium | P2 | Token bucket backed by a Semaphore; sliding-window log per key | [`Q43RateLimiter.java`](Q43RateLimiter.java) |
| Q44 | Deadlock: create it, fix it, detect it | Hard | HIGH | P1 | Global lock ordering; tryLock with timeout and jittered backoff; ThreadMXBean detection | [`Q44DeadlockCreateFixDetect.java`](Q44DeadlockCreateFixDetect.java) |
| Q45 | ThreadPoolExecutor tuning and graceful shutdown | Hard | HIGH | P1 | Explicit ThreadPoolExecutor with a bounded queue and CallerRunsPolicy backpressure | [`Q45ThreadPoolTuning.java`](Q45ThreadPoolTuning.java) |

## Is folder ko kaise padhein

Pehle P1 files is order mein: Q35, Q36, Q37, Q38, Q39, Q41, Q45, Q44. Q38 aur Q39 ko bina IDE
ke memory se do baar likho, kyunki ye whiteboard pe aate hain. Phir P2 (Q40, Q42, Q43) mein code
ek baar type karo aur AISE BOLO zor se bolo. Java 21 wala hissa (virtual threads, retry,
synchronized HashMap vs ConcurrentHashMap) [`08-core-java-and-java21`](../08-core-java-and-java21/)
mein Q49, Q50, Q51 hai.

## Mock round

| Interviewer | Aap |
|---|---|
| Your API calls three downstream services per request. How do you keep latency low? | I fan out with `supplyAsync` on a dedicated I/O executor, give each call its own timeout and fallback, and combine after `allOf`. Latency becomes the slowest call, not the sum. |
| Why not just use the common pool? | It is sized to the CPU cores and shared by the whole JVM, including parallel streams. Blocking I/O there starves everything else. |
| How do you size that executor? | Roughly cores times one plus wait over compute, then I cap it by what the downstream can take, like the DB connection pool, and tune from queue depth and p99 latency. |
| Traffic doubles. What happens to the pool? | With a bounded queue and `CallerRunsPolicy`, the queue fills, the pool grows to max, and then the submitting thread runs the task itself, which slows intake. With `newFixedThreadPool` the queue would just grow until the heap runs out. |
| One downstream becomes slow. Your timeout fires. Is the thread free now? | No. The timeout completes the future, but the call keeps running and holds its thread. I isolate that dependency in its own small pool or behind a semaphore, so it cannot eat the shared threads. |
| Several threads update a shared map of totals. Is ConcurrentHashMap enough? | Only if each update is one atomic call like `merge` or `compute`. Get-then-put on a ConcurrentHashMap still loses updates. |
| The service hangs under load. How do you check for a deadlock? | I take a thread dump with `jcmd <pid> Thread.print` and look for "Found one Java-level deadlock", or BLOCKED threads waiting on each other's locks. The fix is a global lock order, or `tryLock` with a timeout and jitter. |
| Do virtual threads change any of this? | For I/O-bound fan-out, yes: one virtual thread per task, no pool sizing, and a semaphore to protect the downstream. On Java 21 I avoid blocking inside `synchronized` on hot paths, because that pins the carrier thread. |
| Main must wait until five workers finish. Latch or barrier? | A `CountDownLatch` of five, with `countDown` in each worker's finally block, and main calls `await`. A `CyclicBarrier` is for workers that wait for each other, round after round; here only main waits, and only once. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| volatile se count++ safe hai? | Nahi. volatile sirf visibility deta hai; ++ read-modify-write hai, AtomicLong lo. |
| wait() if mein ya while mein? | Hamesha while: spurious wakeup aur condition dobara badal sakti hai. |
| notify vs notifyAll? | Ek monitor pe alag type ke waiters hon to notifyAll; notify galat thread jaga sakta hai. |
| sleep() vs wait()? | sleep lock nahi chhodta; wait lock chhodta hai aur synchronized ke andar hi call hota hai. |
| Runnable vs Callable? | Callable value return karta hai aur checked exception throw kar sakta hai. |
| shutdown vs shutdownNow? | shutdown queued tasks poore karta hai; shutdownNow interrupt karke queued tasks ki list lauta deta hai. |
| newFixedThreadPool kyun avoid? | Andar unbounded LinkedBlockingQueue hai, overload mein OOM. |
| ThreadPoolExecutor mein task ka order? | Core threads, phir queue, phir max tak threads, phir rejection. |
| DCL mein volatile kyun? | Bina volatile ke reference constructor khatam hone se pehle dikh sakta hai. |
| thenApply vs thenCompose? | Function CompletableFuture return kare to thenCompose, warna CF<CF<U>> milega. |
| Deadlock ki 4 conditions? | Mutual exclusion, hold-and-wait, no preemption, circular wait; lock order se circular wait todo. |
| LongAdder kab? | Bahut writes aur kam reads wala metrics counter; ID ya sequence ke liye nahi. |
| Semaphore ka permit kis thread ka hota hai? | Kisi ka nahi: koi bhi thread release kar sakta hai. Isliye acquire try se pehle, release finally mein. |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
