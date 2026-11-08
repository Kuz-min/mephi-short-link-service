package org.example;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UserService {
    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();

    public User create() {
        var user = new User(UUID.randomUUID());
        users.put(user.getId(), user);
        return user;
    }

    public Optional<User> get(UUID id) {
        return Optional.ofNullable(users.get(id));
    }
}
