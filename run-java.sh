#!/usr/bin/env bash
# Compile and run every Java file, and diff the result against the EXPECTED OUTPUT
# block recorded in that file. Exit code 0 means every file still produces what it claims.
set -uo pipefail
cd "$(dirname "$0")"

pass=0; fail=0; failed=()
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT

for f in java/*/*.java; do
  cls=$(basename "$f" .java)
  out="$work/$cls"; mkdir -p "$out"

  if ! javac -nowarn -d "$out" "$f" > "$work/err" 2>&1; then
    echo "COMPILE FAIL  $f"; sed 's/^/    /' "$work/err" | head -5; fail=$((fail+1)); failed+=("$cls"); continue
  fi

  actual=$(cd "$out" && timeout 120 java "$cls" 2>/dev/null)
  expected=$(sed -n '/^ \* EXPECTED OUTPUT/,/^ \* =*\s*\*\/$/p' "$f" \
             | sed '1d;2d;$d' | sed 's/^ \* \{0,1\}//')

  # Four files are deliberately non-deterministic: they demonstrate data races,
  # lost updates, identity hashes and scheduler-dependent pool metrics. Their numbers
  # change every run by design, so for those we verify they still RUN cleanly rather
  # than diffing the output.
  if grep -q 'NON-DETERMINISTIC' "$f"; then
    if [[ -n "$actual" ]]; then
      pass=$((pass+1)); printf '  ok*  %s   (non-deterministic by design: ran clean)\n' "$cls"
    else
      fail=$((fail+1)); failed+=("$cls"); printf '  FAIL %s   (no output)\n' "$cls"
    fi
    continue
  fi

  if [[ "$actual" == "$expected" ]]; then
    pass=$((pass+1)); printf '  ok   %s\n' "$cls"
  else
    fail=$((fail+1)); failed+=("$cls"); printf '  DIFF %s\n' "$cls"
    diff <(echo "$expected") <(echo "$actual") | head -8 | sed 's/^/        /'
  fi
done

echo
echo "passed: $pass   failed: $fail   (ok* = non-deterministic by design, run-only check)"
(( fail == 0 )) || { printf 'failing: %s\n' "${failed[*]}"; exit 1; }
