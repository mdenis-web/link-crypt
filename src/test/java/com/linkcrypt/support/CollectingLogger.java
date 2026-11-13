package com.linkcrypt.support;

import com.linkcrypt.core.port.AppLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CollectingLogger implements AppLogger {

  public record Entry(String level, String message) {}

  private final List<Entry> entries = new CopyOnWriteArrayList<>();

  @Override
  public void info(String message) {
    entries.add(new Entry("INFO", message));
  }

  @Override
  public void warn(String message) {
    entries.add(new Entry("WARN", message));
  }

  @Override
  public void error(String message) {
    entries.add(new Entry("ERROR", message));
  }

  @Override
  public void error(String message, Throwable throwable) {
    entries.add(new Entry("ERROR", message + " | " + throwable));
  }

  public List<Entry> all() {
    return new ArrayList<>(entries);
  }

  public boolean containsLevel(String level) {
    return entries.stream().anyMatch(entry -> entry.level().equals(level));
  }

  public boolean containsMessage(String sub) {
    return entries.stream().anyMatch(entry -> entry.message().contains(sub));
  }

  public void clear() {
    entries.clear();
  }
}
