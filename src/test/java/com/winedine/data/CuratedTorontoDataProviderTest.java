package com.winedine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.data.CuratedTorontoDataProvider.DatasetFile;
import com.winedine.data.CuratedTorontoDataProvider.DealRecord;
import com.winedine.data.CuratedTorontoDataProvider.LandmarkRecord;
import com.winedine.data.CuratedTorontoDataProvider.RestaurantRecord;
import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;
import java.io.StringReader;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CuratedTorontoDataProviderTest {

    private static LandmarkRecord landmark(String name, double lat, double lon, String... aliases) {
        LandmarkRecord l = new LandmarkRecord();
        l.name = name;
        l.latitude = lat;
        l.longitude = lon;
        l.aliases = List.of(aliases);
        return l;
    }

    private static DatasetFile dataset() {
        DealRecord deal = new DealRecord();
        deal.day = "friday";
        deal.startTime = "16:00";
        deal.endTime = "18:00";
        deal.description = "$6 pints";
        deal.drinkPrice = 6.0;
        deal.avgCostPerPerson = null;
        deal.lastUpdated = null;

        RestaurantRecord r = new RestaurantRecord();
        r.id = "r1";
        r.name = "Test Pub";
        r.opensAt = "11:00";
        r.closesAt = "02:00";
        r.tags = List.of("patio");
        r.deals = List.of(deal);

        DatasetFile file = new DatasetFile();
        file.restaurants = List.of(r);
        file.landmarks = List.of(
                landmark("Osgoode Station", 43.6508, -79.3868, "osgoode"),
                landmark("St. Andrew Station", 43.6479, -79.3846, "st andrew"),
                landmark("Downtown Toronto", 43.6532, -79.3832, "downtown"));
        return file;
    }

    @Test
    void convertsRecordsToModelObjectsAndKeepsUnknownValuesNull() {
        CuratedTorontoDataProvider provider = new CuratedTorontoDataProvider(dataset());
        Restaurant r = provider.getRestaurant("r1");
        assertEquals("Test Pub", r.getName());
        assertEquals(LocalTime.of(11, 0), r.getOpensAt());
        assertNull(r.getLatitude());
        assertEquals(1, r.getDeals().size());
        HappyHourDeal deal = r.getDeals().get(0);
        assertEquals(DayOfWeek.FRIDAY, deal.getDay());
        assertEquals(Double.valueOf(6.0), deal.getDrinkPrice());
        assertNull(deal.getAvgCostPerPerson());
        assertNull(deal.getLastUpdated());
    }

    @Test
    void unknownIdGivesNull() {
        assertNull(new CuratedTorontoDataProvider(dataset()).getRestaurant("nope"));
    }

    @Test
    void findRestaurantsReturnsEveryRestaurant() {
        assertEquals(1, new CuratedTorontoDataProvider(dataset()).findRestaurants(new SearchConstraints()).size());
    }

    @Test
    void resolvesExactNameAndAlias() {
        CuratedTorontoDataProvider provider = new CuratedTorontoDataProvider(dataset());
        SearchConstraints c = new SearchConstraints().setLocation("Osgoode Station");
        provider.resolveLocation(c);
        assertEquals(Double.valueOf(43.6508), c.getSearchLatitude());
        assertEquals(Double.valueOf(-79.3868), c.getSearchLongitude());

        SearchConstraints c2 = new SearchConstraints().setLocation("St. Andrew");
        provider.resolveLocation(c2);
        assertEquals(Double.valueOf(43.6479), c2.getSearchLatitude());
    }

    @Test
    void resolvesALandmarkInsideALongerPhrase() {
        CuratedTorontoDataProvider provider = new CuratedTorontoDataProvider(dataset());
        SearchConstraints c = new SearchConstraints().setLocation("somewhere near Osgoode, please");
        provider.resolveLocation(c);
        assertEquals(Double.valueOf(43.6508), c.getSearchLatitude());
    }

    @Test
    void unknownLocationThrows() {
        CuratedTorontoDataProvider provider = new CuratedTorontoDataProvider(dataset());
        SearchConstraints c = new SearchConstraints().setLocation("Atlantis");
        LocationNotFoundException e = assertThrows(LocationNotFoundException.class, () -> provider.resolveLocation(c));
        assertEquals("Atlantis", e.getLocationText());
    }

    @Test
    void doesNothingWithoutLocationTextOrWhenCoordinatesAlreadyExist() {
        CuratedTorontoDataProvider provider = new CuratedTorontoDataProvider(dataset());
        SearchConstraints none = new SearchConstraints();
        provider.resolveLocation(none);
        assertNull(none.getSearchLatitude());

        SearchConstraints has = new SearchConstraints().setLocation("Atlantis")
                .setSearchLatitude(1.0).setSearchLongitude(2.0);
        provider.resolveLocation(has); // must not throw
        assertEquals(Double.valueOf(1.0), has.getSearchLatitude());
    }

    @Test
    void invalidTimeInTheDatasetIsRejected() {
        DatasetFile file = dataset();
        file.restaurants.get(0).opensAt = "25:99";
        assertThrows(DataLoadException.class, () -> new CuratedTorontoDataProvider(file));
    }

    @Test
    void restaurantWithoutIdIsRejected() {
        DatasetFile file = dataset();
        file.restaurants.get(0).id = null;
        assertThrows(DataLoadException.class, () -> new CuratedTorontoDataProvider(file));
    }

    @Test
    void readsTheJsonFormat() {
        String json = "{\"landmarks\":[{\"name\":\"Osgoode Station\",\"latitude\":43.65,\"longitude\":-79.38,"
                + "\"aliases\":[\"osgoode\"]}],"
                + "\"restaurants\":[{\"id\":\"r1\",\"name\":\"Json Pub\",\"latitude\":43.65,\"longitude\":-79.38,"
                + "\"cuisine\":\"Pub\",\"tags\":[\"patio\"],\"opensAt\":\"11:00\",\"closesAt\":\"23:00\","
                + "\"lastUpdated\":\"2026-10-20\",\"deals\":[{\"day\":\"MONDAY\",\"startTime\":\"16:00\","
                + "\"endTime\":\"18:00\",\"description\":\"cheap\",\"drinkPrice\":5.5,\"lastUpdated\":\"2026-10-20\"}]}]}";
        CuratedTorontoDataProvider provider = CuratedTorontoDataProvider.fromReader(new StringReader(json));
        Restaurant r = provider.getRestaurant("r1");
        assertEquals("Json Pub", r.getName());
        assertEquals(Double.valueOf(5.5), r.getDeals().get(0).getDrinkPrice());
        assertNull(r.getDeals().get(0).getAvgCostPerPerson());
    }

    @Test
    void invalidJsonIsReportedAsDataLoadException() {
        assertThrows(DataLoadException.class, () -> CuratedTorontoDataProvider.fromReader(new StringReader("{not json")));
    }

    @Test
    void missingFileIsReportedAsDataLoadException() {
        assertThrows(DataLoadException.class, () -> CuratedTorontoDataProvider.fromFile(Path.of("no/such/file.json")));
    }

    @Test
    void testFixtureLoadsAndContainsTheEdgeCases() {
        CuratedTorontoDataProvider provider = CuratedTorontoDataProvider.fromFile(Path.of("src/test/resources/test-restaurants.json"));
        List<Restaurant> all = provider.findRestaurants(new SearchConstraints());
        assertTrue(all.size() >= 15);
        assertTrue(all.stream().anyMatch(r -> !r.hasValidLocation()), "fixture should include a venue without a map location");
        assertTrue(all.stream().flatMap(r -> r.getDeals().stream()).anyMatch(d -> !d.isCostKnown()),
                "fixture should include a deal with an unknown price");
        SearchConstraints c = new SearchConstraints().setLocation("Osgoode Station");
        provider.resolveLocation(c);
        assertNotNull(c.getSearchLatitude());
    }

    @Test
    void realDatasetInTheRepositoryIsComplete() {
        CuratedTorontoDataProvider provider = CuratedTorontoDataProvider.fromFile(Path.of("data/restaurants.json"));
        List<Restaurant> all = provider.findRestaurants(new SearchConstraints());
        assertTrue(all.size() >= 15);
        for (Restaurant r : all) {
            assertTrue(r.hasValidLocation(), r.getName() + " needs coordinates");
            assertTrue(!r.getName().isEmpty() && !r.getAddress().isEmpty(), "name and address are required");
            assertTrue(!r.getDeals().isEmpty(), r.getName() + " needs at least one deal");
            for (HappyHourDeal d : r.getDeals()) {
                assertNotNull(d.getDay());
                assertNotNull(d.getStartTime());
                assertNotNull(d.getEndTime());
            }
        }
        for (String place : List.of("Osgoode Station", "Union Station", "King West", "Yorkville", "Ossington")) {
            SearchConstraints c = new SearchConstraints().setLocation(place);
            provider.resolveLocation(c);
            assertNotNull(c.getSearchLatitude(), place);
        }
    }
}