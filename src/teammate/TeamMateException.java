// TeamMateException.java
package teammate;

/**
 * Custom exception classes for TeamMate System
 */
public class TeamMateException {

    /**
     * Exception for invalid input data
     */
    public static class InvalidInputException extends Exception {
        public InvalidInputException(String message) {
            super(message);
        }
    }

    /**
     * Exception for duplicate participant entries
     */
    public static class DuplicateParticipantException extends Exception {
        public DuplicateParticipantException(String message) {
            super(message);
        }
    }

    /**
     * Exception for file reading errors
     */
    public static class FileReadException extends Exception {
        public FileReadException(String message) {
            super(message);
        }
    }

    /**
     * Exception for file writing errors
     */
    public static class FileWriteException extends Exception {
        public FileWriteException(String message) {
            super(message);
        }
    }
}