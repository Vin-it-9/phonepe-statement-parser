package org.phonepe.util.ui;

import org.phonepe.config.domain.Direction;
import org.phonepe.config.domain.PendingTransaction;
import org.phonepe.config.domain.Totals;
import org.phonepe.util.FormatUtils;
import org.phonepe.util.MoneyUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ConsoleUI {

    private static final long PROGRESS_INTERVAL_NANOS = 250_000_000L;
    private static final int OUTPUT_BUFFER_SIZE = 32 * 1024;
    private static final int TRANSACTION_BATCH_SIZE = 8 * 1024;
    private static final String LS = System.lineSeparator();
    private static final int BAR_WIDTH = 30;

    private final BufferedWriter out = new BufferedWriter(
            new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
            OUTPUT_BUFFER_SIZE
    );

    private final StringBuilder transactionBuffer = new StringBuilder(TRANSACTION_BATCH_SIZE);
    private final StringBuilder progressLine = new StringBuilder(160);
    private final boolean showTransactions;

    private long lastProgressNanos;
    private boolean progressShown;

    public ConsoleUI(boolean showTransactions) {
        this.showTransactions = showTransactions;
    }

    public void printHeader(String target, int totalPages, long fileBytes) throws IOException {
        out.write(LS);
        out.write("============================================================");
        out.write(LS);
        out.write("                PHONEPE STATEMENT SCANNER");
        out.write(LS);
        out.write("============================================================");
        out.write(LS);

        out.write(" Target       : ");
        out.write(target);
        out.write(LS);

        out.write(" Pages        : ");
        out.write(Integer.toString(totalPages));
        out.write(LS);

        out.write(" File Size    : ");
        out.write(FormatUtils.formatBytes(fileBytes));
        out.write(LS);

        out.write(" Extraction   : streaming");
        out.write(LS);

        out.write(" Memory       : no full-document text buffer");
        out.write(LS);

        out.write("------------------------------------------------------------");
        out.write(LS);
        out.flush();
    }

    public void transaction(PendingTransaction transaction) throws IOException {
        if (!showTransactions) {
            return;
        }

        if (progressShown) {
            out.newLine();
            progressShown = false;
        }

        final boolean credit = transaction.getDirection() == Direction.CREDIT;

        transactionBuffer.append('[')
                .append(credit ? '+' : '-')
                .append("] ")
                .append(transaction.getDate())
                .append(" | ")
                .append(credit ? "RECEIVED" : "SENT")
                .append(" | ");

        MoneyUtils.appendMoney(transactionBuffer, transaction.getAmountPaise());
        transactionBuffer.append(LS);

        if (transactionBuffer.length() >= TRANSACTION_BATCH_SIZE) {
            flushTransactions();
        }
    }

    public void progress(int currentPage, int totalPages, long matches, long startNanos) throws IOException {
        final long now = System.nanoTime();

        if (currentPage != totalPages && now - lastProgressNanos < PROGRESS_INTERVAL_NANOS) {
            return;
        }

        lastProgressNanos = now;

        if (!transactionBuffer.isEmpty()) {
            flushTransactions();
        }

        final double fraction = totalPages == 0 ? 1.0 : (double) currentPage / totalPages;
        final int filled = Math.min(BAR_WIDTH, (int) (fraction * BAR_WIDTH));

        progressLine.setLength(0);
        progressLine.append('\r').append('[');

        for (int i = 0; i < filled; i++) {
            progressLine.append('=');
        }
        for (int i = filled; i < BAR_WIDTH; i++) {
            progressLine.append('.');
        }

        progressLine.append("] ");
        appendPercent(progressLine, (int) (fraction * 100));
        progressLine.append("% | Page ").append(currentPage).append('/').append(totalPages);
        progressLine.append(" | Matches ").append(matches).append(" | ");
        appendElapsedSeconds(progressLine, now - startNanos);
        progressLine.append('s');

        out.write(progressLine.toString());
        out.flush();

        progressShown = true;
    }

    private static void appendPercent(StringBuilder sb, int percent) {
        if (percent < 10) {
            sb.append("  ").append(percent);
        } else if (percent < 100) {
            sb.append(' ').append(percent);
        } else {
            sb.append(100);
        }
    }

    private static void appendElapsedSeconds(StringBuilder sb, long elapsedNanos) {
        final long tenths = (elapsedNanos + 50_000_000L) / 100_000_000L;
        sb.append(tenths / 10).append('.').append(tenths % 10);
    }

    public void finishProgress() throws IOException {
        if (progressShown) {
            out.newLine();
            progressShown = false;
        }
        flushTransactions();
    }

    public void printSummary(Totals totals, int totalPages, long startNanos, long fileBytes, long heapUsed) throws IOException {
        final long elapsedNanos = System.nanoTime() - startNanos;
        final double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        final double pagesPerSecond = elapsedSeconds > 0 ? totalPages / elapsedSeconds : 0.0;

        out.newLine();
        out.write("============================================================");
        out.newLine();
        out.write("                         RESULTS");
        out.newLine();
        out.write("============================================================");
        out.newLine();

        out.write(" Matching Transactions : ");
        out.write(Long.toString(totals.matchingTransactions()));
        out.newLine();

        out.write(" Received Transactions : ");
        out.write(Long.toString(totals.getReceivedCount()));
        out.newLine();

        out.write(" Sent Transactions     : ");
        out.write(Long.toString(totals.getSentCount()));
        out.newLine();

        out.write(" Unresolved            : ");
        out.write(Long.toString(totals.getUnresolvedCount()));
        out.newLine();
        out.newLine();

        out.write(" Total Received (+)    : ");
        MoneyUtils.appendMoney(out, totals.getReceivedPaise());
        out.newLine();

        out.write(" Total Sent (-)        : ");
        MoneyUtils.appendMoney(out, totals.getSentPaise());
        out.newLine();

        out.write(" Net Balance            : ");
        MoneyUtils.appendMoney(out, totals.netPaise());
        out.newLine();
        out.newLine();

        out.write("------------------------------------------------------------");
        out.newLine();
        out.write(" Performance");
        out.newLine();

        out.write("   File Size            : ");
        out.write(FormatUtils.formatBytes(fileBytes));
        out.newLine();

        out.write("   Pages Processed      : ");
        out.write(Integer.toString(totalPages));
        out.newLine();

        out.write("   Elapsed              : ");
        out.write(String.format(Locale.ROOT, "%.3f seconds", elapsedSeconds));
        out.newLine();

        out.write("   Speed                : ");
        out.write(String.format(Locale.ROOT, "%.1f pages/sec", pagesPerSecond));
        out.newLine();

        out.write("   Heap Used            : ");
        out.write(FormatUtils.formatBytes(heapUsed));
        out.newLine();

        out.write("============================================================");
        out.newLine();

        out.flush();
    }

    private void flushTransactions() throws IOException {
        if (transactionBuffer.isEmpty()) {
            return;
        }
        out.write(transactionBuffer.toString());
        transactionBuffer.setLength(0);
        out.flush();
    }
}