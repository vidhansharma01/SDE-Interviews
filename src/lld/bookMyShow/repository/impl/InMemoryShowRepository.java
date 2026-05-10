package lld.bookMyShow.repository.impl;

import lld.bookMyShow.model.Show;
import lld.bookMyShow.repository.ShowRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory ShowRepository for testing and LLD demo.
 * In production, replaced by JPA/JDBC implementation.
 * SOLID - LSP: Fully substitutable for ShowRepository.
 */
public class InMemoryShowRepository implements ShowRepository {

    private final Map<String, Show> store = new ConcurrentHashMap<>();

    @Override
    public Show save(Show show) {
        store.put(show.getShowId(), show);
        return show;
    }

    @Override
    public Optional<Show> findById(String showId) {
        return Optional.ofNullable(store.get(showId));
    }

    @Override
    public List<Show> findByMovieId(String movieId) {
        return store.values().stream()
                .filter(s -> s.getMovie().getMovieId().equals(movieId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Show> findAll() {
        return new ArrayList<>(store.values());
    }
}
