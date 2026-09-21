package org.phonepe.pdf;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.phonepe.parser.ScannerEngine;
import org.phonepe.util.ui.ConsoleUI;

import java.io.IOException;

public final class ProgressStripper extends PDFTextStripper {

    private static final long MIN_REPORT_INTERVAL_NANOS = 50_000_000L;

    private final ConsoleUI ui;
    private final ScannerEngine scanner;
    private final int totalPages;
    private final long startNanos;

    private int currentPage;
    private long lastReportNanos;
    private int lastReportedPercent = -1;

    public ProgressStripper(ConsoleUI ui, ScannerEngine scanner, int totalPages, long startNanos) throws IOException {
        this.ui = ui;
        this.scanner = scanner;
        this.totalPages = totalPages;
        this.startNanos = startNanos;
        this.lastReportNanos = startNanos;

        setSortByPosition(true);
        setLineSeparator("\n");
        setPageStart("");
        setPageEnd("\n");
        setSuppressDuplicateOverlappingText(true);
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        super.endPage(page);
        currentPage++;

        final boolean lastPage = currentPage == totalPages;
        final long now = System.nanoTime();
        final int percent = totalPages > 0 ? (currentPage * 100) / totalPages : 100;

        if (lastPage || percent != lastReportedPercent || (now - lastReportNanos) >= MIN_REPORT_INTERVAL_NANOS) {
            ui.progress(currentPage, totalPages, scanner.getTotals().matchingTransactions(), startNanos);
            lastReportNanos = now;
            lastReportedPercent = percent;
        }
    }
}