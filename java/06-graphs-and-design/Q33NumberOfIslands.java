/* ==============================================================================================
 * Q33  .  Number of islands -- grid traversal
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Graphs & Design Data Structures
 * DIFFICULTY   : HARD     (3 of 3)
 * WEIGHTAGE    : MEDIUM **   common; shows up in most loops
 * PRIORITY     : P3   -- India mein Java backend rounds mein graph kam aata hai; aaye to pehle yahi
 * TECHNIQUE    : Grid DFS/BFS flood fill with in-place visited marking
 *
 * PROBLEM
 *   Count connected groups of '1' in a 2D grid of '1' and '0'.
 *
 * ALGORITHM
 *   1. Scan every cell of the grid.
 *   2. The first unvisited land cell starts a new island, so increment the counter.
 *   3. Flood fill from that cell, overwriting land with water as you go - that is the
 *      visited marker.
 *   4. Recurse or enqueue the four orthogonal neighbours, bounds-checking each.
 *   5. In BFS, mark a cell visited on ENQUEUE, not on dequeue, or it enters the queue
 *      repeatedly.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Union-Find                                     O(mn a)       O(mn)     acceptable
 *       > Correct, and the right call when the grid changes over time.
 *   DFS, marking visited in place                  O(mn)         O(mn)     WRITE THIS
 *       > Shortest correct answer. Sinking the island removes the visited array.
 *       > The O(mn) space is the recursion stack on an all-land grid.
 *   BFS with a queue                               O(mn)         O(mn)     WRITE THIS
 *       > Use when the grid is large -- DFS recursion can be mn deep.
 *       > Queue is about min(m,n) on an all-land grid; O(mn) is the safe worst case.
 *
 * WHY THIS ONE
 *   Every grid question is the same four lines: bounds check, visited check, mark, recurse
 *   into four directions. Marking in place -- sinking the island -- removes the extra
 *   boolean[][] and interviewers notice. State the BFS trade-off unprompted: a 1000x1000
 *   all-ones grid overflows the stack with DFS, and in BFS you must mark on ENQUEUE, not
 *   on dequeue, or the same cell enters the queue many times.
 *
 * SAMJHO
 *   Grid scan karo. Jahan '1' mile, count++ aur us poore island ko "dubo do" (sab '1' ko '0')
 *   DFS ya BFS se. Isse wahi island dobara count nahi hota aur alag visited array nahi
 *   chahiye. Bade grid par DFS recursion stack overflow kar sakta hai, tab BFS lo.
 *
 * AISE BOLO  (30-40 sec)
 *   "I scan every cell, and when I find land I count a new island and flood-fill it, turning
 *   its cells to water so they are never counted again. Each cell is touched a constant number
 *   of times, so it is O(m*n) time, and the DFS recursion can be m*n deep. On a large grid
 *   that recursion can overflow the stack, so I would use BFS with an ArrayDeque and mark
 *   cells when I enqueue them, not when I dequeue them. Sinking modifies the input, so I
 *   either confirm that is allowed or work on a copy, as this file does."
 *
 * TRAP
 *   ! BFS mein cell ko ENQUEUE karte waqt visited mark karo; dequeue par kiya to ek cell kai
 *     baar queue mein jaata hai.
 *   ! Bounds check sabse pehle, g[r][c] padhne se pehle; warna
 *     ArrayIndexOutOfBoundsException.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? When does the DFS version blow the stack, and what do you do about it?
 *     -> Bada island, jaise 1000x1000 sab land: recursion lakhon calls deep, default thread
 *        stack nahi jhel paata, StackOverflowError. Fix: BFS ya explicit stack wala DFS.
 *   ? Why mark visited on enqueue rather than on dequeue?
 *     -> Dequeue par mark kiya to ek cell ke kai neighbours use process hone se pehle hi queue
 *        mein daal dete hain. Count sahi rahega, par kaam aur memory badh jaate hain.
 *   ? Islands can connect diagonally now -- what changes?
 *     -> Sirf DIRS mein 4 ki jagah 8 directions (diagonals bhi). Baaki code same.
 *   ? The grid arrives as a stream of updates -- which approach now?
 *     -> Union-Find: har naya land cell naya set, count++; har land neighbour ke saath union
 *        safal ho to count--. Har update lagbhag O(1) amortised.
 *   ? Max area of an island instead of the count.
 *     -> Wahi traversal; DFS 1 + chaaron neighbours ka area return kare, aur max rakho. Code:
 *        maxAreaOfIsland.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Flood fill
 *   - Max area of island
 *   - Rotting oranges
 *   - Surrounded regions
 *   - Word search
 *   - Shortest path in a binary matrix
 *
 * VARIANTS     : LC 200, LC 695, LC 733, LC 994, LC 130, LC 79, LC 1091, LC 305
 * ============================================================================================== */

import java.util.*;

public class Q33NumberOfIslands {

    /* ---- DFS, marking visited IN PLACE : no extra boolean[][] ---- */
    static int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        char[][] g = copy(grid);
        int count = 0;
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[0].length; c++)
                if (g[r][c] == '1') { count++; sink(g, r, c); }
        return count;
    }

    private static void sink(char[][] g, int r, int c) {
        if (r < 0 || c < 0 || r >= g.length || c >= g[0].length || g[r][c] != '1') return;
        g[r][c] = '0';                       // the visited marker
        sink(g, r + 1, c); sink(g, r - 1, c);
        sink(g, r, c + 1); sink(g, r, c - 1);
    }

    /* ---- BFS : use this when the grid is large; DFS recursion can be rows*cols deep ---- */
    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    static int numIslandsBfs(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        char[][] g = copy(grid);
        boolean[][] seen = new boolean[g.length][g[0].length];
        Queue<int[]> q = new ArrayDeque<>();
        int count = 0;

        for (int r = 0; r < g.length; r++) {
            for (int c = 0; c < g[0].length; c++) {
                if (g[r][c] != '1' || seen[r][c]) continue;
                count++;
                seen[r][c] = true;
                q.add(new int[]{r, c});
                while (!q.isEmpty()) {
                    int[] cur = q.poll();
                    for (int[] d : DIRS) {
                        int nr = cur[0] + d[0], nc = cur[1] + d[1];
                        if (nr < 0 || nc < 0 || nr >= g.length || nc >= g[0].length) continue;
                        if (g[nr][nc] != '1' || seen[nr][nc]) continue;
                        seen[nr][nc] = true;        // mark on ENQUEUE, not on dequeue
                        q.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return count;
    }

    /* ---- same traversal, different accumulator : largest island ---- */
    static int maxAreaOfIsland(char[][] grid) {
        char[][] g = copy(grid);
        int best = 0;
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[0].length; c++)
                if (g[r][c] == '1') best = Math.max(best, area(g, r, c));
        return best;
    }

    private static int area(char[][] g, int r, int c) {
        if (r < 0 || c < 0 || r >= g.length || c >= g[0].length || g[r][c] != '1') return 0;
        g[r][c] = '0';
        return 1 + area(g, r + 1, c) + area(g, r - 1, c) + area(g, r, c + 1) + area(g, r, c - 1);
    }

    private static char[][] copy(char[][] g) {
        char[][] out = new char[g.length][];
        for (int i = 0; i < g.length; i++) out[i] = g[i].clone();
        return out;
    }

    static char[][] grid(String... rows) {
        char[][] g = new char[rows.length][];
        for (int i = 0; i < rows.length; i++) g[i] = rows[i].toCharArray();
        return g;
    }

    public static void main(String[] args) {
        char[][] g1 = grid("11110", "11010", "11000", "00000");
        System.out.println("one island  DFS : " + numIslands(g1) + "  BFS: " + numIslandsBfs(g1));

        char[][] g2 = grid("11000", "11000", "00100", "00011");
        System.out.println("three islands   : " + numIslands(g2) + "  BFS: " + numIslandsBfs(g2));
        System.out.println("max area        : " + maxAreaOfIsland(g2));

        System.out.println("all water       : " + numIslands(grid("000", "000")));
        System.out.println("single cell     : " + numIslands(grid("1")));
        System.out.println("input unchanged : " + new String(g2[0])
                + "   (we copy before sinking)");
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * one island  DFS : 1  BFS: 1
 * three islands   : 3  BFS: 3
 * max area        : 4
 * all water       : 0
 * single cell     : 1
 * input unchanged : 11000   (we copy before sinking)
 * ============================================================================================== */
