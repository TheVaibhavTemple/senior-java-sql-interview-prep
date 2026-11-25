/* ==============================================================================================
 * Q09  .  Top N employees per department
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- product aur GCC rounds mein common; collectingAndThen ka asli test
 * TECHNIQUE    : groupingBy + collectingAndThen post-processing; bounded min-heap for large groups
 *
 * PROBLEM
 *   Return the two highest-paid employees in each department, then the same as a joined
 *   name string.
 *
 * ALGORITHM
 *   1. Group by the key field with Collectors.toList() as the downstream.
 *   2. collectingAndThen receives each group's list as a finisher argument.
 *   3. Inside the finisher, sort the group descending and limit(n).
 *   4. For very large groups, replace the sort with a size-n min-heap: offer then poll when
 *      size > n.
 *   5. Heap is O(g log n) per group versus O(g log g) for the full sort.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Sort the whole list first, then group          O(n log n)    O(n)      acceptable
 *       > Works because groupingBy preserves encounter order -- subtle, so it invites
 *       > follow-ups.
 *   Group, sort each group, then limit             O(n log g)    O(n)      WRITE THIS
 *       > The idiomatic answer; collectingAndThen does the post-processing.
 *   Bounded min-heap of size N per group           O(n log N)    O(gN)     acceptable
 *       > The right answer when groups are huge. Offer it as the optimisation.
 *
 * WHY THIS ONE
 *   collectingAndThen(toList(), list -> ...) is the escape hatch for anything the built-in
 *   Collectors do not do directly. Be fluent in it, because "top N per group",
 *   "unmodifiable per group" and "join names per group" are all the same shape.
 *
 * SAMJHO
 *   Dept se group karo, phir har group ko alag se salary desc sort karke limit(n) lagao.
 *   collectingAndThen(toList(), finisher) har group ki list finisher ko deta hai, wahi sort
 *   aur limit hota hai. Group bahut bada ho to poora sort mat karo, size-n ka min-heap rakho.
 *
 * AISE BOLO  (30-40 sec)
 *   "One option is to sort the whole list by salary first and then group, but that leans on
 *   groupingBy keeping encounter order, which is subtle. I group by department and use
 *   collectingAndThen with toList, and the finisher sorts each group by salary descending and
 *   takes limit(n); that is O(n log g) time for groups of size g, and O(n) space. If a group
 *   has millions of rows, I replace the sort with a min-heap of size n, so it becomes
 *   O(g log n) per group. I would also ask how ties on the nth salary should be handled."
 *
 * TRAP
 *   ! Comparator.comparing(e -> e.getSalary()).reversed() compile nahi hota: reversed() ke
 *     saath lambda ka type Object ban jata hai. Method reference lo ya type likho.
 *   ! limit(n) ties ko chupchap kaat deta hai; sorted() stable hai, to input order jeetta hai.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Groups have a million rows each -- why is sorting each group wasteful, and what
 *     replaces it?
 *     -> Sort O(g log g) hai aur poori list memory mein banti hai, jabki chahiye sirf n. Size-n
 *        min-heap (topNByHeap) se O(g log n); heap ko custom Collector bana do to list bhi nahi.
 *   ? Ties on the Nth salary -- include all of them or cut arbitrarily?
 *     -> Pehle business se poochho. Sab chahiye to Nth employee ki salary nikalo aur salary >=
 *        usse filter karo (SQL: RANK() <= N); cut karna ho to thenComparing(name) tie-breaker.
 *   ? Return names instead of objects.
 *     -> Finisher mein limit ke baad map(Employee::getName), topNamesPerDept() dekho. String ki
 *        jagah list chahiye to joining ki jagah toList().
 *   ? Top N overall, not per group -- what is simpler?
 *     -> Grouping hata do: stream().sorted(salary desc).limit(N). Bada data ho to wahi size-N
 *        min-heap, O(n log N).
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Top 3 products per category
 *   - Latest 5 orders per customer
 *   - Leaderboard per region
 *   - Kth largest per group
 *
 * VARIANTS     : LC 185 (SQL dept top 3 salaries), LC 347 (top K frequent), LC 215 (Kth largest)
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;

public class Q09TopNPerGroup {

    static final class Employee {
        private final String name, dept; private final double salary;
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
                new Employee("Asha",  "IT",      120_000), new Employee("Bilal", "IT",  95_000),
                new Employee("Chen",  "IT",      140_000), new Employee("Dev",   "IT", 110_000),
                new Employee("Esha",  "Finance",  88_000), new Employee("Farid", "Finance", 102_000),
                new Employee("Gauri", "Finance",  99_000), new Employee("Hari",  "HR",  70_000));
    }

    /* ---- BEST FOR INTERVIEW : collectingAndThen post-processes each group ---- */
    static Map<String, List<Employee>> topNPerDept(List<Employee> emps, int n) {
        return emps.stream().collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                Collectors.collectingAndThen(Collectors.toList(),
                        list -> list.stream()
                                .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                                .limit(n)
                                .collect(Collectors.toList()))));
    }

    static Map<String, String> topNamesPerDept(List<Employee> emps, int n) {
        return emps.stream().collect(Collectors.groupingBy(Employee::getDept, TreeMap::new,
                Collectors.collectingAndThen(Collectors.toList(),
                        list -> list.stream()
                                .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                                .limit(n)
                                .map(Employee::getName)
                                .collect(Collectors.joining(", ")))));
    }

    /* ---- THE OPTIMISATION : bounded min-heap, O(g log n) instead of O(g log g) ---- */
    static List<Employee> topNByHeap(List<Employee> group, int n) {
        PriorityQueue<Employee> heap =
                new PriorityQueue<>(Comparator.comparingDouble(Employee::getSalary));
        for (Employee e : group) {
            heap.offer(e);
            if (heap.size() > n) heap.poll();       // evict the weakest
        }
        List<Employee> out = new ArrayList<>(heap);
        out.sort(Comparator.comparingDouble(Employee::getSalary).reversed());
        return out;
    }

    public static void main(String[] args) {
        List<Employee> emps = sample();
        System.out.println("top 2 per dept : " + topNPerDept(emps, 2));
        System.out.println("top 2 names    : " + topNamesPerDept(emps, 2));

        List<Employee> it = emps.stream()
                .filter(e -> e.getDept().equals("IT")).collect(Collectors.toList());
        System.out.println("heap top 2 (IT): " + topNByHeap(it, 2));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * top 2 per dept : {Finance=[Farid:102000, Gauri:99000], HR=[Hari:70000], IT=[Chen:140000, Asha:120000]}
 * top 2 names    : {Finance=Farid, Gauri, HR=Hari, IT=Chen, Asha}
 * heap top 2 (IT): [Chen:140000, Asha:120000]
 * ============================================================================================== */
