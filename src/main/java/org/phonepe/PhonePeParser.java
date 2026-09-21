package org.phonepe;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.phonepe.config.ParserConfig;
import org.phonepe.parser.ScannerEngine;
import org.phonepe.pdf.LineProcessingWriter;
import org.phonepe.pdf.ProgressStripper;
import org.phonepe.util.ui.ConsoleUI;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

public final class PhonePeParser {

    private static final String PREFS_FILE = System.getProperty("user.home") + File.separator + ".phonepe_parser_prefs.properties";

    private PhonePeParser() {
    }

    static void main(String[] args) {

        try {
            System.setOut(new PrintStream(new FileOutputStream(java.io.FileDescriptor.out), true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        System.setProperty("java.awt.headless", "true");
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        final ParserConfig config = resolveConfig(args);

        final File pdfFile = new File(config.pdfFile());
        final ConsoleUI ui = new ConsoleUI(config.showTransactions());

        try {
            if (!pdfFile.isFile()) {
                throw new IOException("PDF file not found: " + pdfFile.getAbsolutePath());
            }

            final long fileBytes = pdfFile.length();

            try (PDDocument document = Loader.loadPDF(pdfFile, config.password())) {
                final int totalPages = document.getNumberOfPages();

                System.out.println();
                ui.printHeader(config.targetName(), totalPages, fileBytes);

                final long programStart = System.nanoTime();

                final ScannerEngine scanner = new ScannerEngine(config.targetName(), ui);
                final ProgressStripper stripper = new ProgressStripper(ui, scanner, totalPages, programStart);

                final LineProcessingWriter writer = new LineProcessingWriter(line -> {
                    try {
                        scanner.acceptLine(line);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                try {
                    stripper.writeText(document, writer);
                    writer.finish();
                    scanner.acceptLine("");
                    ui.finishProgress();

                    final Runtime runtime = Runtime.getRuntime();
                    final long heapUsed = runtime.totalMemory() - runtime.freeMemory();

                    ui.printSummary(scanner.getTotals(), totalPages, programStart, fileBytes, heapUsed);
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof IOException io) {
                        throw io;
                    }
                    throw e;
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println();
            System.err.println("[ERROR] " + e.getMessage());
            System.exit(1);
        }
    }

    private static ParserConfig resolveConfig(String[] args) {
        final Properties prefs = new Properties();
        final File prefsFile = new File(PREFS_FILE);
        boolean hasPrefs = false;

        if (prefsFile.exists()) {
            try (FileInputStream in = new FileInputStream(prefsFile)) {
                prefs.load(in);
                hasPrefs = prefs.containsKey("filePath") && prefs.containsKey("password");
            } catch (IOException ignored) {}
        }

        if (args.length > 0) {
            String cliFile = null, cliPass = null, cliTarget = null;
            boolean cliVerbose = false, helpRequested = false;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-f": case "--file": if (i + 1 < args.length) cliFile = args[++i]; break;
                    case "-p": case "--password": if (i + 1 < args.length) cliPass = args[++i]; break;
                    case "-t": case "--target": if (i + 1 < args.length) cliTarget = args[++i]; break;
                    case "-v": case "--verbose": cliVerbose = true; break;
                    case "-h": case "--help": helpRequested = true; break;
                }
            }

            if (helpRequested) {
                printUsage();
                System.exit(0);
            }

            if (cliFile == null && hasPrefs) cliFile = prefs.getProperty("filePath");
            if (cliPass == null && hasPrefs) cliPass = prefs.getProperty("password");
            if (!cliVerbose && hasPrefs) cliVerbose = Boolean.parseBoolean(prefs.getProperty("showTransactions", "false"));

            if (cliFile == null || cliPass == null || cliTarget == null) {
                System.err.println("[ERROR] Missing required arguments. Please provide -f and -p, or run without arguments once to setup a profile.");
                printUsage();
                System.exit(1);
            }

            return new ParserConfig(cliFile, cliPass, cliTarget, cliVerbose);
        }

        System.out.println("=== PhonePe Statement Parser ===");
        final Console console = System.console();
        final Scanner fallbackScanner = (console == null) ? new Scanner(System.in) : null;

        if (console == null) {
            System.out.println("[WARN] Running inside IDE (Passwords will be visible)");
        }

        String filePath = null;
        String password = null;
        boolean showTransactions = false;
        boolean useSaved = false;

        if (hasPrefs) {
            System.out.println("\n[ Found Saved Profile ]");
            System.out.println("  File    : " + prefs.getProperty("filePath"));
            System.out.println("  Verbose : " + prefs.getProperty("showTransactions", "false"));

            String ans = readInput(console, fallbackScanner, "\nUse saved file and password? (Y/n): ").trim();
            if (ans.isEmpty() || ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("yes")) {
                useSaved = true;
                filePath = prefs.getProperty("filePath");
                password = prefs.getProperty("password");
                showTransactions = Boolean.parseBoolean(prefs.getProperty("showTransactions", "false"));
            }
        }

        if (!useSaved) {
            filePath = readInput(console, fallbackScanner, "PDF File Path: ").trim();

            if (console != null) {
                char[] pwdChars = console.readPassword("PDF Password : ");
                password = pwdChars != null ? new String(pwdChars) : "";
            } else {
                System.out.print("PDF Password : ");
                password = fallbackScanner.nextLine().trim();
            }

            String verbose = readInput(console, fallbackScanner, "Show all transactions? (y/N): ").trim();
            showTransactions = verbose.equalsIgnoreCase("y") || verbose.equalsIgnoreCase("yes");

            prefs.setProperty("filePath", filePath);
            prefs.setProperty("password", password);
            prefs.setProperty("showTransactions", String.valueOf(showTransactions));

            try (FileOutputStream out = new FileOutputStream(prefsFile)) {
                prefs.store(out, "PhonePe Parser Quick-Call Profile");
            } catch (IOException ignored) {}
        }

        System.out.println();
        String targetName = readInput(console, fallbackScanner, "Target Name  : ").trim();

        if (filePath.isEmpty() || password.isEmpty() || targetName.isEmpty()) {
            System.err.println("[ERROR] File path, password, and target name cannot be empty.");
            System.exit(1);
        }

        return new ParserConfig(filePath, password, targetName, showTransactions);
    }

    private static String readInput(Console console, Scanner scanner, String prompt) {
        if (console != null) {
            return console.readLine(prompt);
        } else {
            System.out.print(prompt);
            return scanner.hasNextLine() ? scanner.nextLine() : "";
        }
    }

    private static void printUsage() {
        System.out.println("\nUsage: phonepe-parser [options]\n");
        System.out.println("Options:");
        System.out.println("  -f, --file <path>      Path to the PhonePe PDF statement");
        System.out.println("  -p, --password <pwd>   Password to unlock the PDF");
        System.out.println("  -t, --target <name>    Target name to scan for (Required)");
        System.out.println("  -v, --verbose          Print every matching transaction to the console");
        System.out.println("  -h, --help             Show this help message\n");
        System.out.println("Note: If a profile is saved, -f and -p can be omitted.");
    }
}