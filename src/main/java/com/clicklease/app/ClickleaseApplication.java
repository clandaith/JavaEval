package com.clicklease.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class ClickleaseApplication {

    private static final Logger logger = LoggerFactory.getLogger(ClickleaseApplication.class);

    public static void main(String[] args) {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create data directory for SQLite", e);
        }
        logger.info("DEBUG MSG: Starting Clicklease mini lease application");
        SpringApplication.run(ClickleaseApplication.class, args);
    }
}
