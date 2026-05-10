package lld.bookMyShow.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a movie in the system.
 *
 * Design: Immutable after construction (Builder Pattern).
 * SOLID - SRP: Movie only holds movie metadata, no business logic.
 * SOLID - OCP: Can add new fields via Builder without changing consumers.
 */
public final class Movie {

    private final String       movieId;
    private final String       title;
    private final String       genre;
    private final String       language;
    private final int          durationMinutes;
    private final String       rating;         // "U", "U/A", "A"
    private final List<String> cast;
    private final String       description;

    private Movie(Builder builder) {
        this.movieId         = builder.movieId;
        this.title           = builder.title;
        this.genre           = builder.genre;
        this.language        = builder.language;
        this.durationMinutes = builder.durationMinutes;
        this.rating          = builder.rating;
        this.cast            = List.copyOf(builder.cast);
        this.description     = builder.description;
    }

    public String       getMovieId()         { return movieId; }
    public String       getTitle()           { return title; }
    public String       getGenre()           { return genre; }
    public String       getLanguage()        { return language; }
    public int          getDurationMinutes() { return durationMinutes; }
    public String       getRating()          { return rating; }
    public List<String> getCast()            { return cast; }
    public String       getDescription()     { return description; }

    @Override
    public String toString() {
        return String.format("Movie{id='%s', title='%s', language='%s', duration=%dmin}",
                movieId, title, language, durationMinutes);
    }

    // ── Builder ──────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String       movieId;
        private String       title;
        private String       genre;
        private String       language      = "Hindi";
        private int          durationMinutes;
        private String       rating        = "U/A";
        private List<String> cast          = new ArrayList<>();
        private String       description   = "";

        public Builder movieId(String id)            { this.movieId = id;              return this; }
        public Builder title(String title)           { this.title = title;             return this; }
        public Builder genre(String genre)           { this.genre = genre;             return this; }
        public Builder language(String lang)         { this.language = lang;           return this; }
        public Builder durationMinutes(int mins)     { this.durationMinutes = mins;    return this; }
        public Builder rating(String rating)         { this.rating = rating;           return this; }
        public Builder cast(List<String> cast)       { this.cast = cast;               return this; }
        public Builder description(String desc)      { this.description = desc;        return this; }

        public Movie build() {
            if (movieId == null || title == null)
                throw new IllegalStateException("movieId and title are required");
            return new Movie(this);
        }
    }
}
