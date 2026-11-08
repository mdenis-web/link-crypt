package com.example.shortener.core.port;

public interface AppLogger {
    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable t);
}
