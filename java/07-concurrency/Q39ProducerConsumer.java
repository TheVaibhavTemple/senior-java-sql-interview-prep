/* ==============================================================================================
 * Q39  .  Producer-consumer with a bounded buffer
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- haath se wait/notify aur BlockingQueue, dono roz poochte hain
 * TECHNIQUE    : Bounded buffer with wait/notifyAll; BlockingQueue with a poison pill for shutdown
 *
 * PROBLEM
 *   Producers and consumers over a bounded buffer. Implement it with wait/notify, then
 *   with BlockingQueue, with graceful shutdown.
 *
 * ALGORITHM
 *   1. put waits WHILE the buffer is full; take waits WHILE it is empty.
 *   2. Both use notifyAll, because producers and consumers share a single wait set.
 *   3. notify() could wake the wrong kind of thread and stall the whole buffer.
 *   4. The production version is ArrayBlockingQueue - put and take already block correctly.
 *   5. Shutdown cleanly with a poison-pill sentinel, one per consumer.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Unbounded queue, no blocking                   --            --        avoid
 *       > No backpressure. The queue grows until the heap dies.
 *   wait / notify on a shared bounded buffer       --            --        WRITE THIS
 *       > The version they ask you to write by hand.
 *   ReentrantLock with notFull / notEmpty          --            --        WRITE THIS
 *       > Signals only the threads that can progress -- see Q40.
 *   ArrayBlockingQueue + poison pill               --            --        WRITE THIS
 *       > What you would actually ship.
 *
 * WHY THIS ONE
 *   Two invariants: wait inside a while loop, and notifyAll rather than notify -- with a
 *   single monitor, producers and consumers share one wait set, so notify() can wake a
 *   producer to deliver news a consumer needed, and everything stalls. For shutdown a
 *   poison pill is the standard answer, and the follow-up is always "what about N
 *   consumers": N pills, one each.
 *
 * SAMJHO
 *   Buffer full ho to producer wait kare, khali ho to consumer wait kare, dono while loop mein.
 *   Ek hi monitor hai to producer aur consumer ek hi wait set mein hain, isliye notifyAll.
 *   Production mein ArrayBlockingQueue lo. Band karne ke liye har consumer ko ek poison pill.
 *
 * AISE BOLO  (30-40 sec)
 *   "An unbounded queue has no backpressure, so a fast producer eventually exhausts the heap.
 *   By hand I write a bounded buffer where put waits while full and take waits while empty,
 *   both in while loops, with notifyAll because producers and consumers share one wait set.
 *   put and take are O(1) and memory is capped at the capacity. In real code I use
 *   ArrayBlockingQueue, and for shutdown I send one poison pill per consumer after all the
 *   producers have finished."
 *
 * TRAP
 *   ! N consumers ke liye ek hi pill bheji to baaki N-1 hamesha take() mein atke rahenge.
 *   ! Pill tab daalo jab saare producers khatam ho jaayein (join ke baad), warna pill ke
 *     peeche aayi jobs koi nahi uthayega.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why notifyAll and not notify with a single monitor?
 *     -> Ek wait set mein producer aur consumer dono hain. notify() producer ko jaga sakta hai
 *        jab consumer ko jagana tha; wo phir wait karega aur sab atak sakte hain.
 *   ? N consumers -- how many poison pills, and why?
 *     -> N pills. Har consumer pill dekh ke exit karta hai aur use wapas nahi daalta, to ek
 *        pill sirf ek consumer ko rokti hai.
 *   ? The producer is much faster than the consumer -- what does a bounded queue do about
 *     it?
 *     -> put() block hota hai, to producer consumer ki speed par aa jata hai. Yahi
 *        backpressure hai, aur memory capacity se upar nahi jaati.
 *   ? ArrayBlockingQueue vs LinkedBlockingQueue vs SynchronousQueue -- one scenario each.
 *     -> ABQ: fixed array, ek lock, bounded work queue. LBQ: alag put/take lock, zyada
 *        throughput, par default capacity Integer.MAX_VALUE. SynchronousQueue: capacity zero,
 *        seedha handoff; newCachedThreadPool isi pe chalta hai.
 *   ? put vs offer vs add -- three different failure behaviours.
 *     -> Full queue par: put() block karta hai, offer() false deta hai (timed offer wait karke
 *        false), add() IllegalStateException phenkta hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Bounded buffer
 *   - Graceful shutdown with a poison pill
 *   - BlockingQueue implementations
 *   - Backpressure
 *   - Dining philosophers
 *
 * JAVA 21 MEIN
 *   - Consumers virtual threads ho sakte hain, par Java 21 mein synchronized ke andar wait()
 *     carrier thread ko pin kar deta hai. BlockingQueue aur ReentrantLock (Q40) mein ye
 *     dikkat nahi. JDK 24 mein ye pinning hata di gayi.
 *
 * APNI STORY SE JODO
 *   "Between my services the producer-consumer is Azure Service Bus: the queue decouples them,
 *   and a message that keeps failing goes to the dead-letter queue, which we review."
 *   Note: Poison pill (shutdown signal) aur poison message (DLQ) ka farak bolo, dono alag hain.
 *
 * VARIANTS     : LC 1188, classic - many producers and consumers with N poison pills
 * ============================================================================================== */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Q39ProducerConsumer {

    /* ---- VERSION A : the bounded buffer they ask you to write by hand ----
       put waits WHILE full, take waits WHILE empty, and BOTH use notifyAll
       because producers and consumers share a single wait set. */
    static final class BoundedBuffer<T> {
        private final Queue<T> queue = new ArrayDeque<>();
        private final int capacity;
        private int maxObservedSize = 0;

        BoundedBuffer(int capacity) { this.capacity = capacity; }

        synchronized void put(T item) throws InterruptedException {
            while (queue.size() == capacity) wait();     // WHILE, not if
            queue.add(item);
            maxObservedSize = Math.max(maxObservedSize, queue.size());
            notifyAll();
        }

        synchronized T take() throws InterruptedException {
            while (queue.isEmpty()) wait();
            T item = queue.poll();
            notifyAll();
            return item;
        }

        synchronized int maxSeen() { return maxObservedSize; }
    }

    public static void main(String[] args) throws Exception {
        /* ---- A : hand-written buffer, 2 producers + 3 consumers ---- */
        final int ITEMS = 300, CONSUMERS = 3;
        BoundedBuffer<String> buf = new BoundedBuffer<>(10);
        AtomicInteger consumed = new AtomicInteger();
        final String POISON = "__STOP__";

        List<Thread> threads = new ArrayList<>();
        for (int p = 0; p < 2; p++) {
            final int base = p * (ITEMS / 2);
            threads.add(new Thread(() -> {
                try {
                    for (int i = 0; i < ITEMS / 2; i++) buf.put("job-" + (base + i));
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
        }
        for (int c = 0; c < CONSUMERS; c++) {
            threads.add(new Thread(() -> {
                try {
                    while (true) {
                        String job = buf.take();
                        if (POISON.equals(job)) return;
                        consumed.incrementAndGet();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
        }
        for (Thread t : threads) t.start();
        for (int i = 0; i < 2; i++) threads.get(i).join();          // producers done
        for (int i = 0; i < CONSUMERS; i++) buf.put(POISON);        // ONE PILL PER CONSUMER
        for (Thread t : threads) t.join();

        System.out.println("A produced       : " + ITEMS);
        System.out.println("A consumed       : " + consumed.get());
        System.out.println("A buffer never exceeded capacity : " + (buf.maxSeen() <= 10)
                + " (max seen " + buf.maxSeen() + " of 10)");

        /* ---- B : what you would actually ship ---- */
        BlockingQueue<String> q = new ArrayBlockingQueue<>(10);
        AtomicInteger handled = new AtomicInteger();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) q.put("job-" + i);    // blocks when full
                q.put(POISON);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String job = q.take();                           // blocks when empty
                    if (POISON.equals(job)) break;
                    handled.incrementAndGet();
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "consumer");

        producer.start(); consumer.start();
        producer.join();  consumer.join();
        System.out.println("B BlockingQueue  : handled " + handled.get() + " of 100, queue empty="
                + q.isEmpty());

        /* ---- the three failure behaviours of the insert methods ---- */
        BlockingQueue<Integer> tiny = new ArrayBlockingQueue<>(1);
        tiny.put(1);
        System.out.println("offer on full    : " + tiny.offer(2) + "   (returns false)");
        try { tiny.add(3); } catch (IllegalStateException e) {
            System.out.println("add   on full    : IllegalStateException");
        }
        System.out.println("offer w/ timeout : " + tiny.offer(4, 50, TimeUnit.MILLISECONDS));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * A produced       : 300
 * A consumed       : 300
 * A buffer never exceeded capacity : true (max seen 10 of 10)
 * B BlockingQueue  : handled 100 of 100, queue empty=true
 * offer on full    : false   (returns false)
 * add   on full    : IllegalStateException
 * offer w/ timeout : false
 * ============================================================================================== */
