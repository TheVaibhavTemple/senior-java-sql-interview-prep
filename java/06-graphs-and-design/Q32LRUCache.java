/* ==============================================================================================
 * Q32  .  LRU cache
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Graphs & Design Data Structures
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- senior Java rounds ka favourite design DS; thread safety tak poochte hain
 * TECHNIQUE    : Hash map for lookup + doubly linked list for recency, with head/tail sentinels
 *
 * PROBLEM
 *   Design a cache with O(1) get and put that evicts the least recently used entry when it
 *   hits capacity.
 *
 * ALGORITHM
 *   1. The map answers 'where is this key' in O(1).
 *   2. The doubly linked list answers 'which key is oldest' in O(1).
 *   3. Head and tail sentinel nodes remove every null check from unlink and insert.
 *   4. get: look up, unlink the node, re-insert it after head, return the value.
 *   5. put at capacity: unlink tail.prev, remove it from the map, then insert the new node
 *      after head.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   HashMap + an ArrayList for order               O(n) per op   O(n)      avoid
 *       > Removing from the middle of an ArrayList is O(n). Misses the requirement.
 *   TreeMap keyed by timestamp                     O(log n)      O(n)      avoid
 *   LinkedHashMap with accessOrder = true          O(1)          O(n)      acceptable
 *       > Five lines and correct -- but it hides the structure they are testing.
 *   HashMap + doubly linked list with sentinels    O(1)          O(n)      WRITE THIS
 *       > The intended answer. Map for lookup, list for recency.
 *
 * WHY THIS ONE
 *   Two structures, two jobs: the map answers "where is this key", the doubly linked list
 *   answers "who is oldest". Head and tail sentinel nodes remove every null check from
 *   unlink and insert, which is exactly where the bugs live. Mention the LinkedHashMap
 *   one-liner, then write the full version -- they will ask for it regardless.
 *
 * SAMJHO
 *   Do structure, do kaam. HashMap batata hai key kahan hai, O(1) mein. Doubly linked list
 *   batati hai kaun sabse purana hai: head ke paas latest, tail ke paas oldest. get aur put
 *   dono par node front par jaata hai; capacity full ho to tail.prev hatao, map se bhi.
 *
 * AISE BOLO  (30-40 sec)
 *   "A HashMap with an ArrayList for order makes every access O(n), because removing from the
 *   middle of a list shifts elements. I combine a HashMap from key to node with a doubly
 *   linked list in recency order, with head and tail sentinels, so get and put are both O(1)
 *   and space is O(capacity). On every get or update I unlink the node and insert it after
 *   head, and at capacity I evict tail.prev from both the list and the map. I also mention
 *   that LinkedHashMap with accessOrder = true and removeEldestEntry does this in a few lines,
 *   and that this version is not thread-safe as written."
 *
 * TRAP
 *   ! Evict karte waqt node list aur map DONO se hatao; sirf list se hataya to map mein
 *     memory leak aur stale get.
 *   ! get bhi ek "use" hai: get par node front par nahi gaya to ye LRU nahi, FIFO ban jaata hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why a doubly and not a singly linked list?
 *     -> Beech ke node ko O(1) mein unlink karne ke liye uska prev chahiye. Singly list mein
 *        prev dhoondhne mein O(n) lagta hai.
 *   ? What do the head and tail sentinels buy you?
 *     -> Har real node ka prev aur next hamesha non-null rehta hai, to empty list, first ya
 *        last node ke liye alag if nahi likhna padta. Bugs wahin hote hain.
 *   ? Make it thread-safe -- and why doesn't a ReadWriteLock help?
 *     -> get bhi list badalta hai (node front par jaata hai), to har get ko write lock chahiye;
 *        read lock se koi fayda nahi. Ek lock ya synchronized lo; scale par Caffeine jaisi library.
 *   ? Implement LFU instead. What extra structure do you need?
 *     -> key -> node map ke saath frequency -> keys (LinkedHashSet) ka map, aur minFreq variable.
 *        Same frequency par tie ho to sabse purana evict. Sab O(1).
 *   ? Add a per-key TTL.
 *     -> Node mein expiresAt rakho; get par expired mile to hatao aur miss return karo (lazy).
 *        Saath mein background cleanup; Redis bhi lazy aur periodic dono expiry karta hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - LFU cache
 *   - Design an in-memory cache
 *   - Insert / delete / getRandom in O(1)
 *   - Browser history
 *   - All O(1) data structure
 *
 * JAVA 21 MEIN
 *   LinkedHashMap ab SequencedMap hai (JEP 431): pollFirstEntry() se eldest entry seedha milti
 *   hai aur putLast() key ko most-recent end par le jaata hai. Interview mein phir bhi DLL
 *   wala version likho. Virtual threads ke saath: Java 21 mein synchronized ke andar blocking
 *   call carrier thread ko pin karta hai, to lock ke andar I/O mat karo.
 *
 * APNI STORY SE JODO
 *   "At work I have used Redis as a cache [what it cached]. Redis can evict with an LRU policy
 *   such as allkeys-lru once maxmemory is reached, and its LRU is approximate because it
 *   samples keys; this question is the exact, in-process version of that."
 *   Note: thread-safe follow-up par batch wala example do: synchronized shared HashMap ko
 *   ConcurrentHashMap se badla, lock contention hati, window 45 min se 8 min.
 *
 * VARIANTS     : LC 146, LC 460, LC 380, LC 432, LC 1472
 * ============================================================================================== */

import java.util.*;

public class Q32LRUCache {

    /* ======================================================================
       HashMap answers "where is this key".
       Doubly linked list answers "which key is oldest".
       Sentinels remove every null check from unlink / insert.
       ====================================================================== */
    static final class LRUCache {
        private static final class Node {
            int key, value; Node prev, next;
            Node(int k, int v) { key = k; value = v; }
        }

        private final int capacity;
        private final Map<Integer, Node> map = new HashMap<>();
        private final Node head = new Node(0, 0);
        private final Node tail = new Node(0, 0);

        LRUCache(int capacity) {
            this.capacity = capacity;
            head.next = tail;
            tail.prev = head;
        }

        int get(int key) {
            Node n = map.get(key);
            if (n == null) return -1;
            moveToFront(n);                 // a read counts as a use
            return n.value;
        }

        void put(int key, int value) {
            Node n = map.get(key);
            if (n != null) { n.value = value; moveToFront(n); return; }

            if (map.size() == capacity) {
                Node lru = tail.prev;
                unlink(lru);
                map.remove(lru.key);        // remove from BOTH structures or you leak
            }
            Node fresh = new Node(key, value);
            map.put(key, fresh);
            insertAfterHead(fresh);
        }

        private void moveToFront(Node n)    { unlink(n); insertAfterHead(n); }
        private void unlink(Node n)         { n.prev.next = n.next; n.next.prev = n.prev; }
        private void insertAfterHead(Node n) {
            n.next = head.next; n.prev = head;
            head.next.prev = n; head.next = n;
        }

        String order() {                     // most-recent first, for the demo
            StringBuilder sb = new StringBuilder("[");
            for (Node n = head.next; n != tail; n = n.next)
                sb.append(n.key).append("=").append(n.value).append(n.next == tail ? "" : ", ");
            return sb.append("]").toString();
        }
    }

    /* ---- THE FIVE-LINE VERSION : mention it, then write the one above ---- */
    static final class LRU<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;
        LRU(int capacity) {
            super(capacity, 0.75f, true);        // true = ACCESS order, not insertion order
            this.capacity = capacity;
        }
        @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    public static void main(String[] args) {
        LRUCache c = new LRUCache(2);
        c.put(1, 1);                 System.out.println("put(1,1)        -> " + c.order());
        c.put(2, 2);                 System.out.println("put(2,2)        -> " + c.order());
        System.out.println("get(1) = " + c.get(1) + "      -> " + c.order());
        c.put(3, 3);                 System.out.println("put(3,3) evicts 2 -> " + c.order());
        System.out.println("get(2) = " + c.get(2) + "     (evicted)");
        c.put(4, 4);                 System.out.println("put(4,4) evicts 1 -> " + c.order());
        System.out.println("get(1) = " + c.get(1) + "     get(3) = " + c.get(3)
                + "   get(4) = " + c.get(4));

        LRU<String, Integer> easy = new LRU<>(2);
        easy.put("a", 1); easy.put("b", 2);
        easy.get("a");                        // touching "a" makes "b" the eldest
        easy.put("c", 3);
        System.out.println("LinkedHashMap ver: " + easy);

        LRUCache one = new LRUCache(1);
        one.put(1, 1); one.put(2, 2);
        System.out.println("capacity 1       : " + one.order() + "  get(1)=" + one.get(1));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * put(1,1)        -> [1=1]
 * put(2,2)        -> [2=2, 1=1]
 * get(1) = 1      -> [1=1, 2=2]
 * put(3,3) evicts 2 -> [3=3, 1=1]
 * get(2) = -1     (evicted)
 * put(4,4) evicts 1 -> [4=4, 3=3]
 * get(1) = -1     get(3) = 3   get(4) = 4
 * LinkedHashMap ver: {a=1, c=3}
 * capacity 1       : [2=2]  get(1)=-1
 * ============================================================================================== */
