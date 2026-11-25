/* ==============================================================================================
 * Q54  .  HashMap from scratch: buckets, hash spreading, resize and treeify
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Graphs & Design Data Structures
 * DIFFICULTY   : MEDIUM-HARD  (2-3 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- "HashMap internally kaise kaam karta hai" har Java round mein aata hai
 * TECHNIQUE    : Power-of-two bucket array, hash ^ (hash >>> 16), index = hash & (capacity - 1),
 *                separate chaining, resize at load factor 0.75 with a lo/hi split
 *
 * PROBLEM
 *   Build a generic map with put, get and remove: an array of buckets, a linked list in each
 *   bucket, the null key in bucket 0, and a resize at load factor 0.75. Then show what a bad
 *   hashCode does to it, and what java.util.HashMap does about that.
 *
 * ALGORITHM
 *   1. hash = h ^ (h >>> 16): fold the high 16 bits into the low 16, because the index only
 *      looks at the low bits.
 *   2. index = hash & (capacity - 1). The capacity is a power of two, so this equals
 *      Math.floorMod(hash, capacity): one AND, no division, never negative.
 *   3. put: walk the bucket. Same hash and equals -> replace the value. Otherwise append a
 *      node at the tail, and if ++size > capacity * 0.75, resize.
 *   4. resize: double the table. Each node either stays at index j or moves to j + oldCap,
 *      and one bit decides it: (hash & oldCap). The order inside each list is kept.
 *   5. get / remove: same hash, same index, then walk the bucket comparing the stored hash
 *      first and equals second. The null key has hash 0, so it always lives in bucket 0.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   List of entries, linear search                 O(n)          O(n)      avoid
 *   Buckets with index = hashCode % capacity       O(1) avg      O(n)      avoid
 *       > A negative hashCode gives a negative index: ArrayIndexOutOfBoundsException.
 *   Open addressing (linear probing)               O(1) avg      O(n)      acceptable
 *       > Cache friendly, but remove needs tombstones or shifting. IdentityHashMap uses it.
 *   Power-of-two buckets + chaining + resize       O(1) avg      O(n)      WRITE THIS
 *       > What java.util.HashMap does. Worst case here is O(n) for one crowded bucket.
 *   ... plus treeify a crowded bucket (JDK 8+)     O(log n)      O(n)      mention
 *       > Only when the keys are Comparable or their hashes differ -- see demo 7.
 *
 * WHY THIS ONE
 *   "How does HashMap work internally?" checks whether you know the mechanism, not the API.
 *   Build it once and every follow-up becomes obvious: why the capacity is a power of two,
 *   why the hash is spread, why 0.75, why a mutated key is lost, why it is not thread-safe.
 *   Treeify is where a senior answer stands out: it turns a crowded bucket from O(n) into
 *   O(log n), but only when the keys can be ordered.
 *
 * SAMJHO
 *   HashMap ek array hai, har slot mein ek chhoti linked list. Index ke liye pehle high bits
 *   ko low bits mein milao (h ^ h >>> 16), phir & (capacity - 1). Bucket mein pehle hash
 *   compare karo, phir equals. 75% bhar gaya to array double; har node ya to wahin rehta hai
 *   ya j + oldCap par jaata hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "A plain list of entries makes every lookup O(n), so I use an array of buckets with a
 *   linked list in each. I spread the hashCode with h ^ (h >>> 16) and take the index as
 *   hash & (capacity - 1), which works because the capacity is always a power of two; get and
 *   put are O(1) on average and space is O(n). When the size goes past 0.75 of the capacity I
 *   double the table, and each node either stays at its index or moves by exactly the old
 *   capacity. The worst case is a bad hashCode that sends every key to one bucket: here that
 *   is O(n), and the real HashMap turns such a bucket into a red-black tree once it goes past
 *   8 entries and the table has at least 64 buckets."
 *
 * TRAP
 *   ! index = hashCode % capacity likha to negative hashCode par negative index aata hai aur
 *     ArrayIndexOutOfBoundsException. hash & (capacity - 1) hamesha 0 se capacity - 1 tak.
 *   ! Treeify tabhi O(log n) deta hai jab keys Comparable hon ya hash alag hon. Sabka hashCode
 *     same aur key Comparable nahi to tree bhi poora scan karta hai: demo 7 mein 1001 equals.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why is the capacity always a power of two?
 *     -> Tab hash & (n - 1) hi Math.floorMod(hash, n) hai: ek AND, division nahi, kabhi
 *        negative nahi. Resize par ek bit (hash & oldCap) batata hai node j par rahega ya
 *        j + oldCap par. Kami: sirf low bits use hote hain, isliye h ^ (h >>> 16) (demo 3).
 *   ? What exactly does the JDK do with a crowded bucket?
 *     -> put ko bucket mein pehle se 8 entries milein (TREEIFY_THRESHOLD) aur table 64 ya bada
 *        ho (MIN_TREEIFY_CAPACITY) to bucket red-black tree banta hai, warna resize. Resize ke
 *        split mein 6 ya kam nodes (UNTREEIFY_THRESHOLD) par wapas list; remove par tree ka
 *        shape dekha jaata hai. 8 aur 6 ka gap list-tree ka flip-flop rokta hai.
 *   ? What happens when a key is mutated after insertion?
 *     -> Node mein put ke time ka hash save hai. Naye hash se lookup doosre bucket mein jaata
 *        hai, to get null aur containsKey false, par entry map mein padi rehti hai: leak
 *        (demo 6). Keys immutable rakho: String, Integer, record (Q47).
 *   ? HashMap vs Hashtable vs ConcurrentHashMap?
 *     -> HashMap: koi lock nahi, ek null key aur null values chalte hain. Hashtable: legacy,
 *        public methods synchronized (poore table par ek lock), null key ya value par NPE.
 *        ConcurrentHashMap: null nahi, get bina lock ke, put khali bucket par CAS warna sirf
 *        us bucket ke first node par synchronized; compute aur merge atomic.
 *   ? Why is HashMap not thread-safe?
 *     -> Do threads ek saath put karein to ek ka node overwrite ho sakta hai (lost update) aur
 *        size galat aata hai. Java 7 tak resize head insertion se list ulat deta tha; do
 *        threads ka concurrent resize bucket mein cycle bana sakta tha, phir get() infinite
 *        loop. Java 8 order rakhta hai, par race ab bhi hai: ConcurrentHashMap lo.
 *   ? How do you set the initial capacity for n entries?
 *     -> new HashMap<>(n) capacity hai, entries nahi: 100 par capacity 128, threshold 96, aur
 *        97th put par resize. (int) (n / 0.75f) + 1 do, ya Java 19+ mein HashMap.newHashMap(n),
 *        jo andar ceil(n / 0.75) karta hai. Demo 5: 1 resize vs 0.
 *   ? Why 0.75 and not 1.0?
 *     -> Memory aur chain length ka balance. JDK ke comments: random hashes par 0.75 par bucket
 *        size Poisson(0.5) jaisa hai, 8 entries wale bucket ki probability 0.00000006. Load
 *        factor badhao to memory bachegi, chains lambi hongi.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - HashSet internals (a HashMap whose values are one shared dummy object)
 *   - LinkedHashMap and the LRU cache (Q32)
 *   - The equals / hashCode contract (Q47)
 *   - ConcurrentHashMap: CAS on an empty bucket, lock on the first node (Q51)
 *   - Design HashMap / HashSet without the library
 *
 * JAVA 21 MEIN
 *   HashMap.newHashMap(n), HashSet.newHashSet(n) aur LinkedHashMap.newLinkedHashMap(n) (sab
 *   Java 19+) expected entries lete hain, capacity nahi. Sizing ka 0.75 wala hisaab khud mat
 *   karo, ye factory methods use karo.
 *
 * APNI STORY SE JODO
 *   "In a batch job I replaced a synchronized shared HashMap with ConcurrentHashMap to remove
 *   lock contention, and the batch window went from 45 minutes to 8 minutes."
 *   Note: Ye line thread-safety wale follow-up par bolo. Phir mechanism samjhao: CHM mein get
 *   bina lock ke aur put sirf ek bucket lock karta hai, isliye threads ek lock par queue nahi
 *   lagate.
 *
 * VARIANTS     : LC 706, LC 705
 * ============================================================================================== */

import java.util.*;

public class Q54HashMapFromScratch {

    /* ======================================================================
       A small HashMap: power-of-two table, spread hash, separate chaining,
       resize at 0.75 with the same lo/hi split that java.util.HashMap uses.
       (The JDK allocates the table lazily on the first put; this one does
       it in the constructor to keep the code short.)
       ====================================================================== */
    static final class SimpleHashMap<K, V> {
        static final float LOAD_FACTOR = 0.75f;

        private static final class Node<K, V> {
            final int hash;            // stored at put time; a mutated key keeps the OLD hash
            final K key;
            V value;
            Node<K, V> next;
            Node(int hash, K key, V value) { this.hash = hash; this.key = key; this.value = value; }
        }

        private Node<K, V>[] table;
        private int size, threshold, resizes;
        final List<String> resizeLog = new ArrayList<>();

        SimpleHashMap() { this(16); }

        @SuppressWarnings("unchecked")
        SimpleHashMap(int initialCapacity) {
            table = (Node<K, V>[]) new Node[tableSizeFor(initialCapacity)];
            threshold = (int) (table.length * LOAD_FACTOR);
        }

        /* the same maths as HashMap.newHashMap(n): capacity = ceil(n / 0.75) */
        static <K, V> SimpleHashMap<K, V> withExpectedSize(int n) {
            return new SimpleHashMap<>((int) Math.ceil(n / (double) LOAD_FACTOR));
        }

        static int tableSizeFor(int cap) {              // smallest power of two >= cap
            int n = 1;
            while (n < cap) n <<= 1;
            return n;
        }

        static int hash(Object key) {                   // null key -> 0 -> bucket 0
            int h;
            return key == null ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        }

        private int indexFor(int hash) { return hash & (table.length - 1); }

        private static boolean same(Node<?, ?> p, int h, Object key) {
            return p.hash == h && (p.key == key || (key != null && key.equals(p.key)));
        }

        V put(K key, V value) {
            int h = hash(key), i = indexFor(h);
            Node<K, V> p = table[i];
            if (p == null) {
                table[i] = new Node<>(h, key, value);
            } else {
                while (true) {
                    if (same(p, h, key)) { V old = p.value; p.value = value; return old; }
                    if (p.next == null) break;
                    p = p.next;
                }
                p.next = new Node<>(h, key, value);     // append at the tail, keep the order
            }
            if (++size > threshold) resize();
            return null;
        }

        V get(Object key) {
            Node<K, V> n = find(key);
            return n == null ? null : n.value;
        }

        boolean containsKey(Object key) { return find(key) != null; }

        private Node<K, V> find(Object key) {
            int h = hash(key);
            for (Node<K, V> p = table[indexFor(h)]; p != null; p = p.next)
                if (same(p, h, key)) return p;          // hash first, equals second
            return null;
        }

        V remove(Object key) {
            int h = hash(key), i = indexFor(h);
            Node<K, V> prev = null;
            for (Node<K, V> p = table[i]; p != null; prev = p, p = p.next) {
                if (same(p, h, key)) {
                    if (prev == null) table[i] = p.next; else prev.next = p.next;
                    size--;
                    return p.value;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private void resize() {
            Node<K, V>[] old = table;
            int oldCap = old.length, newCap = oldCap << 1;
            Node<K, V>[] fresh = (Node<K, V>[]) new Node[newCap];
            for (int j = 0; j < oldCap; j++) {
                Node<K, V> loHead = null, loTail = null, hiHead = null, hiTail = null, next;
                for (Node<K, V> e = old[j]; e != null; e = next) {
                    next = e.next;
                    e.next = null;
                    if ((e.hash & oldCap) == 0) {               // this one bit decides:
                        if (loTail == null) loHead = e; else loTail.next = e;
                        loTail = e;                             // stay at j ...
                    } else {
                        if (hiTail == null) hiHead = e; else hiTail.next = e;
                        hiTail = e;                             // ... or move to j + oldCap
                    }
                }
                fresh[j] = loHead;
                fresh[j + oldCap] = hiHead;
            }
            table = fresh;
            threshold = (int) (newCap * LOAD_FACTOR);
            resizes++;
            resizeLog.add("size " + size + ": " + oldCap + " -> " + newCap);
        }

        int size()                  { return size; }
        int capacity()              { return table.length; }
        int resizes()               { return resizes; }
        int bucketOf(Object key)    { return indexFor(hash(key)); }

        int longestChain() {
            int max = 0;
            for (Node<K, V> head : table) {
                int c = 0;
                for (Node<K, V> p = head; p != null; p = p.next) c++;
                max = Math.max(max, c);
            }
            return max;
        }
    }

    /* ---- THE BUG ON PURPOSE : every key has the same hashCode ---- */
    static int equalsCalls = 0;

    static final class BadKey implements Comparable<BadKey> {    // Comparable lets a tree help
        final int id;
        BadKey(int id) { this.id = id; }
        @Override public int hashCode() { return 42; }
        @Override public boolean equals(Object o) {
            equalsCalls++;
            return o instanceof BadKey b && b.id == id;
        }
        @Override public int compareTo(BadKey o) { return Integer.compare(id, o.id); }
    }

    static final class BadKeyNotComparable {                     // same bug, no ordering
        final int id;
        BadKeyNotComparable(int id) { this.id = id; }
        @Override public int hashCode() { return 42; }
        @Override public boolean equals(Object o) {
            equalsCalls++;
            return o instanceof BadKeyNotComparable b && b.id == id;
        }
    }

    /* ---- a key whose hashCode depends on a field that can change ---- */
    static final class MutableKey {
        int id;
        MutableKey(int id) { this.id = id; }
        @Override public int hashCode() { return Integer.hashCode(id); }
        @Override public boolean equals(Object o) {
            return o instanceof MutableKey m && m.id == id;
        }
    }

    public static void main(String[] args) {
        /* ---- 1 : put / get / remove, and the null key ---- */
        System.out.println("== 1. put / get / remove");
        SimpleHashMap<String, String> m = new SimpleHashMap<>();
        m.put("USD/INR", "83.10");
        m.put("GBP/INR", "105.20");
        System.out.println("get(USD/INR)          : " + m.get("USD/INR"));
        System.out.println("put(USD/INR) again    : returns old " + m.put("USD/INR", "83.25")
                + ", size " + m.size());
        m.put(null, "no pair");
        System.out.println("null key              : bucket " + m.bucketOf(null)
                + ", get(null) = " + m.get(null));
        System.out.println("remove(GBP/INR)       : " + m.remove("GBP/INR") + ", size " + m.size()
                + ", containsKey = " + m.containsKey("GBP/INR"));

        /* ---- 2 : THE WRONG INDEX : hashCode % capacity goes negative ---- */
        System.out.println("== 2. index: & (capacity - 1), never %");
        int h = "TXN-1001".hashCode();
        System.out.println("\"TXN-1001\".hashCode() : " + h);
        Object[] slots = new Object[16];
        try {
            slots[h % 16] = "x";
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("hashCode % 16         : " + (h % 16)
                    + "   <-- WRONG, ArrayIndexOutOfBoundsException");
        }
        System.out.println("hash(key) & (16 - 1)  : " + (SimpleHashMap.hash("TXN-1001") & 15));

        /* ---- 3 : why spread the hash : keys that differ only in the high bits ---- */
        System.out.println("== 3. h ^ (h >>> 16)");
        List<Integer> raw = new ArrayList<>(), spread = new ArrayList<>();
        for (int k = 1; k <= 8; k++) {
            int key = k * 65_536;                       // 0x10000, 0x20000, ... low 16 bits = 0
            raw.add(key & 15);
            spread.add(SimpleHashMap.hash(key) & 15);
        }
        System.out.println("without spreading     : " + raw + "   <-- one chain of 8");
        System.out.println("with h ^ (h >>> 16)   : " + spread);

        /* ---- 4 : resize at 0.75, and the lo/hi split ---- */
        System.out.println("== 4. resize at load factor 0.75, from capacity 4");
        SimpleHashMap<Integer, Integer> r = new SimpleHashMap<>(4);
        String before = "", after = "";
        for (int i = 1; i <= 13; i++) {
            if (i == 4) before = "key 1 -> " + r.bucketOf(1) + ", key 5 -> " + r.bucketOf(5);
            r.put(i, i);
            if (i == 4) after = "key 1 -> " + r.bucketOf(1) + ", key 5 -> " + r.bucketOf(5);
        }
        for (String line : r.resizeLog) System.out.println("resize at " + line);
        System.out.println("capacity 4 buckets    : " + before);
        System.out.println("capacity 8 buckets    : " + after + "   (5 & 4 != 0, so it moved +4)");

        /* ---- 5 : sizing for n entries ---- */
        System.out.println("== 5. initial capacity for 100 entries");
        SimpleHashMap<Integer, Integer> naive = new SimpleHashMap<>(100);
        SimpleHashMap<Integer, Integer> sized = SimpleHashMap.withExpectedSize(100);
        int naiveStart = naive.capacity(), sizedStart = sized.capacity();
        for (int i = 0; i < 100; i++) { naive.put(i, i); sized.put(i, i); }
        System.out.println("new (100)             : capacity " + naiveStart + " -> "
                + naive.capacity() + ", resizes = " + naive.resizes());
        System.out.println("withExpectedSize(100) : capacity " + sizedStart + " -> "
                + sized.capacity() + ", resizes = " + sized.resizes());

        /* ---- 6 : a key mutated after insertion ---- */
        System.out.println("== 6. mutated key");
        SimpleHashMap<MutableKey, String> mk = new SimpleHashMap<>();
        MutableKey key = new MutableKey(1);
        mk.put(key, "transfer-1");
        key.id = 2;
        System.out.println("after key.id = 2      : get = " + mk.get(key) + ", containsKey = "
                + mk.containsKey(key) + ", size = " + mk.size() + "   <-- LEAKED");
        key.id = 1;
        System.out.println("after key.id = 1      : get = " + mk.get(key));

        /* ---- 7 : a constant hashCode, and what the JDK does about it ---- */
        System.out.println("== 7. constant hashCode, 1000 keys, get() of a missing key");
        final int N = 1000;
        SimpleHashMap<BadKey, Integer> bad = new SimpleHashMap<>();
        SimpleHashMap<Integer, Integer> good = new SimpleHashMap<>();
        HashMap<BadKey, Integer> jdkComparable = new HashMap<>();
        HashMap<BadKeyNotComparable, Integer> jdkPlain = new HashMap<>();
        for (int i = 0; i < N; i++) {
            bad.put(new BadKey(i), i);
            good.put(i, i);
            jdkComparable.put(new BadKey(i), i);
            jdkPlain.put(new BadKeyNotComparable(i), i);
        }
        System.out.println("Simple, Integer keys     : longest chain " + good.longestChain());
        equalsCalls = 0;
        bad.get(new BadKey(5000));
        System.out.println("Simple, constant hash    : longest chain " + bad.longestChain()
                + ", equals calls " + equalsCalls + "   <-- O(n) walk");
        equalsCalls = 0;
        jdkComparable.get(new BadKey(5000));
        System.out.println("JDK, Comparable key      : equals calls " + equalsCalls
                + "     (treeified, O(log n))");
        equalsCalls = 0;
        jdkPlain.get(new BadKeyNotComparable(5000));
        System.out.println("JDK, not Comparable key  : equals calls " + equalsCalls
                + "   (the tree cannot order them)");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * == 1. put / get / remove
 * get(USD/INR)          : 83.10
 * put(USD/INR) again    : returns old 83.10, size 2
 * null key              : bucket 0, get(null) = no pair
 * remove(GBP/INR)       : 105.20, size 2, containsKey = false
 * == 2. index: & (capacity - 1), never %
 * "TXN-1001".hashCode() : -860734973
 * hashCode % 16         : -13   <-- WRONG, ArrayIndexOutOfBoundsException
 * hash(key) & (16 - 1)  : 1
 * == 3. h ^ (h >>> 16)
 * without spreading     : [0, 0, 0, 0, 0, 0, 0, 0]   <-- one chain of 8
 * with h ^ (h >>> 16)   : [1, 2, 3, 4, 5, 6, 7, 8]
 * == 4. resize at load factor 0.75, from capacity 4
 * resize at size 4: 4 -> 8
 * resize at size 7: 8 -> 16
 * resize at size 13: 16 -> 32
 * capacity 4 buckets    : key 1 -> 1, key 5 -> 1
 * capacity 8 buckets    : key 1 -> 1, key 5 -> 5   (5 & 4 != 0, so it moved +4)
 * == 5. initial capacity for 100 entries
 * new (100)             : capacity 128 -> 256, resizes = 1
 * withExpectedSize(100) : capacity 256 -> 256, resizes = 0
 * == 6. mutated key
 * after key.id = 2      : get = null, containsKey = false, size = 1   <-- LEAKED
 * after key.id = 1      : get = transfer-1
 * == 7. constant hashCode, 1000 keys, get() of a missing key
 * Simple, Integer keys     : longest chain 1
 * Simple, constant hash    : longest chain 1000, equals calls 1000   <-- O(n) walk
 * JDK, Comparable key      : equals calls 18     (treeified, O(log n))
 * JDK, not Comparable key  : equals calls 1001   (the tree cannot order them)
 * ============================================================================================== */
