/* ==============================================================================================
 * Q10  .  map vs flatMap
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : flatMap - one-to-many mapping followed by concatenation
 *
 * PROBLEM
 *   Flatten a List<List<Integer>>. Turn sentences into words. Collect every distinct skill
 *   across employees. Invert the relation to skill -> employees.
 *
 * ALGORITHM
 *   1. map is one-to-one; it would produce Stream<Stream<T>> here.
 *   2. flatMap maps each element to a Stream and concatenates the results into one flat
 *      stream.
 *   3. Flatten nested lists with flatMap(List::stream).
 *   4. Split text with flatMap(s -> Arrays.stream(s.split("\\s+"))).
 *   5. To invert a one-to-many relation, flatMap to (child, parent) pairs first, then
 *      groupingBy.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Nested for loops with an outer accumulator     O(n)          O(n)      acceptable
 *       > Fine, but the question is specifically about flatMap.
 *   map(), then flatten manually                   --            --        avoid
 *       > map gives you Stream<Stream<T>> -- the compile error is the whole lesson.
 *   flatMap                                        O(n)          O(n)      WRITE THIS
 *       > One operation: map each element to a stream, then concatenate.
 *
 * WHY THIS ONE
 *   Have one sentence ready: map is one-to-one, flatMap is one-to-many followed by
 *   concatenation. The pair-building case (case 4 in the code) separates people who
 *   memorised flatMap from people who understand it -- you map each element to several
 *   intermediate pairs, then group.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? When would you use mapMulti (Java 16) instead?
 *   ? Optional.map vs Optional.flatMap -- show the nesting difference.
 *   ? Does flatMap close the substreams it creates? Does it matter for Files.lines?
 *   ? Flatten a three-level nested structure.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Flatten nested collections
 *   - All words from sentences
 *   - All skills / tags across records
 *   - Invert a one-to-many relation
 *   - Cartesian product
 * ============================================================================================== */

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Q10MapVsFlatMap {

    static final class Employee {
        private final String name; private final List<String> skills;
        Employee(String name, String... skills) {
            this.name = name; this.skills = Arrays.asList(skills);
        }
        public String getName()         { return name; }
        public List<String> getSkills() { return skills; }
    }

    public static void main(String[] args) {
        /* 1. flatten nested lists */
        List<List<Integer>> nested = Arrays.asList(
                Arrays.asList(1, 2, 3), Arrays.asList(3, 4), Arrays.asList(5, 1));
        List<Integer> flat = nested.stream()
                .flatMap(List::stream).distinct().sorted().collect(Collectors.toList());
        System.out.println("1 flatten      : " + flat);

        /* map would give Stream<Stream<Integer>> - show the shape */
        Stream<Stream<Integer>> wrong = nested.stream().map(List::stream);
        System.out.println("  map gives    : Stream<Stream<Integer>> -> " + wrong.count() + " inner streams");

        /* 2. sentences -> words */
        List<String> sentences = Arrays.asList("java streams are lazy", "lazy until terminal");
        System.out.println("2 words        : " + sentences.stream()
                .flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toList()));

        List<Employee> emps = Arrays.asList(
                new Employee("Asha",  "java", "kafka", "sql"),
                new Employee("Bilal", "java", "spring"),
                new Employee("Chen",  "sql",  "python", "kafka"));

        /* 3. every distinct skill, sorted */
        System.out.println("3 all skills   : " + emps.stream()
                .flatMap(e -> e.getSkills().stream())
                .collect(Collectors.toCollection(TreeSet::new)));

        /* 4. invert the relation: flatMap to pairs, THEN group */
        Map<String, List<String>> bySkill = emps.stream()
                .flatMap(e -> e.getSkills().stream()
                        .map(sk -> new AbstractMap.SimpleEntry<>(sk, e.getName())))
                .collect(Collectors.groupingBy(Map.Entry::getKey, TreeMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        System.out.println("4 skill->who   : " + bySkill);

        /* 5. Optional.map vs Optional.flatMap */
        Optional<String> name = Optional.of("asha");
        Optional<Optional<String>> nestedOpt = name.map(n -> Optional.of(n.toUpperCase()));
        Optional<String> flatOpt            = name.flatMap(n -> Optional.of(n.toUpperCase()));
        System.out.println("5 map  nests   : " + nestedOpt);
        System.out.println("  flatMap flat : " + flatOpt);

        /* 6. unwrap a list of Optionals (Java 8 form) */
        List<Optional<String>> optionals = Arrays.asList(
                Optional.of("a"), Optional.empty(), Optional.of("c"));
        System.out.println("6 present only : " + optionals.stream()
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 1 flatten      : [1, 2, 3, 4, 5]
 *   map gives    : Stream<Stream<Integer>> -> 3 inner streams
 * 2 words        : [java, streams, are, lazy, lazy, until, terminal]
 * 3 all skills   : [java, kafka, python, spring, sql]
 * 4 skill->who   : {java=[Asha, Bilal], kafka=[Asha, Chen], python=[Chen], spring=[Bilal], sql=[Asha, Chen]}
 * 5 map  nests   : Optional[Optional[ASHA]]
 *   flatMap flat : Optional[ASHA]
 * 6 present only : [a, c]
 * ============================================================================================== */
