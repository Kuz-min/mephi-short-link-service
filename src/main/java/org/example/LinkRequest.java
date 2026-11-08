package org.example;

import java.net.URI;
import java.time.Instant;

public final class LinkRequest {
    public enum RequestResult {
        SUCCESS,
        NOT_FOUND,
        EXPIRATION_REACHED,
        LIMIT_REACHED,
    }

    private final String code;
    private final Instant createdAt;
    private URI redirectUri;
    private RequestResult requestResult;

    public LinkRequest(String code, Instant createdAt) {
        this.code = code;
        this.createdAt = createdAt;
    }

    public String getCode() {
        return code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public URI getRedirectUri() {
        return redirectUri;
    }

    public RequestResult getRequestResult() {
        return requestResult;
    }

    public void setResult(RequestResult requestResult, URI redirectUri) {
        this.requestResult = requestResult;
        this.redirectUri = redirectUri;
    }
}
