
package com.epochstrategy.core.civilization;

import com.epochstrategy.core.city.City;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerCivilizationManagerTest {

    @Test
    void noCivilizationSelectedByDefault() {

        PlayerCivilizationManager manager =
                new PlayerCivilizationManager();

        assertFalse(
                manager.hasSelectedCivilization()
        );

        assertTrue(
                manager.getPlayerCivilizationId().isEmpty()
        );
    }

    @Test
    void selectsRegisteredCivilization() {

        CivilizationManager civilizations =
                createCivilizations();

        PlayerCivilizationManager manager =
                new PlayerCivilizationManager();

        manager.selectCivilization(
                1L,
                civilizations
        );

        assertTrue(
                manager.isPlayerCivilization(1L)
        );

        assertFalse(
                manager.isPlayerCivilization(2L)
        );

        assertEquals(
                1L,
                manager.getPlayerCivilizationId()
                        .orElseThrow()
        );
    }

    @Test
    void rejectsUnknownCivilization() {

        PlayerCivilizationManager manager =
                new PlayerCivilizationManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.selectCivilization(
                        999L,
                        createCivilizations()
                )
        );

        assertFalse(
                manager.hasSelectedCivilization()
        );
    }

    @Test
    void controlsOnlyPlayerCities() {

        PlayerCivilizationManager manager =
                new PlayerCivilizationManager();

        manager.selectCivilization(
                1L,
                createCivilizations()
        );

        City playerCity = new City(
                1L, "Rome",
                3, 3, 500, 1L
        );

        City aiCity = new City(
                2L, "Egypt",
                7, 7, 500, 2L
        );

        City neutralCity = new City(
                3L, "Neutral",
                10, 10, 500, null
        );

        assertTrue(
                manager.controlsCity(playerCity)
        );

        assertFalse(
                manager.controlsCity(aiCity)
        );

        assertFalse(
                manager.controlsCity(neutralCity)
        );
    }

    private CivilizationManager createCivilizations() {

        CivilizationManager manager =
                new CivilizationManager();

        manager.registerCivilization(
                new Civilization(
                        1L, "Rome", 0xB22222
                )
        );

        manager.registerCivilization(
                new Civilization(
                        2L, "Egypt", 0xDAA520
                )
        );

        return manager;
    }
}
