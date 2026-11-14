package com.winedine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.data.CuratedProviderFactory;
import com.winedine.data.RestaurantDataProvider;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchHistory;
import com.winedine.model.SearchResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryServiceTest {
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC);

    private final RecommendationService service = new RecommendationService(new CuratedProviderFactory(Path.of("src/test/resources/test-restaurants.json")));
    private final RestaurantDataProvider provider = service.getProvider();

    private SearchHistory sample(String query, LocalDateTime when) {
        SearchConstraints c = new SearchConstraints().setLocation("Osgoode Station").setMaxBudgetPerPerson(25.0);
        List<SearchResult> results = service.findMatches(c);
        assertFalse(results.isEmpty());
        return new SearchHistory(query, c, results, when);
    }

    @Test
    void savedEntriesAreRestoredFromTheFile() throws IOException {
        Path file = Files.createTempDirectory("hist").resolve("h.json");
        HistoryService h = new HistoryService(file, provider, NOW);
        SearchHistory original = sample("first", LocalDateTime.of(2026, 11, 9, 10, 0));
        assertTrue(h.save(original));

        SearchHistory loaded = new HistoryService(file, provider, NOW).getAll().get(0);
        assertEquals(original.getId(), loaded.getId());
        assertEquals("first", loaded.getOriginalQuery());
        assertEquals(original.getResults().size(), loaded.getResults().size());
        assertEquals(original.getResults().get(0).getRestaurant().getId(), loaded.getResults().get(0).getRestaurant().getId());
        assertEquals(original.getResults().get(0).getScore(), loaded.getResults().get(0).getScore(), 1e-9);
        assertEquals("Osgoode Station", loaded.getConstraints().getLocation());
        assertEquals(Double.valueOf(25.0), loaded.getConstraints().getMaxBudgetPerPerson());
        assertEquals(original.getTimestamp(), loaded.getTimestamp());
    }

    @Test
    void newestEntryComesFirst() throws IOException {
        HistoryService h = new HistoryService(Files.createTempDirectory("hist").resolve("h.json"), provider, NOW);
        h.save(sample("old", LocalDateTime.of(2026, 11, 1, 10, 0)));
        h.save(sample("new", LocalDateTime.of(2026, 11, 9, 10, 0)));
        assertEquals("new", h.getAll().get(0).getOriginalQuery());
    }

    @Test
    void entriesOlderThanSevenDaysAreOutdated() throws IOException {
        HistoryService h = new HistoryService(Files.createTempDirectory("hist").resolve("h.json"), provider, NOW);
        assertTrue(h.isOutdated(sample("a", LocalDateTime.of(2026, 11, 2, 11, 0))));
        assertFalse(h.isOutdated(sample("b", LocalDateTime.of(2026, 11, 3, 11, 0))));
    }

    @Test
    void damagedFileGivesEmptyHistory() throws IOException {
        Path file = Files.createTempDirectory("hist").resolve("h.json");
        Files.writeString(file, "{ this is not json");
        assertTrue(new HistoryService(file, provider, NOW).getAll().isEmpty());
    }

    @Test
    void historyIsCappedAtFiftyEntries() throws IOException {
        HistoryService h = new HistoryService(Files.createTempDirectory("hist").resolve("h.json"), provider, NOW);
        SearchHistory one = sample("x", LocalDateTime.of(2026, 11, 9, 10, 0));
        for (int i = 0; i < HistoryService.MAX_ENTRIES + 5; i++) {
            h.save(one);
        }
        assertEquals(HistoryService.MAX_ENTRIES, h.getAll().size());
    }
}