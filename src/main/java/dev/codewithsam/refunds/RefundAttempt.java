package dev.codewithsam.refunds;

/**
 * One attempt to refund, successful or not.
 *
 * THE OUTCOME IS THE POINT. A refund that was REFUSED still happened, in the
 * sense that somebody tried it and there is a row for it, and the invariant
 * that matters is that it did not consume any of the refundable balance.
 * That distinction is where the interesting bugs live, and it is not something
 * a validation rule on the current request can express.
 */
public record RefundAttempt(String idempotencyKey, long amountCents, Outcome outcome) {

    public enum Outcome { SUCCEEDED, FAILED }

    public static RefundAttempt succeeded(String key, long amountCents) {
        return new RefundAttempt(key, amountCents, Outcome.SUCCEEDED);
    }

    public static RefundAttempt failed(String key, long amountCents) {
        return new RefundAttempt(key, amountCents, Outcome.FAILED);
    }

    public boolean consumedBalance() {
        return outcome == Outcome.SUCCEEDED;
    }
}
