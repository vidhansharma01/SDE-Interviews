package lld.bookMyShow.model;

import java.util.List;
import java.util.ArrayList;

/**
 * A Screen belongs to a Theatre and contains a fixed seat layout.
 * The seat layout is defined once at screen creation and never changes.
 *
 * SOLID - SRP: Screen only knows its own configuration.
 */
public final class Screen {

    private final String     screenId;
    private final String     theatreId;
    private final String     screenName;   // "Screen 1", "IMAX Hall"
    private final int        totalSeats;
    private final List<Seat> seats;        // physical seat layout

    private Screen(Builder builder) {
        this.screenId   = builder.screenId;
        this.theatreId  = builder.theatreId;
        this.screenName = builder.screenName;
        this.seats      = new ArrayList<>(builder.seats);
        this.totalSeats = this.seats.size();
    }

    public String     getScreenId()   { return screenId; }
    public String     getTheatreId()  { return theatreId; }
    public String     getScreenName() { return screenName; }
    public int        getTotalSeats() { return totalSeats; }
    public List<Seat> getSeats()      { return List.copyOf(seats); }

    @Override
    public String toString() {
        return String.format("Screen{id='%s', name='%s', seats=%d}", screenId, screenName, totalSeats);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String     screenId;
        private String     theatreId;
        private String     screenName;
        private List<Seat> seats = new ArrayList<>();

        public Builder screenId(String id)       { this.screenId = id;     return this; }
        public Builder theatreId(String id)      { this.theatreId = id;    return this; }
        public Builder screenName(String name)   { this.screenName = name; return this; }
        public Builder seats(List<Seat> seats)   { this.seats = seats;     return this; }

        public Screen build() {
            if (screenId == null || theatreId == null)
                throw new IllegalStateException("screenId and theatreId are required");
            return new Screen(this);
        }
    }
}
