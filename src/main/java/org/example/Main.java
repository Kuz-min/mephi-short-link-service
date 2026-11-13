package org.example;

import java.awt.Desktop;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Consumer;

public final class Main {
    public static void main(String[] args) {
        new ConsoleApp().run();
    }
}

final class ConsoleApp {
    private record Command(
            String name,
            String usage,
            String description,
            Consumer<String[]> action
    ) {
    }

    private final AppConfig appConfig = new AppConfig();
    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService = new UserService();
    private final CurrentUserAccessor currentUser = new CurrentUserAccessor();
    private final LinkService linkService = new LinkService(new CodeGenerator(), currentUser, appConfig);

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private boolean isRunning = true;

    public ConsoleApp() {
        registerCommands();
    }

    public void run() {
        System.out.println("help для списка команд");
        while (isRunning) {
            System.out.println("Текущий пользователь: " + (currentUser.isAuthenticated() ? currentUser.get().getId() : "отсутствует"));
            if (!scanner.hasNextLine()) {
                break;
            }
            var line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            dispatch(line);
        }
    }

    private void registerCommands() {
        addCommand("help", "help", "справка", this::cmdHelp);
        addCommand("newuser", "newuser", "создать нового пользователя", this::cmdNewUser);
        addCommand("login", "login <uuid>", "войти по UUID", this::cmdLogin);
        addCommand("create", "create <uri> [maxVisits]", "создать короткую ссылку", this::cmdCreate);
        addCommand("list", "list", "список ссылок текущего пользователя", this::cmdList);
        addCommand("visit", "visit <code>", "перейти по короткой ссылке", this::cmdVisit);
        addCommand("delete", "delete <code>", "удалить ссылку", this::cmdDelete);
        addCommand("cleanup", "cleanup", "удалить все просроченные ссылки текущего пользователя", this::cmdCleanup);
        addCommand("exit", "exit", "выход", this::cmdExit);
    }

    private void addCommand(String name, String usage, String description, Consumer<String[]> action) {
        commands.put(name, new Command(name, usage, description, action));
    }

    private void dispatch(String line) {
        var parts = line.split("\\s+", 2);
        var commandName = parts[0].toLowerCase();

        var command = commands.get(commandName);
        if (command == null) {
            System.out.println("Неизвестная команда: " + commandName);
            return;
        }

        var args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
        try {
            command.action().accept(args);
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void cmdHelp(String[] args) {
        System.out.println("Доступные команды:");
        for (var c : commands.values()) {
            System.out.printf("  %-32s %s%n", c.usage(), c.description());
        }
    }

    private void cmdNewUser(String[] args) {
        var user = userService.create();
        currentUser.set(user);
        System.out.println("Создан пользователь: " + user.getId());
    }

    private void cmdLogin(String[] args) {
        if (args.length < 1) {
            System.out.println("Использование: login <uuid>");
            return;
        }

        var id = UUID.fromString(args[0]);
        var user = userService.get(id);

        if (user.isEmpty()) {
            throw new RuntimeException("пользователь " + id + " не найден");
        }

        currentUser.set(user.get());
    }

    private void cmdCreate(String[] args) {
        if (args.length < 1) {
            System.out.println("Использование: create <uri> [maxVisits]");
            return;
        }

        var uri = args[0];
        var maxVisits = args.length < 2 ? null : Integer.parseInt(args[1]);

        if (!currentUser.isAuthenticated()) {
            var user = userService.create();
            currentUser.set(user);
            System.out.println("Автоматически создан пользователь: " + user.getId());
        }

        var link = linkService.create(uri, maxVisits);
        System.out.println("Создана ссылка:");
        printLinks(List.of(link));
    }

    private void cmdList(String[] args) {
        var links = linkService.getAll();

        if (links.isEmpty()) {
            System.out.println("Отсутствуют ссылки у пользователя");
        }

        printLinks(links);
    }

    private void cmdVisit(String[] args) {
        if (args.length < 1) {
            System.out.println("Использование: visit <code>");
            return;
        }

        var request = linkService.visit(args[0]);

        switch (request.getRequestResult()) {
            case SUCCESS -> {
                System.out.println("Переход на " + request.getRedirectUri());
                openInBrowser(request.getRedirectUri());
            }
            case NOT_FOUND -> System.out.println("Ссылка не найдена.");
            case LIMIT_REACHED -> System.out.println("Ссылка недоступна: лимит переходов исчерпан.");
            case EXPIRATION_REACHED -> System.out.println("Ссылка недоступна: срок действия истёк.");
        }
    }

    private void cmdDelete(String[] args) {
        if (args.length < 1) {
            System.out.println("Использование: delete <code>");
            return;
        }
        var code = args[0];
        linkService.delete(code);
        System.out.println("Удалено: " + code);
    }

    private void cmdCleanup(String[] args) {
        if (!currentUser.isAuthenticated()) {
            System.out.println("Требуется аутентификация");
            return;
        }

        linkService.cleanupExpired();
        System.out.println("Просроченные ссылки очищены");
    }

    private void cmdExit(String[] args) {
        isRunning = false;
    }

    private void printLinks(List<Link> links) {
        if (links.isEmpty()) {
            return;
        }

        System.out.printf("%-10s %-10s %-40s %s%n", "code", "visits", "expiresAt", "uri");
        for (var link : links) {
            var visits = link.getVisitCount() + "/" + (link.getMaxVisits() == null ? "∞" : link.getMaxVisits());
            System.out.printf("%-10s %-10s %-40s %s%n", link.getCode(), visits, link.getExpiresAt(), link.getOriginalUri());
        }
    }

    private void openInBrowser(URI uri) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                System.out.println("браузер недоступен — откройте вручную: " + uri);
            }
        } catch (Exception e) {
            System.out.println("не удалось открыть браузер: " + e.getMessage());
        }
    }
}