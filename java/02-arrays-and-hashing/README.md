# Arrays & Hashing

5 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| Q15 | Two Sum — the HashMap complement pattern | Easy | HIGH | P1 | Hash map complement lookup (one pass); two pointers when the input is sorted | [`Q15TwoSum.java`](Q15TwoSum.java) |
| Q16 | Valid anagram and character counting | Easy | HIGH | P1 | Fixed-alphabet frequency counting with a single +1/-1 array | [`Q16ValidAnagram.java`](Q16ValidAnagram.java) |
| Q19 | Missing number, single number, duplicate — the XOR trio | Medium | Medium | P2 | XOR cancellation (x^x == 0); Floyd cycle detection over array values | [`Q19XorTrio.java`](Q19XorTrio.java) |
| Q23 | Subarray sum equals K — prefix sum plus HashMap | Medium | Medium | P2 | Prefix sum plus a hash map of prefix counts | [`Q23SubarraySumEqualsK.java`](Q23SubarraySumEqualsK.java) |
| Q26 | Top K frequent elements | Medium | Medium | P2 | Frequency map + bounded min-heap; bucket-by-frequency for linear time | [`Q26TopKFrequent.java`](Q26TopKFrequent.java) |

## Is folder ko kaise padhein

Pehle Q15 aur Q16 memory se do baar type karo: "yeh pehle dekha hai kya?" wali HashMap habit
yahin banti hai. Phir Q23 (prefix sum + HashMap) aur Q26 (min-heap) type karo aur har ek ka
AISE BOLO zor se bolo. Q19 last mein: XOR ke rules, Floyd wala trick aur overflow wala trap.

## Mock round

| Interviewer | Aap |
|---|---|
| Find two numbers that add up to a target and return their indices. | I scan once with a HashMap from value to index and look up target minus the current value before I insert it. O(n) time, O(n) space. |
| Why look up before you insert? | Otherwise an element can pair with itself, for example 3 + 3 for target 6 at the same index. |
| Now count the subarrays that sum to k, with negatives allowed. | Same idea on prefix sums: a map of prefix-sum counts seeded with 0 -> 1, and at each index I add the count of sum minus k. O(n). |
| Why not a sliding window? | With negative numbers the sum is not monotonic, so there is no safe rule for when to shrink. |
| Return the k most frequent elements. | I count in a HashMap, then keep a min-heap of size k and evict the weakest, which is O(n log k). For linear time I bucket keys by frequency. |
| Why a min-heap when you want the largest? | The smallest of my current top k sits on top, so I evict it in O(log k) and the heap never holds more than k entries. |
| One number is missing from 0 to n. No extra space. | I XOR n with every index and every value; pairs cancel and the missing number is left. The sum formula also works, but it overflows an int for large n. |
| The counts are now built by many threads. What changes? | A plain HashMap is not thread-safe. I use ConcurrentHashMap.merge(key, 1, Integer::sum), which updates each key atomically without locking the whole map. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Two Sum ka time aur space? | O(n) time, O(n) space; ek pass, HashMap. |
| Sorted array pe Two Sum? | Two pointers, O(n) time, O(1) space. |
| 3Sum ka time? | Sort, anchor fix, baaki two pointers: O(n^2). |
| Anagram check sabse fast kaise? | Pehle length check, phir ek int[26] mein +1/-1: O(n). |
| int[26] kab nahi chalega? | Uppercase, spaces ya Unicode ho to; tab HashMap of counts lo. |
| Subarray sum K mein map ka seed? | (0 -> 1), taaki index 0 se shuru hone wale subarray gine jayein. |
| Java mein -7 % 5 kitna? | -2; isliye remainder ko ((sum % k) + k) % k se normalise karo. |
| Top K ke liye kaunsa heap? | Size k ka min-heap, O(n log k). |
| Top K linear time mein? | Bucket by frequency, kyunki frequency n se zyada nahi ho sakti. |
| x ^ x aur x ^ 0? | 0 aur x; isi se pairs cancel hote hain. |
| Read-only array mein duplicate, O(1) space? | Floyd cycle detection: value ko next index maano. |
| Sum formula int mein kab overflow? | n*(n+1) n = 46341 pe hi; long ya XOR lo. |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
