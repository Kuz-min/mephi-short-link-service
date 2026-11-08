package org.example;

import java.util.UUID;

public final class User {
    private final UUID id;

    public User(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
