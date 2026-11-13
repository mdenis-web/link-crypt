package com.linkcrypt.ui;

import com.linkcrypt.config.AppConfig;
import com.linkcrypt.core.domain.ShortLink;
import com.linkcrypt.core.domain.User;
import com.linkcrypt.core.exception.*;
import com.linkcrypt.core.port.AppLogger;
import com.linkcrypt.core.service.LinkService;
import com.linkcrypt.core.service.UserService;
import java.awt.Desktop;
import java.io.PrintStream;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class CommandLineApp {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

  private final LinkService linkService;
  private final UserService userService;
  private final AppConfig config;
  private final Scanner scanner;
  private final PrintStream out;
  private final AppLogger logger;
  private UUID currentUser;

  public CommandLineApp(
      LinkService linkService,
      UserService userService,
      AppConfig config,
      Scanner scanner,
      PrintStream out,
      AppLogger logger) {
    this.linkService = linkService;
    this.userService = userService;
    this.config = config;
    this.scanner = scanner;
    this.out = out;
    this.logger = logger;
  }

  public void run() {
    logger.info("CLI started");
    printWelcome();
    boolean running = true;
    while (running) {
      out.print(prompt());
      out.flush();
      if (!scanner.hasNextLine()) {
        out.println("EOF received — shutting down");
        logger.info("EOF received — shutting down");
        break;
      }

      String line = scanner.nextLine().trim();
      if (line.isEmpty()) {
        continue;
      }

      logger.info("command: user=" + currentUser + " line='" + line + "'");
      try {
        running = handle(line);
      } catch (RuntimeException exception) {
        if (isBusinessError(exception)) {
          logger.warn(
              "command rejected: user="
                  + currentUser
                  + " cmd='"
                  + line
                  + "': "
                  + exception.getMessage());
        } else {
          logger.error(
              "command failed unexpectedly: user=" + currentUser + " cmd='" + line + "'",
              exception);
        }
        out.println("Error: " + exception.getMessage());
      }
    }
    logger.info("CLI stopped");
    out.println("Good Bye!!!");
  }

  private boolean isBusinessError(RuntimeException exception) {
    return exception instanceof ValidationException
        || exception instanceof LinkNotFoundException
        || exception instanceof LimitExceededException
        || exception instanceof LinkExpiredException
        || exception instanceof UnauthorizedException
        || exception instanceof IllegalArgumentException;
  }

  private void printWelcome() {
    out.println("=== Link Crypt CLI ===");
    out.println("Type 'help' to list commands. Logs: " + config.getLogFilePath());
    out.println("Your session UUID: " + (currentUser == null ? "(not logged in)" : currentUser));
  }

  private String prompt() {
    return "[" + (currentUser == null ? "guest" : currentUser.toString().substring(0, 8)) + "]> ";
  }

  private boolean handle(String line) {
    String[] parts = line.split("\\s+");
    String command = parts[0].toLowerCase();
    switch (command) {
      case "help":
        printHelp();
        return true;
      case "exit":
      case "quit":
        return false;
      case "register":
        cmdRegister();
        return true;
      case "login":
        cmdLogin(parts);
        return true;
      case "whoami":
        cmdWhoAmI();
        return true;
      case "create":
        cmdCreate(parts);
        return true;
      case "list":
        cmdList();
        return true;
      case "info":
        cmdInfo(parts);
        return true;
      case "edit":
        cmdEdit(parts);
        return true;
      case "delete":
        cmdDelete(parts);
        return true;
      case "open":
        cmdOpen(parts);
        return true;
      default:
        out.println("Unknown command: " + command + ". Type 'help'.");
        return true;
    }
  }

  private void printHelp() {
    out.println("Commands:");
    out.println("  register                                 create a new user and become it");
    out.println("  login <uuid>                             switch to existing user");
    out.println("  whoami                                   show current user");
    out.println("  create <url> [--limit N]                 shorten URL (creates user if none)");
    out.println("  list                                     list your links");
    out.println("  info <short_url | code>                  show link details");
    out.println("  edit <short_url | code> --limit N        change click limit (owner only)");
    out.println("  delete <short_url | code>                delete link (owner only)");
    out.println(
        "  open <short_url | code>                  open link in browser (counts as a click)");
    out.println("  help                                     show this help");
    out.println("  exit | quit                              leave");
  }

  private void cmdRegister() {
    User user = userService.register();
    currentUser = user.getId();
    out.println("Registered new user. UUID: " + currentUser);
    out.println("Use 'login <uuid>' to restore your session.");
  }

  private void cmdLogin(String[] parts) {
    if (parts.length < 2) {
      out.println("Supported format: login <uuid>");
      return;
    }

    UUID id;
    try {
      id = UUID.fromString(parts[1]);
    } catch (IllegalArgumentException e) {
      out.println("Invalid UUID format.");
      return;
    }

    if (!userService.exists(id)) {
      logger.warn("login failed: unknown user " + id);
      out.println("No such user: " + id);
      return;
    }

    currentUser = id;
    logger.info("login: " + id);
    out.println("Logged in as: " + currentUser);
  }

  private void cmdWhoAmI() {
    out.println(
        currentUser == null
            ? "Not logged in. Use login <uuid> to log in"
            : "Current user: " + currentUser);
  }

  private void cmdCreate(String[] parts) {
    if (parts.length < 2) {
      out.println("Supported format: create <url> [--limit N]");
      return;
    }

    String url = parts[1];
    Integer limit = parseLimitFlag(parts, 2);
    if (currentUser == null) {
      User user = userService.register();
      currentUser = user.getId();
      out.println("Auto-registered user. UUID = " + currentUser);
    }

    ShortLink link = linkService.getByUrl(currentUser, url);
    if (link == null) {
      link = linkService.create(currentUser, url, limit);
    }

    out.println("Short code : " + link.getCode());
    out.println("Short URL  : " + config.getBaseUrl() + link.getCode());
    out.println("Max clicks : " + link.getMaxClicks());
    out.println("Expires at : " + FMT.format(link.getExpiresAt()));
  }

  private void cmdList() {
    requireUser();
    List<ShortLink> links = linkService.listByUser(currentUser);
    if (links.isEmpty()) {
      out.println("You have no links yet.");
      return;
    }

    out.printf("%-10s %-40s %-6s %-6s %-22s%n", "CODE", "URL", "CLICKS", "LIMIT", "EXPIRES");
    links.forEach(
        link ->
            out.printf(
                "%-10s %-40s %-6d %-6d %-22s%n",
                link.getCode(),
                truncate(link.getOriginalUrl(), 40),
                link.getClickCount(),
                link.getMaxClicks(),
                FMT.format(link.getExpiresAt())));
  }

  private void cmdInfo(String[] parts) {
    if (parts.length < 2) {
      out.println("Usage: info <short_url | code>");
      return;
    }

    String code = config.extractCode(parts[1]);
    ShortLink link = linkService.getByCode(code);
    out.println("Short URL  : " + config.getBaseUrl() + link.getCode());
    out.println("Code      : " + link.getCode());
    out.println("Owner     : " + link.getOwnerId());
    out.println("Original  : " + link.getOriginalUrl());
    out.println("Created   : " + FMT.format(link.getCreatedAt()));
    out.println("Expires   : " + FMT.format(link.getExpiresAt()));
    out.println("Clicks    : " + link.getClickCount() + " / " + link.getMaxClicks());
  }

  private void cmdEdit(String[] parts) {
    requireUser();
    if (parts.length < 4) {
      out.println("Usage: edit <short_url | code> --limit N");
      return;
    }

    String code = config.extractCode(parts[1]);
    Integer limit = parseLimitFlag(parts, 2);
    if (limit == null) {
      out.println("Missing --limit N");
      return;
    }

    linkService.updateMaxClicks(code, currentUser, limit);
    out.println("Click limit updated to " + limit + " for " + code);
  }

  private void cmdDelete(String[] parts) {
    requireUser();
    if (parts.length < 2) {
      out.println("Usage: delete <short_url | code>");
      return;
    }

    String code = config.extractCode(parts[1]);
    linkService.delete(code, currentUser);
    out.println("Link " + parts[1] + " deleted.");
  }

  private void cmdOpen(String[] parts) {
    requireUser();
    if (parts.length < 2) {
      out.println("Usage: open <short_url | code>");
      return;
    }

    String code = config.extractCode(parts[1]);
    String url = linkService.open(code, currentUser);
    out.println("Opening: " + url);
    openInBrowser(url);
  }

  private void openInBrowser(String url) {
    try {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(new URI(url));
        logger.info("browser opened for url=" + url);
      } else {
        logger.warn("desktop not supported, url printed only: " + url);
        out.println("(Desktop API not available — open the URL manually.)");
      }
    } catch (Exception exception) {
      logger.error("failed to open browser for url=" + url, exception);
      out.println("(Could not open browser: " + exception.getMessage() + " — open manually.)");
    }
  }

  private void requireUser() {
    if (currentUser == null) {
      throw new UnauthorizedException("Please 'login' or 'register' first.");
    }
  }

  private Integer parseLimitFlag(String[] parts, int start) {
    for (int index = start; index < parts.length - 1; index++) {
      if ("--limit".equals(parts[index])) {
        try {
          return Integer.parseInt(parts[index + 1]);
        } catch (NumberFormatException exception) {
          throw new ValidationException("--limit requires a number");
        }
      }
    }
    return null;
  }

  private static String truncate(String text, int len) {
    return text.length() <= len ? text : text.substring(0, len - 1) + "...";
  }
}
