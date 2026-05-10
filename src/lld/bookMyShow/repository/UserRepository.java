package lld.bookMyShow.repository;

import lld.bookMyShow.model.User;

import java.util.Optional;

/** Repository abstraction for User persistence. */
public interface UserRepository {
    User           save(User user);
    Optional<User> findById(String userId);
}
