package dev.codewithsam.refunds;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The specification, written before the implementation exists.
 *
 * NOTE WHAT MOST OF THESE ARE. The majority say what must be REFUSED. That
 * ratio is the whole point: the happy path is the case everybody already has
 * in their head, and the refusals are the cases that become incidents.
 *
 * AND NOTE WHAT THE LAST THREE ARE ABOUT. They do not check the current
 * request at all. They check what the refundable balance MEANS given a
 * history: that a refusal costs nothing, and that the same command sent twice
 * is one refund. Those are not input validation. They are the domain, and they
 * are the tests that are not "three if statements written early".
 */
class RefundPolicyTest {

    private final RefundPolicy policy = new RefundPolicy();

    @Test
    @DisplayName("refunds an amount within what was captured")
    void refundsWithinCapture() {
        Order order = Order.captured("ord_8812", 10_000);

        RefundResult result = policy.refund(order, "key_a", 6_000);

        assertEquals(6_000, result.refundedCents());
        assertEquals(4_000, result.order().refundableCents());
    }

    @Test
    @DisplayName("refuses a refund larger than the capture")
    void refusesMoreThanCaptured() {
        Order order = Order.captured("ord_8812", 10_000);

        RefundRejected e = assertThrows(RefundRejected.class,
            () -> policy.refund(order, "key_a", 10_001));

        assertEquals("refund 10001 exceeds refundable 10000 on ord_8812", e.getMessage());
    }

    @Test
    @DisplayName("refuses a refund against an order with no capture")
    void refusesUncaptured() {
        Order order = Order.uncaptured("ord_9001");

        RefundRejected e = assertThrows(RefundRejected.class,
            () -> policy.refund(order, "key_a", 1));

        assertEquals("ord_9001 has no capture to refund", e.getMessage());
    }

    @Test
    @DisplayName("refuses a zero or negative refund")
    void refusesNonPositive() {
        Order order = Order.captured("ord_8812", 10_000);

        RefundRejected e = assertThrows(RefundRejected.class,
            () -> policy.refund(order, "key_a", 0));

        assertEquals("refund must be positive, was 0", e.getMessage());
    }

    /*
      THE THREE BELOW ARE THE ONES THAT ARE NOT VALIDATION.
    */

    @Test
    @DisplayName("refuses a second refund that would exceed what is left, not what was captured")
    void refusesSecondRefundBeyondRemaining() {
        Order order = Order.captured("ord_8812", 10_000)
            .with(RefundAttempt.succeeded("key_a", 6_000));

        RefundRejected e = assertThrows(RefundRejected.class,
            () -> policy.refund(order, "key_b", 5_000));

        assertEquals("refund 5000 exceeds refundable 4000 on ord_8812", e.getMessage());
    }

    @Test
    @DisplayName("a FAILED attempt consumes none of the refundable balance")
    void failedAttemptsDoNotConsumeBalance() {
        Order order = Order.captured("ord_8812", 10_000)
            .with(RefundAttempt.failed("key_a", 6_000));

        RefundResult result = policy.refund(order, "key_b", 7_000);

        assertEquals(7_000, result.refundedCents());
        assertEquals(3_000, result.order().refundableCents());
    }

    @Test
    @DisplayName("the same idempotency key returns the same refund and consumes the balance once")
    void sameKeyRefundsOnce() {
        Order order = Order.captured("ord_8812", 10_000);

        RefundResult first = policy.refund(order, "key_a", 6_000);
        RefundResult second = policy.refund(first.order(), "key_a", 6_000);

        assertEquals(6_000, second.refundedCents());
        assertEquals(4_000, second.order().refundableCents());
        assertSame(first.order(), second.order());
    }
}
