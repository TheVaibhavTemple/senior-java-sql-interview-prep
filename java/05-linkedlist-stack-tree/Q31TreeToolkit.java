/* ==============================================================================================
 * Q31  .  Tree traversals, BST validation, LCA
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Linked List, Stack & Tree
 * DIFFICULTY   : MEDIUM   (2 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : BFS with level-size freezing; iterative inorder with an explicit stack; bounds-based BST validation
 *
 * PROBLEM
 *   Level-order traversal grouped by level. Inorder without recursion. Validate a BST.
 *   Lowest common ancestor.
 *
 * ALGORITHM
 *   1. Level order: push the root, then loop while the queue is non-empty.
 *   2. Freeze q.size() at the top of each round - that count is exactly one level.
 *   3. Iterative inorder: dive left pushing every node, pop, visit, then move right.
 *   4. BST validation: carry a (min, max) bound down the recursion, not a per-node
 *      comparison.
 *   5. LCA: recurse both sides; the node where both sides return non-null is the ancestor.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Recursive traversal                            O(n)          O(h)      acceptable
 *       > Fine -- and they will ask for the iterative one anyway.
 *   Per-node comparison for BST                    --            --        avoid
 *       > left < node < right per node PASSES invalid trees. The classic wrong answer.
 *   BFS with a queue, freezing the level size      O(n)          O(w)      WRITE THIS
 *       > The one structure that answers half of all tree questions.
 *   Iterative inorder with an explicit stack       O(n)          O(h)      WRITE THIS
 *       > Asked directly, almost every time.
 *   Min/max bounds for BST                         O(n)          O(h)      WRITE THIS
 *       > The correct validation.
 *
 * WHY THIS ONE
 *   Freezing q.size() at the top of each loop turns a flat BFS into a list of levels --
 *   two characters of code and the entire question. For BST validation, the per-node check
 *   is the most common wrong answer in the whole DSA round: a node deep in the left
 *   subtree can be larger than the root and still pass it. Carry bounds down instead, and
 *   use long so Integer.MIN_VALUE nodes do not break it.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Draw a tree where the per-node BST check passes but the tree is not a BST.
 *   ? Why long bounds and not int?
 *   ? Zigzag level order -- what changes?
 *   ? LCA in a BST is simpler than in a binary tree -- why?
 *   ? Serialize and deserialize a tree.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Zigzag traversal
 *   - Right side view
 *   - Maximum depth
 *   - Validate BST
 *   - LCA in binary tree and BST
 *   - Serialize / deserialize
 *   - Diameter of a tree
 * ============================================================================================== */

import java.util.*;

public class Q31TreeToolkit {

    static final class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int v) { val = v; }
        TreeNode(int v, TreeNode l, TreeNode r) { val = v; left = l; right = r; }
    }

    /* ---- LEVEL ORDER : freezing q.size() is what makes it levels, not a flat list ---- */
    static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> out = new ArrayList<>();
        if (root == null) return out;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            int size = q.size();                       // THE line
            List<Integer> level = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                level.add(n.val);
                if (n.left  != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
            out.add(level);
        }
        return out;
    }

    /* ---- ITERATIVE INORDER : "do it without recursion" ---- */
    static List<Integer> inorder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode cur = root;
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) { stack.push(cur); cur = cur.left; }   // dive left
            cur = stack.pop();
            out.add(cur.val);
            cur = cur.right;
        }
        return out;
    }

    /* ---- BST VALIDATION with BOUNDS, not per-node comparison ---- */
    static boolean isBST(TreeNode n, long min, long max) {
        if (n == null) return true;
        if (n.val <= min || n.val >= max) return false;
        return isBST(n.left, min, n.val) && isBST(n.right, n.val, max);
    }

    /* ---- THE CLASSIC WRONG ANSWER, so you can see it fail ---- */
    static boolean isBstNaive(TreeNode n) {
        if (n == null) return true;
        if (n.left  != null && n.left.val  >= n.val) return false;
        if (n.right != null && n.right.val <= n.val) return false;
        return isBstNaive(n.left) && isBstNaive(n.right);
    }

    /* ---- LCA in a plain binary tree ---- */
    static TreeNode lca(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode l = lca(root.left,  p, q);
        TreeNode r = lca(root.right, p, q);
        if (l != null && r != null) return root;       // p and q diverge here
        return l != null ? l : r;
    }

    public static void main(String[] args) {
        //        3
        //      /   \
        //     9     20
        //          /  \
        //         15   7
        TreeNode root = new TreeNode(3,
                new TreeNode(9),
                new TreeNode(20, new TreeNode(15), new TreeNode(7)));
        System.out.println("levelOrder       : " + levelOrder(root));
        System.out.println("inorder (iter)   : " + inorder(root));

        TreeNode bst = new TreeNode(5, new TreeNode(3, new TreeNode(2), new TreeNode(4)),
                                        new TreeNode(8, new TreeNode(7), new TreeNode(9)));
        System.out.println("isBST valid tree : " + isBST(bst, Long.MIN_VALUE, Long.MAX_VALUE));

        // 10 / (5, 15) where 15's LEFT child is 6 -- locally fine, globally invalid
        TreeNode trap = new TreeNode(10,
                new TreeNode(5),
                new TreeNode(15, new TreeNode(6), new TreeNode(20)));
        System.out.println("isBST trap tree  : " + isBST(trap, Long.MIN_VALUE, Long.MAX_VALUE));
        System.out.println("naive check says : " + isBstNaive(trap) + "   <-- WRONG ANSWER");

        TreeNode p = bst.left, q = bst.left.right;     // 3 and 4
        System.out.println("lca(3,4)         : " + lca(bst, p, q).val);
        System.out.println("lca(3,8)         : " + lca(bst, bst.left, bst.right).val);
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * levelOrder       : [[3], [9, 20], [15, 7]]
 * inorder (iter)   : [9, 3, 15, 20, 7]
 * isBST valid tree : true
 * isBST trap tree  : false
 * naive check says : true   <-- WRONG ANSWER
 * lca(3,4)         : 3
 * lca(3,8)         : 5
 * ============================================================================================== */
