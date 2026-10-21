
package com.epochstrategy.core.city;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityTest {

    @Test
    void createsCityWithCorrectData() {

        City city = new City(
                1L,
                "Warsaw",
                45,
                60,
                1000,
                1L
        );

        assertEquals(1L, city.getId());
        assertEquals("Warsaw", city.getName());

        assertEquals(45, city.getX());
        assertEquals(60, city.getY());

        assertEquals(1000, city.getPopulation());
        assertEquals(1, city.getLevel());

        assertEquals(
                Long.valueOf(1L),
                city.getCivilizationId()
        );

        assertFalse(city.isNeutral());
    }

    @Test
    void createsNeutralCity() {

        City city = new City(
                2L,
                "Neutral Settlement",
                20,
                30,
                250,
                null
        );

        assertTrue(city.isNeutral());
        assertNull(city.getCivilizationId());

        city.setCivilizationId(3L);

        assertFalse(city.isNeutral());
        assertEquals(
                Long.valueOf(3L),
                city.getCivilizationId()
        );
    }

    @Test
    void updatesPopulation() {

        City city = new City(
                3L,
                "Krakow",
                30,
                40,
                500,
                1L
        );

        city.setPopulation(750);

        assertEquals(750, city.getPopulation());

        assertThrows(
                IllegalArgumentException.class,
                () -> city.setPopulation(0)
        );

        // Invalid update must not change population.
        assertEquals(750, city.getPopulation());
    }

    @Test
    void rejectsInvalidCityData() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new City(
                        0L, "City", 10, 10, 100, null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new City(
                        1L, "", 10, 10, 100, null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new City(
                        1L, "City", -1, 10, 100, null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new City(
                        1L, "City", 10, 10, -5, null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new City(
                        1L, "City", 10, 10, 100, 0L
                )
        );
    }
}
