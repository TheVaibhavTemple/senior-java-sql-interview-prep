# Graphs & Design Data Structures

4 problems. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| Q32 | LRU cache | Hard | HIGH | P1 | Hash map for lookup + doubly linked list for recency, with head/tail sentinels | [`Q32LRUCache.java`](Q32LRUCache.java) |
| Q54 | HashMap from scratch: buckets, hash spreading, resize and treeify | Medium-Hard | HIGH | P1 | Power-of-two bucket array, hash ^ (hash >>> 16), index = hash & (capacity - 1), separate chaining, resize at load factor 0.75 with a lo/hi split | [`Q54HashMapFromScratch.java`](Q54HashMapFromScratch.java) |
| Q33 | Number of islands — grid traversal | Hard | Medium | P3 | Grid DFS/BFS flood fill with in-place visited marking | [`Q33NumberOfIslands.java`](Q33NumberOfIslands.java) |
| Q34 | Course schedule — topological sort and cycle detection | Hard | Medium | P3 | Kahn's algorithm - topological sort by in-degree, with free cycle detection | [`Q34CourseSchedule.java`](Q34CourseSchedule.java) |

## Is folder ko kaise padhein

Q32 LRU cache is folder ka P1 hai: HashMap + doubly linked list wala version do baar type karo,
aur LinkedHashMap wala shortcut bolna bhi seekho. Q33 aur Q34 India ke Java backend rounds mein
kam aate hain; SAMJHO aur TRAP padh lo, time bache to islands ka DFS type karo.

## Mock round

| Interviewer | Aap |
|---|---|
| Design an LRU cache with O(1) get and put. | A HashMap from key to node, plus a doubly linked list in recency order with head and tail sentinels. get moves the node to the front; put at capacity evicts tail.prev from both the list and the map. |
| Why a doubly linked list? | To unlink a node from the middle in O(1) I need its prev pointer. With a singly linked list, finding prev is O(n). |
| Is there a shortcut in the JDK? | LinkedHashMap with accessOrder = true, and removeEldestEntry returning size() > capacity. I mention it, but I write the full version because that is what is being tested. |
| Make it thread-safe. | The simplest correct option is one lock around every method. A ReadWriteLock does not help, because get also reorders the list and needs the write lock. At scale I would use a proven library like Caffeine instead of building my own. |
| How would you add a TTL per key? | I store expiresAt in each node and treat an expired entry as a miss on get, which is lazy expiry. A background cleanup removes keys nobody reads; Redis also combines lazy and periodic expiry. |
| Different area: jobs depend on other jobs. In what order do you run them? | That is a topological sort. I add an edge from each dependency to the job that needs it and run Kahn's algorithm from the jobs with in-degree zero. O(V + E). |
| What if the dependencies have a cycle? | Kahn's tells me for free: if fewer than V jobs get placed, the rest are waiting on each other. To show the actual cycle I would use a three-colour DFS. |
| Count the islands in a grid of 1s and 0s. | I scan every cell; on land I count one and flood-fill it to water with DFS or BFS. O(m*n). On a big grid I use BFS so recursion cannot overflow the stack. |
| How does HashMap work internally? | An array of buckets whose length is a power of two. I spread the hashCode with h ^ (h >>> 16), use hash & (capacity - 1) as the index and chain entries in a linked list; past 0.75 full, the table doubles. Since Java 8 a bucket that goes past 8 entries becomes a red-black tree if the table has at least 64 buckets. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| LRU ke do structures? | HashMap (key -> node) + doubly linked list (recency order). |
| Sentinels kya bachate hain? | Empty list, first aur last node ke null checks. |
| LinkedHashMap se LRU kaise? | Constructor mein accessOrder = true, aur removeEldestEntry override. |
| Evict karte waqt kya bhoolte hain? | Map se bhi remove karna. |
| LRU par ReadWriteLock kyun bekaar? | get bhi list badalta hai, to har call ko write lock chahiye. |
| LFU mein extra kya chahiye? | frequency -> keys map aur minFreq. |
| Number of islands ki complexity? | O(m*n) time; DFS stack worst case O(m*n). |
| BFS mein visited kab mark karein? | Enqueue par, dequeue par nahi. |
| Grid updates stream mein aayein to? | Union-Find, har update lagbhag O(1) amortised. |
| Topological sort kab possible hai? | Sirf DAG mein; cycle ho to koi valid order nahi. |
| Kahn's mein cycle kaise pata chalti hai? | Placed nodes ki ginti V se kam ho. |
| Course schedule mein edge direction? | prerequisite -> course. |
| `new HashMap<>(100)` mein 100 entries bina resize? | Nahi: capacity 128, threshold 96, 97th put par resize. `HashMap.newHashMap(100)` lo (Java 19+). |

## Run any file

```bash
java ./QXX...java
```

Every file is single-file runnable (Java 11+ source launcher) and ends with the
output it actually produced on OpenJDK 21.
