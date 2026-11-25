package com.winedine;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private static PrintStream capture(ByteArrayOutputStream b) throws Exception {
        return new PrintStream(b, true, "UTF-8");
    }

    @Test
    void helpPrintsUsageAndExitsZero() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = Main.run(new String[]{"-help"}, new ByteArrayInputStream(new byte[0]), capture(out), capture(new ByteArrayOutputStream()));
        assertEquals(0, code);
        assertTrue(out.toString("UTF-8").contains("-input FILE"));
    }

    @Test
    void badOptionExitsWithTwo() throws Exception {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(new String[]{"-nope"}, new ByteArrayInputStream(new byte[0]), capture(new ByteArrayOutputStream()), capture(err));
        assertEquals(2, code);
        assertTrue(err.toString("UTF-8").contains("Unknown option"));
    }

    @Test
    void missingInputFileExitsWithTwo() throws Exception {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(new String[]{"-mock", "-input", "does-not-exist.txt"}, new ByteArrayInputStream(new byte[0]), capture(new ByteArrayOutputStream()), capture(err));
        assertEquals(2, code);
        assertTrue(err.toString("UTF-8").contains("not found"));
    }

    @Test
    void taskFileRunsInBatchModeWithTheMockModel() throws Exception {
        Path dir = Files.createTempDirectory("winedine-cli");
        Path task = dir.resolve("task.txt");
        Files.writeString(task, "# comment\ncheap beer and wings near Osgoode Station on Friday 5-7pm under $15\n/list\n", StandardCharsets.UTF_8);
        Path outFile = dir.resolve("out.txt");
        int code = Main.run(new String[]{"-mock", "-input", task.toString(), "-output", outFile.toString(),
                "-dataset", "src/test/resources/test-restaurants.json", "-history", dir.resolve("h.json").toString()},
                new ByteArrayInputStream(new byte[0]), capture(new ByteArrayOutputStream()), capture(new ByteArrayOutputStream()));
        assertEquals(0, code);
        String text = Files.readString(outFile, StandardCharsets.UTF_8);
        assertTrue(text.contains("> cheap beer and wings"), text);
        assertFalse(text.contains("# comment"), text);
        assertTrue(text.contains("Bye!"), text);
    }
}