package com.winedine.ui;

import com.winedine.controller.AgentController;
import com.winedine.model.AgentResponse;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchHistory;
import com.winedine.model.SearchResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

/** Command-line interface: a simple read-eval-print loop on top of {@link AgentController}. */
public class CliApp {
    static final String HELP = String.join("\n",
            "Type a request such as: cheap beer and wings near Osgoode Station on Friday 5-7pm under $15",
            "After results appear, type a change in plain words (e.g. \"make it $20\", \"closest first\").",
            "Commands:",
            "  /show N      details of result N          /explain N   why result N was recommended",
            "  /map N       map link for result N        /list        show the current results again",
            "  /history     list saved searches          /open N      open saved search N",
            "  /refresh     search again with current data (for outdated searches)",
            "  /new         start a new search           /help        this help        /quit  exit");

    private final AgentController controller;
    private final BufferedReader in;
    private final PrintStream out;

    public CliApp(AgentController controller, BufferedReader in, PrintStream out) {
        this.controller = controller;
        this.in = in;
        this.out = out;
    }

    public void run() throws IOException {
        out.println("Wine&Dine AI - Toronto happy-hour finder  (language model: " + controller.describeLlm() + ")");
        out.println(HELP);
        String line;
        while (true) {
            out.print("\n> ");
            out.flush();
            line = in.readLine();
            if (line == null || line.trim().equalsIgnoreCase("/quit")) {
                out.println("Bye!");
                return;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("/")) {
                command(line);
            } else {
                show(controller.send(line));
            }
        }
    }

    void command(String line) {
        String[] parts = line.split("\\s+", 2);
        String name = parts[0].toLowerCase();
        Integer number = parts.length > 1 ? parseNumber(parts[1]) : null;
        switch (name) {
            case "/help": out.println(HELP); break;
            case "/new": controller.newSearch(); out.println("Starting a new search. What are you looking for?"); break;
            case "/list": printResults(controller.getResults()); break;
            case "/show": withResult(number, r -> out.print(ResultFormatter.details(r))); break;
            case "/explain": withResult(number, r -> out.println(controller.explain(number))); break;
            case "/map": withResult(number, r -> {
                out.println(MapLinks.describe(r));
                out.println("Google Maps: " + MapLinks.googleUrl(r.getRestaurant()));
            }); break;
            case "/history": printHistory(); break;
            case "/open": open(number); break;
            case "/refresh": show(controller.refresh()); break;
            default: out.println("Unknown command. Type /help.");
        }
    }

    private void withResult(Integer number, java.util.function.Consumer<SearchResult> action) {
        SearchResult r = number == null ? null : controller.getResult(number);
        if (r == null) {
            out.println("Please give a valid result number, for example /show 1");
        } else {
            action.accept(r);
        }
    }

    private void open(Integer number) {
        SearchHistory entry = number == null ? null : controller.reopen(number);
        if (entry == null) {
            out.println("Please give a valid history number, for example /open 1 (see /history).");
            return;
        }
        out.println("Opened: " + entry.getSummary());
        if (controller.isOutdated(entry)) {
            out.println("! This search is more than 7 days old, so the deals may have changed. Type /refresh to search again.");
        }
        printResults(controller.getResults());
    }

    private void printHistory() {
        List<SearchHistory> all = controller.getHistory();
        if (all.isEmpty()) {
            out.println("No saved searches yet.");
        }
        for (int i = 0; i < all.size(); i++) {
            SearchHistory h = all.get(i);
            out.println((i + 1) + ". " + h.getSummary() + (controller.isOutdated(h) ? "  [outdated]" : ""));
        }
    }

    void show(AgentResponse response) {
        switch (response.getType()) {
            case CLARIFICATION:
                out.println(response.getMessage());
                break;
            case ERROR:
                out.println("Error: " + response.getMessage());
                break;
            case RESULTS:
                SearchConstraints c = response.getConstraints();
                out.println("Searching: " + summary(c));
                if (!response.getMessage().isEmpty()) {
                    out.println(response.getMessage());
                }
                printResults(response.getResults());
                break;
            default:
                break;
        }
    }

    private void printResults(List<SearchResult> results) {
        if (results.isEmpty()) {
            out.println("No results.");
            return;
        }
        for (int i = 0; i < results.size(); i++) {
            out.println(ResultFormatter.line(i + 1, results.get(i)));
            for (String w : results.get(i).getWarnings()) {
                out.println("     ! " + w);
            }
        }
        out.println("Use /show N, /explain N or /map N for more. Type a change to refine, or /new for a different search.");
    }

    static String summary(SearchConstraints c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.getLocation() == null ? "anywhere" : "near " + c.getLocation());
        if (c.getDay() != null) sb.append(", ").append(c.getDay());
        if (c.hasTimeWindow()) sb.append(", ").append(c.getStartTime()).append("-").append(c.getEndTime());
        if (c.getMaxBudgetPerPerson() != null) sb.append(", up to $").append(c.getMaxBudgetPerPerson()).append(" pp");
        if (c.getMaxDrinkPrice() != null) sb.append(", drinks up to $").append(c.getMaxDrinkPrice());
        if (!c.allPreferences().isEmpty()) sb.append(", wants ").append(String.join("/", c.allPreferences()));
        sb.append(", ranked ").append(c.getRankingProfile().name().toLowerCase().replace('_', ' '));
        return sb.toString();
    }

    private static Integer parseNumber(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static CliApp forConsole(AgentController controller) {
        return new CliApp(controller, new BufferedReader(new InputStreamReader(System.in)), System.out);
    }
}