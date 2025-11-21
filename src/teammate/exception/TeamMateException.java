package teammate.exception;

/**
 * Custom exception classes for TeamMate System
 */
public class TeamMateException {

    public static class InvalidInputException extends Exception {
        public InvalidInputException(String message) {
            super(message);
        }
    }

    public static class DuplicateParticipantException extends Exception {
        public DuplicateParticipantException(String message) {
            super(message);
        }
    }

    public static class FileReadException extends Exception {
        public FileReadException(String message) {
            super(message);
        }
    }

    public static class FileWriteException extends Exception {
        public FileWriteException(String message) {
            super(message);
        }
    }
}