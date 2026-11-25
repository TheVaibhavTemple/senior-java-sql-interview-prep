/* ==============================================================================================
 * Q34  .  Course schedule -- topological sort and cycle detection
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Graphs & Design Data Structures
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P3   -- backend rounds mein kam aata; topo sort ka idea design mein useful hai
 * TECHNIQUE    : Kahn's algorithm - topological sort by in-degree, with free cycle detection
 *
 * PROBLEM
 *   Given courses and prerequisite pairs, return a valid order to take them, or detect
 *   that it is impossible.
 *
 * ALGORITHM
 *   1. Build the adjacency list with edges pointing prerequisite -> course.
 *   2. Compute each node's in-degree (how many prerequisites it still waits on).
 *   3. Enqueue every node with in-degree zero.
 *   4. Pop a node, append it to the order, and decrement each successor's in-degree.
 *   5. A successor reaching zero is enqueued. If fewer than V nodes were placed, a cycle
 *      exists.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Try every permutation                          O(n!)         O(n)      avoid
 *   DFS with three colours                         O(V+E)        O(V+E)    acceptable
 *       > Use when you must REPORT the cycle, not just detect it.
 *   Kahn's BFS on in-degrees                       O(V+E)        O(V+E)    WRITE THIS
 *       > One loop, and the cycle check is free.
 *
 * WHY THIS ONE
 *   The elegance of Kahn's is that cycle detection is not a separate step -- if you cannot
 *   place every node, something is stuck waiting on something else, which is a cycle by
 *   definition. The two implementation traps are building the edge direction backwards
 *   (prerequisite -> course, not the reverse) and decrementing in-degree without the == 0
 *   check, which enqueues a node several times.
 *
 * SAMJHO
 *   Har course kitne prerequisites ka wait kar raha hai, wahi uska in-degree hai. In-degree 0
 *   walon se shuru karo; ek course hua to uske dependents ka in-degree ghatao, 0 hua to queue
 *   mein daalo. Saare courses place nahi hue to kahin cycle hai.
 *
 * AISE BOLO  (30-40 sec)
 *   "Trying every ordering is factorial, so I model it as a directed graph with an edge from
 *   each prerequisite to the course that needs it. I use Kahn's algorithm: start with every
 *   course whose in-degree is zero, and each time I take one I decrement its successors and
 *   enqueue any that reach zero, which is O(V + E) time and space. If I place fewer than V
 *   courses there is a cycle, so detection comes for free. If I must report the cycle itself,
 *   I switch to a three-colour DFS."
 *
 * TRAP
 *   ! Edge direction ulti mat banao: prerequisite -> course. Ulti banayi to order bhi ulta
 *     aata hai.
 *   ! Successor ko tabhi enqueue karo jab in-degree EXACTLY 0 ho; har decrement par daala to
 *     ek node kai baar aayega.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? How do you know there is a cycle without a separate check?
 *     -> Cycle ke nodes ek doosre ka wait karte hain, unka in-degree kabhi 0 nahi hota, to wo
 *        kabhi place nahi hote. Placed count < V matlab cycle.
 *   ? Which direction do the edges point, and why does it matter?
 *     -> prerequisite -> course, taaki in-degree = bache hue prerequisites. Ulta kiya to Kahn
 *        reverse order dega, yaani course apne prerequisite se pehle.
 *   ? Report the actual cycle, not just its existence -- which approach?
 *     -> Three-colour DFS ke saath parent array. GRAY (state 1) node par wapas pahunche to
 *        current node se parent chain peeche chalo; wahi cycle hai.
 *   ? Alien dictionary -- restate it as this problem.
 *     -> Letters nodes hain; adjacent words ka pehla alag character edge deta hai (c1 -> c2).
 *        Kahn se order. Cycle ho, ya "abc" ke baad "ab" aaye, to invalid.
 *   ? What does the number of BFS levels tell you about parallel scheduling?
 *     -> Level-wise BFS karo (queue size freeze): ek level ke courses saath mein ho sakte hain.
 *        Levels ki ginti = minimum semesters = sabse lambi dependency chain.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Course schedule I and II
 *   - Alien dictionary
 *   - Task scheduling with dependencies
 *   - Detect cycle in a directed graph
 *   - Build order
 *   - Minimum height trees
 *
 * VARIANTS     : LC 207, LC 210, LC 269, LC 310
 * ============================================================================================== */

import java.util.*;

public class Q34CourseSchedule {

    /* ======================================================================
       KAHN'S ALGORITHM.  Cycle detection is FREE: if you cannot place every
       node, something is waiting on something else, which is a cycle.
       ====================================================================== */
    static int[] findOrder(int numCourses, int[][] prereq) {    // prereq[i] = {course, needs}
        List<List<Integer>> adj = new ArrayList<>(numCourses);
        for (int i = 0; i < numCourses; i++) adj.add(new ArrayList<>());
        int[] indegree = new int[numCourses];

        for (int[] p : prereq) {
            adj.get(p[1]).add(p[0]);      // edge direction: needs -> course
            indegree[p[0]]++;
        }

        Queue<Integer> ready = new ArrayDeque<>();
        for (int i = 0; i < numCourses; i++) if (indegree[i] == 0) ready.add(i);

        int[] order = new int[numCourses];
        int idx = 0;
        while (!ready.isEmpty()) {
            int cur = ready.poll();
            order[idx++] = cur;
            for (int next : adj.get(cur))
                if (--indegree[next] == 0) ready.add(next);   // the == 0 guard matters
        }
        return idx == numCourses ? order : new int[0];        // empty == cycle
    }

    static boolean canFinish(int numCourses, int[][] prereq) {
        return numCourses == 0 || findOrder(numCourses, prereq).length == numCourses;
    }

    /* ---- DFS three-colour variant : use it when you must REPORT the cycle ---- */
    static boolean hasCycle(int node, List<List<Integer>> adj, int[] state) {
        if (state[node] == 1) return true;      // back edge into the current path
        if (state[node] == 2) return false;
        state[node] = 1;
        for (int next : adj.get(node)) if (hasCycle(next, adj, state)) return true;
        state[node] = 2;
        return false;
    }

    static boolean hasAnyCycle(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[1]).add(e[0]);
        int[] state = new int[n];
        for (int i = 0; i < n; i++) if (hasCycle(i, adj, state)) return true;
        return false;
    }

    public static void main(String[] args) {
        int[][] ok = {{1, 0}, {2, 0}, {3, 1}, {3, 2}};
        System.out.println("order (4 courses): " + Arrays.toString(findOrder(4, ok)));
        System.out.println("canFinish        : " + canFinish(4, ok));

        int[][] cyclic = {{1, 0}, {0, 1}};
        System.out.println("cyclic order     : " + Arrays.toString(findOrder(2, cyclic))
                + "   (empty == impossible)");
        System.out.println("canFinish cyclic : " + canFinish(2, cyclic));
        System.out.println("DFS sees cycle   : " + hasAnyCycle(2, cyclic));

        System.out.println("no prereqs       : " + Arrays.toString(findOrder(3, new int[0][])));
        System.out.println("self dependency  : " + canFinish(1, new int[][]{{0, 0}}));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * order (4 courses): [0, 1, 2, 3]
 * canFinish        : true
 * cyclic order     : []   (empty == impossible)
 * canFinish cyclic : false
 * DFS sees cycle   : true
 * no prereqs       : [0, 1, 2]
 * self dependency  : false
 * ============================================================================================== */
