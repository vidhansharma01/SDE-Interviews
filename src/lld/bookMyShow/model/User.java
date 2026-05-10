package lld.bookMyShow.model;

/**
 * User entity — the customer interacting with BookMyShow.
 * Immutable after creation.
 */
public final class User {

    private final String userId;
    private final String name;
    private final String email;
    private final String phone;

    public User(String userId, String name, String email, String phone) {
        if (userId == null || email == null)
            throw new IllegalArgumentException("userId and email are required");
        this.userId = userId;
        this.name   = name;
        this.email  = email;
        this.phone  = phone;
    }

    public String getUserId() { return userId; }
    public String getName()   { return name; }
    public String getEmail()  { return email; }
    public String getPhone()  { return phone; }

    @Override
    public String toString() {
        return String.format("User{id='%s', name='%s', email='%s'}", userId, name, email);
    }
}
