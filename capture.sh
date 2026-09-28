#!/usr/bin/env bash
# Capture the real Maven runs the episode puts on screen.
#
# EVERY FRAME IN THE TDD EPISODE IS ONE OF THESE FILES. Same rule as the Modern
# Java repo: if there is no capture, the claim does not go on screen. A retyped
# test report is a fabricated screenshot.
#
# The script drives the actual cycle rather than describing it. It swaps in the
# skeleton, runs, swaps the implementation back, runs, then introduces two
# plausible regressions one at a time and runs after each. So the captures
# cannot drift from the code in this repository, and the "correct"
# implementation is always what is left on disk afterwards.
set -uo pipefail
cd "$(dirname "$0")"
mkdir -p captures
export JAVA_HOME="${JAVA_HOME:-$HOME/.sdkman/candidates/java/21.0.5-amzn}"

POLICY=src/main/java/dev/codewithsam/refunds/RefundPolicy.java
BACKUP=$(mktemp)
cp "$POLICY" "$BACKUP"
restore() { cp "$BACKUP" "$POLICY"; rm -f "$BACKUP"; }
trap restore EXIT

run_into() {
  local out="captures/$1.txt"
  { echo "\$ mvn test"; echo; } > "$out"
  mvn -B test 2>&1 \
    | grep -vE "^\[INFO\] (Downloading|Downloaded|Copying|Nothing|Using|skip|Recompiling|Changes detected)" \
    | grep -vE "^\[INFO\] (---|Building|-{10,}|$)" \
    | grep -vE "maven-(resources|compiler|surefire|jar|install)-plugin" \
    | sed -E 's#/Users/[^ ]*/kafka-youtube-video/tdd-demos#.#g' \
    >> "$out"
  echo "  captured $out"
}

# 1. RED, the honest kind. The test compiles, runs, reaches the code under test
#    and fails on a deliberate "not implemented yet". Failing because a class is
#    missing proves nothing about the test.
cat > "$POLICY" <<'JAVA'
package dev.codewithsam.refunds;

public class RefundPolicy {

    public RefundResult refund(Order order, String idempotencyKey, long amountCents) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
JAVA
run_into red

# 2. GREEN, from the implementation this repository actually ships.
cp "$BACKUP" "$POLICY"
run_into green

# 3. REGRESSION ONE. The simplification that passes review: compare against
#    what was CAPTURED rather than what is still REFUNDABLE. The happy path
#    survives. One test names the case it broke.
sed -E 's/long refundable = order\.refundableCents\(\);/long refundable = order.capturedCents();/' \
  "$BACKUP" > "$POLICY"
run_into regression-balance

# 4. REGRESSION TWO, and this is the subtle one. Drop the idempotency replay
#    guard, which looks like dead code to anyone who has not watched a retry
#    arrive twice. Every validation test still passes.
#
#    READ THE OUTPUT RATHER THAN ASSUMING IT. With a 10,000 capture and two
#    6,000 commands, the replay is REFUSED, because the first one already moved
#    the balance: "refund 6000 exceeds refundable 4000". So the failure a
#    customer meets is not a double refund here, it is a legitimate retry being
#    rejected with a message that blames the amount. Whether the same bug
#    double refunds or refuses depends on the amounts, and both are wrong.
awk '
  /Optional<RefundAttempt> replay/ { skipping = 1 }
  skipping && /^        }$/ { skipping = 0; next }
  !skipping { print }
' "$BACKUP" > "$POLICY"
run_into regression-idempotency

restore
trap - EXIT
echo "done, implementation restored"
