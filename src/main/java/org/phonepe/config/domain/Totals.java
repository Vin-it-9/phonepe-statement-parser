package org.phonepe.config.domain;

public final class Totals {

    private long receivedPaise;
    private long sentPaise;

    private long receivedCount;
    private long sentCount;

    private long unresolvedCount;

    public long getReceivedPaise() {
        return receivedPaise;
    }

    public void addReceivedPaise(long amount) {
        this.receivedPaise += amount;
    }

    public long getSentPaise() {
        return sentPaise;
    }

    public void addSentPaise(long amount) {
        this.sentPaise += amount;
    }

    public long getReceivedCount() {
        return receivedCount;
    }

    public void incrementReceivedCount() {
        this.receivedCount++;
    }

    public long getSentCount() {
        return sentCount;
    }

    public void incrementSentCount() {
        this.sentCount++;
    }

    public long getUnresolvedCount() {
        return unresolvedCount;
    }

    public void incrementUnresolvedCount() {
        this.unresolvedCount++;
    }

    public long matchingTransactions() {
        return receivedCount + sentCount;
    }

    public long netPaise() {
        return receivedPaise - sentPaise;
    }
}
