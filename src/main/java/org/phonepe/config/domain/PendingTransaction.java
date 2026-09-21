package org.phonepe.config.domain;

public final class PendingTransaction {

    private final String date;
    private final Direction direction;
    private final boolean target;

    private long amountPaise = -1;
    private boolean counted;

    public PendingTransaction(
            String date,
            Direction direction,
            boolean target
    ) {
        this.date = date;
        this.direction = direction;
        this.target = target;
    }

    public String getDate() {
        return date;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isTarget() {
        return target;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public boolean isCounted() {
        return counted;
    }

    public void setCounted(boolean counted) {
        this.counted = counted;
    }
}
