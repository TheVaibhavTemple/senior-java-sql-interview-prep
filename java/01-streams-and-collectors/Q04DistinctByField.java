/* ==============================================================================================
 * Q04  .  Distinct by a field
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
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
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Keep the LAST occurrence instead of the first -- what changes?
 *   ? Why is a stateful predicate a problem in parallel but not sequentially?
 *   ? Distinct on a composite key of two fields.
 *   ? What happens if the key extractor returns null?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Deduplicate records by ID
 *   - Latest record per key
 *   - Highest-scoring entry per group
 *   - Remove duplicate objects from a List
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
