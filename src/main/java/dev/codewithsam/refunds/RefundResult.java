package dev.codewithsam.refunds;

/**
 * What a refund produced: the amount, and the order as it now stands.
 *
 * The order comes back rather than being mutated, so a caller cannot end up
 * holding a stale balance, and so the test can assert on the history the
 * refund left behind rather than only on the return value.
 */
public record RefundResult(long refundedCents, Order order) {
}
