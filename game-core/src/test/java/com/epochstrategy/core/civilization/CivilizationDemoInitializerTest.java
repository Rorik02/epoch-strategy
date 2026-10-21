
package com.epochstrategy.core.civilization;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.engine.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CivilizationDemoInitializerTest {

    @Test
    void initializesFourCivilizations() {

        GameState state = new GameState();

        CivilizationDemoInitializer.initialize(
                state.getCivilizationManager(),
                state.getCityManager()
        );

        assertEquals(
                4,
                state.getCivilizationManager()
                        .getCivilizationCount()
        );

        assertEquals(
                "Rome",
                state.getCivilizationManager()
                        .findCivilization(2L)
                        .orElseThrow()
                        .getName()
        );
    }

    @Test
    void distributesTwelveCitiesBetweenCivilizations() {

        GameState state = new GameState();

        CityManager cities = state.getCityManager();

        for (int i = 0; i < 12; i++) {

            City city = new City(
                    i + 1L,
                    "Settlement " + (i + 1),
                    i * 6,
                    10,
                    300,
                    null
            );

            cities.registerCity(city);
        }

        CivilizationDemoInitializer.initialize(
                state.getCivilizationManager(),
                cities
        );

        for (long civilizationId = 1L;
             civilizationId <= 4L;
             civilizationId++) {

            assertEquals(
                    3,
                    cities.getCitiesByCivilization(
                            civilizationId
                    ).size()
            );
        }

        assertEquals(
                Long.valueOf(1L),
                cities.findCity(1L)
                        .orElseThrow()
                        .getCivilizationId()
        );

        assertEquals(
                Long.valueOf(4L),
                cities.findCity(12L)
                        .orElseThrow()
                        .getCivilizationId()
        );
    }

    @Test
    void rejectsDuplicateInitialization() {

        GameState state = new GameState();

        CivilizationDemoInitializer.initialize(
                state.getCivilizationManager(),
                state.getCityManager()
        );

        assertThrows(
                IllegalStateException.class,
                () -> CivilizationDemoInitializer.initialize(
                        state.getCivilizationManager(),
                        state.getCityManager()
                )
        );

        assertEquals(
                4,
                state.getCivilizationManager()
                        .getCivilizationCount()
        );
    }

    @Test
    void gameStatesHaveIndependentCivilizations() {

        GameState first = new GameState();
        GameState second = new GameState();

        assertNotSame(
                first.getCivilizationManager(),
                second.getCivilizationManager()
        );

        CivilizationDemoInitializer.initialize(
                first.getCivilizationManager(),
                first.getCityManager()
        );

        assertEquals(
                4,
                first.getCivilizationManager()
                        .getCivilizationCount()
        );

        assertEquals(
                0,
                second.getCivilizationManager()
                        .getCivilizationCount()
        );
    }
}
