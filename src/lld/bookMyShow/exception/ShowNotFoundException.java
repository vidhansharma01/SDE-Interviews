package lld.bookMyShow.exception;

public class ShowNotFoundException extends BookMyShowException {
    public ShowNotFoundException(String showId) {
        super("Show not found: '" + showId + "'");
    }
}
