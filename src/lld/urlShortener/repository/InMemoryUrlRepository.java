package lld.urlShortener.repository;

import lld.urlShortener.model.Url;
import lld.urlShortener.model.UrlStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory URL repository — for testing and LLD demo.
 * Replaced by PostgreSQL/DynamoDB implementation in production with zero service changes.
 *
 * ── Indexes ──────────────────────────────────────────────────────────
 * primaryIndex  : shortCode → Url           (O(1) redirect lookup)
 * hashUserIndex : "hash:userId" → shortCode (O(1) duplicate detection)
 * userIndex     : userId → [shortCode]      (user dashboard queries)
 */
public class InMemoryUrlRepository implements UrlRepository {

    private final Map<String, Url>         primaryIndex  = new ConcurrentHashMap<>();
    private final Map<String, String>      hashUserIndex = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userIndex   = new ConcurrentHashMap<>();

    @Override
    public Url save(Url url) {
        primaryIndex.put(url.getShortCode(), url);
        hashUserIndex.put(hashKey(url.getLongUrlHash(), url.getUserId()), url.getShortCode());
        if (url.getUserId() != null) {
            userIndex.computeIfAbsent(url.getUserId(),
                    k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(url.getShortCode());
        }
        return url;
    }

    @Override
    public Optional<Url> findByShortCode(String shortCode) {
        return Optional.ofNullable(primaryIndex.get(shortCode));
    }

    @Override
    public Optional<Url> findByHashAndUser(String longUrlHash, String userId) {
        String code = hashUserIndex.get(hashKey(longUrlHash, userId));
        if (code == null) return Optional.empty();
        Url url = primaryIndex.get(code);
        // Only return if still active (not deleted/expired)
        if (url != null && url.getStatus() == UrlStatus.ACTIVE && !url.isExpired())
            return Optional.of(url);
        return Optional.empty();
    }

    @Override
    public List<Url> findByUserId(String userId, int limit, int offset) {
        return userIndex.getOrDefault(userId, Collections.emptyList()).stream()
                .skip(offset).limit(limit)
                .map(primaryIndex::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Url::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByShortCode(String shortCode) {
        return primaryIndex.containsKey(shortCode);
    }

    @Override
    public boolean updateStatus(String shortCode, String newStatus) {
        Url url = primaryIndex.get(shortCode);
        if (url == null) return false;
        switch (UrlStatus.valueOf(newStatus)) {
            case EXPIRED -> url.markExpired();
            case DELETED -> url.markDeleted();
            default      -> throw new IllegalArgumentException("Cannot set status: " + newStatus);
        }
        return true;
    }

    private String hashKey(String hash, String userId) {
        return hash + ":" + (userId != null ? userId : "anonymous");
    }
}
