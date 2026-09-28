package dev.codewithsam.refunds;

import java.util.Optional;

/**
 * The implementation, written AFTER the specification it has to satisfy.
 *
 * Read the order of the guards. The idempotency check comes FIRST, because a
 * replay of a command that already succeeded is not a new refund and must not
 * be validated as one: the second send of the same command can arrive after
 * the balance has moved, and rejecting it would turn a retry into an error the
 * caller cannot resolve.
 *
 * Then a positive amount, then a capture to refund against, then the remaining
 * refundable balance. That order is not a style choice. It is the order the
 * tests demanded, and each guard carries a message a test asserts on and a
 * support agent has to be able to act on.
 */
public class RefundPolicy {

    public RefundResult refund(Order order, String idempotencyKey, long amountCents) {
        Optional<RefundAttempt> replay = order.attemptWithKey(idempotencyKey);
        if (replay.isPresent()) {
            return new RefundResult(replay.get().amountCents(), order);
        }
        if (amountCents <= 0) {
            throw new RefundRejected("refund must be positive, was " + amountCents);
        }
        if (order.capturedCents() <= 0) {
            throw new RefundRejected(order.id() + " has no capture to refund");
        }
        long refundable = order.refundableCents();
        if (amountCents > refundable) {
            throw new RefundRejected(
                "refund " + amountCents + " exceeds refundable " + refundable + " on " + order.id());
        }
        return new RefundResult(amountCents,
            order.with(RefundAttempt.succeeded(idempotencyKey, amountCents)));
    }
}
