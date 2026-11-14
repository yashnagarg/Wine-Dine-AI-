package com.winedine.llm;

import com.winedine.model.RankingProfile;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline stand-in for the language model, used for tests, demos without an API key, and as the
 * fallback when no key is configured. It understands simple phrasing with regular expressions.
 */
public class MockLLMClient implements LLMClient {
    private static final Pattern DRINK_PRICE = Pattern.compile(
            "(?:drinks?|pints?|beers?|cocktails?)\\s*(?:under|below|for|at|max)\\s*\\$?(\\d+(?:\\.\\d+)?)");
    private static final Pattern BUDGET = Pattern.compile(
            "(?:under|below|max(?:imum)?|up to|less than|budget(?: of)?|make it|raise (?:it )?to|change (?:it )?to)"
                    + "\\s*\\$?(\\d+(?:\\.\\d+)?)|\\$(\\d+(?:\\.\\d+)?)\\s*(?:per person|pp|each|a head|/person)?");
    private static final Pattern GROUP = Pattern.compile(
            "(\\d+)\\s*(?:people|persons|friends|of us|guests)|(?:group|party|table) of (\\d+)|table for (\\d+)");
    private static final Pattern TIME_RANGE = Pattern.compile(
            "(?:between\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:-|to|until|and)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?");
    private static final Pattern AFTER_TIME = Pattern.compile("after\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)");
    private static final Pattern LOCATION = Pattern.compile(
            "\\b(?:near|at|around|by|close to|in)\\s+([a-z][a-z.' -]*?)"
                    + "(?=\\s+(?:on|for|under|below|with|between|from|after|before|at|tonight|today|tomorrow|that|where|please)\\b|[,.;!?]|$)");
    private static final List<String> NOT_A_PLACE = Arrays.asList("tonight", "today", "tomorrow", "night", "happy hour");

    private static final List<String> FOOD = Arrays.asList("tacos", "wings", "nachos", "sushi", "fries", "pizza",
            "burgers", "oysters", "ramen", "pasta", "vegetarian", "poutine", "olives", "dumplings", "bruschetta", "chicken");
    private static final List<String> DRINKS = Arrays.asList("beer", "wine", "cocktails", "whisky", "sake", "cider", "negroni", "margarita", "spritz", "martini", "caesar", "mimosa", "old fashioned", "lager", "tequila");
    private static final List<String> VENUE = Arrays.asList("patio", "rooftop", "quiet", "live music", "sports",
            "dog friendly", "pub");

    private final Clock clock;

    public MockLLMClient() { this(Clock.systemDefaultZone()); }

    public MockLLMClient(Clock clock) { this.clock = clock; }

    @Override
    public SearchConstraints extractConstraints(String userText) {
        String t = userText == null ? "" : userText.toLowerCase(Locale.ROOT);
        SearchConstraints c = new SearchConstraints();

        Matcher drink = DRINK_PRICE.matcher(t);
        if (drink.find()) {
            c.setMaxDrinkPrice(Double.valueOf(drink.group(1)));
            t = t.replace(drink.group(), " ");
        }
        Matcher budget = BUDGET.matcher(t);
        if (budget.find()) {
            String value = budget.group(1) != null ? budget.group(1) : budget.group(2);
            c.setMaxBudgetPerPerson(Double.valueOf(value));
        }
        Matcher group = GROUP.matcher(t);
        if (group.find()) {
            for (int i = 1; i <= 3; i++) {
                if (group.group(i) != null) {
                    c.setGroupSize(Integer.valueOf(group.group(i)));
                }
            }
        }
        for (DayOfWeek d : DayOfWeek.values()) {
            if (t.contains(d.name().toLowerCase(Locale.ROOT))) {
                c.setDay(d);
            }
        }
        LocalDate today = LocalDate.now(clock);
        if (t.contains("tonight") || t.contains("today")) {
            c.setDay(today.getDayOfWeek());
        } else if (t.contains("tomorrow")) {
            c.setDay(today.plusDays(1).getDayOfWeek());
        }
        readTimes(t, c);
        Matcher loc = LOCATION.matcher(t);
        while (loc.find()) {
            String place = loc.group(1).trim();
            if (!place.isEmpty() && !NOT_A_PLACE.contains(place)) {
                c.setLocation(place);
                break;
            }
        }
        c.setFoodPreferences(found(t, FOOD));
        c.setDrinkPreferences(found(t, DRINKS));
        c.setVenuePreferences(found(t, VENUE));
        if (t.contains("happy hour")) {
            c.setHappyHourRequired(true);
        }
        if (t.contains("cheapest") || t.contains("lowest price")) {
            c.setRankingProfile(RankingProfile.BUDGET_FIRST);
        } else if (t.contains("closest") || t.contains("nearest") || t.contains("walking distance")) {
            c.setRankingProfile(RankingProfile.DISTANCE_FIRST);
        }
        return c;
    }

    @Override
    public SearchConstraints interpretRefinement(String userText, SearchConstraints current) {
        SearchConstraints changes = extractConstraints(userText);
        String raw = userText == null ? "" : userText.trim();
        // A bare answer to a clarification question: "$15", "20", or "Osgoode Station".
        if (current.getMaxBudgetPerPerson() == null && changes.getMaxBudgetPerPerson() == null
                && raw.matches("\\$?\\d+(\\.\\d+)?")) {
            changes.setMaxBudgetPerPerson(Double.valueOf(raw.replace("$", "")));
        }
        if (current.getLocation() == null && changes.getLocation() == null && !raw.isEmpty()
                && raw.length() < 40 && !raw.matches(".*\\d.*") && !isSkip(raw)) {
            changes.setLocation(raw.replaceFirst("(?i)^(near|at|around|by|close to|in)\\s+", ""));
        }
        return changes;
    }

    @Override
    public String askClarification(List<String> missingFields, SearchConstraints known) {
        List<String> asks = new ArrayList<>();
        for (String f : missingFields) {
            switch (f) {
                case "budget": asks.add("your budget per person"); break;
                case "time": asks.add("the time window (for example 5-7pm)"); break;
                case "location": asks.add("where you want to go (a Toronto station or neighbourhood)"); break;
                default: asks.add(f);
            }
        }
        return "Could you tell me " + String.join(" and ", asks) + "?";
    }

    @Override
    public String explain(SearchResult result, SearchConstraints constraints) {
        return templateExplanation(result);
    }

    @Override
    public String describe() { return "offline mock (no API key)"; }

    /** Plain explanation built straight from the verified facts. Also the fallback when the real LLM fails. */
    public static String templateExplanation(SearchResult result) {
        return result.getMatchSummary().trim().replace("\n", "; ");
    }

    static boolean isSkip(String text) {
        String t = text.trim().toLowerCase(Locale.ROOT);
        return t.equals("skip") || t.equals("any") || t.equals("no preference") || t.equals("doesn't matter")
                || t.equals("doesnt matter") || t.equals("whatever");
    }

    private static void readTimes(String t, SearchConstraints c) {
        Matcher range = TIME_RANGE.matcher(t);
        while (range.find()) {
            String m1 = range.group(3);
            String m2 = range.group(6);
            if (m1 == null && m2 == null) {
                continue; // "10 to 20" is not a time
            }
            if (m1 == null) m1 = m2;
            if (m2 == null) m2 = m1;
            LocalTime start = toTime(range.group(1), range.group(2), m1);
            LocalTime end = toTime(range.group(4), range.group(5), m2);
            if (start != null && end != null) {
                c.setStartTime(start).setEndTime(end);
                return;
            }
        }
        Matcher after = AFTER_TIME.matcher(t);
        if (after.find()) {
            LocalTime start = toTime(after.group(1), after.group(2), after.group(3));
            if (start != null) {
                c.setStartTime(start).setEndTime(LocalTime.of(23, 59));
            }
        }
    }

    private static LocalTime toTime(String hour, String minute, String meridiem) {
        int h = Integer.parseInt(hour);
        int m = minute == null ? 0 : Integer.parseInt(minute);
        if (h > 23 || m > 59) {
            return null;
        }
        if (h <= 12) { // 24-hour values such as "22:00" are used as they are
            if ("pm".equals(meridiem) && h < 12) h += 12;
            if ("am".equals(meridiem) && h == 12) h = 0;
        }
        return LocalTime.of(h, m);
    }

    private static List<String> found(String text, List<String> words) {
        List<String> out = new ArrayList<>();
        for (String w : words) {
            if (text.contains(w)) {
                out.add(w);
            }
        }
        return out;
    }
}