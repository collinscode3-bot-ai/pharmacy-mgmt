package org.pharmacy.mgmt.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LoggingInitializer implements ApplicationRunner {

    private final Environment env;

    public LoggingInitializer(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String logPath = env.getProperty("app.logging.path");
        if (logPath != null && !logPath.isBlank()) {
            Path path = Paths.get(logPath);
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }
        }
    }
}
