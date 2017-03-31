package nz.paulin.spaceflight;

/**
 * Exception encountered when parsing
 */
class ParseException extends Exception {
    ParseException(String message) {
        super(message);
    }

    ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
