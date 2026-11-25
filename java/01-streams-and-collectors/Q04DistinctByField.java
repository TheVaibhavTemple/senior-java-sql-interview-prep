/* ==============================================================================================
 * Q04  .  Distinct by a field
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- product aur fintech rounds mein aata hai; dedupe by id roz ka kaam hai
 * TECHNIQUE    : toMap with a merge function (duplicate policy made explicit)
 *
 * PROBLEM
 *   From a List<Employee>, keep one employee per name (or per any key), preserving
 *   encounter order.
 *
 * ALGORITHM
 *   1. Collect to a Map keyed by the field you want distinct on.
 *   2. Value mapper is identity; merge function (first, second) -> first keeps the
 *      earliest.
 *   3. LinkedHashMap::new preserves encounter order.
 *   4. collectingAndThen wraps the map's values back into a List.
 *   5. Alternative: a distinctByKey(Predicate) helper backed by a ConcurrentHashMap key
 *      set.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   distinct() on the list                         O(n)          O(n)      avoid
 *       > Only works if equals/hashCode is defined on that field. Usually it is not.
 *   Override equals/hashCode on Employee           O(n)          O(n)      avoid
 *       > Changes object semantics globally to solve a local problem.
 *   toMap with a merge function                    O(n)          O(n)      WRITE THIS
 *       > No stateful lambda, correct in parallel, duplicate policy is explicit.
 *   Stateful distinctByKey() predicate             O(n)          O(n)      acceptable
 *       > Reads beautifully, but the Set lives inside the lambda.
 *
 * WHY THIS ONE
 *   toMap(key, value, mergeFn, LinkedHashMap::new) answers three questions at once: what
 *   makes two records the same, which one wins, and whether order is kept. Know
 *   distinctByKey because they often ask for it by name -- just never present it without
 *   mentioning the shared mutable Set.
 *
 * SAMJHO
 *   Jis field pe distinct chahiye, usko map ki key banao. Do record same key pe takrayein to
 *   merge function decide karta hai kaun bachega: (first, second) -> first matlab pehla.
 *   LinkedHashMap order rakhta hai, aur end mein values() ko List bana do.
 *
 * AISE BOLO  (30-40 sec)
 *   "Plain distinct() will not work, because it uses equals and hashCode of the whole object,
 *   and I do not want to override those for one query. I collect with toMap keyed on the
 *   name, with a merge function that keeps the first record and LinkedHashMap::new for
 *   order, then take the values. That is O(n) time and O(n) space, and the duplicate policy
 *   is visible in the code. A distinctByKey predicate also works, but it keeps a Set inside
 *   the lambda, so on a parallel stream which record wins is not guaranteed."
 *
 * TRAP
 *   ! Keep-last ke liye (first, second) -> second likho, par LinkedHashMap mein key ki
 *     position pehli baar wali hi rehti hai; sirf value badalti hai.
 *   ! distinctByKey mein key null aayi to ConcurrentHashMap NullPointerException deta hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Keep the LAST occurrence instead of the first -- what changes?
 *     -> Sirf merge function: (first, second) -> second. Key apni pehli occurrence ki jagah
 *        pe hi dikhegi, value last wali hogi.
 *   ? Why is a stateful predicate a problem in parallel but not sequentially?
 *     -> Sequential mein elements encounter order mein aate hain, to pehla hi jeetta hai.
 *        Parallel mein jo thread pehle add kare woh jeetta hai; har run alag ho sakta hai.
 *   ? Distinct on a composite key of two fields.
 *     -> Key extractor se record ya List.of(name, dept) return karo, equals/hashCode sahi
 *        milte hain. "name|dept" jaisa string concat se bacho, separator data mein ho sakta hai.
 *   ? What happens if the key extractor returns null?
 *     -> toMap + LinkedHashMap mein null ek normal key ban jati hai (sab null ek saath). Par
 *        distinctByKey (ConcurrentHashMap) aur groupingBy null key pe NPE dete hain.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Deduplicate records by ID
 *   - Latest record per key
 *   - Highest-scoring entry per group
 *   - Remove duplicate objects from a List
 *
 * JAVA 21 MEIN
 *   Composite key ke liye record EmpKey(String name, String dept) {} likho (records Java 16
 *   se): equals, hashCode aur toString free milte hain. Employee khud bhi record ho sakta hai.
 *
 * APNI STORY SE JODO
 *   "On my event-driven file-processing platform, Azure Service Bus duplicate detection did
 *   this at the broker: a message whose id repeated inside the detection window was dropped,
 *   so the first copy won."
 *   Note: yeh broker level ka keep-first dedup hai. Window ke bahar wala repeat nahi pakda
 *   jata, isliye consumer ko bhi idempotent rakhne ki baat karo.
 *
 * VARIANTS     : classic -- latest record per id (merge on timestamp), distinctByKey helper
 * ============================================================================================== */

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Q04DistinctByField {

    static final class Employee {
        private final String name, dept;
        private final double salary;
        Employee(String name, String dept, double salary) {
            this.name = name; this.dept = dept; this.salary = salary;
        }
        public String getName()   { return name; }
        public String getDept()   { return dept; }
        public double getSalary() { return salary; }
        @Override public String toString() { return name + "(" + dept + "," + (long) salary + ")"; }
    }

    /* ---- BEST FOR INTERVIEW : toMap makes the duplicate policy explicit ---- */
    static List<Employee> distinctByNameKeepFirst(List<Employee> emps) {
        return emps.stream().collect(Collectors.collectingAndThen(
                Collectors.toMap(
                        Employee::getName,          // distinct on
                        e -> e,                     // value
                        (first, second) -> first,   // duplicate policy
                        LinkedHashMap::new),        // encounter order
                m -> new ArrayList<>(m.values())));
    }

    static List<Employee> distinctByNameKeepHighestPaid(List<Employee> emps) {
        return emps.stream().collect(Collectors.collectingAndThen(
                Collectors.toMap(Employee::getName, e -> e,
                        (a, b) -> a.getSalary() >= b.getSalary() ? a : b,
                        LinkedHashMap::new),
                m -> new ArrayList<>(m.values())));
    }

    /* ---- THE HELPER THEY ASK FOR BY NAME (stateful: sequential only) ---- */
    static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static void main(String[] args) {
        List<Employee> emps = Arrays.asList(
                new Employee("Asha",  "IT",      90_000),
                new Employee("Bilal", "Finance", 75_000),
                new Employee("Asha",  "IT",     110_000),
                new Employee("Chen",  "IT",      95_000),
                new Employee("Bilal", "Finance", 60_000));

        System.out.println("input          : " + emps);
        System.out.println("keep first     : " + distinctByNameKeepFirst(emps));
        System.out.println("keep top payer : " + distinctByNameKeepHighestPaid(emps));
        System.out.println("distinctByKey  : " + emps.stream()
                .filter(distinctByKey(Employee::getName))
                .collect(Collectors.toList()));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * input          : [Asha(IT,90000), Bilal(Finance,75000), Asha(IT,110000), Chen(IT,95000), Bilal(Finance,60000)]
 * keep first     : [Asha(IT,90000), Bilal(Finance,75000), Chen(IT,95000)]
 * keep top payer : [Asha(IT,110000), Bilal(Finance,75000), Chen(IT,95000)]
 * distinctByKey  : [Asha(IT,90000), Bilal(Finance,75000), Chen(IT,95000)]
 * ============================================================================================== */
