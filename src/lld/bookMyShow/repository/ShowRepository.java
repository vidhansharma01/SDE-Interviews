package lld.bookMyShow.repository;

import lld.bookMyShow.model.Show;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for Show persistence.
 * SOLID - DIP: Services depend on this interface, not on ConcurrentHashMap or JPA.
 */
public interface ShowRepository {
    Show       save(Show show);
    Optional<Show> findById(String showId);
    List<Show> findByMovieId(String movieId);
    List<Show> findAll();
}
