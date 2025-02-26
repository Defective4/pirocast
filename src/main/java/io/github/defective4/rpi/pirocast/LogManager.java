package io.github.defective4.rpi.pirocast;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

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
    private static boolean logArchive;

    public static boolean isLogArchivingEnabled() {
        return logArchive;
    }

    public static void setLogArchiving(boolean enabled) {
        LogManager.logArchive = enabled;
    }

    public static File getLogDir() {
        logDir.mkdirs();
        return logDir;
    }

    public static LogLevel getLogLevel() {
        return logLevel;
    }

    public static PrintWriter prepareLogWriter(String name, LogLevel level) {
        if (logLevel.moreThan(level)) {
            File target = prepareFileForName(name, level);
            try {
                return new PrintWriter(new FileWriter(target, StandardCharsets.UTF_8), true);
            } catch (Exception e) {}
        }
        return new PrintWriter(OutputStream.nullOutputStream(), true);
    }

    public static ProcessBuilder redirectProcess(ProcessBuilder builder, String name, LogLevel level) {
        if (logLevel.moreThan(level)) {
            File target = prepareFileForName(name, level);
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

    private static File prepareFileForName(String name, LogLevel level) {
        if (level == LogLevel.ERRORS) name += ".error";
        File logFile = new File(getLogDir(), name + ".log");
        if (logFile.isFile() && logArchive) {
            long time = System.currentTimeMillis();
            try {
                time = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();
            } catch (Exception e) {
                e.printStackTrace();
            }
            File archiveFile = new File(logFile.getParentFile(),
                    logFile.getName() + "." + new SimpleDateFormat("yy_MM_dd-HH_mm_ss").format(new Date(time)) + ".gz");
            try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(archiveFile.toPath()))) {
                Files.copy(logFile.toPath(), os);
                logFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return logFile;
    }
}
