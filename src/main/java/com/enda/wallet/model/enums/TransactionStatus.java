package com.enda.wallet.model.enums;



import java.util.EnumSet;
import java.util.Set;

public enum TransactionStatus {
    CREATED,
    AWAITING_VALIDATION,
    VALIDATED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    REJECTED,
    CANCELLED,
    TIMEOUT;

    private static final Set<TransactionStatus> TERMINAL_STATUSES =
            EnumSet.of(SUCCEEDED, FAILED, REJECTED, CANCELLED, TIMEOUT);

    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }

    public boolean canTransitionTo(TransactionStatus newStatus) {
        switch (this) {
            case CREATED:
                return newStatus == AWAITING_VALIDATION || newStatus == CANCELLED;
            case AWAITING_VALIDATION:
                return newStatus == VALIDATED || newStatus == REJECTED || newStatus == TIMEOUT;
            case VALIDATED:
                return newStatus == PROCESSING;
            case PROCESSING:
                return newStatus == SUCCEEDED || newStatus == FAILED;
            default:
                return false;
        }
    }
}
