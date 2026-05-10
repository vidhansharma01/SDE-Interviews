package lld.urlShortener.exception;

public class UrlNotFoundException extends UrlShortenerException {
    public UrlNotFoundException(String code) { super("Short URL not found: '" + code + "'"); }
}
