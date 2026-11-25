# Concurrency & Multithreading

11 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| Q35 | Threads, Callable, Future and clean shutdown | Easy | HIGH | ExecutorService with Callable/Future, timed get, and the two-phase shutdown | [`Q35CallableFutureAndShutdown.java`](java/07-concurrency/Q35CallableFutureAndShutdown.java) |
| Q36 | Thread-safe counter | Easy | HIGH | CAS via AtomicLong; striped cells via LongAdder; atomic map operations | [`Q36ThreadSafeCounter.java`](java/07-concurrency/Q36ThreadSafeCounter.java) |
| Q37 | Thread-safe singleton | Medium | HIGH | Initialization-on-demand holder; enum singleton; double-checked locking with volatile | [`Q37ThreadSafeSingleton.java`](java/07-concurrency/Q37ThreadSafeSingleton.java) |
| Q38 | Print odd and even alternately with two threads | Medium | HIGH | Monitor coordination with wait/notifyAll; Semaphore turn-passing as the alternative | [`Q38PrintOddEvenTwoThreads.java`](java/07-concurrency/Q38PrintOddEvenTwoThreads.java) |
| Q39 | Producer–consumer with a bounded buffer | Medium | HIGH | Bounded buffer with wait/notifyAll; BlockingQueue with a poison pill for shutdown | [`Q39ProducerConsumer.java`](java/07-concurrency/Q39ProducerConsumer.java) |
| Q41 | CompletableFuture fan-out / fan-in | Medium | HIGH | Asynchronous fan-out / fan-in with allOf, per-call timeout and fallback | [`Q41CompletableFutureFanOut.java`](java/07-concurrency/Q41CompletableFutureFanOut.java) |
| Q40 | Implement a bounded BlockingQueue from scratch | Hard | Medium | ReentrantLock with two Conditions over a circular array buffer | [`Q40CustomBlockingQueue.java`](java/07-concurrency/Q40CustomBlockingQueue.java) |
| Q42 | Thread-safe LRU cache | Hard | Medium | Single-lock guard over an access-ordered LinkedHashMap; approximate LRU for scale | [`Q42ThreadSafeLruCache.java`](java/07-concurrency/Q42ThreadSafeLruCache.java) |
| Q43 | Rate limiter | Hard | Medium | Token bucket backed by a Semaphore; sliding-window log per key | [`Q43RateLimiter.java`](java/07-concurrency/Q43RateLimiter.java) |
| Q44 | Deadlock: create it, fix it, detect it | Hard | HIGH | Global lock ordering; tryLock with timeout and jittered backoff; ThreadMXBean detection | [`Q44DeadlockCreateFixDetect.java`](java/07-concurrency/Q44DeadlockCreateFixDetect.java) |
| Q45 | ThreadPoolExecutor tuning and graceful shutdown | Hard | HIGH | Explicit ThreadPoolExecutor with a bounded queue and CallerRunsPolicy backpressure | [`Q45ThreadPoolTuning.java`](java/07-concurrency/Q45ThreadPoolTuning.java) |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
