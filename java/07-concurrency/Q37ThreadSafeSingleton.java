/* ==============================================================================================
 * Q37  .  Thread-safe singleton
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Initialization-on-demand holder; enum singleton; double-checked locking with volatile
 *
 * PROBLEM
 *   Implement a singleton that is correct under concurrency, lazy if possible, and immune
 *   to reflection and serialization attacks.
 *
 * ALGORITHM
 *   1. Enum: the JVM guarantees one instance and blocks reflection, serialization and clone
 *      attacks.
 *   2. Holder idiom: a nested class is loaded on first use, so the class loader provides
 *      the lazy lock-free guarantee.
 *   3. Double-checked locking needs volatile because construction can be reordered after
 *      the assignment.
 *   4. Without volatile another thread can observe a non-null but half-initialised object.
 *   5. Read the volatile field into a local first, so the fast path performs only one
 *      volatile read.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Eager static final field                       --            --        acceptable
 *       > Thread-safe and simple; initialises even if never used.
 *   synchronized getInstance()                     --            --        avoid
 *       > Correct but locks on every read, forever.
 *   Double-checked locking WITHOUT volatile        --            --        avoid
 *       > Broken. Another thread can see a partially constructed object.
 *   Double-checked locking WITH volatile           --            --        acceptable
 *       > Correct, and the version they want you to explain.
 *   Initialization-on-demand holder                --            --        WRITE THIS
 *       > Lazy, lock-free, and the class loader does the work.
 *   enum                                           --            --        WRITE THIS
 *       > Immune to reflection, serialization and cloning. Not lazy, cannot extend.
 *
 * WHY THIS ONE
 *   The real question is "do you understand instruction reordering". Without volatile, the
 *   reference assignment can become visible before the constructor finishes, so another
 *   thread gets a non-null half-built object. Name the holder idiom and enum as the two
 *   you would actually ship, and be ready to say why enum defeats reflection: the JVM
 *   refuses to construct enum instances reflectively.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Exactly what can be reordered without volatile? Name the two steps.
 *   ? How does reflection break a private-constructor singleton, and how does enum stop
 *     it?
 *   ? Why does deserialization create a second instance, and what is readResolve?
 *   ? Singleton across multiple classloaders -- still a singleton?
 *   ? Is a Spring singleton the same thing?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Double-checked locking
 *   - Instruction reordering and the JMM
 *   - Safe publication
 *   - readResolve and serialization
 *   - Class loading and initialization
 * ============================================================================================== */

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.*;

public class Q37ThreadSafeSingleton {

    /* ---- BEST : enum. Immune to reflection, serialization and clone attacks. ---- */
    enum ConfigEnum {
        INSTANCE;
        private final Map<String, String> data = new ConcurrentHashMap<>();
        ConfigEnum() { data.put("env", "prod"); }
        public String get(String k) { return data.get(k); }
    }

    /* ---- BEST WHEN LAZY MATTERS : initialization-on-demand holder.
           The class loader provides the lock-free guarantee for free. ---- */
    static final class Config {
        private Config() {
            if (Holder.INSTANCE != null)
                throw new IllegalStateException("use getInstance()");   // blocks reflection
        }
        private static final class Holder { static final Config INSTANCE = new Config(); }
        static Config getInstance() { return Holder.INSTANCE; }
    }

    /* ---- DOUBLE-CHECKED LOCKING : correct ONLY with volatile ----
       Without volatile, the constructor can be reordered after the reference
       assignment, so another thread sees a non-null, half-built object. */
    static final class DclConfig {
        private static volatile DclConfig instance;
        private DclConfig() { }
        static DclConfig getInstance() {
            DclConfig local = instance;             // one volatile read on the fast path
            if (local == null) {
                synchronized (DclConfig.class) {
                    local = instance;
                    if (local == null) instance = local = new DclConfig();
                }
            }
            return local;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("enum value       : " + ConfigEnum.INSTANCE.get("env"));
        System.out.println("enum same ref    : " + (ConfigEnum.INSTANCE == ConfigEnum.valueOf("INSTANCE")));

        /* 32 threads racing for the instance - all must get the same object */
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            ConcurrentHashMap<Integer, Boolean> holderIds = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, Boolean> dclIds = new ConcurrentHashMap<>();
            CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(32);
            for (int i = 0; i < 32; i++) {
                pool.execute(() -> {
                    try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    holderIds.put(System.identityHashCode(Config.getInstance()), true);
                    dclIds.put(System.identityHashCode(DclConfig.getInstance()), true);
                    done.countDown();
                });
            }
            start.countDown();
            done.await(5, TimeUnit.SECONDS);
            System.out.println("holder distinct  : " + holderIds.size() + "  (must be 1)");
            System.out.println("DCL    distinct  : " + dclIds.size() + "  (must be 1)");
        } finally {
            pool.shutdownNow();
        }

        /* reflection attack on the holder singleton */
        Constructor<Config> ctor = Config.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
            System.out.println("reflection       : BREACHED");
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            System.out.println("reflection       : blocked -> " + cause.getClass().getSimpleName()
                    + ": " + cause.getMessage());
        }

        /* reflection cannot construct an enum at all - the JVM forbids it */
        try {
            Constructor<?> ec = ConfigEnum.class.getDeclaredConstructors()[0];
            ec.setAccessible(true);
            ec.newInstance();
            System.out.println("enum reflection  : BREACHED");
        } catch (Exception e) {
            System.out.println("enum reflection  : blocked by the JVM -> "
                    + e.getClass().getSimpleName());
        }
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * enum value       : prod
 * enum same ref    : true
 * holder distinct  : 1  (must be 1)
 * DCL    distinct  : 1  (must be 1)
 * reflection       : blocked -> IllegalStateException: use getInstance()
 * enum reflection  : blocked by the JVM -> IllegalArgumentException
 * ============================================================================================== */
