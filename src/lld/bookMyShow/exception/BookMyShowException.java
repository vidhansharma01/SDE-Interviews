package lld.bookMyShow.exception;

/** Base exception for all BookMyShow domain errors. */
public class BookMyShowException extends RuntimeException {
    public BookMyShowException(String message) { super(message); }
    public BookMyShowException(String message, Throwable cause) { super(message, cause); }
}
