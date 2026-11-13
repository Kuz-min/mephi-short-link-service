package org.example;

import java.time.Duration;

public final class AppConfig {
    private static final String LINK_EXPIRATION_DELAY_ENV = "LINK_EXPIRATION_DELAY";

    public Duration getLinkExpirationDelay() {
        var linkExpirationDelay = System.getenv(LINK_EXPIRATION_DELAY_ENV);

        if (linkExpirationDelay == null || linkExpirationDelay.isBlank()) {
            return null;
        }

        try {
            return Duration.parse(linkExpirationDelay.trim());
        } catch (Exception _) {
            return null;
        }
    }
}
