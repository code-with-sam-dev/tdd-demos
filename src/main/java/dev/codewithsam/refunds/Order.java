package dev.codewithsam.refunds;

import java.util.List;
import java.util.Optional;

/**
 * An order, its capture, and every refund attempt made against it.
 *
 * The refundable balance is DERIVED from the history rather than stored,
 * because a stored balance is a second source of truth and the two disagree
 * the first time an attempt fails halfway.
 */
public record Order(String id, long capturedCents, List<RefundAttempt> attempts) {

    public Order {
        attempts = List.copyOf(attempts);
    }

    public static Order captured(String id, long capturedCents) {
        return new Order(id, capturedCents, List.of());
    }

    public static Order uncaptured(String id) {
        return new Order(id, 0, List.of());
    }

    public Order with(RefundAttempt attempt) {
        return new Order(id, capturedCents,
            java.util.stream.Stream.concat(attempts.stream(), java.util.stream.Stream.of(attempt)).toList());
    }

    /** Only SUCCEEDED attempts consume it. A refusal costs nothing. */
    public long refundedCents() {
        return attempts.stream().filter(RefundAttempt::consumedBalance)
            .mapToLong(RefundAttempt::amountCents).sum();
    }

    public long refundableCents() {
        return capturedCents - refundedCents();
    }

    public Optional<RefundAttempt> attemptWithKey(String idempotencyKey) {
        return attempts.stream()
            .filter(a -> a.idempotencyKey().equals(idempotencyKey))
            .filter(RefundAttempt::consumedBalance)
            .findFirst();
    }
}
