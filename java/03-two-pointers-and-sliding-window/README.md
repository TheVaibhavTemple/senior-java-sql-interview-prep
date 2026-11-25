# Two Pointers & Sliding Window

5 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| Q17 | Reverse a string, reverse words, rotate an array | Easy | HIGH | P1 | Two-pointer in-place reversal; rotation by three reversals | [`Q17ReverseAndRotate.java`](Q17ReverseAndRotate.java) |
| Q18 | Palindrome with two pointers | Easy | HIGH | P1 | Converging two pointers; expand-around-centre for the longest substring | [`Q18PalindromeFamily.java`](Q18PalindromeFamily.java) |
| Q21 | Sliding window — the template that covers a dozen problems | Medium | HIGH | P1 | Sliding window: expand right, shrink while invalid, record | [`Q21SlidingWindowTemplate.java`](Q21SlidingWindowTemplate.java) |
| Q22 | Fixed window and Kadane | Medium | HIGH | P1 | Fixed-size window slide; Kadane's extend-or-restart decision | [`Q22FixedWindowAndKadane.java`](Q22FixedWindowAndKadane.java) |
| Q24 | Dutch national flag — three-way partition | Medium | Medium | P2 | Three-way partition with low / mid / high pointers | [`Q24DutchNationalFlag.java`](Q24DutchNationalFlag.java) |

## Is folder ko kaise padhein

Pehle Q21 ka teen-step template (expand, shrink while invalid, record) memory se likho; yeh is
folder ki jaan hai. Phir Q22 (Kadane) aur Q17/Q18 (two pointers) do baar type karo, kyunki yeh
warm-up mein sabse zyada aate hain. Q24 last mein: invariant bolna aur mid wala trap yaad rakho.

## Mock round

| Interviewer | Aap |
|---|---|
| Reverse a string without StringBuilder.reverse(). | I take a char array and swap from both ends until the pointers cross. O(n) time and O(1) extra space on the array. |
| Now rotate an array right by k in O(1) space. | I normalise k with modulo n, then reverse the whole array, then the first k elements, then the rest. Three reversals, O(n). |
| Check a palindrome, ignoring punctuation and case. | Two pointers from both ends, skipping non-alphanumeric characters and comparing in lowercase. If one deletion is allowed, at the first mismatch I check both remaining ranges. |
| Longest substring without repeating characters? | A sliding window. I store the last index of each character and jump the left pointer past it when it repeats inside the window. O(n). |
| Change it to at most k distinct characters. | Same template: a map of counts, shrink while the map has more than k keys, then record the length. |
| Maximum subarray sum? | Kadane: at each index I either extend the current run or restart there, and keep the best separately. I seed with the first element so an all-negative array works. |
| Sort an array of 0s, 1s and 2s in one pass. | Three pointers, low, mid and high. On a 0 I swap with low and move both; on a 1 I move mid; on a 2 I swap with high and keep mid, because the new value is unchecked. |
| When does a sliding window not work? | When the condition is not monotonic, for example sum equals k with negative numbers. There I use prefix sums with a HashMap. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Sliding window ke teen step? | Right se expand, jab tak invalid left se shrink, phir record. |
| Sliding window kab fail hota hai? | Jab condition monotonic nahi, jaise negative numbers ke saath sum. |
| Rotate mein k ko kaise normalise karein? | ((k % n) + n) % n, aur usse pehle n == 0 check. |
| Rotate right ka trick? | Poora reverse, phir [0, k-1] reverse, phir [k, n-1] reverse. |
| Palindrome ke kitne centres? | 2n - 1: n characters aur n - 1 gaps. |
| Longest palindromic substring ka time? | Expand around centre: O(n^2) time, O(1) space. |
| Kadane ka core line? | cur = max(a[i], cur + a[i]); best alag rakho. |
| All-negative array pe Kadane? | Sabse bada element; best ko 0 se seed mat karo. |
| Minimum window ka shrink kab? | Jab tak window valid hai (missing == 0), record karo aur shrink karo. |
| Exactly K distinct ki ginti? | atMost(k) - atMost(k - 1). |
| Dutch flag mein 2 milne pe mid? | Nahi badhta; high se aaya value abhi check karna hai. |
| Move zeroes order rakhte hue? | Write pointer se non-zero aage likho, baaki jagah 0 bhar do. |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
