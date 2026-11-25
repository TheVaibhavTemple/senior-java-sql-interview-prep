/* ==============================================================================================
 * Q18  .  Palindrome with two pointers
 * ----------------------------------------------------------------------------------------------
 * TRACK        : Two Pointers & Sliding Window
 * DIFFICULTY   : EASY     (1 of 3)
 * WEIGHTAGE    : HIGH   ***  asked in almost every round
 * TECHNIQUE    : Converging two pointers; expand-around-centre for the longest substring
 *
 * PROBLEM
 *   Is the string a palindrome ignoring case and punctuation? Then allow one deletion.
 *   Then the longest palindromic substring.
 *
 * ALGORITHM
 *   1. Start pointers at both ends.
 *   2. Advance each inward past non-alphanumeric characters.
 *   3. Compare case-insensitively; a mismatch means not a palindrome.
 *   4. One-deletion variant: at the first mismatch try skipping the left character or the
 *      right one.
 *   5. Longest substring: for each of the 2n-1 centres expand outward while characters
 *      match.
 *
 * APPROACHES  (what to compare out loud before you write a line)
 *   APPROACH                                       TIME          SPACE     VERDICT
 *   ----------------------------------------------------------------------------------------
 *   Clean the string, reverse, compare             O(n)          O(n)      acceptable
 *       > Readable; allocates two extra strings.
 *   Two pointers converging, skipping junk         O(n)          O(1)      WRITE THIS
 *       > No allocation, and it generalises to the one-deletion variant.
 *   Recursion                                      O(n)          O(n)      avoid
 *   Expand around centre                           O(n^2)        O(1)      WRITE THIS
 *       > The right answer for longest-substring. Manacher's is over-engineering here.
 *
 * WHY THIS ONE
 *   Two pointers is the pattern, not the answer. The moment they mismatch, every harder
 *   variant becomes "what do I do at the first mismatch" -- skip left, skip right, or
 *   expand. Knowing there are 2n-1 centres (n characters plus n-1 gaps) is what makes the
 *   longest-substring version click.
 *
 * FOLLOW-UPS THE INTERVIEWER ASKS NEXT
 *   ? Allow TWO deletions -- does your approach still work?
 *   ? Longest palindromic SUBSEQUENCE -- why is that a different problem?
 *   ? Palindrome check for a linked list.
 *   ? Count all palindromic substrings.
 *
 * LEARN THIS AND YOU ALSO GET
 *   - Valid palindrome II
 *   - Longest palindromic substring
 *   - Palindrome linked list
 *   - Palindrome number without strings
 *   - Count palindromic substrings
 * ============================================================================================== */

public class Q18PalindromeFamily {

    /* ---- 1. two pointers, skipping non-alphanumerics, case-insensitive ---- */
    static boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            while (i < j && !Character.isLetterOrDigit(s.charAt(i))) i++;
            while (i < j && !Character.isLetterOrDigit(s.charAt(j))) j--;
            if (Character.toLowerCase(s.charAt(i)) != Character.toLowerCase(s.charAt(j)))
                return false;
            i++; j--;
        }
        return true;
    }

    /* ---- 2. allow at most ONE deletion : branch at the first mismatch ---- */
    static boolean validPalindromeWithOneRemoval(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            if (s.charAt(i) != s.charAt(j))
                return isPlain(s, i + 1, j) || isPlain(s, i, j - 1);
            i++; j--;
        }
        return true;
    }

    static boolean isPlain(String s, int i, int j) {
        while (i < j) if (s.charAt(i++) != s.charAt(j--)) return false;
        return true;
    }

    /* ---- 3. longest palindromic substring : expand around 2n-1 centres ---- */
    static String longestPalindrome(String s) {
        if (s.isEmpty()) return s;
        int start = 0, end = 0;
        for (int i = 0; i < s.length(); i++) {
            int odd  = expand(s, i, i);          // centre ON a character
            int even = expand(s, i, i + 1);      // centre BETWEEN two characters
            int len  = Math.max(odd, even);
            if (len > end - start) {
                start = i - (len - 1) / 2;
                end   = i + len / 2;
            }
        }
        return s.substring(start, end + 1);
    }

    static int expand(String s, int l, int r) {
        while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) { l--; r++; }
        return r - l - 1;
    }

    public static void main(String[] args) {
        System.out.println("\"A man, a plan, a canal: Panama\" : "
                + isPalindrome("A man, a plan, a canal: Panama"));
        System.out.println("\"race a car\"                     : " + isPalindrome("race a car"));
        System.out.println("\"\"                               : " + isPalindrome(""));
        System.out.println("one deletion  abca                : "
                + validPalindromeWithOneRemoval("abca"));
        System.out.println("one deletion  abc                 : "
                + validPalindromeWithOneRemoval("abc"));
        System.out.println("longest in  babad                 : " + longestPalindrome("babad"));
        System.out.println("longest in  cbbd                  : " + longestPalindrome("cbbd"));
        System.out.println("longest in  forgeeksskeegfor      : "
                + longestPalindrome("forgeeksskeegfor"));
    }
}

/* ==============================================================================================
 * EXPECTED OUTPUT   (captured by actually running this file on OpenJDK 21)
 * ----------------------------------------------------------------------------------------------
 * "A man, a plan, a canal: Panama" : true
 * "race a car"                     : false
 * ""                               : true
 * one deletion  abca                : true
 * one deletion  abc                 : false
 * longest in  babad                 : aba
 * longest in  cbbd                  : bb
 * longest in  forgeeksskeegfor      : geeksskeeg
 * ============================================================================================== */
