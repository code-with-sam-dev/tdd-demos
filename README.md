# TDD for Java, step by step

One rule, three runs, and a regression that only a test catches.

Companion to the episode. Everything here is runnable end to end, with Docker
or without, and every frame the video puts on screen is one of the files in
`captures/`.

## The specification came first

`src/test/java/dev/codewithsam/refunds/RefundPolicyTest.java` is seven tests
against a refund rule. Most of them say what must be REFUSED:

| Case | Why it is in the spec |
|---|---|
| refunds an amount within what was captured | the happy path, and the least interesting one |
| refuses a refund larger than the capture | the one everybody remembers |
| refuses a refund against an order with no capture | the one nobody remembers |
| refuses a zero or negative refund | the one that looks like a typo and is not |
| refuses a second refund beyond what is LEFT | depends on history, not on the request |
| a FAILED attempt consumes no balance | a refusal has to cost nothing |
| the same idempotency key refunds once | a retry is not a second refund |

That ratio is the point. The happy path is already in your head. The refusals
are the cases that become incidents.

And note what the last three are. They do not look at the current request at
all. They say what the refundable balance MEANS given a history: that a refusal
costs nothing, and that the same command sent twice is one refund. Those are
not input validation, they are the domain, and they are the tests that are not
"three if statements written early".

## The four runs

    ./capture.sh

It drives the actual cycle rather than describing it, and it leaves the correct
implementation on disk afterwards.

**1. Red, the honest kind.** The skeleton throws
`UnsupportedOperationException`, so the test compiles, runs, reaches the code
under test, and fails on a deliberate "not implemented yet". Seven run, four
fail, three error.

A test that fails because the class does not exist has told you nothing about
the test. This is the step almost every TDD demonstration skips, and skipping
it is what turns the cycle into a ritual.

**2. Green.** Seven run, zero fail, against a specification that was fixed
before any code had an opinion about it.

**3. Regression one: the balance.** One plausible simplification, exactly the
kind that passes review. Compare the amount against what was CAPTURED rather
than what is still REFUNDABLE:

    - long refundable = order.refundableCents();
    + long refundable = order.capturedCents();

Six tests stay green. One does not:

    RefundPolicyTest.refusesSecondRefundBeyondRemaining:81
      Expected RefundRejected to be thrown, but nothing was thrown.

A 10,000 capture, 6,000 already refunded, and a 5,000 refund goes through.

**4. Regression two: the replay guard.** Delete the idempotency check, which
looks like dead code to anyone who has not watched a retry arrive twice. Every
validation test still passes. One does not:

    RefundPolicyTest.sameKeyRefundsOnce:105
      RefundRejected: refund 6000 exceeds refundable 4000 on ord_8812

Read that carefully, because it is not what most people expect. The replay is
REFUSED, since the first command already moved the balance. So the customer
meets a legitimate retry being rejected with a message that blames the amount.
Whether that same bug double refunds or refuses depends on the numbers, and
both answers are wrong.

## Run it

With Docker, which needs nothing installed but Docker:

    ./docker-run.sh

Without Docker, with Maven and a JDK 21 or newer:

    mvn test

The only dependency is JUnit 5. No Spring, no fixtures framework, no test
containers: every dependency added here is another thing you would have to
install before you could reproduce what you watched.
