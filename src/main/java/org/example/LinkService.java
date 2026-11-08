package org.example;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class LinkService {
    private static final Duration DEFAULT_EXPIRATION_DELAY = Duration.ofHours(24);

    private final CodeGenerator codeGenerator;
    private final CurrentUserAccessor currentUserAccessor;
    private final ConcurrentHashMap<String, Link> links = new ConcurrentHashMap<>();
    private final Duration expirationDelay;

    public LinkService(
            CodeGenerator codeGenerator,
            CurrentUserAccessor currentUserAccessor,
            AppConfig appConfig) {
        this.codeGenerator = codeGenerator;
        this.currentUserAccessor = currentUserAccessor;
        this.expirationDelay = Objects.requireNonNullElse(appConfig.getLinkExpirationDelay(), DEFAULT_EXPIRATION_DELAY);
    }

    public List<Link> getAll() {
        var ownerId = currentUserAccessor.get().getId();
        return links.values().stream()
                .filter(link -> ownerId.equals(link.getOwnerId()))
                .toList();
    }

    public Link create(String ruri, Integer maxVisits) {
        var user = currentUserAccessor.get();
        if (user == null) {
            throw new RuntimeException("Отсутствует текущий пользователь");
        }

        URI uri;
        try {
            uri = new URI(ruri);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Некорректный URL: " + ruri, ex);
        }

        var code = Stream.generate(codeGenerator::next)
                .dropWhile(links::containsKey)
                .findFirst()
                .orElseThrow();

        var now = Instant.now();
        var link = new Link(code, uri, user.getId(), now, now.plus(expirationDelay), maxVisits);
        links.put(link.getCode(), link);
        return link;
    }

    public void delete(String code) {
        var user = currentUserAccessor.get();
        var link = links.get(code);

        if (link == null) {
            throw new RuntimeException("Не найдена ссылка с идентификатором: " + code);
        }

        if (!link.getOwnerId().equals(user.getId())) {
            throw new RuntimeException("Удалять ссылку может только её создатель");
        }

        links.remove(code, link);
    }

    public LinkRequest visit(String code) {
        var linkRequest = new LinkRequest(code, Instant.now());

        var link = links.get(code);
        if (link == null) {
            linkRequest.setResult(LinkRequest.RequestResult.NOT_FOUND, null);
            return linkRequest;
        }

        link.visit(linkRequest);
        return linkRequest;
    }

    public void cleanupExpired() {
        var userId = currentUserAccessor.get().getId();
        var now = Instant.now();

        for (var link : links.values()) {
            if (userId.equals(link.getOwnerId()) && link.isExpirationReached(now)) {
                links.remove(link.getCode());
            }
        }
    }
}
