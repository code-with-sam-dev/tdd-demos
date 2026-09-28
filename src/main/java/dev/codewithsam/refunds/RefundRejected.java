package dev.codewithsam.refunds;

/** Refused, with the reason a support agent would need to read. */
public class RefundRejected extends RuntimeException {
    public RefundRejected(String message) {
        super(message);
    }
}
