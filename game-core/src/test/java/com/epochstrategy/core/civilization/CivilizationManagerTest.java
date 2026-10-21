
package com.epochstrategy.core.civilization;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CivilizationManagerTest {

    @Test
    void createsCivilizationWithValidData() {

        Civilization rome = new Civilization(
                1L,
                "Rome",
                0xB22222
        );

        assertEquals(1L, rome.getId());
        assertEquals("Rome", rome.getName());
        assertEquals(0xB22222, rome.getColorRgb());
        assertEquals("#B22222", rome.getHexColor());

        assertThrows(
                IllegalArgumentException.class,
                () -> new Civilization(
                        0L, "Rome", 0xB22222
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Civilization(
                        1L, "", 0xB22222
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Civilization(
                        1L, "Rome", -1
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Civilization(
                        1L, "Rome", 0x1000000
                )
        );
    }

    @Test
    void registersAndFindsCivilizations() {

        CivilizationManager manager =
                new CivilizationManager();

        Civilization rome =
                new Civilization(1L, "Rome", 0xB22222);

        Civilization egypt =
                new Civilization(2L, "Egypt", 0xDAA520);

        manager.registerCivilization(rome);
        manager.registerCivilization(egypt);

        assertEquals(2, manager.getCivilizationCount());

        assertSame(
                rome,
                manager.findCivilization(1L).orElseThrow()
        );

        assertSame(
                egypt,
                manager.findCivilizationByName(
                        " EGYPT "
                ).orElseThrow()
        );

        assertEquals(
                List.of(rome, egypt),
                manager.getCivilizations()
        );
    }

    @Test
    void rejectsDuplicateIdsAndNames() {

        CivilizationManager manager =
                new CivilizationManager();

        manager.registerCivilization(
                new Civilization(1L, "Rome", 0xB22222)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.registerCivilization(
                        new Civilization(
                                1L, "Egypt", 0xDAA520
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.registerCivilization(
                        new Civilization(
                                2L, "ROME", 0xDAA520
                        )
                )
        );

        assertEquals(1, manager.getCivilizationCount());
    }

    @Test
    void assignsCityToCivilization() {

        CivilizationManager civilizations =
                new CivilizationManager();

        CityManager cities = new CityManager();

        civilizations.registerCivilization(
                new Civilization(
                        1L, "Rome", 0xB22222
                )
        );

        City city = new City(
                1L,
                "Settlement 1",
                10,
                20,
                500,
                null
        );

        cities.registerCity(city);

        civilizations.assignCity(
                1L,
                1L,
                cities
        );

        assertFalse(city.isNeutral());

        assertEquals(
                Long.valueOf(1L),
                city.getCivilizationId()
        );

        assertEquals(
                List.of(city),
                cities.getCitiesByCivilization(1L)
        );
    }

    @Test
    void transfersCityAndMakesItNeutral() {

        CivilizationManager civilizations =
                new CivilizationManager();

        CityManager cities = new CityManager();

        civilizations.registerCivilization(
                new Civilization(1L, "Rome", 0xB22222)
        );

        civilizations.registerCivilization(
                new Civilization(2L, "Egypt", 0xDAA520)
        );

        City city = new City(
                1L, "Settlement 1",
                10, 20,
                500, null
        );

        cities.registerCity(city);

        civilizations.assignCity(1L, 1L, cities);

        assertEquals(
                1,
                cities.getCitiesByCivilization(1L).size()
        );

        civilizations.assignCity(1L, 2L, cities);

        assertTrue(
                cities.getCitiesByCivilization(1L).isEmpty()
        );

        assertEquals(
                List.of(city),
                cities.getCitiesByCivilization(2L)
        );

        civilizations.makeCityNeutral(1L, cities);

        assertTrue(city.isNeutral());

        assertEquals(
                List.of(city),
                cities.getCitiesByCivilization(null)
        );
    }

    @Test
    void rejectsUnknownCityAndCivilization() {

        CivilizationManager civilizations =
                new CivilizationManager();

        CityManager cities = new CityManager();

        civilizations.registerCivilization(
                new Civilization(1L, "Rome", 0xB22222)
        );

        City city = new City(
                1L, "Settlement",
                10, 20,
                500, null
        );

        cities.registerCity(city);

        assertThrows(
                IllegalArgumentException.class,
                () -> civilizations.assignCity(
                        1L, 999L, cities
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> civilizations.assignCity(
                        999L, 1L, cities
                )
        );

        assertTrue(city.isNeutral());
    }
}
