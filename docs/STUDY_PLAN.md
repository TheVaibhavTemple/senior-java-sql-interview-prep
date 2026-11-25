# 10 din ka plan (roz ~2 ghante)

Pehle saare **P1**, phir **P2**, aur **P3** sirf time bache to. Har file ka format
[STYLE.md](STYLE.md) mein hai, aur kaunsa question kyun rakha, wo [AUDIT.md](AUDIT.md) mein.

## Roz ka loop

| Time | Kya karna hai |
|---|---|
| 10 min | Kal wale folder ka **Rapid fire** (folder README mein) bina dekhe bolo |
| ~80 min | Har question par neeche wala loop |
| 15 min | Aaj ke folder ka **Mock round**: Interviewer wali line padho, apna jawab zor se bolo, phir table se milao |
| 5 min | Weak points ek-ek line mein likho; agle din pehle 10 minute unhi par |

**Har question ka loop:**

1. `SAMJHO` aur `APPROACHES` padho (2-3 min).
2. File band karo. Apne IDE mein khud type karo. P1 ke liye 15 minute ki limit rakho.
3. `java path/File.java` chalao aur file ke end mein `EXPECTED OUTPUT` se milao. Fark aaya to
   apne edge cases dekho, wahi asli seekh hai.
4. `AISE BOLO` zor se bolo, phone par record karo, ek baar sun lo.
5. `TRAP` aur `FOLLOW-UPS KE JAWAB` bina dekhe bolo, phir check karo.

SQL ke liye: query pehle kagaz par likho, phir `python sql/run_all.py --dry` ya kisi SQLite /
Postgres console mein chalao, aur file ke end ka `RESULTS` block milao.

## 10 din

| Din | Java | SQL | Note |
|---|---|---|---|
| 1 | Q01, Q02, Q03, Q05, Q53 | S05, S06 | Streams basics, Nth highest, duplicates |
| 2 | Q06, Q07, Q08, Q10, Q52 | S09, S23 | groupingBy, top N per group, above average, BigDecimal |
| 3 | Q15, Q16, Q17, Q18, Q21, Q22 | S01, S02, S28 | HashMap habit, sliding window, joins aur row-count puzzle |
| 4 | Q46, Q47, Q48, Q54 | S08, S21, S22 | Core Java: immutable, equals/hashCode, Java 21, HashMap internals |
| 5 | Q35, Q36, Q37, Q38, Q39 | S18, S19 | Executor, counter, singleton, odd-even, producer-consumer |
| 6 | Q41, Q44, Q45, Q56, Q49, Q51 | S17, S24 | CompletableFuture, deadlock, pool, latches, virtual threads, CHM batch story, reconciliation |
| 7 | Q28, Q29, Q32 | S10, S25, S20 | Binary search, linked list, LRU; running balance; index story |
| 8 | Q50, Q04, Q09, Q11, Q12, Q13, Q14 | S26, S27 | Retry + idempotency, P2 streams; pagination, upsert |
| 9 | Q19, Q20, Q23, Q24, Q25, Q26, Q27, Q30, Q31, Q40, Q42, Q43, Q55 | S03, S07, S11, S12, S16 | Saare P2: sirf AISE BOLO + TRAP, type nahi |
| 10 | P3 skim: Q33, Q34 | P3 skim: S04, S13, S14, S15 | Har folder ka Mock round + Rapid fire, phir weak list |

Din 1 se 8 tak saare P1 cover ho jaate hain (Java 34, SQL 19). Din 9 aur 10 revision aur P2/P3.

## Interview wale din

- Subah: un folders ka Rapid fire jo JD se milte hain (Java + SQL dono).
- Apni 3 stories ek baar zor se: reconciliation query (S20, S24), batch fix (Q51), virtual
  threads / migration (Q48, Q49).
- Coding round mein pehle grain, ties aur NULL ke baare mein poochho, phir brute force bolo,
  phir better approach. Ye sab har file ke AISE BOLO mein pehle se likha hai.

## Jo tumhe bharna hai

21 files mein `APNI STORY SE JODO` ke andar `[bracket]` hai. Wahan sirf wahi likho jo sach
hai (kaunsa data structure, kaunsi service, kaunsa column). Pata nahi ho to line hata do.
Poori list [AUDIT.md](AUDIT.md) ke "Tumhe bharna hai" section mein hai. Din 1 se pehle 20 minute
mein bhar do, kyunki interviewer story ke baad wahi detail poochta hai.

Repo public hai, isliye bracket bharte waqt bhi company, product ya client ka naam mat likho.
