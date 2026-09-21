package org.phonepe.config;

public record ParserConfig(
        String pdfFile,
        String password,
        String targetName,
        boolean showTransactions
) {
}
