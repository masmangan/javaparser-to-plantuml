package io.github.masmangan.assis.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

final class JulLogCaptor implements AutoCloseable {
  private final Logger logger;
  private final Handler handler;

  private final List<LogRecord> records = new ArrayList<>();

  private final Level oldLevel;
  private final boolean oldUseParentHandlers;
  private final Handler[] oldHandlers;

  JulLogCaptor(Class<?> loggerOwner) {
    this(Logger.getLogger(loggerOwner.getName()));
  }

  JulLogCaptor(Logger logger) {
    this.logger = logger;

    // Snapshot old state so we can restore it safely
    this.oldLevel = logger.getLevel();
    this.oldUseParentHandlers = logger.getUseParentHandlers();
    this.oldHandlers = logger.getHandlers();

    // A handler that collects records in-memory
    this.handler = new Handler() {
      @Override public void publish(LogRecord record) {
        if (record != null) records.add(record);
      }
      @Override public void flush() {}
      @Override public void close() throws SecurityException {}
    };

    // Make sure we see CONFIG/INFO/SEVERE regardless of environment defaults
    logger.setUseParentHandlers(false);
    for (Handler h : oldHandlers) {
      logger.removeHandler(h);
    }
    logger.addHandler(handler);
    logger.setLevel(Level.ALL);
    handler.setLevel(Level.ALL);
  }

  List<LogRecord> records() { return records; }

  List<String> messages() {
    List<String> msgs = new ArrayList<>();
    for (LogRecord r : records) {
      msgs.add(r.getMessage());
    }
    return msgs;
  }

  boolean any(Level level, String contains) {
    String needle = contains.toLowerCase();
    for (LogRecord r : records) {
      if (r.getLevel().intValue() == level.intValue()) {
        String msg = String.valueOf(r.getMessage());
        if (msg.toLowerCase().contains(needle)) return true;
      }
    }
    return false;
  }

  String dump() {
    StringBuilder sb = new StringBuilder();
    for (LogRecord r : records) {
      sb.append(r.getLevel()).append(": ").append(r.getMessage()).append("\n");
    }
    return sb.toString();
  }

  @Override public void close() {
    logger.removeHandler(handler);
    // restore
    for (Handler h : oldHandlers) {
      logger.addHandler(h);
    }
    logger.setUseParentHandlers(oldUseParentHandlers);
    logger.setLevel(oldLevel);
  }
}