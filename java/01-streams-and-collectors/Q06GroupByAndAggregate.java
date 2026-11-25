/* ==============================================================================================
 * Q06  .  Group by department and aggregate
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- "per department" wale sawal har round mein; poori Collectors class yahin
 * TECHNIQUE    : Collectors.groupingBy with downstream collectors (counting, averaging, mapping, summarizing, nested groupingBy)
 *
 * PROBLEM
 *   From a List<Employee>: headcount per department, average salary per department, just
 *   the names per department, and a two-level dept -> city grouping.
 *
 * ALGORITHM
 *   1. Pick the classifier function - the field that defines a group.
 *   2. Pick the downstream collector that folds each group: counting, averagingDouble,
 *      mapping, toList.
 *   3. For a sorted result, pass a map supplier (TreeMap::new) as the middle argument.
 *   4. For several aggregates in one traversal, use summarizingDouble.
 *   5. Nest a second groupingBy as the downstream for multi-level grouping.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Loop with computeIfAbsent(...).add(...)        O(n)          O(n)      acceptable
 *       > Perfectly fine and often faster. Say it, then write the Collector.
 *   Several separate streams, one per aggregate    O(kn)         O(n)      avoid
 *       > k passes for something summarizingDouble does in one.
 *   groupingBy with a downstream collector         O(n)          O(n)      WRITE THIS
 *       > The three-argument form is the actual subject of the question.
 *
 * WHY THIS ONE
 *   This single question is a container for the entire Collectors class. They are checking
 *   whether you know the second argument of groupingBy can be another Collector: counting,
 *   mapping, summingInt, averagingDouble, summarizing, or another groupingBy. Once that
 *   clicks, every "per X" question is the same question.
 *
 * SAMJHO
 *   groupingBy ke teen hisse: kis field se group (classifier), kaunsa map (sorted chahiye to
 *   TreeMap::new), aur har group ka kya karna hai (downstream). Downstream mein counting,
 *   averagingDouble, mapping, ya ek aur groupingBy. Yeh samjha to har "per X" sawal same hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "A loop with computeIfAbsent works, but one stream per aggregate would mean k passes, so
 *   I use groupingBy. The department is the classifier, TreeMap::new gives sorted keys, and
 *   the downstream folds each group: counting, averagingDouble, mapping, or
 *   summarizingDouble for count, sum, min, average and max in one pass. That is O(n) time
 *   and O(n) space, plus a log factor on the department count for the TreeMap. One trap I
 *   watch: filtering before grouping drops departments with no match."
 *
 * TRAP
 *   ! filter() groupingBy se pehle lagaya to jis department mein koi match nahi, woh map se
 *     gayab. Department chahiye to Collectors.filtering downstream mein (empty pe avg 0.0).
 *   ! groupingBy null key pe NPE deta hai ("element cannot be mapped to a null key").
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Get departments in sorted order -- where does TreeMap::new go?
 *     -> Beech wala argument: groupingBy(classifier, TreeMap::new, downstream). Is form mein
 *        downstream dena hi padta hai, sirf list chahiye to toList().
 *   ? Return an unmodifiable Map of unmodifiable Lists.
 *     -> Andar collectingAndThen(toList(), List::copyOf) (section 7 jaisa), bahar poore
 *        groupingBy ko Collections::unmodifiableMap se wrap karo. Map.copyOf order kho deta hai.
 *   ? Average salary per department, but only for employees above 30.
 *     -> Section 6: pehle filter, phir groupingBy, par 30+ ke bina wala dept gayab. Rakhna ho to
 *        groupingBy(dept, filtering(e -> e.getAge() > 30, averagingDouble(...))).
 *   ? Which is faster on 10 million records: the loop or the collector? Why?
 *     -> Dono O(n). Loop mein lambda aur Collector ka overhead nahi, to thoda tez ho sakta hai,
 *        par JIT ke baad farak chhota hai. Andaaza mat lagao, JMH se measure karo.
 *   ? What does Collectors.mapping actually do here?
 *     -> Group mein daalne se pehle har Employee ko uske name mein badalta hai, phir toList().
 *        Group ke andar ka map(); result List<String> aata hai, List<Employee> nahi.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Count per category
 *   - Average / min / max per group
 *   - Two-level grouping
 *   - Names joined per group
 *   - Partition then count
 *
 * JAVA 21 MEIN
 *   Collectors.filtering (Java 9) filter ko downstream mein le jata hai, empty dept bhi rehta
 *   hai. Collectors.toUnmodifiableList() (Java 10) seedha immutable list deta hai, bina
 *   collectingAndThen ke.
 *
 * VARIANTS     : LC 49 (groupingBy on a sorted key); classic -- partitioningBy, joining per group
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;

public class Q06GroupByAndAggregate {

    static final class Employee {
        private final String name, dept, city;
        private final double salary;
        private final int age;
        Employee(String name, String dept, String city, double salary, int age) {
            this.name = name; this.dept = dept; this.city = city;
            this.salary = salary; this.age = age;
        }
        public String getName()   { return name; }
        public String getDept()   { return dept; }
        public String getCity()   { return city; }
        public double getSalary() { return salary; }
        public int    getAge()    { return age; }
        @Override public String toString() { return name; }
    }

    static List<Employee> sample() {
        return Arrays.asList(
                new Employee("Asha",  "IT",      "Pune",   120_000, 34),
                new Employee("Bilal", "IT",      "Indore",  95_000, 29),
                new Employee("Chen",  "IT",      "Pune",   140_000, 41),
                new Employee("Divya", "Finance", "Mumbai",  88_000, 37),
                new Employee("Eshan", "Finance", "Pune",   102_000, 45),
                new Employee("Farah", "HR",      "Indore",  70_000, 31));
    }

    public static void main(String[] args) {
        List<Employee> emps = sample();

        // 1. headcount per department
        Map<String, Long> count = emps.stream()
                .collect(Collectors.groupingBy(Employee::getDept, TreeMap::new, Collectors.counting()));
        System.out.println("1 headcount    : " + count);

        // 2. average salary per department
        Map<String, Double> avg = emps.stream()
                .collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                        Collectors.averagingDouble(Employee::getSalary)));
        System.out.println("2 avg salary   : " + avg);

        // 3. mapping = transform each element BEFORE collecting the group
        Map<String, List<String>> names = emps.stream()
                .collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                        Collectors.mapping(Employee::getName, Collectors.toList())));
        System.out.println("3 names        : " + names);

        // 4. two levels of grouping
        Map<String, Map<String, Long>> deptCity = emps.stream()
                .collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                        Collectors.groupingBy(Employee::getCity, TreeMap::new, Collectors.counting())));
        System.out.println("4 dept->city   : " + deptCity);

        // 5. five aggregates in ONE traversal
        Map<String, DoubleSummaryStatistics> stats = emps.stream()
                .collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                        Collectors.summarizingDouble(Employee::getSalary)));
        DoubleSummaryStatistics it = stats.get("IT");
        System.out.printf("5 IT stats     : n=%d min=%.0f avg=%.1f max=%.0f%n",
                it.getCount(), it.getMin(), it.getAverage(), it.getMax());

        // 6. filtered aggregate: average salary of employees over 30, per department
        Map<String, Double> avgOver30 = emps.stream()
                .filter(e -> e.getAge() > 30)
                .collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                        Collectors.averagingDouble(Employee::getSalary)));
        System.out.println("6 avg (age>30) : " + avgOver30);

        // 7. unmodifiable result
        Map<String, List<Employee>> frozen = emps.stream()
                .collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList)));
        try {
            frozen.get("HR").add(null);
        } catch (UnsupportedOperationException ex) {
            System.out.println("7 frozen groups: UnsupportedOperationException as intended");
        }
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 1 headcount    : {Finance=2, HR=1, IT=3}
 * 2 avg salary   : {Finance=95000.0, HR=70000.0, IT=118333.33333333333}
 * 3 names        : {Finance=[Divya, Eshan], HR=[Farah], IT=[Asha, Bilal, Chen]}
 * 4 dept->city   : {Finance={Mumbai=1, Pune=1}, HR={Indore=1}, IT={Indore=1, Pune=2}}
 * 5 IT stats     : n=3 min=95000 avg=118333.3 max=140000
 * 6 avg (age>30) : {Finance=95000.0, HR=70000.0, IT=130000.0}
 * 7 frozen groups: UnsupportedOperationException as intended
 * ============================================================================================== */
