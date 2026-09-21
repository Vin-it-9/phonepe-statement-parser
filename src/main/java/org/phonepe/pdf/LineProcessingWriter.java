package org.phonepe.pdf;

import java.io.Writer;
import java.util.function.Consumer;

public final class LineProcessingWriter extends Writer {

    private final Consumer<String> lineConsumer;
    private final StringBuilder carry = new StringBuilder(256);

    public LineProcessingWriter(Consumer<String> lineConsumer) {
        this.lineConsumer = lineConsumer;
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        final int end = off + len;
        int segmentStart = off;

        for (int i = off; i < end; i++) {
            if (cbuf[i] != '\n') {
                continue;
            }
            emitSegment(cbuf, segmentStart, i);
            segmentStart = i + 1;
        }

        if (segmentStart < end) {
            carry.append(cbuf, segmentStart, end - segmentStart);
        }
    }

    @Override
    public void write(String str, int off, int len) {
        final int end = off + len;
        int segmentStart = off;

        int nl;
        while ((nl = str.indexOf('\n', segmentStart)) >= 0 && nl < end) {
            emitSegment(str, segmentStart, nl);
            segmentStart = nl + 1;
        }

        if (segmentStart < end) {
            carry.append(str, segmentStart, end);
        }
    }

    @Override
    public void write(String str) {
        write(str, 0, str.length());
    }

    @Override
    public void write(int c) {
        if (c == '\n') {
            emitLine();
        } else {
            carry.append((char) c);
        }
    }

    private void emitSegment(char[] buf, int start, int newlineIndex) {
        int lineEnd = newlineIndex;
        if (lineEnd > start && buf[lineEnd - 1] == '\r') {
            lineEnd--;
        }

        if (carry.isEmpty()) {
            lineConsumer.accept(new String(buf, start, lineEnd - start));
            return;
        }

        if (lineEnd > start) {
            carry.append(buf, start, lineEnd - start);
        }
        emitFromCarry();
    }

    private void emitSegment(String str, int start, int newlineIndex) {
        int lineEnd = newlineIndex;
        if (lineEnd > start && str.charAt(lineEnd - 1) == '\r') {
            lineEnd--;
        }

        if (carry.isEmpty()) {
            lineConsumer.accept(str.substring(start, lineEnd));
            return;
        }

        if (lineEnd > start) {
            carry.append(str, start, lineEnd);
        }
        emitFromCarry();
    }

    private void emitLine() {
        final int length = carry.length();
        if (length > 0 && carry.charAt(length - 1) == '\r') {
            carry.setLength(length - 1);
        }
        emitFromCarry();
    }

    private void emitFromCarry() {
        lineConsumer.accept(carry.toString());
        carry.setLength(0);
    }

    public void finish() {
        if (!carry.isEmpty()) {
            emitFromCarry();
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        finish();
    }
}