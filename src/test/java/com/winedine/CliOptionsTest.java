package com.winedine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliOptionsTest {
    @Test
    void parsesInputOutputAndApiFlag() {
        CliOptions o = CliOptions.parse(new String[]{"-LLMAPI", "-input", "task.txt", "-output", "out.txt"});
        assertEquals("task.txt", o.getInput());
        assertEquals("out.txt", o.getOutput());
        assertTrue(o.isUseApi());
        assertEquals("false", o.getOverrides().get("USE_MOCK_LLM"));
    }

    @Test
    void mockFlagForcesTheMockModel() {
        CliOptions o = CliOptions.parse(new String[]{"-mock", "-model", "x", "-dataset", "d.json"});
        assertEquals("true", o.getOverrides().get("USE_MOCK_LLM"));
        assertEquals("x", o.getOverrides().get("LLM_MODEL"));
        assertEquals("d.json", o.getOverrides().get("DATASET_PATH"));
    }

    @Test
    void helpIsRecognised() {
        assertTrue(CliOptions.parse(new String[]{"-help"}).isHelp());
    }

    @Test
    void rejectsUnknownMissingValueAndConflictingOptions() {
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"-bogus"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"-input"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"-input", "-mock"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"-LLMAPI", "-mock"}));
    }
}