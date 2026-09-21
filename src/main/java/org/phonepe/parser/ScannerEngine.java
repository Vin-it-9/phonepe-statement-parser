package org.phonepe.parser;

import org.phonepe.config.domain.Direction;
import org.phonepe.config.domain.PendingTransaction;
import org.phonepe.config.domain.Totals;
import org.phonepe.config.domain.TransactionHeader;
import org.phonepe.util.ui.ConsoleUI;
import org.phonepe.util.StringUtils;

import java.io.IOException;
import java.util.Locale;

public final class ScannerEngine {

    private static final char[] CREDIT = "credit".toCharArray();
    private static final char[] DEBIT = "debit".toCharArray();

    private final String targetName;
    private final char[] targetLower;
    private final ConsoleUI ui;
    private final Totals totals = new Totals();

    private PendingTransaction current;

    private int creditPosition;
    private int debitPosition;
    private boolean targetFound;

    public ScannerEngine(String targetName, ConsoleUI ui) {
        this.targetName = targetName;
        this.targetLower = targetName.toLowerCase(Locale.ROOT).toCharArray();
        this.ui = ui;
    }

    public Totals getTotals() {
        return totals;
    }

    public void acceptLine(String rawLine) throws IOException {
        final String line = StringUtils.trimLine(rawLine);

        if (line.isEmpty()) {
            return;
        }

        final TransactionHeader header = TransactionParser.parseTransactionHeader(line);

        if (header != null) {
            finalizeCurrent();

            final String part = header.transactionPart();
            scanKeywordsOnce(part);

            if (creditPosition < 0 && debitPosition < 0) {
                current = null;
                return;
            }

            final Direction direction = resolveDirection(creditPosition, debitPosition);
            current = new PendingTransaction(header.date(), direction, line, targetFound);

            final long amount = TransactionParser.extractAmountAfterINR(part);
            if (amount >= 0) {
                current.setAmountPaise(amount);
                finalizeCurrent();
            }

            return;
        }

        if (current == null || !current.isTarget() || current.isCounted() || current.getAmountPaise() >= 0) {
            return;
        }

        if (StringUtils.containsIgnoreCase(line, "Transaction ID")) {
            final long amount = TransactionParser.extractTrailingAmount(line);
            if (amount >= 0) {
                current.setAmountPaise(amount);
                finalizeCurrent();
            }
            return;
        }

        final long standaloneAmount = TransactionParser.parseFullMoneyLine(line);
        if (standaloneAmount >= 0) {
            current.setAmountPaise(standaloneAmount);
            finalizeCurrent();
        }
    }

    /**
     * Single forward pass that locates "Credit", "Debit", and the target
     * name simultaneously — replaces three independent full-string scans
     * with one, and does zero String allocation (no toLowerCase copies).
     */
    private void scanKeywordsOnce(String part) {
        creditPosition = -1;
        debitPosition = -1;
        targetFound = false;

        final int len = part.length();
        final int targetLen = targetLower.length;

        for (int i = 0; i < len; i++) {
            if (creditPosition < 0 && matchesWordAt(part, i, CREDIT)) {
                creditPosition = i;
            }
            if (debitPosition < 0 && matchesWordAt(part, i, DEBIT)) {
                debitPosition = i;
            }
            if (!targetFound && targetLen > 0 && matchesAt(part, i, targetLower)) {
                targetFound = true;
            }

            if (creditPosition >= 0 && debitPosition >= 0 && targetFound) {
                break;
            }
        }
    }

    private static boolean matchesAt(String value, int index, char[] pattern) {
        final int plen = pattern.length;
        if (index + plen > value.length()) {
            return false;
        }
        for (int k = 0; k < plen; k++) {
            if (Character.toLowerCase(value.charAt(index + k)) != pattern[k]) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesWordAt(String value, int index, char[] pattern) {
        if (!matchesAt(value, index, pattern)) {
            return false;
        }
        if (index > 0 && Character.isLetterOrDigit(value.charAt(index - 1))) {
            return false;
        }
        final int end = index + pattern.length;
        return end >= value.length() || !Character.isLetterOrDigit(value.charAt(end));
    }

    private void finalizeCurrent() throws IOException {
        if (current == null) {
            return;
        }

        if (!current.isTarget()) {
            current = null;
            return;
        }

        if (current.getAmountPaise() < 0) {
            totals.incrementUnresolvedCount();
            current = null;
            return;
        }

        if (current.isCounted()) {
            current = null;
            return;
        }

        switch (current.getDirection()) {
            case CREDIT -> {
                totals.addReceivedPaise(current.getAmountPaise());
                totals.incrementReceivedCount();
            }
            case DEBIT -> {
                totals.addSentPaise(current.getAmountPaise());
                totals.incrementSentCount();
            }
        }

        current.setCounted(true);
        ui.transaction(current);
        current = null;
    }

    private static Direction resolveDirection(int creditPosition, int debitPosition) {
        if (creditPosition >= 0 && (debitPosition < 0 || creditPosition < debitPosition)) {
            return Direction.CREDIT;
        }
        return Direction.DEBIT;
    }
}