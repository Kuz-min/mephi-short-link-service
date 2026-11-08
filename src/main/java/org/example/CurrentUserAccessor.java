package org.example;

import java.util.List;

public final class CurrentUserAccessor {
    private User user;

    public boolean isAuthenticated() {
        return user != null;
    }

    public User get() {
        return user;
    }

    public void set(User user) {
        this.user = user;
    }

    public void clear() {
        this.user = null;
    }
}