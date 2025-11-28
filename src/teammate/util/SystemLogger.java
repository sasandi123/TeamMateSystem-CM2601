package teammate.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * System-wide logging utility for tracking operations and errors
 * Week 6-7 Requirement: Add logging
 */
public class SystemLogger {
    private static final String LOG_FILE = "teammate_system.log";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public enum LogLevel {
        INFO, WARNING, ERROR, SUCCESS
    }

    /**
     * Log a message with timestamp and level
     */
    public static synchronized void log(LogLevel level, String message) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);

        // Write to file
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(logEntry);

        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    /**
     * Log info message
     */
    public static void info(String message) {
        log(LogLevel.INFO, message);
    }

    /**
     * Log warning message
     */
    public static void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    /**
     * Log error message
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }

    /**
     * Log success message
     */
    public static void success(String message) {
        log(LogLevel.SUCCESS, message);
    }

    /**
     * Log exception with stack trace
     */
    public static void logException(String context, Exception e) {
        error(context + " - Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
    }

    /**
     * Log file operation (for FileManager)
     */
    public static void logFileOperation(String operation, String filename, boolean success) {
        if (success) {
            info("FILE: " + operation + " - " + filename + " - SUCCESS");
        } else {
            error("FILE: " + operation + " - " + filename + " - FAILED");
        }
    }
}