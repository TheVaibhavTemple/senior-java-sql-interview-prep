/* ==============================================================================================
 * Q52  .  Money with BigDecimal: total per currency, rounding, equals, and an exact split
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Java 8 Streams & Collectors
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every fintech round
 * PRIORITY     : P1   -- fintech profile hai; paise pe double likha to round wahin khatam
 * TECHNIQUE    : groupingBy + reducing(BigDecimal.ZERO, mapper, BigDecimal::add) into a TreeMap;
 *                an explicit RoundingMode on every setScale and divide; split in minor units
 *
 * PROBLEM
 *   From a List<Txn> (id, account, currency, BigDecimal amount), return the total per
 *   currency in currency order. Then answer what always follows: why not double, how to
 *   build a BigDecimal, why 1.0 and 1.00 are not equal, which RoundingMode, why 10 / 3
 *   throws, and how to split an amount into N parts that add back to the total exactly.
 *
 * ALGORITHM
 *   1. Keep the amount a BigDecimal from the start. Build it from a String (or a long with
 *      a scale), or with BigDecimal.valueOf(double); never with new BigDecimal(double).
 *   2. groupingBy(Txn::currency, TreeMap::new, reducing(BigDecimal.ZERO, Txn::amount,
 *      BigDecimal::add)). Collectors has no summing collector for BigDecimal.
 *   3. Compare amounts with compareTo, and test for zero with signum(); never equals.
 *   4. Round only with an explicit RoundingMode, at the currency's own scale
 *      (Currency.getDefaultFractionDigits: JPY 0, INR 2, KWD 3).
 *   5. Every divide gets a scale and a RoundingMode, or a MathContext.
 *   6. To split: move to minor units (long), base = total / n, hand out the remainder one
 *      minor unit at a time, and come back with BigDecimal.valueOf(minor, scale).
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   double amount + summingDouble                  O(n)          O(k)      avoid
 *       > 0.1 has no exact binary form. INR total comes out as 300.29999999999995.
 *   long minor units (paise) + summingLong         O(n)          O(k)      acceptable
 *       > Exact and fast for add and subtract; you carry each currency's scale yourself.
 *   BigDecimal + groupingBy + reducing             O(n)          O(k)      WRITE THIS
 *       > Exact decimal, the scale is kept, and rounding happens only where you ask.
 *
 * WHY THIS ONE
 *   For a fintech profile this is not really a stream question, it is a "do you know money"
 *   question. The collector is one line. The marks are in everything after it: the double
 *   drift, the new BigDecimal(0.1) trap, equals vs compareTo, an explicit RoundingMode, and a
 *   split whose parts add back to the total down to the last paisa.
 *
 * SAMJHO
 *   double binary mein hota hai, 0.1 exact nahi banta, isliye paisa kabhi double mein nahi.
 *   BigDecimal decimal mein exact hai: String se banao, groupingBy + reducing se jodo.
 *   Compare compareTo se, round sirf apne RoundingMode se, aur split paise (minor units)
 *   mein karo, taaki parts ka sum total ke barabar hi aaye.
 *
 * AISE BOLO  (30-40 sec)
 *   "The brute force is a double and summingDouble, but 0.1 has no exact binary form, so
 *   totals drift, and for money that is not acceptable. I keep the amount as a BigDecimal
 *   built from a String, and group with groupingBy, TreeMap::new and reducing with
 *   BigDecimal.ZERO and BigDecimal::add, which is O(n) time and O(k) space for k currencies.
 *   I compare amounts with compareTo, because equals also checks the scale, so 1.0 and 1.00
 *   are not equal. Every divide and setScale gets an explicit RoundingMode, and when I split
 *   an amount I work in minor units and spread the remainder, so the parts add back exactly."
 *
 * TRAP
 *   ! new BigDecimal(0.1) double ki galti saath le aata hai (0.1000000000000000055...).
 *     new BigDecimal("0.1") likho, ya BigDecimal.valueOf(0.1), jo Double.toString se "0.1"
 *     banata hai (section 3).
 *   ! equals scale bhi dekhta hai: 1.0 aur 1.00 equal nahi, HashSet mein dono rehte hain,
 *     TreeSet mein ek. amount.equals(BigDecimal.ZERO) bhi 0.00 pe false hai; signum() == 0.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? Why HALF_EVEN and not HALF_UP?
 *     -> HALF_UP har .5 ko upar le jata hai, to bahut saari rounding ke baad total upar
 *        khisakta hai. HALF_EVEN (banker's rounding) .5 ko even digit pe le jata hai, bias
 *        cancel hota hai: section 5 mein exact 0.560, HALF_UP 0.58, HALF_EVEN 0.56. Business
 *        ya regulator koi aur rule de to wahi follow karo.
 *   ? Store money as a long in minor units, or as a BigDecimal?
 *     -> long (paise) exact aur fast hai, add / subtract ke liye badhiya. Par currency ka
 *        scale har jagah saath rakhna padta hai, aur FX rate, interest ya percentage jaise
 *        decimal calculation ke liye BigDecimal hi chahiye. Jo bhi lo, poore system mein ek.
 *   ? Which column type in Oracle and PostgreSQL?
 *     -> Oracle mein NUMBER(p, s); DECIMAL(p, s) ya NUMERIC(p, s) likho to Oracle unhe
 *        NUMBER(p, s) bana deta hai. PostgreSQL mein NUMERIC(p, s), DECIMAL wahi type hai.
 *        Oracle ka BINARY_DOUBLE aur Postgres ka REAL / DOUBLE PRECISION nahi (binary floating
 *        point), Postgres ka money type bhi nahi (lc_monetary locale pe chalta hai). Column ke
 *        scale se zyada digits DB khud round kar deta hai, isliye Java mein hi setScale karke
 *        bhejo. JDBC mein getBigDecimal / setBigDecimal.
 *   ? Currencies have different scales. How do you handle that?
 *     -> 2 hardcode mat karo. Currency.getInstance(code).getDefaultFractionDigits() deta hai
 *        JPY 0, INR 2, KWD 3 (section 8). Rounding, split aur minor units sab isi scale se
 *        (section 7 mein JPY ka split 334, 333, 333).
 *   ? BigDecimal is immutable. What does that mean for your code?
 *     -> add, subtract, setScale naya object return karte hain, purana nahi badalta.
 *        total.add(x) likha aur assign nahi kiya to kuch nahi hua (section 9). Faayda: object
 *        thread-safe hai, share karna aur map mein value rakhna safe hai.
 *   ? Why does 10 / 3 throw, and what do you pass?
 *     -> divide(BigDecimal) exact result maangta hai; 3.333... kabhi khatam nahi hota, to
 *        ArithmeticException. divide(x, 2, RoundingMode.HALF_EVEN) ya divide(x,
 *        MathContext.DECIMAL64) do. 10 / 4 = 2.5 exact hai, wahan exception nahi (section 6).
 *   ? summingDouble gives 1000.0 for 10,000 x 0.10. So is double fine?
 *     -> Nahi. OpenJDK ka summingDouble aur DoubleStream.sum compensated (Kahan) summation
 *        karte hain, isliye lambi drift chhup jati hai; plain += loop 1000.0000000001588
 *        deta hai (section 2). Par value khud exact nahi: section 1 mein summingDouble ne hi
 *        INR ka total 300.29999999999995 diya.
 *   ? Why reducing and not reduce?
 *     -> groupingBy ke andar downstream ek Collector chahiye, aur Collectors mein sirf
 *        summingInt / Long / Double hain, to reducing(ZERO, mapper, add). Poori list ka total
 *        bina grouping: stream().map(Txn::amount).reduce(BigDecimal.ZERO, BigDecimal::add).
 *   ? How do you split 100.00 into 3 so nothing is lost? What about a refund?
 *     -> Har part pe round karke jodo to 99.99 (section 7, naive). Remainder last part mein
 *        do (33.33, 33.33, 33.34), ya paise mein base nikaal ke remainder ek-ek paisa baanto.
 *        floorDiv / floorMod ki wajah se negative (refund) pe bhi sum exact aata hai.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Total of a BigDecimal field over a whole stream (reduce with ZERO and add)
 *   - Per-account or per-day totals (same reducing, a different classifier)
 *   - Fee, tax and FX conversion with a fixed scale and RoundingMode
 *   - Splitting a bill or an EMI so the parts add back to the total
 *   - Why a HashSet and a TreeSet disagree when equals and compareTo disagree
 *
 * JAVA 21 MEIN
 *   Txn yahan record hai (Java 16). Record ka equals har field ka equals chalata hai, to
 *   amount 1.0 aur 1.00 wale do Txn equal nahi (section 4). BigDecimal.ROUND_HALF_UP jaise int
 *   constants Java 9 se deprecated hain; purane code mein dikhe to RoundingMode enum lo.
 *
 * APNI STORY SE JODO
 *   "I work in fintech and remittance, so for me money is never a double. I would keep the
 *   amount as a BigDecimal at the currency's scale, round only with an explicit RoundingMode,
 *   and compare amounts with compareTo."
 *   Note: ye sirf domain se jodta hai; koi project detail ya number mat jodo. Pooche "aapke
 *   system mein kaise tha" to [jo sach ho] bharo.
 *
 * VARIANTS     : classic -- split a bill into N shares, an EMI schedule that adds up to the
 *                principal, FX conversion with rounding, sum of BigDecimal per account
 * ============================================================================================== */

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Q52MoneyWithBigDecimal {

    record Txn(String id, String account, String currency, BigDecimal amount) {}

    static List<Txn> sample() {
        return List.of(
                new Txn("T1", "ACC-1", "INR", new BigDecimal("100.10")),
                new Txn("T2", "ACC-2", "USD", new BigDecimal("1.10")),
                new Txn("T3", "ACC-1", "INR", new BigDecimal("200.20")),
                new Txn("T4", "ACC-3", "JPY", new BigDecimal("1500")),
                new Txn("T5", "ACC-2", "USD", new BigDecimal("2.20")),
                new Txn("T6", "ACC-3", "KWD", new BigDecimal("12.345")),
                new Txn("T7", "ACC-1", "JPY", new BigDecimal("250")),
                new Txn("T8", "ACC-2", "KWD", new BigDecimal("0.005")));
    }

    /* ---- BEST FOR INTERVIEW : exact total per currency, currencies in sorted order ---- */
    static Map<String, BigDecimal> totalPerCurrency(List<Txn> txns) {
        return txns.stream()
                .collect(Collectors.groupingBy(Txn::currency, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Txn::amount, BigDecimal::add)));
    }

    /* ---- THE BUG : the same total in double ---- */
    static Map<String, Double> totalPerCurrencyDouble(List<Txn> txns) {
        return txns.stream()
                .collect(Collectors.groupingBy(Txn::currency, TreeMap::new,
                        Collectors.summingDouble(t -> t.amount().doubleValue())));
    }

    /* ---- the scale comes from the currency, never a hardcoded 2 ---- */
    static int scaleOf(String currency) {
        return Currency.getInstance(currency).getDefaultFractionDigits();
    }

    /* ---- SPLIT 1 : every part rounded down, the remainder goes into the last part ---- */
    static List<BigDecimal> splitRemainderLast(BigDecimal total, int n, int scale) {
        BigDecimal part = total.divide(BigDecimal.valueOf(n), scale, RoundingMode.DOWN);
        List<BigDecimal> parts = new ArrayList<>(Collections.nCopies(n - 1, part));
        parts.add(total.subtract(part.multiply(BigDecimal.valueOf(n - 1))));
        return parts;
    }

    /* ---- SPLIT 2 : work in minor units, spread the remainder one paisa at a time ---- */
    static List<BigDecimal> splitSpread(BigDecimal total, int n, int scale) {
        long minor = total.setScale(scale).unscaledValue().longValueExact(); // 100.005 INR throws
        long base = Math.floorDiv(minor, n);
        long rem  = Math.floorMod(minor, n);            // always 0..n-1, even for a refund
        List<BigDecimal> parts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            parts.add(BigDecimal.valueOf(i < rem ? base + 1 : base, scale));
        }
        return parts;
    }

    static BigDecimal sum(List<BigDecimal> xs) {
        return xs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static String showSplit(List<BigDecimal> parts) {
        return parts + " sum " + sum(parts);
    }

    public static void main(String[] args) {
        List<Txn> txns = sample();

        // 1. total per currency: BigDecimal vs double
        Map<String, BigDecimal> exactTotals = totalPerCurrency(txns);
        System.out.println("1 BigDecimal       : " + exactTotals);
        for (Map.Entry<String, Double> e : totalPerCurrencyDouble(txns).entrySet()) {
            BigDecimal asDecimal = BigDecimal.valueOf(e.getValue());
            boolean same = asDecimal.compareTo(exactTotals.get(e.getKey())) == 0;
            System.out.println("1 double " + e.getKey() + "       : " + e.getValue()
                    + (same ? "" : "   (WRONG)"));
        }

        // 2. why double is wrong: 0.1 has no exact binary form, and the error adds up
        double d = 0;
        BigDecimal b = BigDecimal.ZERO;
        BigDecimal tenPaise = new BigDecimal("0.10");
        for (int i = 0; i < 10_000; i++) {
            d += 0.10;
            b = b.add(tenPaise);
        }
        System.out.println("2 0.1 + 0.2        : " + (0.1 + 0.2) + "   (0.1 + 0.2 == 0.3 is "
                + (0.1 + 0.2 == 0.3) + ")");
        System.out.println("2 10,000 x 0.10    : double " + d + "   BigDecimal " + b);

        // 3. how you build it decides whether the error comes along
        System.out.println("3 new BigDecimal(0.1)     : " + new BigDecimal(0.1));
        System.out.println("3 BigDecimal.valueOf(0.1) : " + BigDecimal.valueOf(0.1));
        System.out.println("3 new BigDecimal(\"0.1\")   : " + new BigDecimal("0.1"));

        // 4. equals checks value AND scale; compareTo checks value only
        BigDecimal oneA = new BigDecimal("1.0"), oneB = new BigDecimal("1.00");
        Set<BigDecimal> hash = new HashSet<>(List.of(oneA, oneB));
        Set<BigDecimal> tree = new TreeSet<>(List.of(oneA, oneB));
        BigDecimal zero2 = new BigDecimal("0.00");
        System.out.println("4 1.0 vs 1.00      : equals " + oneA.equals(oneB)
                + "   compareTo " + oneA.compareTo(oneB));
        System.out.println("4 HashSet / TreeSet: " + hash.size() + " " + hash
                + "   " + tree.size() + " " + tree);
        System.out.println("4 is 0.00 zero?    : equals(ZERO) " + zero2.equals(BigDecimal.ZERO)
                + "   signum() == 0 " + (zero2.signum() == 0));
        System.out.println("4 record equals    : "
                + new Txn("T9", "ACC-1", "INR", oneA).equals(new Txn("T9", "ACC-1", "INR", oneB)));

        // 5. rounding mode: HALF_UP vs HALF_EVEN
        BigDecimal x = new BigDecimal("2.345");
        System.out.println("5 2.345 -> 2 places: HALF_UP " + x.setScale(2, RoundingMode.HALF_UP)
                + "   HALF_EVEN " + x.setScale(2, RoundingMode.HALF_EVEN));
        BigDecimal bill = new BigDecimal("100.25"), two = BigDecimal.valueOf(2);
        BigDecimal up = bill.divide(two, 2, RoundingMode.HALF_UP);
        BigDecimal even = bill.divide(two, 2, RoundingMode.HALF_EVEN);
        System.out.println("5 100.25 / 2       : HALF_UP " + up + " x2 = " + up.multiply(two)
                + "   HALF_EVEN " + even + " x2 = " + even.multiply(two));
        BigDecimal sumUp = BigDecimal.ZERO, sumEven = BigDecimal.ZERO, exact = BigDecimal.ZERO;
        for (String s : List.of("0.125", "0.135", "0.145", "0.155")) {
            BigDecimal v = new BigDecimal(s);
            exact = exact.add(v);
            sumUp = sumUp.add(v.setScale(2, RoundingMode.HALF_UP));
            sumEven = sumEven.add(v.setScale(2, RoundingMode.HALF_EVEN));
        }
        System.out.println("5 round 4 halves   : exact " + exact + "   HALF_UP " + sumUp
                + "   HALF_EVEN " + sumEven);

        // 6. divide without a scale or MathContext
        BigDecimal ten = BigDecimal.TEN, three = BigDecimal.valueOf(3);
        try {
            ten.divide(three);
        } catch (ArithmeticException ex) {
            System.out.println("6 10 / 3           : " + ex.getClass().getSimpleName());
            System.out.println("6   message        : " + ex.getMessage());
        }
        System.out.println("6 10 / 3 fixed     : scale 2 "
                + ten.divide(three, 2, RoundingMode.HALF_EVEN)
                + "   DECIMAL64 " + ten.divide(three, MathContext.DECIMAL64));
        System.out.println("6 10 / 4           : " + ten.divide(BigDecimal.valueOf(4))
                + " (exact, so no exception)");

        // 7. split so that the parts add back to the total exactly
        BigDecimal hundred = new BigDecimal("100.00");
        BigDecimal naive = hundred.divide(three, 2, RoundingMode.HALF_EVEN);
        System.out.println("7 naive 100.00 / 3 : " + naive + " x3 = " + naive.multiply(three)
                + "   (WRONG, a paisa lost)");
        System.out.println("7 100.00 / 3 last  : " + showSplit(splitRemainderLast(hundred, 3, 2)));
        System.out.println("7 100.00 / 6 last  : " + showSplit(splitRemainderLast(hundred, 6, 2)));
        System.out.println("7 100.00 / 6 spread: " + showSplit(splitSpread(hundred, 6, 2)));
        System.out.println("7 JPY 1000 / 3     : "
                + showSplit(splitSpread(new BigDecimal("1000"), 3, scaleOf("JPY"))));
        System.out.println("7 refund -100.00/6 : "
                + showSplit(splitSpread(hundred.negate(), 6, scaleOf("INR"))));

        // 8. the scale is a property of the currency
        System.out.println("8 currency scale   : " + List.of("JPY", "INR", "USD", "KWD").stream()
                .map(c -> c + "=" + scaleOf(c))
                .collect(Collectors.joining(" ")));

        // 9. BigDecimal is immutable: add() returns a new object
        BigDecimal total = BigDecimal.ZERO;
        total.add(BigDecimal.TEN);                       // result thrown away: the classic bug
        String ignored = total.toString();
        total = total.add(BigDecimal.TEN);
        System.out.println("9 total.add(10)    : " + ignored + " (WRONG, not assigned)"
                + "   total = total.add(10) : " + total);
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * 1 BigDecimal       : {INR=300.30, JPY=1750, KWD=12.350, USD=3.30}
 * 1 double INR       : 300.29999999999995   (WRONG)
 * 1 double JPY       : 1750.0
 * 1 double KWD       : 12.350000000000001   (WRONG)
 * 1 double USD       : 3.3000000000000003   (WRONG)
 * 2 0.1 + 0.2        : 0.30000000000000004   (0.1 + 0.2 == 0.3 is false)
 * 2 10,000 x 0.10    : double 1000.0000000001588   BigDecimal 1000.00
 * 3 new BigDecimal(0.1)     : 0.1000000000000000055511151231257827021181583404541015625
 * 3 BigDecimal.valueOf(0.1) : 0.1
 * 3 new BigDecimal("0.1")   : 0.1
 * 4 1.0 vs 1.00      : equals false   compareTo 0
 * 4 HashSet / TreeSet: 2 [1.0, 1.00]   1 [1.0]
 * 4 is 0.00 zero?    : equals(ZERO) false   signum() == 0 true
 * 4 record equals    : false
 * 5 2.345 -> 2 places: HALF_UP 2.35   HALF_EVEN 2.34
 * 5 100.25 / 2       : HALF_UP 50.13 x2 = 100.26   HALF_EVEN 50.12 x2 = 100.24
 * 5 round 4 halves   : exact 0.560   HALF_UP 0.58   HALF_EVEN 0.56
 * 6 10 / 3           : ArithmeticException
 * 6   message        : Non-terminating decimal expansion; no exact representable decimal result.
 * 6 10 / 3 fixed     : scale 2 3.33   DECIMAL64 3.333333333333333
 * 6 10 / 4           : 2.5 (exact, so no exception)
 * 7 naive 100.00 / 3 : 33.33 x3 = 99.99   (WRONG, a paisa lost)
 * 7 100.00 / 3 last  : [33.33, 33.33, 33.34] sum 100.00
 * 7 100.00 / 6 last  : [16.66, 16.66, 16.66, 16.66, 16.66, 16.70] sum 100.00
 * 7 100.00 / 6 spread: [16.67, 16.67, 16.67, 16.67, 16.66, 16.66] sum 100.00
 * 7 JPY 1000 / 3     : [334, 333, 333] sum 1000
 * 7 refund -100.00/6 : [-16.66, -16.66, -16.67, -16.67, -16.67, -16.67] sum -100.00
 * 8 currency scale   : JPY=0 INR=2 USD=2 KWD=3
 * 9 total.add(10)    : 0 (WRONG, not assigned)   total = total.add(10) : 10
 * ============================================================================================== */
