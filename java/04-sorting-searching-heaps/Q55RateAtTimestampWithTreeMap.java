/* ==============================================================================================
 * Q55  .  Which FX rate applied at time t? TreeMap.floorEntry vs binary search
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Sorting, Searching & Heaps
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P2   -- "rate at time t" fintech rounds ka sawaal; saath mein TreeMap API pakki
 * TECHNIQUE    : One TreeMap per currency pair with floorEntry; upperBound - 1 on a sorted array;
 *                BigDecimal with setScale(2, HALF_EVEN)
 *
 * PROBLEM
 *   An FX feed publishes rates per currency pair, each with an effective timestamp. Answer
 *   "which rate applied at time t" for any t, handle t before the first rate, t exactly on a
 *   boundary and a timestamp published twice, then convert a transfer at the rate in effect
 *   when it was booked.
 *
 * ALGORITHM
 *   1. Keep Map<pair, TreeMap<Instant, BigDecimal>>. The TreeMap keeps each pair's rates
 *      sorted by effective time.
 *   2. Rate at t = floorEntry(t): the greatest effective time <= t. O(log n).
 *   3. floorEntry returns null when t is before the first rate: return Optional.empty and
 *      reject the transfer, never price it at zero.
 *   4. A rate published again for the same timestamp replaces the old one: TreeMap.put
 *      returns the old value, so the latest insert wins.
 *   5. Read-only table: a sorted array and upperBound(t) - 1 (Q28) give the same answer.
 *   6. Convert: amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN).
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   HashMap<Instant, rate>.get(t)                  O(1)          O(n)      avoid
 *       > Finds only exact timestamps. A transfer at 10:30 gets null.
 *   Linear scan for the last time <= t             O(n)          O(1)      avoid
 *   Sorted array + upperBound(t) - 1               O(log n)      O(n)      acceptable
 *       > Less memory, cache friendly. Insert in the middle is O(n): build once, read many.
 *   TreeMap per pair + floorEntry(t)               O(log n)      O(n)      WRITE THIS
 *       > O(log n) for both put and lookup, plus ceiling / higher / subMap for free.
 *   ConcurrentSkipListMap per pair                 O(log n) avg  O(n)      mention
 *       > Same NavigableMap API when rates change while transfers read them.
 *
 * WHY THIS ONE
 *   "Value as of time t" is a floor lookup, not an equality lookup, and that one idea solves
 *   rates, fee slabs, tax slabs and versioned config. TreeMap gives it in one call, and the
 *   NavigableMap methods answer the follow-ups (next change, validity window, a day's
 *   changes) without new code. The senior part is the edges: before the first rate, exactly
 *   on a boundary, a republished timestamp, and money in BigDecimal with explicit rounding.
 *
 * SAMJHO
 *   Har currency pair ke liye TreeMap<time, rate>. "Time t par kaunsa rate tha" matlab sabse
 *   bada effective time jo t se chhota ya barabar ho: floorEntry(t), O(log n). t pehle rate
 *   se pehle ho to null milta hai, transfer reject karo. Paisa hamesha BigDecimal mein, aur
 *   rounding saaf likho: setScale(2, HALF_EVEN).
 *
 * AISE BOLO  (30-40 sec)
 *   "A HashMap keyed by timestamp only finds exact matches, and a linear scan is O(n) per
 *   lookup. I keep one TreeMap per currency pair from effective time to rate, and the rate at
 *   time t is floorEntry(t), the greatest time at or before t, which is O(log n). If the table
 *   is built once and only read, a sorted array with upper bound minus one gives the same
 *   answer with less memory. A time before the first rate returns Optional.empty and the
 *   transfer is rejected, and I convert with BigDecimal and setScale(2, HALF_EVEN)."
 *
 * TRAP
 *   ! lowerEntry strict < hai, floorEntry <= hai. Exact boundary par lowerEntry purana rate
 *     deta hai (demo 2: 88.10 jabki 88.20 lag chuka tha). ceilingEntry future ka rate deta hai.
 *   ! Pehle rate se pehle ka t: floorEntry null. Ise zero ya pehla rate mat banao; Optional
 *     ya exception, aur transfer reject.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? What if t is before the first rate?
 *     -> floorEntry null deta hai. rateAt Optional.empty lautata hai aur convert exception
 *        phenkta hai (demo 6). Default rate ya zero se paisa galat convert hoga.
 *   ? The same timestamp is published twice. Which rate wins?
 *     -> TreeMap.put purani value replace karke use return karta hai: latest insert wins.
 *        Sorted array mein dono rehte hain; upperBound - 1 baad wala deta hai, lowerBound
 *        wala floor pehla (stale) deta hai (demo 3).
 *   ? When is a sorted array better than a TreeMap?
 *     -> Table ek baar banti hai aur reads bahut hain: array kam memory leta hai aur cache
 *        friendly hai, lookup dono mein O(log n). Beech mein insert O(n) hai, to live updates
 *        ke liye TreeMap.
 *   ? Rates change while transfers read them. What changes?
 *     -> TreeMap thread-safe nahi. ConcurrentSkipListMap lo: wahi floorEntry, bina lock ke
 *        (CAS), expected O(log n). Ya poori table immutable banao aur volatile reference swap.
 *   ? How do you find the rate's validity window, or all changes in a day?
 *     -> floorKey(t) se start, higherKey(t) se end (exclusive). Din ke changes:
 *        subMap(dayStart, true, nextDay, false). Demo 4 mein sab NavigableMap calls hain.
 *   ? How would you do this lookup in SQL?
 *     -> WHERE pair = ? AND effective_at <= ? ORDER BY effective_at DESC FETCH FIRST 1 ROW
 *        ONLY, index (pair, effective_at) ke saath. DB index se seedha ek entry padhta hai;
 *        ye B-tree par wahi floor lookup hai.
 *   ? A correction arrives after a transfer was booked. Which rate does the transfer keep?
 *     -> Jo booking par lagaya tha. Applied rate transfer ke saath save karo, baad mein dobara
 *        lookup mat karo. Audit ke liye effective_at ke saath published_at bhi rakho.
 *   ? Why HALF_EVEN and not HALF_UP?
 *     -> HALF_UP har exact .5 ko upar le jaata hai, to bahut saare amounts ka total upar
 *        jhukta hai. HALF_EVEN tie par even digit chunta hai, bias cancel hota hai. Demo 6:
 *        22071.3250 -> 22071.32 vs 22071.33. Rule business se confirm karo.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Time based key-value store
 *   - Fee slab and tax slab lookup (greatest key <= amount)
 *   - "As of date" lookups on versioned rows (SCD type 2)
 *   - Calendar booking overlap check with floor and ceiling
 *   - Range queries with subMap / headMap / tailMap
 *
 * JAVA 21 MEIN
 *   TreeMap ab SequencedMap bhi hai (JEP 431). firstEntry() / lastEntry() pehle se
 *   NavigableMap mein the; naya reversed() descendingMap() jaisa ulta view deta hai.
 *   SortedMap par putFirst / putLast hamesha UnsupportedOperationException dete hain, kyunki
 *   order key se tay hota hai.
 *
 * APNI STORY SE JODO
 *   "Most of my 8 years in Java backend have been in fintech and remittance, so for a question
 *   like this I first ask which rate applies when a transfer is booked: it is a lookup by
 *   time, not by exact key."
 *   Note: Project ki koi detail mat banao. Kaam par rate kaise milta tha, wo pata ho to
 *   [bhar do: DB query / cache / jo sach ho].
 *
 * VARIANTS     : LC 981, classic - fee slab by amount, price as of a date
 * ============================================================================================== */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

public class Q55RateAtTimestampWithTreeMap {

    record Tick(String pair, Instant at, BigDecimal rate) {}

    static Tick tick(String pair, String at, String rate) {
        return new Tick(pair, Instant.parse(at), new BigDecimal(rate));   // String, never double
    }

    static String fmt(Instant t) { return t.toString().substring(5, 16).replace('T', ' '); }

    /* ======================================================================
       A : one TreeMap per pair. Rate at t = floorEntry(t) = greatest key <= t.
       ====================================================================== */
    static final class FxRateTable {
        private final Map<String, TreeMap<Instant, BigDecimal>> byPair = new HashMap<>();

        /* returns the rate it replaced when this exact timestamp was already there */
        BigDecimal publish(Tick t) {
            return byPair.computeIfAbsent(t.pair(), p -> new TreeMap<>()).put(t.at(), t.rate());
        }

        Optional<BigDecimal> rateAt(String pair, Instant t) {
            TreeMap<Instant, BigDecimal> rates = byPair.get(pair);
            if (rates == null) return Optional.empty();
            return Optional.ofNullable(rates.floorEntry(t)).map(Map.Entry::getValue);
        }

        NavigableMap<Instant, BigDecimal> history(String pair) {
            return Collections.unmodifiableNavigableMap(
                    byPair.getOrDefault(pair, new TreeMap<>()));
        }
    }

    /* ======================================================================
       B : sorted arrays + binary search (the Q28 bounds). Build once, read many.
       Duplicates stay in the array, in feed order (List.sort is stable).
       ====================================================================== */
    static final class SortedRates {
        final Instant[] times;
        final BigDecimal[] rates;

        SortedRates(List<Tick> feed) {
            List<Tick> sorted = new ArrayList<>(feed);
            sorted.sort(Comparator.comparing(Tick::at));
            times = sorted.stream().map(Tick::at).toArray(Instant[]::new);
            rates = sorted.stream().map(Tick::rate).toArray(BigDecimal[]::new);
        }

        /* first index with times[i] > t. No t + 1, so it works for any key type */
        int upperBound(Instant t) {
            int lo = 0, hi = times.length;
            while (lo < hi) {
                int mid = lo + (hi - lo) / 2;
                if (times[mid].compareTo(t) <= 0) lo = mid + 1; else hi = mid;
            }
            return lo;
        }

        int lowerBound(Instant t) {                    // first index with times[i] >= t
            int lo = 0, hi = times.length;
            while (lo < hi) {
                int mid = lo + (hi - lo) / 2;
                if (times[mid].compareTo(t) < 0) lo = mid + 1; else hi = mid;
            }
            return lo;
        }

        BigDecimal floor(Instant t) {                  // last index with time <= t
            int i = upperBound(t) - 1;
            return i < 0 ? null : rates[i];
        }

        /* THE WRONG FLOOR : lowerBound stops at the FIRST of equal timestamps (stale) */
        BigDecimal floorViaLowerBound(Instant t) {
            int i = lowerBound(t);
            if (i < times.length && times[i].equals(t)) return rates[i];
            return i == 0 ? null : rates[i - 1];
        }

        BigDecimal linearScan(Instant t) {             // O(n): keep the last time <= t
            BigDecimal ans = null;
            for (int i = 0; i < times.length && times[i].compareTo(t) <= 0; i++) ans = rates[i];
            return ans;
        }
    }

    /* ---- the remittance part : convert at the rate in effect when the transfer was booked ---- */
    record Transfer(String id, Instant bookedAt, String pair, BigDecimal amount) {}

    static BigDecimal convert(FxRateTable table, Transfer tx) {
        BigDecimal rate = table.rateAt(tx.pair(), tx.bookedAt())
                .orElseThrow(() -> new IllegalStateException("no " + tx.pair()
                        + " rate at or before " + fmt(tx.bookedAt())));
        return tx.amount().multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
    }

    static String show(Optional<BigDecimal> r) {
        return r.map(BigDecimal::toPlainString).orElse("Optional.empty");
    }

    static String show(Map.Entry<Instant, BigDecimal> e) {
        return e == null ? "null" : fmt(e.getKey()) + "=" + e.getValue();
    }

    public static void main(String[] args) {
        List<Tick> feed = new ArrayList<>(List.of(
                tick("USD/INR", "2026-11-20T09:00:00Z", "88.10"),
                tick("USD/INR", "2026-11-20T10:00:00Z", "88.20"),
                tick("USD/INR", "2026-11-20T12:00:00Z", "88.35"),
                tick("USD/INR", "2026-11-20T15:30:00Z", "88.05"),
                tick("USD/INR", "2026-11-21T09:00:00Z", "88.40"),
                tick("GBP/INR", "2026-11-20T09:00:00Z", "111.40")));
        FxRateTable table = new FxRateTable();
        for (Tick t : feed) table.publish(t);
        String usd = "USD/INR";

        /* ---- 1 : rate at t, with the edges ---- */
        System.out.println("== 1. rate at t = floorEntry(t)");
        String[][] queries = {
                {"2026-11-20T08:59:00Z", "before the first rate"},
                {"2026-11-20T09:00:00Z", "exactly on a boundary: the new rate applies"},
                {"2026-11-20T10:30:00Z", ""},
                {"2026-11-20T23:59:00Z", ""},
                {"2026-11-22T12:00:00Z", "after the last rate: the last one still applies"}};
        for (String[] q : queries) {
            Instant t = Instant.parse(q[0]);
            String line = String.format("USD/INR %s -> %-15s %s",
                    fmt(t), show(table.rateAt(usd, t)), q[1]);
            System.out.println(line.stripTrailing());
        }
        Instant t1030 = Instant.parse("2026-11-20T10:30:00Z");
        System.out.println("GBP/INR 11-20 10:30 -> " + show(table.rateAt("GBP/INR", t1030))
                + "   (each pair has its own TreeMap)");
        System.out.println("EUR/INR 11-20 10:30 -> " + show(table.rateAt("EUR/INR", t1030)));

        /* ---- 2 : THE WRONG LOOKUPS ---- */
        System.out.println("== 2. the wrong lookups");
        NavigableMap<Instant, BigDecimal> h = table.history(usd);
        Instant t1000 = Instant.parse("2026-11-20T10:00:00Z");
        System.out.println("floorEntry(10:00)   -> " + h.floorEntry(t1000).getValue());
        System.out.println("lowerEntry(10:00)   -> " + h.lowerEntry(t1000).getValue()
                + "   <-- WRONG, strict <, misses the boundary");
        System.out.println("ceilingEntry(10:30) -> " + h.ceilingEntry(t1030).getValue()
                + "   <-- WRONG, a rate from the future");
        Map<Instant, BigDecimal> exact = new HashMap<>(h);
        System.out.println("HashMap.get(10:30)  -> " + exact.get(t1030)
                + "    <-- WRONG, only exact timestamps match");

        /* ---- 3 : the same timestamp published twice (a correction) ---- */
        System.out.println("== 3. same timestamp published twice");
        Tick correction = tick("USD/INR", "2026-11-20T10:00:00Z", "88.25");
        feed.add(correction);
        BigDecimal replaced = table.publish(correction);
        System.out.println("put(10:00, 88.25)   -> returned old " + replaced + ", entries still "
                + table.history(usd).size());
        System.out.println("TreeMap at 10:30    -> " + show(table.rateAt(usd, t1030))
                + "   (latest insert wins)");
        List<Tick> usdFeed = feed.stream().filter(t -> t.pair().equals(usd)).toList();
        SortedRates arr = new SortedRates(usdFeed);
        System.out.println("array, entries      -> " + arr.times.length + " (both 10:00 kept)");
        System.out.println("array at 10:00, upperBound - 1 -> " + arr.floor(t1000)
                + "   (the last of the equal run)");
        System.out.println("array at 10:00, lowerBound     -> " + arr.floorViaLowerBound(t1000)
                + "   <-- WRONG, the stale duplicate");

        /* ---- 4 : the NavigableMap API ---- */
        System.out.println("== 4. NavigableMap");
        h = table.history(usd);
        Instant t1200 = Instant.parse("2026-11-20T12:00:00Z");
        System.out.println("rate at 10:30 valid : from " + fmt(h.floorKey(t1030))
                + " until " + fmt(h.higherKey(t1030)) + "   (floorKey, higherKey)");
        System.out.println("ceilingEntry(12:00) : " + show(h.ceilingEntry(t1200)) + "   (>=)");
        System.out.println("higherEntry(12:00)  : " + show(h.higherEntry(t1200)) + "   (>)");
        System.out.println("lowerEntry(12:00)   : " + show(h.lowerEntry(t1200)) + "   (<)");
        Instant day = Instant.parse("2026-11-20T00:00:00Z");
        Instant nextDay = Instant.parse("2026-11-21T00:00:00Z");
        List<String> changes = h.subMap(day, true, nextDay, false).keySet().stream()
                .map(Q55RateAtTimestampWithTreeMap::fmt).toList();
        System.out.println("subMap(20 Nov)      : " + changes);
        System.out.println("first / last        : " + show(h.firstEntry()) + " / "
                + show(h.lastEntry()));
        System.out.println("headMap(12:00, excl): " + h.headMap(t1200, false).size()
                + " rates before noon");

        /* ---- 5 : three approaches, same answers ---- */
        System.out.println("== 5. TreeMap vs binary search vs linear scan");
        int checked = 0;
        boolean agree = true;
        for (Instant t = Instant.parse("2026-11-20T08:00:00Z");
             !t.isAfter(Instant.parse("2026-11-21T10:00:00Z")); t = t.plusSeconds(15 * 60)) {
            BigDecimal a = table.rateAt(usd, t).orElse(null);
            agree &= Objects.equals(a, arr.floor(t)) && Objects.equals(a, arr.linearScan(t));
            checked++;
        }
        System.out.println(checked + " queries, every 15 min, all three agree = " + agree);

        /* ---- 6 : convert a transfer at the rate in effect when it was booked ---- */
        System.out.println("== 6. convert at the booking-time rate");
        Transfer tx1 = new Transfer("TXN-1", Instant.parse("2026-11-20T10:45:00Z"), usd,
                new BigDecimal("250.10"));
        BigDecimal rate = table.rateAt(usd, tx1.bookedAt()).orElseThrow();
        BigDecimal raw = tx1.amount().multiply(rate);
        System.out.println("TXN-1 at " + fmt(tx1.bookedAt()) + " : USD " + tx1.amount() + " x "
                + rate + " = " + raw);
        System.out.println("  HALF_EVEN         : INR " + convert(table, tx1));
        System.out.println("  HALF_UP           : INR " + raw.setScale(2, RoundingMode.HALF_UP)
                + "   (ties always go up)");
        Transfer tx2 = new Transfer("TXN-2", Instant.parse("2026-11-20T08:30:00Z"), usd,
                new BigDecimal("100.00"));
        try {
            convert(table, tx2);
        } catch (IllegalStateException e) {
            System.out.println("TXN-2 rejected      : " + e.getMessage());
        }
        System.out.println("new BigDecimal(88.1): " + new BigDecimal(88.1) + "   <-- WRONG");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * == 1. rate at t = floorEntry(t)
 * USD/INR 11-20 08:59 -> Optional.empty  before the first rate
 * USD/INR 11-20 09:00 -> 88.10           exactly on a boundary: the new rate applies
 * USD/INR 11-20 10:30 -> 88.20
 * USD/INR 11-20 23:59 -> 88.05
 * USD/INR 11-22 12:00 -> 88.40           after the last rate: the last one still applies
 * GBP/INR 11-20 10:30 -> 111.40   (each pair has its own TreeMap)
 * EUR/INR 11-20 10:30 -> Optional.empty
 * == 2. the wrong lookups
 * floorEntry(10:00)   -> 88.20
 * lowerEntry(10:00)   -> 88.10   <-- WRONG, strict <, misses the boundary
 * ceilingEntry(10:30) -> 88.35   <-- WRONG, a rate from the future
 * HashMap.get(10:30)  -> null    <-- WRONG, only exact timestamps match
 * == 3. same timestamp published twice
 * put(10:00, 88.25)   -> returned old 88.20, entries still 5
 * TreeMap at 10:30    -> 88.25   (latest insert wins)
 * array, entries      -> 6 (both 10:00 kept)
 * array at 10:00, upperBound - 1 -> 88.25   (the last of the equal run)
 * array at 10:00, lowerBound     -> 88.20   <-- WRONG, the stale duplicate
 * == 4. NavigableMap
 * rate at 10:30 valid : from 11-20 10:00 until 11-20 12:00   (floorKey, higherKey)
 * ceilingEntry(12:00) : 11-20 12:00=88.35   (>=)
 * higherEntry(12:00)  : 11-20 15:30=88.05   (>)
 * lowerEntry(12:00)   : 11-20 10:00=88.25   (<)
 * subMap(20 Nov)      : [11-20 09:00, 11-20 10:00, 11-20 12:00, 11-20 15:30]
 * first / last        : 11-20 09:00=88.10 / 11-21 09:00=88.40
 * headMap(12:00, excl): 2 rates before noon
 * == 5. TreeMap vs binary search vs linear scan
 * 105 queries, every 15 min, all three agree = true
 * == 6. convert at the booking-time rate
 * TXN-1 at 11-20 10:45 : USD 250.10 x 88.25 = 22071.3250
 *   HALF_EVEN         : INR 22071.32
 *   HALF_UP           : INR 22071.33   (ties always go up)
 * TXN-2 rejected      : no USD/INR rate at or before 11-20 08:30
 * new BigDecimal(88.1): 88.099999999999994315658113919198513031005859375   <-- WRONG
 * ============================================================================================== */
