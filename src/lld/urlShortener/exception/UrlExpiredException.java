package lld.urlShortener.exception;

/** Thrown when redirect attempted on a URL past its TTL. Maps to HTTP 410 Gone. */
public class UrlExpiredException extends UrlShortenerException {
    public UrlExpiredException(String code) { super("URL expired: '" + code + "'"); }
}
