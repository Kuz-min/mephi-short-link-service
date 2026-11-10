package org.example;

import java.time.Duration;
import java.util.Properties;

public final class AppConfig {
    private static final String LINK_EXPIRATION_DELAY_KEY = "link.expirationDelay";

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public Duration getLinkExpirationDelay() {
        var property = properties.getProperty(LINK_EXPIRATION_DELAY_KEY);

        if (property == null || property.isBlank()) {
            return null;
        }

        try {
            return Duration.parse(property.trim());
        } catch (Exception _) {
            return null;
        }
    }

    public static AppConfig load(String resourceName) {
        var properties = new Properties();
        try (var input = AppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception _) {
        }
        return new AppConfig(properties);
    }
}
