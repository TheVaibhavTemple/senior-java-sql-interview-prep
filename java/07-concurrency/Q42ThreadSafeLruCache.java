/* ==============================================================================================
 * Q42  .  Thread-safe LRU cache
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : Single-lock guard over an access-ordered LinkedHashMap; approximate LRU for scale
 *
 * PROBLEM
 *   Make an LRU cache correct under concurrent access. Then say what you would actually
 *   ship.
 *
 * ALGORITHM
 *   1. LinkedHashMap with accessOrder=true reorders the list on every get.
 *   2. That makes a read a structural mutation, so a ReadWriteLock read lock is UNSAFE
 *      here.
 *   3. Guard both get and put with the same exclusive lock.
 *   4. Override removeEldestEntry to evict once size exceeds capacity.
 *   5. To scale, drop strict LRU: ConcurrentHashMap plus best-effort eviction, or a real
 *      cache library.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Collections.synchronizedMap around LinkedHashM --            --        acceptable
 *       > Correct if you also lock iteration. Coarse.
 *   ReadWriteLock, read lock on get                --            --        avoid
 *       > THE TRAP. get() mutates access order, so every read is a write.
 *   A single ReentrantLock around both operations  --            --        WRITE THIS
 *       > Correct, and honest about the serialisation cost.
 *   ConcurrentHashMap + approximate LRU            --            --        WRITE THIS
 *       > Scales; eviction order becomes best-effort. Name the trade-off.
 *   Caffeine / Guava cache                         --            --        WRITE THIS
 *       > The production answer. Say it even if you hand-write one.
 *
 * WHY THIS ONE
 *   The read-write lock trap is the entire question. Reading from an access-ordered
 *   LinkedHashMap reorders the list, so a read lock permits concurrent mutation and
 *   corrupts it. Candidates reach for ReadWriteLock reflexively because "caches are read-
 *   heavy". Getting this right, then volunteering that strict LRU is a contention
 *   bottleneck so real caches use approximate eviction, is what a senior answer looks
 *   like.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why does a ReadWriteLock not work here?
 *   ? How would you shard the cache to reduce contention?
 *   ? What does Caffeine do differently (Window-TinyLFU)?
 *   ? Add a TTL -- does eviction stay O(1)?
 *   ? Two threads miss the same key at once -- how do you avoid loading it twice?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Cache stampede prevention
 *   - Lock striping
 *   - computeIfAbsent atomicity
 *   - Approximate vs strict LRU
 *   - Distributed cache design
 * ============================================================================================== */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class Q42ThreadSafeLruCache {

    /* ======================================================================
       OPTION A - one exclusive lock around an access-ordered LinkedHashMap.

       THE TRAP: a ReadWriteLock does NOT work here. get() reorders the
       access list, so every read is a structural mutation. A read lock
       would allow concurrent mutation and corrupt the map.
       ====================================================================== */
    static final class SynchronizedLru<K, V> {
        private final LinkedHashMap<K, V> map;
        private final ReentrantLock lock = new ReentrantLock();

        SynchronizedLru(final int capacity) {
            this.map = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > capacity;
                }
            };
        }

        V get(K key) { lock.lock(); try { return map.get(key); } finally { lock.unlock(); } }
        void put(K k, V v) { lock.lock(); try { map.put(k, v); } finally { lock.unlock(); } }
        int size() { lock.lock(); try { return map.size(); } finally { lock.unlock(); } }
        String snapshot() { lock.lock(); try { return map.toString(); } finally { lock.unlock(); } }
    }

    /* ======================================================================
       OPTION B - what you would actually ship: ConcurrentHashMap with
       best-effort (approximate) eviction. Strict LRU is a contention point.
       computeIfAbsent is atomic per key, so the loader runs at most once.
       ====================================================================== */
    static final class StripedCache<K, V> {
        private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<K> order = new ConcurrentLinkedQueue<>();
        private final int capacity;
        final AtomicInteger loaderCalls = new AtomicInteger();

        StripedCache(int capacity) { this.capacity = capacity; }

        V get(K key, Function<K, V> loader) {
            V v = map.computeIfAbsent(key, k -> {
                loaderCalls.incrementAndGet();
                order.add(k);
                return loader.apply(k);
            });
            while (map.size() > capacity) {          // best-effort, not strict LRU
                K victim = order.poll();
                if (victim == null) break;
                map.remove(victim);
            }
            return v;
        }
        int size() { return map.size(); }
    }

    public static void main(String[] args) throws Exception {
        SynchronizedLru<Integer, String> lru = new SynchronizedLru<>(3);
        lru.put(1, "a"); lru.put(2, "b"); lru.put(3, "c");
        lru.get(1);                                   // touching 1 makes 2 the eldest
        lru.put(4, "d");
        System.out.println("A after evict    : " + lru.snapshot() + "  (2 evicted, 1 was touched)");

        /* hammer it: 8 threads, no exception, size never exceeds capacity */
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(8);
        AtomicInteger overflow = new AtomicInteger();
        for (int t = 0; t < 8; t++) {
            final int seed = t;
            pool.execute(() -> {
                try {
                    for (int i = 0; i < 20_000; i++) {
                        lru.put((seed * 31 + i) % 50, "v" + i);
                        lru.get(i % 50);
                        if (lru.size() > 3) overflow.incrementAndGet();
                    }
                } finally { done.countDown(); }
            });
        }
        done.await(30, TimeUnit.SECONDS);
        System.out.println("A under 8 threads: size=" + lru.size()
                + "  capacity violations=" + overflow.get());

        /* B: concurrent miss on the same key must call the loader exactly once */
        StripedCache<String, String> striped = new StripedCache<>(100);
        CountDownLatch start = new CountDownLatch(1), finished = new CountDownLatch(8);
        for (int t = 0; t < 8; t++) {
            pool.execute(() -> {
                try {
                    start.await();
                    striped.get("hot-key", k -> {
                        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        return "loaded";
                    });
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { finished.countDown(); }
            });
        }
        start.countDown();
        finished.await(20, TimeUnit.SECONDS);
        System.out.println("B stampede guard : loader invoked " + striped.loaderCalls.get()
                + " time(s) for 8 concurrent misses  (computeIfAbsent is atomic per key)");
        pool.shutdownNow();

        System.out.println("WHY NOT RWLock   : get() mutates the access order, so a read "
                + "lock permits concurrent structural mutation");
        System.out.println("IN PRODUCTION    : Caffeine (Window-TinyLFU) - O(1), striped, with TTL and stats");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * A after evict    : {3=c, 1=a, 4=d}  (2 evicted, 1 was touched)
 * A under 8 threads: size=3  capacity violations=0
 * B stampede guard : loader invoked 1 time(s) for 8 concurrent misses  (computeIfAbsent is atomic per key)
 * WHY NOT RWLock   : get() mutates the access order, so a read lock permits concurrent structural mutation
 * IN PRODUCTION    : Caffeine (Window-TinyLFU) - O(1), striped, with TTL and stats
 * ============================================================================================== */
