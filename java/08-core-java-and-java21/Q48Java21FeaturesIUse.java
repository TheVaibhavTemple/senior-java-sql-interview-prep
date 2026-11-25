/* ==============================================================================================
 * Q48  .  Java 8 to 21: the features a backend developer actually uses
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Core Java & Java 21
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- resume par Java 21 hai, to "Java 8 ke baad kya use kiya" pakka aayega
 * TECHNIQUE    : sealed interface + records + exhaustive pattern-matching switch; text blocks;
 *                var; sequenced collections; Stream.toList()
 *
 * PROBLEM
 *   "Your resume says Java 21. What did you actually use after Java 8?" Answer it in running
 *   code with payment events: records, a sealed interface, a switch with record patterns and
 *   a `when` guard and no default, a text block, var, getFirst / getLast / reversed, and the
 *   Stream.toList() trap. Show the Java 8 style next to it and how it silently goes wrong.
 *
 * ALGORITHM
 *   1. Model each event as a record (Java 16) and close the set with a sealed interface
 *      (Java 17).
 *   2. Switch over the sealed type with record patterns and a `when` guard (Java 21). No
 *      default: the compiler checks that every permitted type has a case.
 *   3. Write JSON or SQL as a text block (Java 15) and fill it with formatted().
 *   4. Use var (Java 10) for locals whose type is obvious from the right-hand side.
 *   5. Use getFirst / getLast / reversed (Java 21) instead of get(0) and get(size() - 1).
 *   6. Stream.toList() (Java 16) returns an unmodifiable list; Collectors.toList() promises
 *      nothing about mutability.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   instanceof chain + casts + catch-all else      --            --        avoid
 *       > Java 8 style. A new event type falls into the else and nobody notices.
 *   enum + switch with a default                   --            --        acceptable
 *       > Fine for constants, but an enum cannot carry different data per case.
 *   sealed interface + record patterns, no default --            --        WRITE THIS
 *       > Add a new type and every switch that misses it stops compiling.
 *
 * WHY THIS ONE
 *   When a resume says Java 21, the interviewer checks whether you used it or only bumped the
 *   version in the pom. A list of JEP numbers does not help. One connected example does:
 *   records for the data, a sealed interface for the closed set, and a switch that breaks the
 *   build when someone adds a type. Then the small daily things: text blocks, var, getLast,
 *   and the toList() trap that bites during a migration.
 *
 * SAMJHO
 *   Java 8 se 21 tak sabse kaam ka combo: record (data), sealed interface (types ki band list)
 *   aur pattern-matching switch (har type ka case, default ke bina). Naya type add karo to
 *   compiler khud batata hai kahan case missing hai. Roz ke kaam ke liye: text block, var,
 *   getFirst / getLast, aur Stream.toList() jo unmodifiable list deta hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "From Java 8 to 21, the features that change backend code most are records for DTOs and
 *   events, sealed interfaces, and switch with pattern matching. With a sealed interface of
 *   records, a switch that deconstructs each record and has no default breaks the build when
 *   someone adds a new event type, instead of the event silently falling into an else. Then
 *   text blocks for JSON and SQL, var for obvious locals, getFirst and getLast, and virtual
 *   threads for blocking I/O, which I have used at work. One migration trap: Stream.toList()
 *   is unmodifiable, so code that adds to the result afterwards throws an exception."
 *
 * TRAP
 *   ! Stream.toList() unmodifiable list deta hai. Collectors.toList() ko toList() se badla aur
 *     baad mein add() kiya to runtime par UnsupportedOperationException. Migration ka aam bug.
 *   ! `case Initiated i` ko `case Initiated(...) when ...` se pehle likha to compile error
 *     (dominated label): guard wala case pehle. Aur null par switch NPE deta hai, jab tak
 *     `case null` na likho.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why is the switch exhaustive without a default, and why is that better than a default?
 *     -> Sealed interface ke saare permitted types compiler ko pata hain. default likha to naya
 *        type chupchap default mein jaayega; default nahi likha to compile error. Output mein
 *        Java 8 style ka "UNKNOWN P4" dekho.
 *   ? Record or Lombok @Value / @Data?
 *     -> Record language feature hai, annotation processor nahi chahiye. Par record ke fields
 *        final hain, setter nahi, aur wo class extend nahi kar sakta. Entity ke liye normal
 *        class, DTO aur event ke liye record.
 *   ? What does var NOT allow?
 *     -> Sirf local variable, for loop aur try-with-resources mein (lambda params Java 11 se).
 *        Field, method parameter, return type mein nahi. `var x = null` nahi chalta, aur
 *        `var list = new ArrayList<>()` ArrayList<Object> ban jaata hai.
 *   ? Stream.toList() vs Collectors.toList() vs Collectors.toUnmodifiableList()?
 *     -> Stream.toList() (16): unmodifiable, null allowed. Collectors.toList(): mutability ki
 *        koi guarantee nahi (aaj ArrayList). toUnmodifiableList() (10): unmodifiable, null par NPE.
 *   ? What else breaks when you move an old codebase to 17 or 21?
 *     -> Java 11 mein Java EE modules (JAXB, JAX-WS) JDK se hat gaye, dependency alag se daalni
 *        padti hai. Java 17 mein JDK internals strongly encapsulated hain, purani reflection
 *        wali libraries --add-opens maangti hain. Spring Boot 3 ko Java 17+ aur jakarta chahiye.
 *   ? What else came in Java 21 besides virtual threads?
 *     -> Pattern matching for switch aur record patterns final hue, sequenced collections aaye,
 *        generational ZGC opt-in aaya. String templates, structured concurrency aur scoped
 *        values 21 mein sirf preview the, production ke liye nahi.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Sealed types + records as algebraic data types
 *   - Replacing instanceof chains and visitor boilerplate
 *   - "Java 8 vs 11 vs 17 vs 21" comparison questions
 *   - Immutable collections: List.of, Stream.toList()
 *   - DTO and event design with records
 *
 * JAVA 21 MEIN
 *   Version yaad rakho: List.of 9, var 10, HttpClient 11, switch expression 14, text block 15,
 *   record + instanceof pattern + Stream.toList() 16, sealed 17, pattern switch + record
 *   pattern + sequenced collections + virtual threads 21.
 *
 * APNI STORY SE JODO
 *   "In a team of three, I moved seven production microservices from Java 1.7 directly to
 *   Java 21 and Spring Boot 3, and from on-premises to AKS, in about six months. The biggest
 *   break was javax to jakarta, and ActiveMQ was replaced by Azure Service Bus through the
 *   JMS library."
 *   Note: Iske baad sawaal aayega "kya toota aur kaise fix kiya?". javax se jakarta pehla
 *   jawab hai; kaunse packages the, wo [bhar do: jo sach mein code mein the]. Naye features
 *   mein se khud kya use kiya, wo bhi [bhar do]; jo use nahi kiya, wo mat bolo.
 *
 * VARIANTS     : "Java 8 vs 17 vs 21 differences", replace an instanceof chain with a switch,
 *                record vs class vs Lombok
 * ============================================================================================== */

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class Q48Java21FeaturesIUse {

    /* ---- records (Java 16) closed by a sealed interface (Java 17) ---- */
    sealed interface PaymentEvent permits Initiated, Settled, Failed, Refunded {
        String id();                                   // every record's id() accessor implements it
    }
    record Initiated(String id, BigDecimal amount, String currency) implements PaymentEvent { }
    record Settled(String id, BigDecimal amount) implements PaymentEvent { }
    record Failed(String id, String reason) implements PaymentEvent { }
    record Refunded(String id, BigDecimal amount) implements PaymentEvent { }   // added later

    static final BigDecimal REVIEW_LIMIT = new BigDecimal("500000");

    /* ---- THE OLD WAY (Java 8 style): instanceof chain, casts, a catch-all else ---- */
    static String describeJava8(PaymentEvent e) {
        if (e instanceof Initiated) {
            Initiated i = (Initiated) e;
            return (i.amount().compareTo(REVIEW_LIMIT) > 0 ? "REVIEW    " : "INITIATED ") + i.id();
        } else if (e instanceof Settled) {
            return "SETTLED   " + ((Settled) e).id();
        } else if (e instanceof Failed) {
            return "FAILED    " + ((Failed) e).id();
        }
        return "UNKNOWN   " + e.id();             // Refunded was added later and lands here
    }

    /* ---- JAVA 21 : record patterns + a when guard, exhaustive, NO default ---- */
    static String describe(PaymentEvent e) {
        return switch (e) {
            case Initiated(var id, var amount, var currency)
                    when amount.compareTo(REVIEW_LIMIT) > 0 -> "REVIEW    " + id + " " + amount
                                                               + " " + currency + " (over limit)";
            case Initiated i                     -> "INITIATED " + i.id();   // type pattern
            case Settled(var id, var amount)     -> "SETTLED   " + id + " " + amount;
            case Failed(var id, var reason)      -> "FAILED    " + id + " (" + reason + ")";
            case Refunded(var id, var amount)    -> "REFUNDED  " + id + " " + amount;
            // no default: delete any case above and this method stops compiling
        };
    }

    static String tryAdd(List<String> list) {
        try {
            list.add("X");
            return "ok, mutable";
        } catch (UnsupportedOperationException e) {
            return "UnsupportedOperationException";
        }
    }

    public static void main(String[] args) {
        List<PaymentEvent> events = List.of(
                new Initiated("P1", new BigDecimal("750000.00"), "INR"),
                new Initiated("P2", new BigDecimal("1200.50"), "INR"),
                new Settled("P2", new BigDecimal("1200.50")),
                new Failed("P3", "beneficiary bank timeout"),
                new Refunded("P4", new BigDecimal("300.00")));

        // 1. the Java 8 style chain: the new Refunded type silently becomes UNKNOWN
        for (PaymentEvent e : events) {
            String line = describeJava8(e);
            System.out.println("java 8 chain   : " + line
                    + (line.startsWith("UNKNOWN") ? "  <-- new type fell into the else" : ""));
        }

        // 2. the Java 21 switch: every type handled, the compiler made sure of it
        for (PaymentEvent e : events) System.out.println("java 21 switch : " + describe(e));
        try {
            describe(null);
        } catch (NullPointerException ex) {
            System.out.println("switch on null : NullPointerException (no case null)");
        }

        // 3. text block (Java 15) + formatted(): no "\n" + escaped quotes
        String json = """
                {
                  "id": "%s",
                  "amount": %s,
                  "currency": "%s"
                }""".formatted("P2", "1200.50", "INR");
        System.out.println("text block     :");
        System.out.println(json);

        // 4. var (Java 10) + instanceof pattern (Java 16)
        var settledTotal = BigDecimal.ZERO;
        for (var e : events) {
            if (e instanceof Settled s) settledTotal = settledTotal.add(s.amount());   // no cast
        }
        System.out.println("var + instanceof pattern : settled total = " + settledTotal);

        // 5. sequenced collections (Java 21)
        List<String> ids = events.stream().map(PaymentEvent::id).toList();
        System.out.println("getFirst / getLast       : " + ids.getFirst() + " / " + ids.getLast()
                + "   (Java 8: get(0) / get(size() - 1))");
        System.out.println("reversed()               : " + ids.reversed());
        var distinctIds = new LinkedHashSet<>(ids);
        System.out.println("LinkedHashSet getLast    : " + distinctIds.getLast()
                + "   (Java 8: iterate to the end)");
        var lastSeen = new LinkedHashMap<String, String>();
        for (var e : events) lastSeen.put(e.id(), e.getClass().getSimpleName());
        System.out.println("LinkedHashMap first/last : " + lastSeen.firstEntry() + " / "
                + lastSeen.lastEntry());

        // 6. Stream.toList() is unmodifiable; Collectors.toList() happens to be an ArrayList
        System.out.println("Stream.toList().add      : " + tryAdd(ids));
        List<String> collected = events.stream().map(PaymentEvent::id).collect(Collectors.toList());
        System.out.println("Collectors.toList().add  : " + tryAdd(collected)
                + " (ArrayList today, not promised)");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * java 8 chain   : REVIEW    P1
 * java 8 chain   : INITIATED P2
 * java 8 chain   : SETTLED   P2
 * java 8 chain   : FAILED    P3
 * java 8 chain   : UNKNOWN   P4  <-- new type fell into the else
 * java 21 switch : REVIEW    P1 750000.00 INR (over limit)
 * java 21 switch : INITIATED P2
 * java 21 switch : SETTLED   P2 1200.50
 * java 21 switch : FAILED    P3 (beneficiary bank timeout)
 * java 21 switch : REFUNDED  P4 300.00
 * switch on null : NullPointerException (no case null)
 * text block     :
 * {
 *   "id": "P2",
 *   "amount": 1200.50,
 *   "currency": "INR"
 * }
 * var + instanceof pattern : settled total = 1200.50
 * getFirst / getLast       : P1 / P4   (Java 8: get(0) / get(size() - 1))
 * reversed()               : [P4, P3, P2, P2, P1]
 * LinkedHashSet getLast    : P4   (Java 8: iterate to the end)
 * LinkedHashMap first/last : P1=Initiated / P4=Refunded
 * Stream.toList().add      : UnsupportedOperationException
 * Collectors.toList().add  : ok, mutable (ArrayList today, not promised)
 * ============================================================================================== */
