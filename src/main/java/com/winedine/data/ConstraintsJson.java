package com.winedine.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.winedine.model.RankingProfile;
import com.winedine.model.SearchConstraints;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Plain-text (JSON) form of {@link SearchConstraints}. It is used in two places: to read the JSON the
 * LLM returns when it extracts constraints, and to save constraints in the history file.
 * Parsing is lenient: a value that cannot be understood is treated as "not given".
 */
public class ConstraintsJson {
    public Integer groupSize;
    public Double maxBudgetPerPerson;
    public Double maxDrinkPrice;
    public String day;
    public String startTime;
    public String endTime;
    public String location;
    public Double searchLatitude;
    public Double searchLongitude;
    public Double maxDistanceKm;
    public List<String> foodPreferences;
    public List<String> drinkPreferences;
    public List<String> venuePreferences;
    public Boolean happyHourRequired;
    public String rankingProfile;

    public static ConstraintsJson from(SearchConstraints c) {
        ConstraintsJson j = new ConstraintsJson();
        j.groupSize = c.getGroupSize();
        j.maxBudgetPerPerson = c.getMaxBudgetPerPerson();
        j.maxDrinkPrice = c.getMaxDrinkPrice();
        j.day = c.getDay() == null ? null : c.getDay().name();
        j.startTime = c.getStartTime() == null ? null : c.getStartTime().toString();
        j.endTime = c.getEndTime() == null ? null : c.getEndTime().toString();
        j.location = c.getLocation();
        j.searchLatitude = c.getSearchLatitude();
        j.searchLongitude = c.getSearchLongitude();
        j.maxDistanceKm = c.getMaxDistanceKm();
        j.foodPreferences = c.getFoodPreferences();
        j.drinkPreferences = c.getDrinkPreferences();
        j.venuePreferences = c.getVenuePreferences();
        j.happyHourRequired = c.isHappyHourRequired() ? Boolean.TRUE : null;
        j.rankingProfile = c.getRankingProfile().name();
        return j;
    }

    /** Parses the first JSON object found in {@code text} (LLMs sometimes wrap it in extra words). */
    public static ConstraintsJson parse(String text) {
        if (text == null) {
            throw new JsonParseException("empty response");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new JsonParseException("no JSON object found");
        }
        ConstraintsJson j = new Gson().fromJson(new java.io.StringReader(text.substring(start, end + 1)),
                ConstraintsJson.class);
        if (j == null) {
            throw new JsonParseException("empty JSON");
        }
        return j;
    }

    public SearchConstraints toConstraints() {
        SearchConstraints c = new SearchConstraints();
        if (groupSize != null && groupSize > 0) c.setGroupSize(groupSize);
        if (maxBudgetPerPerson != null && maxBudgetPerPerson > 0) c.setMaxBudgetPerPerson(maxBudgetPerPerson);
        if (maxDrinkPrice != null && maxDrinkPrice > 0) c.setMaxDrinkPrice(maxDrinkPrice);
        c.setDay(parseDay(day));
        c.setStartTime(parseTime(startTime));
        c.setEndTime(parseTime(endTime));
        if (location != null && !location.isBlank()) c.setLocation(location.trim());
        c.setSearchLatitude(searchLatitude);
        c.setSearchLongitude(searchLongitude);
        if (maxDistanceKm != null && maxDistanceKm > 0) c.setMaxDistanceKm(maxDistanceKm);
        c.setFoodPreferences(clean(foodPreferences));
        c.setDrinkPreferences(clean(drinkPreferences));
        c.setVenuePreferences(clean(venuePreferences));
        if (happyHourRequired != null) c.setHappyHourRequired(happyHourRequired);
        c.setRankingProfile(parseProfile(rankingProfile));
        return c;
    }

    private static DayOfWeek parseDay(String s) {
        try {
            return s == null ? null : DayOfWeek.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalTime parseTime(String s) {
        try {
            return s == null || s.isBlank() ? null : LocalTime.parse(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static RankingProfile parseProfile(String s) {
        try {
            return s == null ? null : RankingProfile.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<String> clean(List<String> in) {
        List<String> out = new ArrayList<>();
        if (in != null) {
            for (String s : in) {
                if (s != null && !s.isBlank()) {
                    out.add(s.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return out;
    }
}
