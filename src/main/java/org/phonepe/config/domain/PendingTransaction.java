package org.phonepe.config.domain;

public final class PendingTransaction {

    private final String date;
    private final Direction direction;
    private final String originalLine;
    private final boolean target;

    private long amountPaise = -1;
    private boolean counted;

    public PendingTransaction(
            String date,
            Direction direction,
            String originalLine,
            boolean target
    ) {
        this.date = date;
        this.direction = direction;
        this.originalLine = originalLine;
        this.target = target;
    }

    public String getDate() {
        return date;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getOriginalLine() {
        return originalLine;
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
