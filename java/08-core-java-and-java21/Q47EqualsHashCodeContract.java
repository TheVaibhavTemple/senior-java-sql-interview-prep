/* ==============================================================================================
 * Q47  .  The equals / hashCode contract: three ways a key goes missing
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Core Java & Java 21
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- "equals kiya, hashCode nahi, to kya hoga?" har round ka sawaal hai
 * TECHNIQUE    : equals and hashCode from the same immutable fields (or a record); a comparator
 *                that is consistent with equals
 *
 * PROBLEM
 *   Show three bugs: (1) only equals is overridden, so an equal key is not found in a
 *   HashMap; (2) a key's field changes after insertion, so the entry becomes unreachable;
 *   (3) a TreeSet whose compareTo disagrees with equals silently drops an element. Then show
 *   the fixed version of each, including the BigDecimal 100.0 vs 100.00 case from money code.
 *
 * ALGORITHM
 *   1. HashMap lookup is two steps: hashCode picks the bucket, then equals picks the entry.
 *   2. Contract: a.equals(b) means a.hashCode() == b.hashCode(). The reverse is not needed;
 *      a shared hash is just a collision.
 *   3. Build both from the SAME fields, and only from fields that never change.
 *   4. HashMap stores each entry's hash at put time. Change the field later and the entry
 *      sits in a bucket that no lookup will ever search with a matching hash.
 *   5. TreeMap and TreeSet never call equals: compareTo == 0 means "same element". Break
 *      ties on the identity field so compareTo is 0 only when equals is true.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   equals only, Object.hashCode kept              --            --        avoid
 *       > Equal keys get different identity hashes and land in different buckets.
 *   equals + hashCode on a mutable field           --            --        avoid
 *       > Correct until the field changes; then a silent leak.
 *   equals + hashCode on final fields, or a record O(1) lookup   --        WRITE THIS
 *       > A record generates both from the same components.
 *   compareTo on amount only                       --            --        avoid
 *       > A TreeSet treats two different payments with the same amount as one.
 *   comparing(amount).thenComparing(id)            O(log n)      --        WRITE THIS
 *       > Consistent with equals: 0 only for the same payment.
 *
 * WHY THIS ONE
 *   Every interviewer asks "what happens if you override equals but not hashCode". The
 *   senior answer explains the mechanism (bucket first, then equals) and then goes past the
 *   textbook: a mutable key leaks silently, and sorted collections use compareTo instead of
 *   equals. In fintech the BigDecimal scale case is the one that bites in real code.
 *
 * SAMJHO
 *   HashMap pehle hashCode se bucket dhoondta hai, phir us bucket mein equals se entry. Sirf
 *   equals override kiya to do equal objects alag bucket mein jaate hain aur get() null deta
 *   hai. Key ka field baad mein badla to entry purane bucket mein phans jaati hai. TreeSet
 *   equals dekhta hi nahi; compareTo 0 aaya matlab duplicate.
 *
 * AISE BOLO  (30-40 sec)
 *   "A HashMap finds a key in two steps: hashCode picks the bucket and equals picks the entry,
 *   so if I override only equals, two equal keys usually land in different buckets and get
 *   returns null. I build equals and hashCode from the same fields, and only from fields that
 *   never change, because the hash is stored at put time; if the key changes later, contains
 *   returns false and the entry leaks. In practice my keys are records or immutable classes.
 *   For TreeMap and TreeSet, compareTo must agree with equals, because they never call
 *   equals; BigDecimal 100.0 and 100.00 are equal by compareTo but not by equals."
 *
 * TRAP
 *   ! BigDecimal ka equals scale bhi dekhta hai: 100.0 aur 100.00 equals mein false, compareTo
 *     mein 0. Paisa HashMap key ya HashSet mein daalne se pehle setScale karo.
 *   ! JPA entity ka equals/hashCode us generated id par mat banao jo save ke baad set hoti hai:
 *     save se pehle HashSet mein daala to yahi mutable-key bug aa jaata hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? What is the full equals contract?
 *     -> Reflexive, symmetric, transitive, consistent, aur x.equals(null) false. Saath mein
 *        hashCode rule: equal objects ka hashCode same.
 *   ? Two unequal objects have the same hashCode. Is that a bug?
 *     -> Nahi, collision allowed hai, bas speed girti hai. Sabka hashCode same (return 1) to
 *        saari keys ek bucket mein: Java 8+ mein 8 entries ke baad bin tree ban jaata hai
 *        (table 64 ya zyada ho tab), warna list scan O(n).
 *   ? instanceof or getClass() in equals?
 *     -> getClass() subclass ko kabhi equal nahi maanta, symmetry safe rehti hai. instanceof
 *        final class ya record ke liye theek hai, aur Hibernate proxy ke saath wahi chalta hai.
 *   ? Why is the mutable key unreachable, and can it ever be found again?
 *     -> Node mein put ke time ka hash save hai. Nayi value se lookup ka hash match nahi
 *        hota; purani value se lookup sahi bucket mein jaata hai par equals fail. Field purani
 *        value par lautega tabhi milega.
 *   ? Why does a TreeSet drop an element when compareTo is inconsistent with equals?
 *     -> TreeMap sirf compareTo use karta hai; 0 aaya to "already present". Comparator mein
 *        tie-breaker do: comparing(amount).thenComparing(id).
 *   ? What does a record generate for equals and hashCode?
 *     -> Saare components par equals aur hashCode. Array component reference se compare hota
 *        hai, content se nahi, isliye record mein array rakhne se bacho.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - HashMap internals: bucket, stored hash, treeify
 *   - Records and immutable classes as map keys
 *   - Comparator consistent with equals (TreeMap, TreeSet)
 *   - BigDecimal comparison in money code
 *   - equals and hashCode for JPA entities
 *
 * JAVA 21 MEIN
 *   record se equals, hashCode aur toString compiler banata hai, haath ki galti khatam.
 *   `o instanceof Key k && ...` (pattern matching, Java 16) se equals ek line ka ho jaata hai.
 *
 * VARIANTS     : "Override equals without hashCode: what happens?", a cache key class,
 *                BigDecimal equals vs compareTo
 * ============================================================================================== */

import java.math.BigDecimal;
import java.util.*;

public class Q47EqualsHashCodeContract {

    /* ---- BUG 1 : equals overridden, hashCode NOT (Object's identity hashCode stays) ---- */
    static final class EqualsOnlyKey {
        private final String accountNo;
        EqualsOnlyKey(String accountNo) { this.accountNo = accountNo; }
        @Override public boolean equals(Object o) {
            return o instanceof EqualsOnlyKey k && accountNo.equals(k.accountNo);
        }
        // no hashCode(): two equal keys still get two different identity hash codes
    }

    /* ---- BUG 2 : equals and hashCode agree, but they are built on a MUTABLE field ---- */
    static final class MutableKey {
        String accountNo;                                  // not final: this is the bug
        MutableKey(String accountNo) { this.accountNo = accountNo; }
        @Override public boolean equals(Object o) {
            return o instanceof MutableKey k && accountNo.equals(k.accountNo);
        }
        @Override public int hashCode() { return accountNo.hashCode(); }
    }

    /* ---- FIX for 1 and 2 : a record = final fields + equals and hashCode on the same ones ---- */
    record AccountKey(String accountNo) { }

    /* ---- BUG 3 : compareTo looks at amount only; equals looks at id AND amount ---- */
    record Payment(String id, BigDecimal amount) implements Comparable<Payment> {
        @Override public int compareTo(Payment o) { return amount.compareTo(o.amount); }
    }

    /* ---- FIX for 3 : break ties on the identity field, so 0 means "the same payment" ---- */
    static final Comparator<Payment> BY_AMOUNT_THEN_ID =
            Comparator.comparing(Payment::amount).thenComparing(Payment::id);

    static List<String> ids(Collection<Payment> payments) {
        return payments.stream().map(Payment::id).toList();
    }

    public static void main(String[] args) {
        // 1. equals only
        Map<EqualsOnlyKey, String> m1 = new HashMap<>();
        m1.put(new EqualsOnlyKey("ACC-1"), "balance 500");
        Set<EqualsOnlyKey> s1 = new HashSet<>();
        s1.add(new EqualsOnlyKey("ACC-1"));
        s1.add(new EqualsOnlyKey("ACC-1"));
        System.out.println("1 equals only  : equals() = "
                + new EqualsOnlyKey("ACC-1").equals(new EqualsOnlyKey("ACC-1"))
                + ", but get(equal key) = " + m1.get(new EqualsOnlyKey("ACC-1"))
                + "  <-- identity hashes differ");
        System.out.println("1 equals only  : HashSet of two equal keys, size = " + s1.size()
                + "  <-- a duplicate");

        Map<AccountKey, String> f1 = new HashMap<>();
        f1.put(new AccountKey("ACC-1"), "balance 500");
        Set<AccountKey> fs1 = new HashSet<>();
        fs1.add(new AccountKey("ACC-1"));
        fs1.add(new AccountKey("ACC-1"));
        System.out.println("1 fixed record : get(equal key) = " + f1.get(new AccountKey("ACC-1"))
                + ", HashSet size = " + fs1.size());

        // 2. a mutable key changed after insertion
        Map<MutableKey, String> m2 = new HashMap<>();
        MutableKey key = new MutableKey("ACC-1");
        m2.put(key, "balance 500");
        key.accountNo = "ACC-9";                           // changed AFTER it went into the map
        System.out.println("2 mutable key  : containsKey(the same object)    = "
                + m2.containsKey(key));
        System.out.println("2 mutable key  : containsKey(key with old value) = "
                + m2.containsKey(new MutableKey("ACC-1")));
        System.out.println("2 mutable key  : size = " + m2.size() + ", remove(key) = "
                + m2.remove(key) + "  <-- unreachable: a leak");

        Map<AccountKey, String> f2 = new HashMap<>();
        AccountKey oldKey = new AccountKey("ACC-1");
        f2.put(oldKey, "balance 500");
        String moved = f2.remove(oldKey);                  // a record cannot change: remove + put
        f2.put(new AccountKey("ACC-9"), moved);
        System.out.println("2 fixed record : remove + put a new key, containsKey(ACC-9) = "
                + f2.containsKey(new AccountKey("ACC-9")) + ", size = " + f2.size());

        // 3. TreeSet with compareTo inconsistent with equals
        Payment p1 = new Payment("P1", new BigDecimal("100.00"));
        Payment p2 = new Payment("P2", new BigDecimal("100.00"));
        System.out.println("3 payments     : p1.equals(p2) = " + p1.equals(p2)
                + ", p1.compareTo(p2) = " + p1.compareTo(p2));
        Set<Payment> tree = new TreeSet<>(List.of(p1, p2));
        Set<Payment> hash = new HashSet<>(List.of(p1, p2));
        System.out.println("3 amount only  : TreeSet " + ids(tree) + ", HashSet size = "
                + hash.size() + "  <-- P2 silently dropped");
        Set<Payment> fixedTree = new TreeSet<>(BY_AMOUNT_THEN_ID);
        fixedTree.addAll(List.of(p1, p2));
        System.out.println("3 fixed cmp    : amount then id, TreeSet " + ids(fixedTree));

        // 3b. the money-code version of the same bug: BigDecimal scale
        BigDecimal a = new BigDecimal("100.0"), b = new BigDecimal("100.00");
        System.out.println("3 BigDecimal   : 100.0 vs 100.00 equals = " + a.equals(b)
                + ", compareTo = " + a.compareTo(b));
        System.out.println("3 BigDecimal   : HashSet size = " + new HashSet<>(List.of(a, b)).size()
                + ", TreeSet size = " + new TreeSet<>(List.of(a, b)).size()
                + "  <-- same two numbers, two answers");
        Set<BigDecimal> normalised = new HashSet<>(List.of(a.setScale(2), b.setScale(2)));
        System.out.println("3 setScale(2)  : HashSet size = " + normalised.size());
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 1 equals only  : equals() = true, but get(equal key) = null  <-- identity hashes differ
 * 1 equals only  : HashSet of two equal keys, size = 2  <-- a duplicate
 * 1 fixed record : get(equal key) = balance 500, HashSet size = 1
 * 2 mutable key  : containsKey(the same object)    = false
 * 2 mutable key  : containsKey(key with old value) = false
 * 2 mutable key  : size = 1, remove(key) = null  <-- unreachable: a leak
 * 2 fixed record : remove + put a new key, containsKey(ACC-9) = true, size = 1
 * 3 payments     : p1.equals(p2) = false, p1.compareTo(p2) = 0
 * 3 amount only  : TreeSet [P1], HashSet size = 2  <-- P2 silently dropped
 * 3 fixed cmp    : amount then id, TreeSet [P1, P2]
 * 3 BigDecimal   : 100.0 vs 100.00 equals = false, compareTo = 0
 * 3 BigDecimal   : HashSet size = 2, TreeSet size = 1  <-- same two numbers, two answers
 * 3 setScale(2)  : HashSet size = 1
 * ============================================================================================== */
