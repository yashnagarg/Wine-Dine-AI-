package com.winedine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void parsesKeyValueLinesAndIgnoresCommentsAndBlankLines() {
        Map<String, String> parsed = AppConfig.parseLines(List.of(
                "# comment", "", "LLM_API_KEY=abc123", "LLM_MODEL = \"my-model\"", "BROKEN LINE"));
        assertEquals("abc123", parsed.get("LLM_API_KEY"));
        assertEquals("my-model", parsed.get("LLM_MODEL"));
        assertEquals(2, parsed.size());
    }

    @Test
    void defaultsApplyWhenNothingIsConfigured() {
        AppConfig config = AppConfig.fromMap(Map.of());
        assertEquals(AppConfig.DEFAULT_MODEL, config.getLlmModel());
        assertEquals(AppConfig.DEFAULT_DATASET, config.getDatasetPath());
        assertEquals(AppConfig.DEFAULT_HISTORY, config.getHistoryPath());
        assertFalse(config.hasApiKey());
        assertFalse(config.isUseMockLlm());
    }

    @Test
    void placeholderKeyDoesNotCountAsARealKey() {
        assertFalse(AppConfig.fromMap(Map.of("LLM_API_KEY", "YOUR_LOCAL_API_KEY_HERE")).hasApiKey());
        assertTrue(AppConfig.fromMap(Map.of("LLM_API_KEY", "sk-real")).hasApiKey());
    }

    @Test
    void mockFlagIsReadFromConfig() {
        assertTrue(AppConfig.fromMap(Map.of("USE_MOCK_LLM", "TRUE")).isUseMockLlm());
    }

    @Test
    void toStringNeverContainsTheApiKey() {
        AppConfig config = AppConfig.fromMap(Map.of("LLM_API_KEY", "sk-super-secret"));
        assertFalse(config.toString().contains("sk-super-secret"));
    }

    @Test
    void loadsValuesFromAnEnvFile() throws IOException {
        Path file = Files.createTempFile("winedine", ".env");
        try {
            Files.writeString(file, "DATASET_PATH=custom/data.json\n");
            assertEquals("custom/data.json", AppConfig.load(file).getDatasetPath());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void missingEnvFileIsNotAnError() {
        AppConfig config = AppConfig.load(Path.of("does-not-exist.env"));
        assertEquals(AppConfig.DEFAULT_MODEL, config.getLlmModel());
    }
}
