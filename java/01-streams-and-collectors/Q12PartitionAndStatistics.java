/* ==============================================================================================
 * Q12  .  Partition, and summary statistics in one pass
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * TECHNIQUE    : partitioningBy (always both keys) and summarizing collectors (one traversal, five aggregates)
 *
 * PROBLEM
 *   Split numbers into even and odd, count each side, and get count/min/avg/max/sum of
 *   salaries without four passes.
 *
 * ALGORITHM
 *   1. partitioningBy takes a Predicate and always returns both true and false keys.
 *   2. groupingBy on a boolean omits a key when no element falls on that side - the
 *      difference being tested.
 *   3. A downstream collector can be added to partitioningBy exactly like groupingBy.
 *   4. summarizingDouble yields count, min, max, sum and average in one pass.
 *   5. Above/below-average needs two passes: the average must exist before it can be
 *      compared against.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Two filtered streams, one per side             O(2n)         O(n)      avoid
 *       > Two passes for what partitioningBy does in one.
 *   groupingBy on a boolean classifier             O(n)          O(n)      acceptable
 *       > Works, but an empty side means a MISSING key, not an empty list.
 *   partitioningBy (+ downstream collector)        O(n)          O(n)      WRITE THIS
 *       > Guarantees both true and false keys exist.
 *   summarizingInt / summarizingDouble             O(n)          O(1)      WRITE THIS
 *       > One traversal for five aggregates.
 *
 * WHY THIS ONE
 *   partitioningBy versus groupingBy on a boolean is a favourite gotcha: partitioningBy
 *   always returns both keys, groupingBy only the keys that occurred. And
 *   summarizingDouble answers "min, max, average and count without four passes" --
 *   candidates almost always write four streams.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? All numbers are odd -- what does each version return for get(true)?
 *   ? Statistics per department, partitioned by above/below the company average.
 *   ? Can you partition and summarise in one collector?
 *   ? Why is there no partitioningBy with a map supplier?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Split pass / fail
 *   - Above / below average
 *   - Active vs inactive records
 *   - Min, max, average, count in one pass
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Q12PartitionAndStatistics {

    static final class Employee {
        private final String name, dept; private final double salary;
        Employee(String name, String dept, double salary) {
            this.name = name; this.dept = dept; this.salary = salary;
        }
        public String getName()   { return name; }
        public String getDept()   { return dept; }
        public double getSalary() { return salary; }
    }

    public static void main(String[] args) {
        List<Integer> nums = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());

        /* 1. partitioningBy ALWAYS returns both keys */
        Map<Boolean, List<Integer>> evenOdd = nums.stream()
                .collect(Collectors.partitioningBy(x -> x % 2 == 0));
        System.out.println("1 even         : " + evenOdd.get(true));
        System.out.println("  odd          : " + evenOdd.get(false));

        /* 2. the difference being tested: an empty side */
        List<Integer> allOdd = Arrays.asList(1, 3, 5);
        System.out.println("2 partitioning : " + allOdd.stream()
                .collect(Collectors.partitioningBy(x -> x % 2 == 0)));
        System.out.println("  groupingBy   : " + allOdd.stream()
                .collect(Collectors.groupingBy(x -> x % 2 == 0))
                + "   <-- the 'true' key is MISSING");

        /* 3. partition + downstream collector */
        System.out.println("3 counts       : " + nums.stream()
                .collect(Collectors.partitioningBy(x -> x % 2 == 0, Collectors.counting())));

        List<Employee> emps = Arrays.asList(
                new Employee("Asha",  "IT",      120_000), new Employee("Bilal", "IT",  95_000),
                new Employee("Chen",  "Finance", 140_000), new Employee("Divya", "HR",  70_000),
                new Employee("Eshan", "Finance",  88_000));

        /* 4. five aggregates in one traversal */
        DoubleSummaryStatistics stats = emps.stream()
                .collect(Collectors.summarizingDouble(Employee::getSalary));
        System.out.printf("4 stats        : n=%d min=%.0f avg=%.1f max=%.0f sum=%.0f%n",
                stats.getCount(), stats.getMin(), stats.getAverage(),
                stats.getMax(), stats.getSum());

        /* 5. above / below average -- NOTE: two passes, the average must exist first */
        double avg = stats.getAverage();
        Map<Boolean, List<String>> aboveAvg = emps.stream()
                .collect(Collectors.partitioningBy(e -> e.getSalary() >= avg,
                        Collectors.mapping(Employee::getName, Collectors.toList())));
        System.out.println("5 >= avg       : " + aboveAvg.get(true));
        System.out.println("  <  avg       : " + aboveAvg.get(false));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 1 even         : [2, 4, 6, 8, 10]
 *   odd          : [1, 3, 5, 7, 9]
 * 2 partitioning : {false=[1, 3, 5], true=[]}
 *   groupingBy   : {false=[1, 3, 5]}   <-- the 'true' key is MISSING
 * 3 counts       : {false=5, true=5}
 * 4 stats        : n=5 min=70000 avg=102600.0 max=140000 sum=513000
 * 5 >= avg       : [Asha, Chen]
 *   <  avg       : [Bilal, Divya, Eshan]
 * ============================================================================================== */
