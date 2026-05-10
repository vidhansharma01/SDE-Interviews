package lld.bookMyShow.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Show represents a single screening of a movie on a specific screen at a specific time.
 *
 * Key design decision: Show owns the seat-state for THIS show instance.
 * The same physical Screen has different seat availability per show —
 * so Show maintains its own copy of seat states as a Map<seatId, Seat>.
 *
 * SOLID - SRP: Show manages its own seat state, nothing else.
 * SOLID - OCP: New fields (format, subtitles) can be added without breaking consumers.
 *
 * Concurrency: seatMap is a ConcurrentHashMap for thread-safe reads.
 * Writes (lock/confirm/release) are delegated to Seat's synchronized methods.
 */
public class Show {

    private final String                     showId;
    private final Movie                      movie;
    private final Screen                     screen;
    private final LocalDateTime              startTime;
    private final LocalDateTime              endTime;
    private final String                     format;       // "2D", "3D", "IMAX"
    private final String                     language;
    private final Map<String, Seat>          seatMap;      // seatId → Seat for this show
    private final Map<SeatCategory, Double>  priceMap;     // category → price

    private Show(Builder builder) {
        this.showId    = builder.showId;
        this.movie     = builder.movie;
        this.screen    = builder.screen;
        this.startTime = builder.startTime;
        this.endTime   = builder.endTime;
        this.format    = builder.format;
        this.language  = builder.language;
        this.priceMap  = Map.copyOf(builder.priceMap);

        // Clone seats for this show instance — each show has independent seat state
        this.seatMap = new ConcurrentHashMap<>();
        for (Seat seat : builder.screen.getSeats()) {
            double price = priceMap.getOrDefault(seat.getCategory(), seat.getPrice());
            Seat showSeat = new Seat(seat.getSeatId(), seat.getScreenId(),
                    seat.getRowLabel(), seat.getSeatNumber(), seat.getCategory(), price);
            this.seatMap.put(seat.getSeatId(), showSeat);
        }
    }

    // ── Accessors ────────────────────────────────────────────────────

    public String          getShowId()    { return showId; }
    public Movie           getMovie()     { return movie; }
    public Screen          getScreen()    { return screen; }
    public LocalDateTime   getStartTime() { return startTime; }
    public LocalDateTime   getEndTime()   { return endTime; }
    public String          getFormat()    { return format; }
    public String          getLanguage()  { return language; }

    public Seat getSeat(String seatId) {
        return seatMap.get(seatId);
    }

    /** Returns all available seats for this show. */
    public List<Seat> getAvailableSeats() {
        return seatMap.values().stream()
                .filter(Seat::isAvailable)
                .toList();
    }

    /** Count of available seats — used for "Filling Fast" badges. */
    public long getAvailableCount() {
        return seatMap.values().stream()
                .filter(Seat::isAvailable)
                .count();
    }

    public boolean hasStarted() {
        return LocalDateTime.now().isAfter(startTime);
    }

    @Override
    public String toString() {
        return String.format("Show{id='%s', movie='%s', time=%s, available=%d/%d}",
                showId, movie.getTitle(), startTime, getAvailableCount(), seatMap.size());
    }

    // ── Builder ──────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String                    showId;
        private Movie                     movie;
        private Screen                    screen;
        private LocalDateTime             startTime;
        private LocalDateTime             endTime;
        private String                    format   = "2D";
        private String                    language = "Hindi";
        private Map<SeatCategory, Double> priceMap = new ConcurrentHashMap<>();

        public Builder showId(String id)                   { this.showId = id;       return this; }
        public Builder movie(Movie movie)                  { this.movie = movie;     return this; }
        public Builder screen(Screen screen)               { this.screen = screen;   return this; }
        public Builder startTime(LocalDateTime t)          { this.startTime = t;     return this; }
        public Builder endTime(LocalDateTime t)            { this.endTime = t;       return this; }
        public Builder format(String format)               { this.format = format;   return this; }
        public Builder language(String lang)               { this.language = lang;   return this; }
        public Builder priceMap(Map<SeatCategory, Double> p) { this.priceMap = p;   return this; }

        public Show build() {
            if (showId == null || movie == null || screen == null || startTime == null)
                throw new IllegalStateException("showId, movie, screen, startTime are required");
            return new Show(this);
        }
    }
}
