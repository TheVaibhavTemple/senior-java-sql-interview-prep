/* ==============================================================================================
 * Q07  .  Highest-paid employee in each department
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : groupingBy + maxBy wrapped in collectingAndThen to unwrap the Optional
 *
 * PROBLEM
 *   Return a Map of department -> the highest-paid employee in it. Then return just the
 *   name.
 *
 * ALGORITHM
 *   1. Group by the key field.
 *   2. Downstream maxBy(Comparator) reduces each group to an Optional.
 *   3. A group produced by groupingBy is never empty, so Optional::get is safe.
 *   4. collectingAndThen applies that finisher so the map value is the object itself.
 *   5. teeing (Java 12+) gets min and max of each group in a single pass.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Group to a List, sort each list, take index 0  O(n log n)    O(n)      acceptable
 *       > Correct but does more work than needed.
 *   groupingBy + maxBy                             O(n)          O(n)      acceptable
 *       > Leaves you with Optional<Employee> as the map value -- awkward to consume.
 *   groupingBy + collectingAndThen(maxBy, Optional O(n)          O(n)      WRITE THIS
 *       > Unwraps the Optional so the value is the employee itself.
 *   toMap with BinaryOperator.maxBy                O(n)          O(n)      acceptable
 *       > Also fine; slightly less readable.
 *
 * WHY THIS ONE
 *   maxBy returns an Optional because a Collector must handle the empty case -- but a
 *   group created by groupingBy is never empty, so Optional::get is safe and you should
 *   say that sentence out loud. collectingAndThen is the idiom for "collect, then
 *   transform", and it returns in almost every follow-up.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Why is Optional::get safe here but not in general?
 *   ? Two employees tie on the highest salary -- which one do you get, and can you control
 *     it?
 *   ? Lowest and highest in one pass (Collectors.teeing).
 *   ? Second highest per department -- what changes?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Best-selling product per category
 *   - Latest order per customer
 *   - Min / max per group
 *   - Longest tenure per team
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;

public class Q07MaxPerGroup {

    static final class Employee {
        private final String name, dept;
        private final double salary;
        Employee(String name, String dept, double salary) {
            this.name = name; this.dept = dept; this.salary = salary;
        }
        public String getName()   { return name; }
        public String getDept()   { return dept; }
        public double getSalary() { return salary; }
        @Override public String toString() { return name + ":" + (long) salary; }
    }

    static List<Employee> sample() {
        return Arrays.asList(
                new Employee("Asha",  "IT",      120_000),
                new Employee("Bilal", "IT",       95_000),
                new Employee("Chen",  "IT",      140_000),
                new Employee("Divya", "Finance",  88_000),
                new Employee("Eshan", "Finance", 102_000),
                new Employee("Farah", "HR",       70_000));
    }

    /* ---- BEST FOR INTERVIEW : collectingAndThen unwraps maxBy's Optional ---- */
    static Map<String, Employee> topPaidPerDept(List<Employee> emps) {
        return emps.stream().collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary)),
                        Optional::get)));   // safe: a groupingBy group is never empty
    }

    static Map<String, String> topPaidNamePerDept(List<Employee> emps) {
        return emps.stream().collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary)),
                        o -> o.map(Employee::getName).orElse("-"))));
    }

    /* ---- Java 12+ : min AND max of each group in one pass ---- */
    static Map<String, String> salaryRangePerDept(List<Employee> emps) {
        return emps.stream().collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                Collectors.teeing(
                        Collectors.minBy(Comparator.comparingDouble(Employee::getSalary)),
                        Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary)),
                        (lo, hi) -> (long) lo.get().getSalary() + ".." + (long) hi.get().getSalary())));
    }

    public static void main(String[] args) {
        List<Employee> emps = sample();
        System.out.println("top per dept   : " + topPaidPerDept(emps));
        System.out.println("name per dept  : " + topPaidNamePerDept(emps));
        System.out.println("range per dept : " + salaryRangePerDept(emps));
        System.out.println("highest overall: " + emps.stream()
                .max(Comparator.comparingDouble(Employee::getSalary)).orElse(null));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * top per dept   : {Finance=Eshan:102000, HR=Farah:70000, IT=Chen:140000}
 * name per dept  : {Finance=Eshan, HR=Farah, IT=Chen}
 * range per dept : {Finance=88000..102000, HR=70000..70000, IT=95000..140000}
 * highest overall: Chen:140000
 * ============================================================================================== */
