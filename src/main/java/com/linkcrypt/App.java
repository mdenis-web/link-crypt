package com.linkcrypt;

import com.linkcrypt.config.AppConfig;
import com.linkcrypt.config.ConfigLoader;
import com.linkcrypt.core.port.*;
import com.linkcrypt.core.port.Clock;
import com.linkcrypt.core.port.LinkRepository;
import com.linkcrypt.core.port.NotificationService;
import com.linkcrypt.core.port.UserRepository;
import com.linkcrypt.core.service.*;
import com.linkcrypt.core.service.LinkExpirationScheduler;
import com.linkcrypt.core.service.LinkService;
import com.linkcrypt.core.service.ShortCodeGenerator;
import com.linkcrypt.core.service.UserService;
import com.linkcrypt.infra.*;
import com.linkcrypt.ui.CommandLineApp;
import java.nio.file.Path;
import java.util.Scanner;

public class App {
  public static void main(String[] args) throws Exception {
    Path configPath = Path.of(args.length > 0 ? args[0] : "application.properties");
    AppConfig config = ConfigLoader.load(configPath);

    try (FileLogger logger = new FileLogger(Path.of(config.getLogFilePath()))) {
      logger.info("application starting; config=" + configPath.toAbsolutePath());

      Clock clock = new SystemClock();
      UserRepository userRepo = new InMemoryUserRepository();
      LinkRepository linkRepo = new InMemoryLinkRepository();
      NotificationService notifier = new ConsoleNotificationService(System.out, logger);
      ShortCodeGenerator codeGen = new ShortCodeGenerator(config.getCodeLength());

      UserService userService = new UserService(userRepo, logger);
      LinkService linkService =
          new LinkService(linkRepo, userRepo, notifier, clock, config, codeGen, logger);

      try (LinkExpirationScheduler scheduler =
          new LinkExpirationScheduler(
              linkRepo, notifier, clock, config.getCleanupInterval(), logger)) {
        scheduler.start();
        CommandLineApp cli =
            new CommandLineApp(
                linkService, userService, config, new Scanner(System.in), System.out, logger);
        cli.run();
      }

      logger.info("application stopped");
    }
  }
}
