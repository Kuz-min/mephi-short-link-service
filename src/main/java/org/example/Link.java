package org.example;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class Link {
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final String code;
    private final URI originalUri;
    private final UUID ownerId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Integer maxVisits;
    private int visitCount;

    public Link(String code,
                URI originalUri,
                UUID ownerId,
                Instant createdAt,
                Instant expiresAt,
                Integer maxVisits) {
        if (originalUri.getScheme() == null || !ALLOWED_SCHEMES.contains(originalUri.getScheme().toLowerCase())) {
            throw new RuntimeException("URL должен использовать схему http или https");
        }

        if (originalUri.getHost() == null || originalUri.getHost().isBlank()) {
            throw new RuntimeException("URL должен содержать хост");
        }

        if (maxVisits != null && maxVisits <= 0) {
            throw new RuntimeException("Количество переходов по ссылке должен быть положительным числом или отсутствовать");
        }

        this.code = code;
        this.originalUri = originalUri;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.maxVisits = maxVisits;
        this.visitCount = 0;
    }

    public String getCode() {
        return code;
    }

    public URI getOriginalUri() {
        return originalUri;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Integer getMaxVisits() {
        return maxVisits;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public boolean isExpirationReached(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isLimitReached() {
        return maxVisits != null && visitCount >= maxVisits;
    }

    public synchronized void visit(LinkRequest request) {
        if (isLimitReached()) {
            request.setResult(LinkRequest.RequestResult.LIMIT_REACHED, null);
            return;
        }

        if (isExpirationReached(request.getCreatedAt())) {
            request.setResult(LinkRequest.RequestResult.EXPIRATION_REACHED, null);
            return;
        }

        visitCount++;
        request.setResult(LinkRequest.RequestResult.SUCCESS, originalUri);
    }
}
