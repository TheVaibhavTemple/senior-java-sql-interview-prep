# Arrays & Hashing

5 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Technique | File |
|---|---|---|---|---|---|
| Q15 | Two Sum — the HashMap complement pattern | Easy | HIGH | Hash map complement lookup (one pass); two pointers when the input is sorted | [`Q15TwoSum.java`](java/02-arrays-and-hashing/Q15TwoSum.java) |
| Q16 | Valid anagram and character counting | Easy | HIGH | Fixed-alphabet frequency counting with a single +1/-1 array | [`Q16ValidAnagram.java`](java/02-arrays-and-hashing/Q16ValidAnagram.java) |
| Q19 | Missing number, single number, duplicate — the XOR trio | Medium | Medium | XOR cancellation (x^x == 0); Floyd cycle detection over array values | [`Q19XorTrio.java`](java/02-arrays-and-hashing/Q19XorTrio.java) |
| Q23 | Subarray sum equals K — prefix sum plus HashMap | Medium | Medium | Prefix sum plus a hash map of prefix counts | [`Q23SubarraySumEqualsK.java`](java/02-arrays-and-hashing/Q23SubarraySumEqualsK.java) |
| Q26 | Top K frequent elements | Medium | Medium | Frequency map + bounded min-heap; bucket-by-frequency for linear time | [`Q26TopKFrequent.java`](java/02-arrays-and-hashing/Q26TopKFrequent.java) |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
