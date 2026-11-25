package com.winedine;

import java.util.HashMap;
import java.util.Map;

/** Command-line options of the standalone CLI (see {@link #USAGE}). */
public final class CliOptions {
    public static final String USAGE = String.join("\n",
            "Wine&Dine AI - Toronto happy-hour finder (command line)",
            "",
            "Usage: java -jar Agent.jar [options]",
            "",
            "Options:",
            "  -input FILE     run the agent on a task file instead of typing. Each line is one message or",
            "                  command (blank lines and lines starting with # are ignored)",
            "  -output FILE    write the results to FILE instead of the terminal",
            "  -LLMAPI         use the real LLM API (needs LLM_API_KEY in .env or the environment)",
            "  -mock           use the offline mock language model (no key, simple phrasing only)",
            "  -model NAME     LLM model name (default gpt-4o-mini)",
            "  -dataset FILE   restaurant dataset (default data/restaurants.json)",
            "  -history FILE   search history file (default data/history.json)",
            "  -help           show this help",
            "",
            "With no -input the agent is interactive; type /help inside it.",
            "Exit codes: 0 ok, 1 could not start, 2 bad options.");

    private String input;
    private String output;
    private boolean useApi;
    private boolean useMock;
    private boolean help;
    private final Map<String, String> overrides = new HashMap<>();

    public static CliOptions parse(String[] args) {
        CliOptions o = new CliOptions();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a.toLowerCase()) {
                case "-input": o.input = value(args, ++i, a); break;
                case "-output": o.output = value(args, ++i, a); break;
                case "-llmapi": o.useApi = true; break;
                case "-mock": o.useMock = true; break;
                case "-model": o.overrides.put("LLM_MODEL", value(args, ++i, a)); break;
                case "-dataset": o.overrides.put("DATASET_PATH", value(args, ++i, a)); break;
                case "-history": o.overrides.put("HISTORY_PATH", value(args, ++i, a)); break;
                case "-help": case "--help": case "-h": o.help = true; break;
                default: throw new IllegalArgumentException("Unknown option: " + a);
            }
        }
        if (o.useApi && o.useMock) {
            throw new IllegalArgumentException("Use either -LLMAPI or -mock, not both");
        }
        if (o.useApi) {
            o.overrides.put("USE_MOCK_LLM", "false");
        } else if (o.useMock) {
            o.overrides.put("USE_MOCK_LLM", "true");
        }
        return o;
    }

    private static String value(String[] args, int i, String option) {
        if (i >= args.length || args[i].startsWith("-")) {
            throw new IllegalArgumentException("Option " + option + " needs a value");
        }
        return args[i];
    }

    public String getInput() { return input; }
    public String getOutput() { return output; }
    public boolean isUseApi() { return useApi; }
    public boolean isHelp() { return help; }
    public Map<String, String> getOverrides() { return overrides; }
}