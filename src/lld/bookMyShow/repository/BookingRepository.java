package lld.bookMyShow.repository;

import lld.bookMyShow.model.Booking;

import java.util.List;
import java.util.Optional;

/** Repository abstraction for Booking persistence. */
public interface BookingRepository {
    Booking            save(Booking booking);
    Optional<Booking>  findById(String bookingId);
    List<Booking>      findByUserId(String userId);
    List<Booking>      findByShowId(String showId);
}
