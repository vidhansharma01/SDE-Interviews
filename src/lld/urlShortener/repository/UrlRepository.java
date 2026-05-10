package lld.urlShortener.repository;

import lld.urlShortener.model.Url;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction — isolates domain model from persistence mechanism.
 * SOLID - DIP: UrlShortenerServiceImpl depends on this interface.
 */
public interface UrlRepository {
    Url               save(Url url);
    Optional<Url>     findByShortCode(String shortCode);
    Optional<Url>     findByHashAndUser(String longUrlHash, String userId);  // dedup
    List<Url>         findByUserId(String userId, int limit, int offset);
    boolean           existsByShortCode(String shortCode);
    boolean           updateStatus(String shortCode, String newStatus);
}
