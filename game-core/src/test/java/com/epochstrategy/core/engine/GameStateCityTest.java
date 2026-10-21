
package com.epochstrategy.core.engine;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateCityTest {

    @Test
    void gameStateContainsCityManager() {

        GameState gameState = new GameState();

        assertNotNull(gameState.getCityManager());

        assertEquals(
                0,
                gameState.getCityManager().getCityCount()
        );
    }

    @Test
    void citiesRemainRegisteredInGameState() {

        GameState gameState = new GameState();

        City city = new City(
                1L,
                "Warsaw",
                45,
                60,
                1000,
                1L
        );

        gameState.getCityManager().registerCity(city);

        CityManager manager =
                gameState.getCityManager();

        assertEquals(1, manager.getCityCount());

        assertSame(
                city,
                manager.findCity(1L).orElseThrow()
        );

        assertSame(
                city,
                manager.findCityAt(45, 60).orElseThrow()
        );

        // Every access returns the same manager.
        assertSame(
                manager,
                gameState.getCityManager()
        );
    }

    @Test
    void differentGameStatesHaveIndependentCities() {

        GameState firstGame = new GameState();
        GameState secondGame = new GameState();

        firstGame.getCityManager().registerCity(
                new City(
                        1L,
                        "Krakow",
                        20,
                        30,
                        500,
                        null
                )
        );

        assertEquals(
                1,
                firstGame.getCityManager().getCityCount()
        );

        assertEquals(
                0,
                secondGame.getCityManager().getCityCount()
        );

        assertNotSame(
                firstGame.getCityManager(),
                secondGame.getCityManager()
        );
    }
}
