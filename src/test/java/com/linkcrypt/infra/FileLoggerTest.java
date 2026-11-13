package com.linkcrypt.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLoggerTest {

  @TempDir Path tmp;

  @Test
  void createsFileAndParentDirs() throws Exception {
    Path logPath = tmp.resolve("a/b/c/app.log");
    try (FileLogger logger = new FileLogger(logPath)) {
      logger.info("hello");
    }
    assertTrue(Files.exists(logPath));
    assertTrue(Files.readString(logPath).contains("hello"));
  }

  @Test
  void writesAllLevels() throws Exception {
    Path logPath = tmp.resolve("app.log");
    try (FileLogger logger = new FileLogger(logPath)) {
      logger.info("I");
      logger.warn("W");
      logger.error("E");
    }
    String content = Files.readString(logPath);
    assertTrue(content.contains("[INFO]"));
    assertTrue(content.contains("[WARN]"));
    assertTrue(content.contains("[ERROR]"));
  }

  @Test
  void includesStackTraceForErrors() throws Exception {
    Path logPath = tmp.resolve("app.log");
    try (FileLogger logger = new FileLogger(logPath)) {
      logger.error("boom", new RuntimeException("kaboom"));
    }
    String content = Files.readString(logPath);
    assertTrue(content.contains("boom"));
    assertTrue(content.contains("RuntimeException"));
    assertTrue(content.contains("kaboom"));
  }

  @Test
  void appendsToExistingFile() throws Exception {
    Path logPath = tmp.resolve("app.log");
    try (FileLogger logger = new FileLogger(logPath)) {
      logger.info("first");
    }
    try (FileLogger logger = new FileLogger(logPath)) {
      logger.info("second");
    }
    String content = Files.readString(logPath);
    assertTrue(content.contains("first"));
    assertTrue(content.contains("second"));
    assertEquals(2, content.lines().count());
  }
}
