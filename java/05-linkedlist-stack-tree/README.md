# Linked List, Stack & Tree

3 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| Q29 | Linked list: reverse, cycle, middle | Medium | HIGH | P1 | Three-pointer reversal; Floyd slow/fast traversal | [`Q29LinkedListToolkit.java`](Q29LinkedListToolkit.java) |
| Q30 | Stacks: parentheses, min stack, monotonic stack | Medium | Medium | P2 | Stack of expected closers; value+min pairs; monotonic stack | [`Q30StackPatterns.java`](Q30StackPatterns.java) |
| Q31 | Tree traversals, BST validation, LCA | Medium | HIGH | P2 | BFS with level-size freezing; iterative inorder with an explicit stack; bounds-based BST validation | [`Q31TreeToolkit.java`](Q31TreeToolkit.java) |

## Is folder ko kaise padhein

Q29 sabse pehle: reverse aur Floyd cycle bina dekhe do baar type karo. Phir Q30 mein balanced
brackets (ArrayDeque ke saath) aur Q31 mein level order + validate BST. Monotonic stack aur LCA
ke liye SAMJHO aur TRAP kaafi hain.

## Mock round

| Interviewer | Aap |
|---|---|
| Reverse a singly linked list. | I use three pointers, prev, cur and next. I save next, point cur.next at prev, then move both forward. O(n) time, O(1) space, and prev is the new head. |
| Can you do it recursively? Any concern? | Yes, but the recursion is n deep, so a long list can throw StackOverflowError. In production code I prefer the iterative one. |
| How do you detect a cycle? | Floyd's slow and fast pointers. If they meet there is a cycle; if fast reaches null there is none. |
| And where does the cycle start? | After they meet, I restart one pointer at the head and move both one step at a time. They meet at the entry, because the distance from the head to the entry equals the distance from the meeting point to the entry, modulo the cycle length. |
| Validate a string of brackets. | I use a stack: for each opener I push the closer I expect, and on each closer I pop and compare. At the end the stack must be empty. |
| Which stack class do you use, and why? | ArrayDeque. java.util.Stack extends Vector, so every call is synchronized for no reason, and its own Javadoc recommends Deque. |
| Check whether a binary tree is a valid BST. | I pass min and max bounds down the recursion, as longs. Comparing a node only with its children passes invalid trees. O(n) time, O(h) stack. |
| Print the tree level by level. | BFS with a queue, and I freeze q.size() at the start of each round so each round is one level. O(n) time, O(w) space. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Reverse linked list mein pehla step? | next = cur.next save karo, phir link ulta karo. |
| Middle node kaise nikalein? | slow 1 step, fast 2 step; fast end par to slow middle par. |
| Cycle entry kaise milti hai? | Meet ke baad ek pointer head par, dono 1-1 step; jahan milein wahi entry. |
| Nth from end ek pass mein? | Do pointers mein n ka gap; dummy node head delete wala case sambhalta hai. |
| Dummy node kyun? | Head badalne wale case ka alag if hat jaata hai. |
| Java mein stack ke liye kya use karein? | ArrayDeque (Deque interface); java.util.Stack nahi. |
| ArrayDeque mein null daal sakte hain? | Nahi, NullPointerException aata hai. |
| Min stack mein getMin O(1) kaise? | Har entry ke saath min-so-far bhi rakho. |
| Monotonic stack mein kya push hota hai? | Index, value nahi; jinka next greater abhi pata nahi. |
| BST ka inorder kya deta hai? | Sorted order. |
| BFS mein levels alag kaise? | Har round ke start par q.size() freeze karo. |
| BST validate mein long bounds kyun? | Integer.MIN_VALUE / MAX_VALUE wale nodes ke liye bounds int range ke bahar chahiye. |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
