package com.winedine.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.winedine.data.ConstraintsJson;
import com.winedine.data.RestaurantDataProvider;
import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchHistory;
import com.winedine.model.SearchResult;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves past searches to a JSON file so they can be opened again (F10, F11).
 * Results are stored as restaurant ids plus the computed values and are looked up again
 * in the dataset when the history is loaded.
 */
public class HistoryService {
    /** A saved search older than this many days is shown as outdated and should be refreshed. */
    public static final int OUTDATED_AFTER_DAYS = 7;
    static final int MAX_ENTRIES = 50;

    private final Path file;
    private final RestaurantDataProvider provider;
    private final Clock clock;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<SearchHistory> entries = new ArrayList<>(); // newest first

    public HistoryService(Path file, RestaurantDataProvider provider, Clock clock) {
        this.file = file;
        this.provider = provider;
        this.clock = clock;
        load();
    }

    public Clock getClock() { return clock; }

    /** All saved searches, newest first. */
    public List<SearchHistory> getAll() { return new ArrayList<>(entries); }

    /** Saves a search and writes the file. Returns false if the file could not be written. */
    public boolean save(SearchHistory entry) {
        entries.add(0, entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
        return write();
    }

    public void clear() {
        entries.clear();
        write();
    }

    public boolean isOutdated(SearchHistory entry) {
        return ChronoUnit.DAYS.between(entry.getTimestamp(), LocalDateTime.now(clock)) >= OUTDATED_AFTER_DAYS;
    }

    // ---- file format ----

    static class HistoryFile {
        List<EntryRecord> entries = new ArrayList<>();
    }

    static class EntryRecord {
        String id;
        String query;
        String timestamp;
        ConstraintsJson constraints;
        List<ResultRecord> results = new ArrayList<>();
    }

    static class ResultRecord {
        String restaurantId;
        Integer dealIndex;
        Double distanceKm;
        Double score;
        Boolean nearMatch;
        Boolean comparable;
        List<String> matchedPreferences;
        List<String> warnings;
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            HistoryFile data = gson.fromJson(in, HistoryFile.class);
            if (data == null || data.entries == null) {
                return;
            }
            for (EntryRecord record : data.entries) {
                SearchHistory entry = toEntry(record);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException | JsonParseException e) {
            // A damaged history file must not stop the app; start with an empty history.
            entries.clear();
        }
    }

    private SearchHistory toEntry(EntryRecord record) {
        try {
            List<SearchResult> results = new ArrayList<>();
            for (ResultRecord rr : record.results) {
                Restaurant restaurant = provider.getRestaurant(rr.restaurantId);
                if (restaurant == null) {
                    continue; // removed from the dataset since
                }
                HappyHourDeal deal = null;
                if (rr.dealIndex != null && rr.dealIndex >= 0 && rr.dealIndex < restaurant.getDeals().size()) {
                    deal = restaurant.getDeals().get(rr.dealIndex);
                }
                SearchResult result = new SearchResult(restaurant, deal, rr.matchedPreferences, rr.warnings,
                        rr.distanceKm, Boolean.TRUE.equals(rr.nearMatch));
                result.setScore(rr.score == null ? 0 : rr.score);
                result.setComparableAlternative(Boolean.TRUE.equals(rr.comparable));
                results.add(result);
            }
            return new SearchHistory(record.id, record.query, record.constraints.toConstraints(), results,
                    LocalDateTime.parse(record.timestamp));
        } catch (RuntimeException e) {
            return null; // skip one unreadable entry
        }
    }

    private boolean write() {
        HistoryFile data = new HistoryFile();
        for (SearchHistory h : entries) {
            EntryRecord record = new EntryRecord();
            record.id = h.getId();
            record.query = h.getOriginalQuery();
            record.timestamp = h.getTimestamp().toString();
            record.constraints = ConstraintsJson.from(h.getConstraints());
            for (SearchResult r : h.getResults()) {
                ResultRecord rr = new ResultRecord();
                rr.restaurantId = r.getRestaurant().getId();
                rr.dealIndex = r.getHappyHourDeal() == null ? null
                        : r.getRestaurant().getDeals().indexOf(r.getHappyHourDeal());
                rr.distanceKm = r.getDistanceKm();
                rr.score = r.getScore();
                rr.nearMatch = r.isNearMatch();
                rr.comparable = r.isComparableAlternative();
                rr.matchedPreferences = new ArrayList<>(r.getMatchedPreferences());
                rr.warnings = new ArrayList<>(r.getWarnings());
                record.results.add(rr);
            }
            data.entries.add(record);
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                gson.toJson(data, out);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
