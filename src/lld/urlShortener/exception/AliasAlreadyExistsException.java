package lld.urlShortener.exception;

public class AliasAlreadyExistsException extends UrlShortenerException {
    public AliasAlreadyExistsException(String alias) {
        super("Custom alias '" + alias + "' is already in use");
    }
}
