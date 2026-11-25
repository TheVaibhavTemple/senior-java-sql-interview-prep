/* ==============================================================================================
 * Q46  .  Immutable class, defensive copies, and the record gotcha
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Core Java & Java 21
 * DIFFICULTY   : EASY-MEDIUM  (1-2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- "immutable class kaise banaoge" har round mein aata hai, record ke saath
 * TECHNIQUE    : final class + private final fields + defensive copy in and out; record with a
 *                copying compact constructor
 *
 * PROBLEM
 *   Design an immutable money transfer: id, amount, a list of tags and a created-at date.
 *   First show a class that looks immutable but leaks its state through the constructor
 *   argument and through a getter. Fix it. Then write it as a record, show the record's
 *   gotcha, and say why immutability matters in a backend service.
 *
 * ALGORITHM
 *   1. Make the class final, so no subclass can override a getter or add mutable state.
 *   2. Every field private final. No setters. A "change" returns a new object (withAmount).
 *   3. Copy IN: copy every mutable argument in the constructor (List.copyOf,
 *      new Date(d.getTime())), so the caller's reference is cut off.
 *   4. Copy OUT: never return a mutable field itself; return a copy, or a list that is
 *      already unmodifiable.
 *   5. Prefer field types that are immutable already: String, BigDecimal, Instant, List.of.
 *   6. Record: final fields come free, but step 3 is still yours, in the compact constructor.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   final fields, arguments assigned directly      O(1)          O(1)      avoid
 *       > final freezes the reference, not the List or Date behind it. Leaks both ways.
 *   Collections.unmodifiableList(arg) in the ctor  O(1)          O(1)      avoid
 *       > A read-only VIEW of the caller's list. The caller can still change it.
 *   Copy in + copy out, final class                O(n)          O(n)      WRITE THIS
 *       > The textbook answer. Costs one copy of the list per object.
 *   record + List.copyOf in the compact ctor       O(n)          O(n)      WRITE THIS
 *       > Same guarantee, far less code. Without the copy it is shallow only.
 *
 * WHY THIS ONE
 *   Everyone says "final class, private final fields, no setters". The marks are in the next
 *   sentence: final freezes the reference, not the object, so a List or a Date field must be
 *   copied on the way in and on the way out. Then the record question: it looks immutable,
 *   but a List component is only as immutable as the list that was passed in.
 *
 * SAMJHO
 *   final sirf reference ko lock karta hai, andar ki List ya Date ko nahi. Isliye mutable
 *   cheez constructor mein copy karke rakho, aur getter se copy ya unmodifiable list do.
 *   Record bhi yahi galti karta hai, jab tak compact constructor mein List.copyOf na likho.
 *
 * AISE BOLO  (30-40 sec)
 *   "To make a class immutable I make it final, keep every field private final and drop the
 *   setters; a change returns a new object. The part people miss is that final only freezes
 *   the reference, so for a List or a Date I copy in the constructor and copy again in the
 *   getter, otherwise the caller can still change my state. A record gives me final fields,
 *   equals and hashCode for free, but a List component is only shallowly immutable, so I copy
 *   it with List.copyOf in the compact constructor. The payoff is an object that is
 *   thread-safe without locks, safe as a HashMap key, and safe to cache and share."
 *
 * TRAP
 *   ! Collections.unmodifiableList(list) copy nahi hai, sirf view hai: caller apni original
 *     list badlega to aapka "immutable" object bhi badal jaayega (output line 2).
 *   ! Record ka List component shallow hai. Compact constructor mein List.copyOf likho; dhyan
 *     rahe, List.copyOf null element par NullPointerException deta hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why must the class be final? What breaks if it is not?
 *     -> Subclass getter override karke mutable value de sakti hai, ya naya mutable field jod
 *        sakti hai. Doosra raasta: private constructor + static factory method.
 *   ? Is a final List field enough?
 *     -> Nahi. final sirf reassignment rokta hai, list.add() phir bhi chalta hai. Output
 *        line 1 dekho.
 *   ? Why is an immutable object thread-safe without synchronized?
 *     -> State kabhi badalti nahi, to race hi nahi. JMM final fields ki guarantee deta hai:
 *        constructor ke baad har thread ko sahi value dikhti hai, bas `this` escape na ho.
 *   ? Why are immutable objects safe HashMap keys and good for caching?
 *     -> hashCode kabhi nahi badalta, to key apne bucket mein hi rehti hai (Q47 mein mutable
 *        key ka bug dekho). Object freely share aur cache ho sakta hai; String apna hash
 *        bhi cache karta hai.
 *   ? Date or Instant? Where does BigDecimal fit?
 *     -> Naye code mein Instant / LocalDate lo, wo khud immutable hain, copy nahi lagti.
 *        Paise ke liye BigDecimal, wo bhi immutable hai: add() naya object deta hai.
 *   ? What does a record give you, and what can it not do?
 *     -> private final fields, canonical constructor, accessors, equals, hashCode, toString.
 *        Record final hai, class extend nahi kar sakta (interface implement kar sakta hai),
 *        extra instance field nahi rakh sakta. JPA entity nahi banta; DTO aur value object
 *        ke liye best hai.
 *   ? The list is copied, but its elements are mutable objects. Is it immutable now?
 *     -> Nahi. List.copyOf sirf list ko freeze karta hai, elements ko nahi. Elements bhi
 *        immutable rakho (String, record), warna unki bhi deep copy karo.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Defensive copying at every API boundary
 *   - Records as DTOs and value objects
 *   - Safe publication through final fields
 *   - Why String is immutable
 *   - Wither and builder methods for "changing" a value
 *
 * JAVA 21 MEIN
 *   record Java 16 se final hai, List.copyOf Java 10 se, List.of Java 9 se. Java 8 mein yahi
 *   kaam Collections.unmodifiableList(new ArrayList<>(list)) se hota tha: pehle copy, phir wrap.
 *
 * VARIANTS     : "Why is String immutable?", an immutable Money value object, unmodifiable vs
 *                immutable collections
 * ============================================================================================== */

import java.math.BigDecimal;
import java.util.*;

public class Q46ImmutableClassAndRecord {

    /* ---- THE TRAP : final fields, but the List and the Date leak in both directions ---- */
    static final class BrokenTransfer {
        private final String id;
        private final List<String> tags;
        private final Date createdAt;

        BrokenTransfer(String id, List<String> tags, Date createdAt) {
            this.id = id;
            this.tags = tags;                  // keeps a reference to the CALLER's list
            this.createdAt = createdAt;        // keeps a reference to the CALLER's Date
        }
        List<String> getTags()  { return tags; }         // hands out the internal list
        Date getCreatedAt()     { return createdAt; }    // hands out the internal Date
    }

    /* ---- HALF FIX : an unmodifiable VIEW of the caller's list is not a copy ---- */
    static final class ViewTransfer {
        private final List<String> tags;
        ViewTransfer(List<String> tags) { this.tags = Collections.unmodifiableList(tags); }
        List<String> getTags() { return tags; }
    }

    /* ---- THE FIX : final class, private final fields, copy in, copy out ---- */
    static final class Transfer {
        private final String id;
        private final BigDecimal amount;       // BigDecimal is immutable already: no copy needed
        private final List<String> tags;       // an unmodifiable COPY
        private final Date createdAt;          // Date is mutable: copy in AND copy out

        Transfer(String id, BigDecimal amount, List<String> tags, Date createdAt) {
            this.id = Objects.requireNonNull(id, "id");
            this.amount = Objects.requireNonNull(amount, "amount");
            this.tags = List.copyOf(tags);                     // copy in (Java 10+)
            this.createdAt = new Date(createdAt.getTime());    // copy in
        }
        String getId()          { return id; }
        BigDecimal getAmount()  { return amount; }
        List<String> getTags()  { return tags; }                          // unmodifiable already
        Date getCreatedAt()     { return new Date(createdAt.getTime()); } // copy out

        Transfer withAmount(BigDecimal newAmount) {                       // "change" = new object
            return new Transfer(id, newAmount, tags, createdAt);
        }
        @Override public boolean equals(Object o) {
            return o instanceof Transfer t && id.equals(t.id) && amount.equals(t.amount)
                    && tags.equals(t.tags) && createdAt.equals(t.createdAt);
        }
        @Override public int hashCode() { return Objects.hash(id, amount, tags, createdAt); }
    }

    /* ---- record WITHOUT a copy : the reference is final, the list behind it is not ---- */
    record ShallowRecord(String id, List<String> tags) { }

    /* ---- record WITH a copying compact constructor : really immutable ---- */
    record TransferRecord(String id, BigDecimal amount, List<String> tags) {
        TransferRecord {                                       // compact canonical constructor
            Objects.requireNonNull(id, "id");
            if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be > 0");
            tags = List.copyOf(tags);                          // reassigns the PARAMETER
        }
    }

    static String tryAdd(List<String> list) {
        try {
            list.add("X");
            return "added, so it is mutable";
        } catch (UnsupportedOperationException e) {
            return "UnsupportedOperationException";
        }
    }

    public static void main(String[] args) {
        // 1. BROKEN: change the state through the constructor argument and through the getter
        List<String> tags = new ArrayList<>(List.of("INR->USD"));
        Date when = new Date(0L);
        BrokenTransfer broken = new BrokenTransfer("T1", tags, when);
        tags.add("via-ctor-arg");                    // the caller still holds the same list
        broken.getTags().add("via-getter");          // the getter returned the internal list
        when.setTime(1_000L);                        // the caller still holds the same Date
        broken.getCreatedAt().setTime(2_000L);       // the getter returned the internal Date
        System.out.println("1 broken  tags      : " + broken.getTags()
                + "  <-- changed from outside");
        System.out.println("1 broken  createdAt : " + broken.getCreatedAt().getTime()
                + " ms, was 0  <-- changed from outside");

        // 2. HALF FIX: the view blocks add() on itself, but not on the caller's original list
        List<String> viewSource = new ArrayList<>(List.of("INR->USD"));
        ViewTransfer view = new ViewTransfer(viewSource);
        viewSource.add("via-ctor-arg");
        System.out.println("2 view    add       : " + tryAdd(view.getTags()));
        System.out.println("2 view    tags      : " + view.getTags()
                + "  <-- the caller's list still writes through");

        // 3. FIX: copy in, copy out
        List<String> fixSource = new ArrayList<>(List.of("INR->USD"));
        Date fixWhen = new Date(0L);
        Transfer t = new Transfer("T1", new BigDecimal("100.00"), fixSource, fixWhen);
        fixSource.add("via-ctor-arg");
        fixWhen.setTime(1_000L);
        t.getCreatedAt().setTime(2_000L);            // changes only the copy we were handed
        System.out.println("3 fixed   add       : " + tryAdd(t.getTags()));
        System.out.println("3 fixed   tags      : " + t.getTags());
        System.out.println("3 fixed   createdAt : " + t.getCreatedAt().getTime() + " ms");
        Transfer bigger = t.withAmount(new BigDecimal("250.00"));
        System.out.println("3 withAmount        : new object " + bigger.getAmount()
                + ", original still " + t.getAmount());

        // 4. record WITHOUT a copy: shallowly immutable only
        List<String> recordSource = new ArrayList<>(List.of("INR->USD"));
        ShallowRecord shallow = new ShallowRecord("T1", recordSource);
        recordSource.add("via-ctor-arg");
        shallow.tags().add("via-accessor");
        System.out.println("4 record, no copy   : " + shallow.tags() + "  <-- shallow only");

        // 5. record WITH List.copyOf in the compact constructor
        List<String> goodSource = new ArrayList<>(List.of("INR->USD"));
        TransferRecord rec = new TransferRecord("T1", new BigDecimal("100.00"), goodSource);
        goodSource.add("via-ctor-arg");
        System.out.println("5 record + copyOf   : " + rec.tags() + ", add -> "
                + tryAdd(rec.tags()));
        try {
            new TransferRecord("T2", BigDecimal.ZERO, List.of());
        } catch (IllegalArgumentException e) {
            System.out.println("5 record validation : IllegalArgumentException: " + e.getMessage());
        }
        try {
            new TransferRecord("T3", BigDecimal.TEN, Arrays.asList("ok", null));
        } catch (NullPointerException e) {
            System.out.println("5 null tag          : NullPointerException from List.copyOf");
        }
        TransferRecord sameRec = new TransferRecord("T1", new BigDecimal("100.00"),
                List.of("INR->USD"));
        System.out.println("5 record equals     : " + rec.equals(sameRec)
                + "  (value equality for free)");

        // 6. why it matters: an immutable value is a safe HashMap / cache key
        Map<Transfer, String> cache = new HashMap<>();
        cache.put(t, "fx quote cached");
        Transfer equalKey = new Transfer("T1", new BigDecimal("100.00"), List.of("INR->USD"),
                new Date(0L));
        System.out.println("6 cache, equal key  : " + cache.get(equalKey));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 1 broken  tags      : [INR->USD, via-ctor-arg, via-getter]  <-- changed from outside
 * 1 broken  createdAt : 2000 ms, was 0  <-- changed from outside
 * 2 view    add       : UnsupportedOperationException
 * 2 view    tags      : [INR->USD, via-ctor-arg]  <-- the caller's list still writes through
 * 3 fixed   add       : UnsupportedOperationException
 * 3 fixed   tags      : [INR->USD]
 * 3 fixed   createdAt : 0 ms
 * 3 withAmount        : new object 250.00, original still 100.00
 * 4 record, no copy   : [INR->USD, via-ctor-arg, via-accessor]  <-- shallow only
 * 5 record + copyOf   : [INR->USD], add -> UnsupportedOperationException
 * 5 record validation : IllegalArgumentException: amount must be > 0
 * 5 null tag          : NullPointerException from List.copyOf
 * 5 record equals     : true  (value equality for free)
 * 6 cache, equal key  : fx quote cached
 * ============================================================================================== */
