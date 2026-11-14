package com.winedine.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Concrete Product: reads the hand-curated Toronto dataset (restaurants, happy-hour deals and
 * landmark coordinates) from a JSON file. Missing prices or dates stay null, never guessed.
 */
public class CuratedTorontoDataProvider implements RestaurantDataProvider {

    // ---- JSON file shape (plain strings and numbers, converted to model types below) ----
    public static class DatasetFile {
        public List<LandmarkRecord> landmarks;
        public List<RestaurantRecord> restaurants;
    }

    public static class LandmarkRecord {
        public String name;
        public List<String> aliases;
        public Double latitude;
        public Double longitude;
    }

    public static class RestaurantRecord {
        public String id;
        public String name;
        public String address;
        public Double latitude;
        public Double longitude;
        public String cuisine;
        public List<String> tags;
        public String opensAt;
        public String closesAt;
        public String lastUpdated;
        public List<DealRecord> deals;
    }

    public static class DealRecord {
        public String day;
        public String startTime;
        public String endTime;
        public String description;
        public Double drinkPrice;
        public Double avgCostPerPerson;
        public String lastUpdated;
    }

    private final Map<String, Restaurant> restaurantsById = new LinkedHashMap<>();
    private final Map<String, double[]> landmarksByName = new LinkedHashMap<>();

    public CuratedTorontoDataProvider(DatasetFile dataset) {
        if (dataset == null) {
            throw new DataLoadException("Dataset is empty");
        }
        if (dataset.restaurants != null) {
            for (RestaurantRecord record : dataset.restaurants) {
                Restaurant r = toRestaurant(record);
                restaurantsById.put(r.getId(), r);
            }
        }
        if (dataset.landmarks != null) {
            for (LandmarkRecord record : dataset.landmarks) {
                addLandmark(record);
            }
        }
    }

    public static CuratedTorontoDataProvider fromFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return fromReader(reader);
        } catch (IOException e) {
            throw new DataLoadException("Could not read dataset file: " + file, e);
        }
    }

    public static CuratedTorontoDataProvider fromReader(Reader reader) {
        try {
            DatasetFile dataset = new Gson().fromJson(reader, DatasetFile.class);
            return new CuratedTorontoDataProvider(dataset);
        } catch (JsonParseException e) {
            throw new DataLoadException("Dataset is not valid JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Restaurant> findRestaurants(SearchConstraints constraints) {
        return new ArrayList<>(restaurantsById.values());
    }

    @Override
    public Restaurant getRestaurant(String id) {
        return restaurantsById.get(id);
    }

    @Override
    public void resolveLocation(SearchConstraints constraints) {
        String text = constraints.getLocation();
        if (text == null || text.isBlank() || constraints.hasLocationCoordinates()) {
            return;
        }
        double[] coords = lookup(text);
        if (coords == null) {
            throw new LocationNotFoundException(text);
        }
        constraints.setSearchLatitude(coords[0]);
        constraints.setSearchLongitude(coords[1]);
    }

    /** Exact name or alias first; otherwise the longest known name or alias contained in the text. */
    private double[] lookup(String text) {
        String normalized = normalize(text);
        double[] exact = landmarksByName.get(normalized);
        if (exact != null) {
            return exact;
        }
        String padded = " " + normalized + " ";
        String bestName = null;
        for (String name : landmarksByName.keySet()) {
            if (padded.contains(" " + name + " ") && (bestName == null || name.length() > bestName.length())) {
                bestName = name;
            }
        }
        return bestName == null ? null : landmarksByName.get(bestName);
    }

    private void addLandmark(LandmarkRecord record) {
        if (record.name == null || record.latitude == null || record.longitude == null) {
            throw new DataLoadException("Landmark needs a name, latitude and longitude");
        }
        double[] coords = {record.latitude, record.longitude};
        landmarksByName.put(normalize(record.name), coords);
        if (record.aliases != null) {
            for (String alias : record.aliases) {
                landmarksByName.put(normalize(alias), coords);
            }
        }
    }

    static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static Restaurant toRestaurant(RestaurantRecord r) {
        if (r.id == null || r.id.isBlank() || r.name == null || r.name.isBlank()) {
            throw new DataLoadException("Every restaurant needs an id and a name");
        }
        List<HappyHourDeal> deals = new ArrayList<>();
        if (r.deals != null) {
            for (DealRecord d : r.deals) {
                deals.add(new HappyHourDeal(
                        parseDay(d.day, r.id),
                        parseTime(d.startTime, r.id),
                        parseTime(d.endTime, r.id),
                        d.description,
                        d.drinkPrice,
                        d.avgCostPerPerson,
                        parseDate(d.lastUpdated, r.id)));
            }
        }
        return new Restaurant(r.id, r.name, r.address, r.latitude, r.longitude, r.cuisine, r.tags,
                parseTime(r.opensAt, r.id), parseTime(r.closesAt, r.id), parseDate(r.lastUpdated, r.id), deals);
    }

    private static DayOfWeek parseDay(String value, String id) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DataLoadException("Invalid day '" + value + "' for restaurant " + id);
        }
    }

    private static LocalTime parseTime(String value, String id) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new DataLoadException("Invalid time '" + value + "' for restaurant " + id);
        }
    }

    private static LocalDate parseDate(String value, String id) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new DataLoadException("Invalid date '" + value + "' for restaurant " + id);
        }
    }
}
