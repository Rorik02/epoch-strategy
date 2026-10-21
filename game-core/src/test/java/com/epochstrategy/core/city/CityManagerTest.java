
package com.epochstrategy.core.city;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CityManagerTest {

    @Test
    void registersAndFindsCity() {

        CityManager manager = new CityManager();

        City city = new City(
                1L, "Warsaw",
                45, 60,
                1000, 1L
        );

        manager.registerCity(city);

        assertEquals(1, manager.getCityCount());

        assertSame(
                city,
                manager.findCity(1L).orElseThrow()
        );

        assertSame(
                city,
                manager.findCityAt(45, 60).orElseThrow()
        );

        assertTrue(manager.isTileOccupied(45, 60));

        assertTrue(manager.findCityAt(20, 20).isEmpty());
    }

    @Test
    void rejectsDuplicateCityId() {

        CityManager manager = new CityManager();

        manager.registerCity(
                new City(
                        1L, "Warsaw",
                        10, 10,
                        1000, 1L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.registerCity(
                        new City(
                                1L, "Krakow",
                                20, 20,
                                500, 1L
                        )
                )
        );

        assertEquals(1, manager.getCityCount());
        assertFalse(manager.isTileOccupied(20, 20));
    }

    @Test
    void rejectsTwoCitiesOnSameTile() {

        CityManager manager = new CityManager();

        manager.registerCity(
                new City(
                        1L, "Warsaw",
                        10, 10,
                        1000, 1L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.registerCity(
                        new City(
                                2L, "Krakow",
                                10, 10,
                                500, 1L
                        )
                )
        );

        assertEquals(1, manager.getCityCount());
        assertTrue(manager.findCity(2L).isEmpty());
    }

    @Test
    void removesCityAndFreesTile() {

        CityManager manager = new CityManager();

        manager.registerCity(
                new City(
                        1L, "Warsaw",
                        15, 25,
                        1000, 1L
                )
        );

        assertTrue(manager.removeCity(1L));

        assertEquals(0, manager.getCityCount());
        assertFalse(manager.isTileOccupied(15, 25));
        assertTrue(manager.findCity(1L).isEmpty());

        assertFalse(manager.removeCity(1L));

        manager.registerCity(
                new City(
                        2L, "New Settlement",
                        15, 25,
                        100, null
                )
        );

        assertEquals(1, manager.getCityCount());
    }

    @Test
    void filtersCitiesByCivilization() {

        CityManager manager = new CityManager();

        City cityA = new City(
                1L, "Warsaw",
                10, 10,
                1000, 1L
        );

        City cityB = new City(
                2L, "Krakow",
                20, 20,
                500, 1L
        );

        City cityC = new City(
                3L, "Berlin",
                30, 30,
                800, 2L
        );

        City neutralCity = new City(
                4L, "Neutral Village",
                40, 40,
                100, null
        );

        manager.registerCity(cityA);
        manager.registerCity(cityB);
        manager.registerCity(cityC);
        manager.registerCity(neutralCity);

        assertEquals(
                List.of(cityA, cityB),
                manager.getCitiesByCivilization(1L)
        );

        assertEquals(
                List.of(cityC),
                manager.getCitiesByCivilization(2L)
        );

        assertEquals(
                List.of(neutralCity),
                manager.getCitiesByCivilization(null)
        );

        // Ownership changes must be reflected.
        neutralCity.setCivilizationId(2L);

        assertEquals(
                List.of(cityC, neutralCity),
                manager.getCitiesByCivilization(2L)
        );

        assertTrue(
                manager.getCitiesByCivilization(null).isEmpty()
        );
    }

    @Test
    void returnsImmutableCityListSnapshot() {

        CityManager manager = new CityManager();

        City first = new City(
                1L, "Warsaw",
                10, 10,
                1000, 1L
        );

        manager.registerCity(first);

        List<City> snapshot = manager.getCities();

        assertEquals(1, snapshot.size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.clear()
        );

        manager.registerCity(
                new City(
                        2L, "Krakow",
                        20, 20,
                        500, 1L
                )
        );

        // The original collection snapshot is unchanged.
        assertEquals(1, snapshot.size());

        // The manager contains both cities.
        assertEquals(2, manager.getCities().size());
    }
}
