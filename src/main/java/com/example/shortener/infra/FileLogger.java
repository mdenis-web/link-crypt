package com.example.shortener.infra;

import com.example.shortener.core.port.AppLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class FileLogger implements AppLogger, AutoCloseable {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private final BufferedWriter writer;
    private final Object lock = new Object();
    private boolean closed = false;

    public FileLogger(Path filePath) throws IOException {
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        this.writer = Files.newBufferedWriter(
                filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public void info(String message) {
        write("INFO", message, null);
    }

    @Override
    public void warn(String message) {
        write("WARN", message, null);
    }

    @Override
    public void error(String message) {
        write("ERROR", message, null);
    }

    @Override
    public void error(String message, Throwable t) {
        write("ERROR", message, t);
    }

    private void write(String level, String message, Throwable t) {
        String thread = Thread.currentThread().getName();
        StringBuilder sb = new StringBuilder()
                .append(FMT.format(Instant.now()))
                .append(" [").append(level).append("]")
                .append(" [").append(thread).append("] ")
                .append(message);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            sb.append(System.lineSeparator()).append(sw);
        }
        sb.append(System.lineSeparator());

        synchronized (lock) {
            if (closed) return;
            try {
                writer.write(sb.toString());
                writer.flush();
            } catch (IOException e) {
                System.err.println("[FileLogger] write failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            writer.close();
        }
    }
}