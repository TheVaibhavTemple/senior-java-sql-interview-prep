/* ==============================================================================================
 * Q53  .  Sort by several fields, one of them descending, with nulls
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : EASY-MEDIUM  (1-2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * PRIORITY     : P1   -- "dept, phir salary desc, phir name" har round mein; galti reversed() pe
 * TECHNIQUE    : Comparator chain: comparing + thenComparing, reverse one key only, nullsLast /
 *                nullsFirst for a nullable key, comparingInt to avoid boxing
 *
 * PROBLEM
 *   Sort a List<Employee> by department ascending, then salary descending, then name
 *   ascending. Then the follow-ups: where does reversed() go, what if a department or a name
 *   is null, why does the lambda version not compile, Comparable vs Comparator, sorted() vs
 *   List.sort, and is the sort stable.
 *
 * ALGORITHM
 *   1. Start the chain with the first key: Comparator.comparing(Employee::dept).
 *   2. Add every next key with thenComparing; an int key uses comparingInt (no boxing).
 *   3. Make ONE key descending by reversing only that key's comparator:
 *      thenComparing(Comparator.comparingInt(Employee::salary).reversed()), or
 *      thenComparing(Employee::salary, Comparator.reverseOrder()).
 *   4. A key that can be null gets its own key comparator:
 *      comparing(Employee::dept, Comparator.nullsLast(Comparator.naturalOrder())).
 *   5. Sort in place with list.sort(cmp), or get a new list with stream().sorted(cmp).
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Anonymous Comparator, if / else per field      O(n log n)    O(n)      acceptable
 *       > Works, but long, and every field is a fresh chance to swap a and b.
 *   Subtraction compare: a.salary() - b.salary()   O(n log n)    O(n)      avoid
 *       > int overflow flips the sign for large or negative values.
 *   Three stable sorts, least important key first  O(k n log n)  O(n)      avoid
 *       > Correct only because the sort is stable; k passes and easy to get backwards.
 *   comparing().thenComparing() chain              O(n log n)    O(n)      WRITE THIS
 *       > One comparator; each key's direction and null rule sits on that key.
 *
 * WHY THIS ONE
 *   Everyone can write comparing(dept). The marks are in the second key: reversed() reverses
 *   the whole chain built so far, so it must sit on the salary comparator alone. Then the two
 *   gotchas they love: a null field throws a NullPointerException in the middle of the sort,
 *   and Comparator.comparing(e -> e.name()).reversed() does not even compile.
 *
 * SAMJHO
 *   Comparator ek chain hai: pehle dept, tie ho to salary, phir bhi tie ho to name.
 *   reversed() ab tak ki poori chain ulti karta hai, isliye sirf salary wale comparator pe
 *   lagao. Jo key null ho sakti hai, use nullsLast(naturalOrder()) do. int key pe
 *   comparingInt, taaki har compare pe boxing na ho.
 *
 * AISE BOLO  (30-40 sec)
 *   "I could write one Comparator with an if-else per field, but that is long and easy to get
 *   wrong, so I build a chain: comparing by department, then salary descending, then name. I
 *   reverse only the salary comparator, because reversed() at the end flips the whole chain,
 *   and I use comparingInt so the salary is not boxed. The sort is O(n log n) time with O(n)
 *   extra space for TimSort, and it is stable. If the department can be null, I give that key
 *   nullsLast(naturalOrder()), otherwise the sort throws a NullPointerException halfway."
 *
 * TRAP
 *   ! reversed() chain ke end mein lagaya to dept aur name bhi ulte ho jaate hain (section 2).
 *     Sirf salary pe lagao: thenComparing(Comparator.comparingInt(Employee::salary).reversed()).
 *   ! Comparator.comparing(e -> e.name()).reversed() compile nahi hota: e ka type Object
 *     infer hota hai. Employee::name likho, ya (Employee e) -> e.name() (section 3).
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Where exactly does reversed() go, and what does it reverse?
 *     -> reversed() us comparator ko ulta karta hai jis pe call hua, yaani ab tak ki poori
 *        chain. Ek key ulti chahiye to sirf us key ke comparator pe lagao, ya
 *        thenComparing(key, Comparator.reverseOrder()) likho (section 1, dono same order).
 *   ? Why does Comparator.comparing(e -> e.name()).reversed() not compile?
 *     -> comparing(...) ke turant baad .reversed() call hai, to comparing ko bahar se target
 *        type nahi milta, aur e ka type Object ban jata hai: "cannot find symbol". Fix:
 *        Employee::name, ya (Employee e) -> ..., ya Comparator.<Employee, String>comparing.
 *        Akela list.sort(Comparator.comparing(e -> e.name())) compile ho jata hai.
 *   ? A department can be null. What happens, and how do you fix it?
 *     -> Bina rule ke compareTo pe NullPointerException (section 4). Fix: us key ko apna
 *        comparator do: comparing(Employee::dept, Comparator.nullsLast(Comparator.naturalOrder())).
 *        Poora element hi null ho sakta hai to bahar se wrap karo: Comparator.nullsLast(cmp).
 *   ? Descending, but nulls still last?
 *     -> nullsLast(reverseOrder()) likho. nullsLast(naturalOrder()).reversed() nulls ko first
 *        bhej deta hai, kyunki reversed() null wala rule bhi ulta karta hai (section 4).
 *   ? comparing vs comparingInt: why does it matter?
 *     -> comparing(Employee::salary) har compare pe int ko Integer mein box karta hai.
 *        comparingInt seedha Integer.compare chalata hai. double ke liye comparingDouble
 *        (Double.compare), long ke liye comparingLong.
 *   ? Why not (a, b) -> a.salary() - b.salary()?
 *     -> Subtraction overflow kar sakta hai: -2_000_000_000 minus 2_000_000_000 ka result
 *        positive aata hai, to sign galat. Integer.compare ya comparingInt lo.
 *   ? Comparable or Comparator?
 *     -> Comparable class ka ek natural order hai (compareTo), yahan id. sorted() aur
 *        new TreeSet<>() bina argument wahi lete hain (section 5). Comparator bahar se koi bhi
 *        order deta hai, class badle bina. Class Comparable nahi aur sorted() bina comparator
 *        chalaya to ClassCastException.
 *   ? Can a TreeSet with a salary comparator hold every employee?
 *     -> Nahi. TreeSet duplicate equals se nahi, compare se decide karta hai: compare 0 to
 *        element add hi nahi hota. 6 mein se 4 bache (section 6). Tie-break key (id) jodo.
 *   ? stream().sorted() or List.sort()?
 *     -> List.sort in-place hai, original list badalti hai; List.of ya Stream.toList() wali
 *        list pe UnsupportedOperationException (section 8). sorted() naya stream deta hai,
 *        source same rehta hai, par saare elements buffer karta hai. Dono O(n log n).
 *   ? Is the sort stable, and why would you care?
 *     -> Haan. List.sort / Collections.sort (TimSort) stable hai, aur ordered stream pe
 *        sorted() bhi. Equal key wale apna input order rakhte hain (section 7: HR mein Asha,
 *        phir Amit). Isi wajah se teen stable passes bhi sahi order dete hain.
 *   ? Case-insensitive order on the name?
 *     -> thenComparing(Employee::name, String.CASE_INSENSITIVE_ORDER). Natural String order
 *        mein A-Z saare a-z se pehle aate hain, isliye "Zoya" "amit" se pehle aata.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Sort a Map by value, then by key (Q05)
 *   - Top N per group with a comparator (Q09)
 *   - max / min by a field (maxBy, Collections.max with the same comparator)
 *   - TreeMap, TreeSet and PriorityQueue with a custom order
 *   - SQL ORDER BY dept, salary DESC, name with NULLS LAST
 *
 * JAVA 21 MEIN
 *   Stream.toList() (Java 16) sorted list deta hai par unmodifiable; uspe .sort() kiya to
 *   UnsupportedOperationException. List.reversed() (Java 21) sorted list ka ulta view deta hai,
 *   par woh poori list ulti karta hai, ek key nahi.
 *
 * VARIANTS     : LC 1636 (frequency asc, then value desc), LC 406 (height desc, then k asc),
 *                LC 937 (multi-key log sort), LC 179 (custom comparator)
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;

public class Q53SortByMultipleFieldsWithNulls {

    /* natural order (Comparable) = by id; every other order comes from a Comparator */
    record Employee(int id, String name, String dept, int salary)
            implements Comparable<Employee> {
        @Override public int compareTo(Employee o) { return Integer.compare(id, o.id); }
        @Override public String toString() { return dept + ":" + name + ":" + salary; }
    }

    static List<Employee> sample() {                 // salary in LPA, input deliberately unsorted
        return List.of(
                new Employee(4, "Ravi",  "IT",  25),
                new Employee(2, "Asha",  "HR",  18),
                new Employee(6, "Kabir", "IT",  32),
                new Employee(1, "Neha",  "IT",  25),
                new Employee(5, "Zoya",  "FIN", 20),
                new Employee(3, "Amit",  "HR",  18));
    }

    /* ---- BEST FOR INTERVIEW : dept asc, salary desc, name asc ---- */
    static final Comparator<Employee> BY_DEPT_SALARY_DESC_NAME =
            Comparator.comparing(Employee::dept)
                    .thenComparing(Comparator.comparingInt(Employee::salary).reversed())
                    .thenComparing(Employee::name);

    /* same order, with reverseOrder() as the key comparator (boxes the int salary) */
    static final Comparator<Employee> WITH_REVERSE_ORDER =
            Comparator.comparing(Employee::dept)
                    .thenComparing(Employee::salary, Comparator.reverseOrder())
                    .thenComparing(Employee::name);

    /* ---- NULL-SAFE : a null dept or a null name goes last instead of throwing ---- */
    static final Comparator<Employee> NULLS_LAST =
            Comparator.comparing(Employee::dept, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Comparator.comparingInt(Employee::salary).reversed())
                    .thenComparing(Employee::name, Comparator.nullsLast(Comparator.naturalOrder()));

    static final Comparator<Employee> NULLS_FIRST =
            Comparator.comparing(Employee::dept, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Comparator.comparingInt(Employee::salary).reversed())
                    .thenComparing(Employee::name,
                            Comparator.nullsFirst(Comparator.naturalOrder()));

    /* ---- THE BUG 1 : reversed() at the end flips EVERY key, not just salary ---- */
    static final Comparator<Employee> WRONG_REVERSED_AT_END =
            Comparator.comparing(Employee::dept)
                    .thenComparingInt(Employee::salary)
                    .thenComparing(Employee::name)
                    .reversed();

    /* ---- THE BUG 2 : reversed() in the middle flips dept too ---- */
    static final Comparator<Employee> WRONG_REVERSED_MIDWAY =
            Comparator.comparing(Employee::dept)
                    .thenComparingInt(Employee::salary)
                    .reversed()
                    .thenComparing(Employee::name);

    static List<Employee> sortedCopy(List<Employee> in, Comparator<Employee> cmp) {
        List<Employee> out = new ArrayList<>(in);
        out.sort(cmp);
        return out;
    }

    public static void main(String[] args) {
        List<Employee> emps = sample();

        // 0. input
        System.out.println("0 input           : " + emps);

        // 1. the answer, written two ways
        List<Employee> right = sortedCopy(emps, BY_DEPT_SALARY_DESC_NAME);
        System.out.println("1 right           : " + right);
        System.out.println("1 reverseOrder()  : same as line above -> "
                + right.equals(sortedCopy(emps, WITH_REVERSE_ORDER)));

        // 2. where reversed() goes
        System.out.println("2 WRONG at end    : " + sortedCopy(emps, WRONG_REVERSED_AT_END));
        System.out.println("2 WRONG midway    : " + sortedCopy(emps, WRONG_REVERSED_MIDWAY));

        // 3. the lambda gotcha. This does NOT compile: e is inferred as Object, so e.name()
        //    is "cannot find symbol":
        //        Comparator.comparing(e -> e.name()).reversed()
        //    Fix: a method reference, or give the lambda parameter a type.
        Comparator<Employee> byNameDesc = Comparator.comparing((Employee e) -> e.name()).reversed();
        System.out.println("3 name desc       : " + emps.stream().sorted(byNameDesc)
                .map(Employee::name).collect(Collectors.toList()));

        // 4. nulls: a null dept and a null name
        List<Employee> withNulls = List.of(
                new Employee(6, "Kabir", "IT", 32),
                new Employee(7, null,    "IT", 25),
                new Employee(8, "Tara",  null, 22),
                new Employee(1, "Neha",  "IT", 25));
        try {
            sortedCopy(withNulls, BY_DEPT_SALARY_DESC_NAME);
        } catch (NullPointerException ex) {
            System.out.println("4 no null rule    : " + ex.getClass().getSimpleName());
        }
        System.out.println("4 nullsLast       : " + sortedCopy(withNulls, NULLS_LAST));
        System.out.println("4 nullsFirst      : " + sortedCopy(withNulls, NULLS_FIRST));
        // descending AND nulls last: reversed() would flip the null rule as well
        List<String> depts = new ArrayList<>(Arrays.asList("HR", null, "IT"));
        depts.sort(Comparator.nullsLast(Comparator.<String>naturalOrder()).reversed());
        System.out.println("4 desc, WRONG     : nullsLast(naturalOrder()).reversed() " + depts
                + "   <-- null first");
        depts.sort(Comparator.nullsLast(Comparator.<String>reverseOrder()));
        System.out.println("4 desc, right     : nullsLast(reverseOrder())            " + depts);

        // 5. Comparable = the one natural order of the class (here: id)
        System.out.println("5 natural (by id) : " + emps.stream().sorted()
                .map(e -> e.id() + "." + e.name()).collect(Collectors.toList()));

        // 6. a TreeSet treats compare == 0 as a duplicate
        Set<Employee> bySalary = new TreeSet<>(Comparator.comparingInt(Employee::salary));
        bySalary.addAll(emps);
        Set<Employee> bySalaryThenId = new TreeSet<>(
                Comparator.comparingInt(Employee::salary).thenComparingInt(Employee::id));
        bySalaryThenId.addAll(emps);
        System.out.println("6 TreeSet salary  : keeps " + bySalary.size() + " of " + emps.size()
                + "   with id tie-break keeps " + bySalaryThenId.size() + " of " + emps.size());

        // 7. the sort is stable: equal keys keep their input order
        List<Employee> byDeptOnly = sortedCopy(emps, Comparator.comparing(Employee::dept));
        System.out.println("7 stable by dept  : " + byDeptOnly);
        // least important key first; this works ONLY because each sort is stable
        List<Employee> threePass = sortedCopy(emps, Comparator.comparing(Employee::name));
        threePass.sort(Comparator.comparingInt(Employee::salary).reversed());
        threePass.sort(Comparator.comparing(Employee::dept));
        System.out.println("7 3 stable passes : same as line 1 -> " + threePass.equals(right));

        // 8. stream sorted() vs List.sort()
        List<Employee> source = new ArrayList<>(emps);
        List<Employee> viaStream = source.stream().sorted(BY_DEPT_SALARY_DESC_NAME).toList();
        System.out.println("8 stream sorted() : source untouched " + source.equals(emps)
                + "   result same as line 1 " + viaStream.equals(right));
        try {
            emps.sort(BY_DEPT_SALARY_DESC_NAME);                  // emps came from List.of
        } catch (UnsupportedOperationException ex) {
            System.out.println("8 List.of(..).sort: " + ex.getClass().getSimpleName());
        }
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 0 input           : [IT:Ravi:25, HR:Asha:18, IT:Kabir:32, IT:Neha:25, FIN:Zoya:20, HR:Amit:18]
 * 1 right           : [FIN:Zoya:20, HR:Amit:18, HR:Asha:18, IT:Kabir:32, IT:Neha:25, IT:Ravi:25]
 * 1 reverseOrder()  : same as line above -> true
 * 2 WRONG at end    : [IT:Kabir:32, IT:Ravi:25, IT:Neha:25, HR:Asha:18, HR:Amit:18, FIN:Zoya:20]
 * 2 WRONG midway    : [IT:Kabir:32, IT:Neha:25, IT:Ravi:25, HR:Amit:18, HR:Asha:18, FIN:Zoya:20]
 * 3 name desc       : [Zoya, Ravi, Neha, Kabir, Asha, Amit]
 * 4 no null rule    : NullPointerException
 * 4 nullsLast       : [IT:Kabir:32, IT:Neha:25, IT:null:25, null:Tara:22]
 * 4 nullsFirst      : [null:Tara:22, IT:Kabir:32, IT:null:25, IT:Neha:25]
 * 4 desc, WRONG     : nullsLast(naturalOrder()).reversed() [null, IT, HR]   <-- null first
 * 4 desc, right     : nullsLast(reverseOrder())            [IT, HR, null]
 * 5 natural (by id) : [1.Neha, 2.Asha, 3.Amit, 4.Ravi, 5.Zoya, 6.Kabir]
 * 6 TreeSet salary  : keeps 4 of 6   with id tie-break keeps 6 of 6
 * 7 stable by dept  : [FIN:Zoya:20, HR:Asha:18, HR:Amit:18, IT:Ravi:25, IT:Kabir:32, IT:Neha:25]
 * 7 3 stable passes : same as line 1 -> true
 * 8 stream sorted() : source untouched true   result same as line 1 true
 * 8 List.of(..).sort: UnsupportedOperationException
 * ============================================================================================== */
