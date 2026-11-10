package com.winedine.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application settings. Values come from a local {@code .env} file (never committed) and from
 * environment variables, which take priority. Real API keys must only live there.
 */
public final class AppConfig {
    public static final String KEY_API_KEY = "LLM_API_KEY";
    public static final String KEY_MODEL = "LLM_MODEL";
    public static final String KEY_DATASET = "DATASET_PATH";
    public static final String KEY_HISTORY = "HISTORY_PATH";
    public static final String KEY_USE_MOCK = "USE_MOCK_LLM";

    public static final String DEFAULT_MODEL = "gpt-4o-mini";
    public static final String DEFAULT_DATASET = "data/restaurants.json";
    public static final String DEFAULT_HISTORY = "data/history.json";

    private final Map<String, String> values;

    private AppConfig(Map<String, String> values) {
        this.values = new HashMap<>(values);
    }

    /** Loads {@code .env} from the working directory (if present), then environment variables. */
    public static AppConfig load() {
        return load(Path.of(".env"));
    }

    public static AppConfig load(Path envFile) {
        Map<String, String> merged = new HashMap<>();
        if (envFile != null && Files.isRegularFile(envFile)) {
            try {
                List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
                merged.putAll(parseLines(lines));
            } catch (IOException e) {
                throw new IllegalStateException("Could not read config file: " + envFile, e);
            }
        }
        for (String key : new String[]{KEY_API_KEY, KEY_MODEL, KEY_DATASET, KEY_HISTORY, KEY_USE_MOCK}) {
            String fromEnv = System.getenv(key);
            if (fromEnv != null && !fromEnv.isBlank()) {
                merged.put(key, fromEnv.trim());
            }
        }
        return new AppConfig(merged);
    }

    /** Builds a config from explicit values (used by tests). */
    public static AppConfig fromMap(Map<String, String> values) {
        return new AppConfig(values);
    }

    /** Parses KEY=VALUE lines. Blank lines and lines starting with # are ignored. */
    static Map<String, String> parseLines(List<String> lines) {
        Map<String, String> result = new HashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            result.put(key, value);
        }
        return result;
    }

    public String getLlmApiKey() { return values.getOrDefault(KEY_API_KEY, ""); }

    /** True if a real key is configured (the placeholder from .env.example does not count). */
    public boolean hasApiKey() {
        String key = getLlmApiKey();
        return !key.isBlank() && !key.startsWith("YOUR_");
    }

    public String getLlmModel() { return values.getOrDefault(KEY_MODEL, DEFAULT_MODEL); }
    public String getDatasetPath() { return values.getOrDefault(KEY_DATASET, DEFAULT_DATASET); }
    public String getHistoryPath() { return values.getOrDefault(KEY_HISTORY, DEFAULT_HISTORY); }

    public boolean isUseMockLlm() {
        return "true".equalsIgnoreCase(values.getOrDefault(KEY_USE_MOCK, "false"));
    }

    /** Never prints the API key. */
    @Override
    public String toString() {
        return "AppConfig{model=" + getLlmModel() + ", dataset=" + getDatasetPath()
                + ", history=" + getHistoryPath() + ", apiKeySet=" + hasApiKey()
                + ", mockLlm=" + isUseMockLlm() + "}";
    }
}
