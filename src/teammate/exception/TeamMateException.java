package teammate.exception;

// Custom exception classes for TeamMate system
public class TeamMateException {

    // Thrown when user input is invalid
    public static class InvalidInputException extends Exception {
        public InvalidInputException(String message) {
            super(message);
        }
    }

    // Thrown when attempting to add duplicate participant
    public static class DuplicateParticipantException extends Exception {
        public DuplicateParticipantException(String message) {
            super(message);
        }
    }

    // Thrown when file reading fails
    public static class FileReadException extends Exception {
        public FileReadException(String message) {
            super(message);
        }
    }

    // Thrown when file writing fails
    public static class FileWriteException extends Exception {
        public FileWriteException(String message) {
            super(message);
        }
    }

    // Thrown when participant is already assigned to a team
    public static class ParticipantAlreadyAssignedException extends Exception {
        public ParticipantAlreadyAssignedException(String message) {
            super(message);
        }
    }
}