/* ==============================================================================================
 * Q08  .  Second highest salary / second largest number
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : distinct + sort + skip (readable), then a single-pass two-variable scan (optimal)
 *
 * PROBLEM
 *   Find the second largest distinct value, and the second highest-paid employee. Then do
 *   it in a single pass.
 *
 * ALGORITHM
 *   1. Stream version: distinct() first, or duplicates of the maximum masquerade as the
 *      runner-up.
 *   2. sorted(reverseOrder()).skip(1).findFirst() returns the second distinct value.
 *   3. One-pass version: track max and second as longs seeded to Long.MIN_VALUE.
 *   4. If v > max, shift max into second and take v as the new max.
 *   5. Else if v < max and v > second, v is the new second (the v < max guard skips
 *      duplicates).
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Sort descending, take index 1                  O(n log n)    O(n)      acceptable
 *       > Forgetting distinct() is the classic failure on [99, 99, 45].
 *   TreeSet, pollLast twice                        O(n log n)    O(n)      acceptable
 *       > Handles distinctness for free.
 *   distinct().sorted().skip(1).findFirst()        O(n log n)    O(n)      WRITE THIS
 *       > What they expect first: readable and obviously correct.
 *   Single pass, two variables                     O(n)          O(1)      WRITE THIS
 *       > What they ask for second. Have both ready.
 *
 * WHY THIS ONE
 *   This is a two-part question and candidates answer only half. Write the stream one-
 *   liner, then say "that's O(n log n) -- if you want O(n) I'd track max and second in one
 *   pass", and write it. The trap in the one-pass version is duplicates: the v < max guard
 *   is what makes [99, 99, 45] return 45 and not 99.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? What does your code return for [5, 5]? For one element? For an empty list?
 *   ? Generalise to Nth highest.
 *   ? What is the SQL window-function version?
 *   ? Second highest per department.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Nth largest element
 *   - Second smallest
 *   - Kth largest (heap version)
 *   - Top 2 per group
 *   - Second most frequent element
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;

public class Q08SecondHighest {

    static final class Employee {
        private final String name; private final double salary;
        Employee(String name, double salary) { this.name = name; this.salary = salary; }
        public String getName()   { return name; }
        public double getSalary() { return salary; }
        @Override public String toString() { return name + ":" + (long) salary; }
    }

    /* ---- ANSWER 1 : the stream version they expect first ---- */
    static Optional<Integer> secondHighestStream(List<Integer> nums) {
        return nums.stream()
                .distinct()                              // without this, 99,99 breaks it
                .sorted(Comparator.reverseOrder())
                .skip(1)
                .findFirst();
    }

    static Optional<Integer> nthHighest(List<Integer> nums, int n) {
        return nums.stream().distinct()
                .sorted(Comparator.reverseOrder())
                .skip(n - 1L)
                .findFirst();
    }

    /* ---- ANSWER 2 : "can you do it without sorting?"  O(n), one pass ---- */
    static Integer secondHighestOnePass(int[] a) {
        long max = Long.MIN_VALUE, second = Long.MIN_VALUE;
        for (int v : a) {
            if (v > max)                        { second = max; max = v; }
            else if (v < max && v > second)     { second = v; }   // v<max skips duplicates
        }
        return second == Long.MIN_VALUE ? null : (int) second;
    }

    /* ---- THE BUG : forgetting distinct() ---- */
    static Optional<Integer> buggy(List<Integer> nums) {
        return nums.stream().sorted(Comparator.reverseOrder()).skip(1).findFirst();
    }

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(10, 45, 45, 12, 99, 99, 3);
        int[] arr = nums.stream().mapToInt(Integer::intValue).toArray();

        System.out.println("input          : " + nums);
        System.out.println("stream  answer : " + secondHighestStream(nums).orElse(null));
        System.out.println("onepass answer : " + secondHighestOnePass(arr));
        System.out.println("no distinct()  : " + buggy(nums).orElse(null) + "   <-- WRONG");
        System.out.println("3rd highest    : " + nthHighest(nums, 3).orElse(null));

        System.out.println("edge [5,5]     : " + secondHighestOnePass(new int[]{5, 5}));
        System.out.println("edge [7]       : " + secondHighestOnePass(new int[]{7}));
        System.out.println("edge []        : " + secondHighestOnePass(new int[]{}));

        List<Employee> emps = Arrays.asList(
                new Employee("Asha", 120_000), new Employee("Bilal", 95_000),
                new Employee("Chen", 140_000), new Employee("Divya", 102_000));
        System.out.println("2nd paid emp   : " + emps.stream()
                .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                .skip(1).findFirst().orElse(null));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * input          : [10, 45, 45, 12, 99, 99, 3]
 * stream  answer : 45
 * onepass answer : 45
 * no distinct()  : 99   <-- WRONG
 * 3rd highest    : 12
 * edge [5,5]     : null
 * edge [7]       : null
 * edge []        : null
 * 2nd paid emp   : Asha:120000
 * ============================================================================================== */
