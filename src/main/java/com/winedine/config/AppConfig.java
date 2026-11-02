package com.winedine.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Reads settings from real environment variables first, then from a local .env file. */
public class AppConfig {
    private final Map<String, String> fileValues = new HashMap<>();

    public AppConfig(Path envFile) {
        if (envFile != null && Files.exists(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    String s = line.trim();
                    if (s.isEmpty() || s.startsWith("#") || !s.contains("=")) continue;
                    int i = s.indexOf('=');
                    String key = s.substring(0, i).trim();
                    String value = s.substring(i + 1).trim();
                    if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                            || value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    fileValues.put(key, value);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + envFile, e);
            }
        }
    }

    public static AppConfig load() {
        return new AppConfig(Path.of(".env"));
    }

    public String get(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = fileValues.get(key);
        return (v == null || v.isBlank()) ? null : v;
    }

    public String require(String key) {
        String v = get(key);
        if (v == null) throw new IllegalStateException("Missing setting: " + key + " (set it in .env)");
        return v;
    }
}