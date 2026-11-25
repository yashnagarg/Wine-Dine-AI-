package com.winedine;

import com.winedine.config.AppConfig;
import com.winedine.controller.AgentController;
import com.winedine.data.DataLoadException;
import com.winedine.ui.CliApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Standalone command-line launcher, for example
 * {@code java -jar Agent.jar -LLMAPI -input task.txt}. Run with {@code -help} for the options.
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException {
        System.exit(run(args, System.in, System.out, System.err));
    }

    /** Runs the CLI and returns the exit code (0 ok, 1 could not start, 2 bad options). */
    static int run(String[] args, java.io.InputStream stdin, PrintStream stdout, PrintStream stderr) throws IOException {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            stderr.println("Error: " + e.getMessage());
            stderr.println("Run with -help for the options.");
            return 2;
        }
        if (options.isHelp()) {
            stdout.println(CliOptions.USAGE);
            return 0;
        }
        AppConfig config = AppConfig.load().withOverrides(options.getOverrides());
        if (options.isUseApi() && !config.hasApiKey()) {
            stderr.println("Error: -LLMAPI needs LLM_API_KEY in .env or the environment (see .env.example).");
            return 2;
        }
        BufferedReader reader;
        boolean batch = options.getInput() != null;
        if (batch) {
            Path file = Path.of(options.getInput());
            if (!Files.isRegularFile(file)) {
                stderr.println("Error: input file not found: " + file);
                return 2;
            }
            reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        } else {
            reader = new BufferedReader(new InputStreamReader(stdin, StandardCharsets.UTF_8));
        }
        PrintStream out = stdout;
        if (options.getOutput() != null) {
            out = new PrintStream(Files.newOutputStream(Path.of(options.getOutput())), true, StandardCharsets.UTF_8);
        }
        try {
            AgentController controller = AgentController.create(config);
            new CliApp(controller, reader, out, batch).run();
            return 0;
        } catch (DataLoadException e) {
            stderr.println("Could not start: " + e.getMessage());
            return 1;
        } finally {
            reader.close();
            if (out != stdout) {
                out.close();
            }
        }
    }
}