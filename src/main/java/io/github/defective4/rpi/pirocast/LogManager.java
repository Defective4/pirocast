package io.github.defective4.rpi.pirocast;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class LogManager {

    public enum LogLevel {
        ALL(2), ERRORS(1), OFF(0);

        private final int level;

        private LogLevel(int level) {
            this.level = level;
        }

        public boolean moreThan(LogLevel level) {
            return this.level >= level.level;
        }
    }

    private static File logDir = new File("logs");
    private static LogLevel logLevel = LogLevel.OFF;

    public static File getLogDir() {
        logDir.mkdirs();
        return logDir;
    }

    public static LogLevel getLogLevel() {
        return logLevel;
    }

    public static PrintWriter prepareLogWriter(String name, LogLevel level) {
        if (logLevel.moreThan(level)) {
            File target = getFileForName(name, level);
            try {
                return new PrintWriter(new FileWriter(target, StandardCharsets.UTF_8), true);
            } catch (Exception e) {}
        }
        return new PrintWriter(OutputStream.nullOutputStream(), true);
    }

    public static ProcessBuilder redirectProcess(ProcessBuilder builder, String name, LogLevel level) {
        if (logLevel.moreThan(level)) {
            File target = getFileForName(name, level);
            return logLevel == LogLevel.ERRORS ? builder.redirectError(target) : builder.redirectOutput(target);
        }
        return builder;
    }

    public static void setLogDir(File logDir) {
        Objects.requireNonNull(logDir);
        LogManager.logDir = logDir;
    }

    public static void setLogLevel(LogLevel logLevel) {
        Objects.requireNonNull(logLevel);
        LogManager.logLevel = logLevel;
    }

    private static File getFileForName(String name, LogLevel level) {
        if (level == LogLevel.ERRORS) name += ".error";
        return new File(getLogDir(), name + ".log");
    }
}
