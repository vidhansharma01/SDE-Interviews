package lld.bookMyShow.repository.impl;

import lld.bookMyShow.model.Booking;
import lld.bookMyShow.repository.BookingRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory BookingRepository for testing and LLD demo.
 *
 * Uses two indexes for O(1) lookups:
 *  - primaryIndex : bookingId → Booking  (main store)
 *  - userIndex    : userId → [bookingId] (for user history queries)
 */
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking>       primaryIndex = new ConcurrentHashMap<>();
    private final Map<String, List<String>>  userIndex    = new ConcurrentHashMap<>();
    private final Map<String, List<String>>  showIndex    = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        primaryIndex.put(booking.getBookingId(), booking);
        userIndex.computeIfAbsent(booking.getUserId(),
                k -> Collections.synchronizedList(new ArrayList<>()))
                .add(booking.getBookingId());
        showIndex.computeIfAbsent(booking.getShowId(),
                k -> Collections.synchronizedList(new ArrayList<>()))
                .add(booking.getBookingId());
        return booking;
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(primaryIndex.get(bookingId));
    }

    @Override
    public List<Booking> findByUserId(String userId) {
        return userIndex.getOrDefault(userId, Collections.emptyList())
                .stream()
                .map(primaryIndex::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByShowId(String showId) {
        return showIndex.getOrDefault(showId, Collections.emptyList())
                .stream()
                .map(primaryIndex::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
