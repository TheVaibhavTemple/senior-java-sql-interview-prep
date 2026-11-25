/* ==============================================================================================
 * Q37  .  Thread-safe singleton
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Concurrency & Multithreading
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- singleton aur DCL service aur product dono companies mein aata hai
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
 * SAMJHO
 *   Singleton matlab ek classloader mein class ka ek hi object. Ship karne ke liye do: enum
 *   (reflection aur serialization se safe) ya holder idiom (lazy, bina lock ke). DCL tabhi
 *   sahi hai jab field volatile ho, warna doosra thread aadha bana object dekh sakta hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "A synchronized getInstance is correct but takes a lock on every call, forever. I would
 *   ship either an enum, which the JVM protects from reflection and serialization, or the
 *   holder idiom, where class loading gives me lazy, lock-free initialization. If they want
 *   double-checked locking, the field must be volatile, otherwise the reference can be
 *   published before the constructor finishes. Every call after the first is then a single
 *   volatile read."
 *
 * TRAP
 *   ! DCL bina volatile ke likha to seedha minus. Reason bolo: constructor ki writes aur
 *     reference assign reorder ho sakte hain.
 *   ! Private constructor reflection se nahi bachata, setAccessible(true) se naya object ban
 *     jata hai. Constructor mein guard lagao ya enum lo.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Exactly what can be reordered without volatile? Name the two steps.
 *     -> new mein: memory allocate, constructor ki field writes, reference assign. Bina volatile
 *        field writes aur reference publish reorder ho sakte hain; reader ko default values.
 *   ? How does reflection break a private-constructor singleton, and how does enum stop
 *     it?
 *     -> getDeclaredConstructor() + setAccessible(true) private constructor bhi chala deta hai.
 *        Enum ke liye Constructor.newInstance khud IllegalArgumentException phenkta hai.
 *   ? Why does deserialization create a second instance, and what is readResolve?
 *     -> readObject constructor ke bina naya object bana deta hai. readResolve() se INSTANCE
 *        return karo to wahi object milta hai. Enum naam se serialize hota hai, safe hai.
 *   ? Singleton across multiple classloaders -- still a singleton?
 *     -> Nahi. Class ki identity = naam + classloader, to har classloader ka apna instance.
 *        App server aur hot reload mein ye hota hai.
 *   ? Is a Spring singleton the same thing?
 *     -> Nahi. Spring singleton ek container mein ek bean definition ka ek object hai; new se
 *        aur bana sakte ho. Thread-safe tabhi hai jab usmein mutable state na ho.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Double-checked locking
 *   - Instruction reordering and the JMM
 *   - Safe publication
 *   - readResolve and serialization
 *   - Class loading and initialization
 *
 * VARIANTS     : classic - Bill Pugh holder, enum singleton, serialization-safe singleton
 *                with readResolve
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
