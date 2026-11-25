/* ==============================================================================================
 * Q34  .  Course schedule -- topological sort and cycle detection
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Graphs & Design Data Structures
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
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
 *   DFS with three colours                         O(V+E)        O(V)      acceptable
 *       > Use when you must REPORT the cycle, not just detect it.
 *   Kahn's BFS on in-degrees                       O(V+E)        O(V)      WRITE THIS
 *       > One loop, and the cycle check is free.
 *
 * WHY THIS ONE
 *   The elegance of Kahn's is that cycle detection is not a separate step -- if you cannot
 *   place every node, something is stuck waiting on something else, which is a cycle by
 *   definition. The two implementation traps are building the edge direction backwards
 *   (prerequisite -> course, not the reverse) and decrementing in-degree without the == 0
 *   check, which enqueues a node several times.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? How do you know there is a cycle without a separate check?
 *   ? Which direction do the edges point, and why does it matter?
 *   ? Report the actual cycle, not just its existence -- which approach?
 *   ? Alien dictionary -- restate it as this problem.
 *   ? What does the number of BFS levels tell you about parallel scheduling?
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Course schedule I and II
 *   - Alien dictionary
 *   - Task scheduling with dependencies
 *   - Detect cycle in a directed graph
 *   - Build order
 *   - Minimum height trees
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
