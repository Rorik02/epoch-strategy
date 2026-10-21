
package com.epochstrategy.core.engine;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.world.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineCityTest {

    @Test
    void advancesCityEconomyOnEndTurn() {

        WorldMap map = createMap(10, 10);

        RiverNetwork rivers =
                new RiverNetwork(10, 10);

        // Freshwater next to the first city.
        map.setTile(
                new Tile(
                        3, 2,
                        TerrainType.LAKE,
                        1
                )
        );

        GameState state = new GameState();

        City riversideCity = new City(
                1L,
                "Riverside",
                3,
                3,
                400,
                null
        );

        City dryCity = new City(
                2L,
                "Drylands",
                7,
                7,
                400,
                null
        );

        state.getCityManager()
                .registerCity(riversideCity);

        state.getCityManager()
                .registerCity(dryCity);

        GameEngine engine = new GameEngine(
                state,
                map,
                rivers
        );

        var report = engine.endTurn();

        // Freshwater city grows.
        assertEquals(
                412,
                riversideCity.getPopulation()
        );

        assertEquals(
                4,
                riversideCity.getInventory()
                        .getAmount(ResourceType.FOOD)
        );

        // City with insufficient food loses population.
        assertEquals(
                395,
                dryCity.getPopulation()
        );

        assertEquals(
                0,
                dryCity.getInventory()
                        .getAmount(ResourceType.FOOD)
        );

        // Calendar advances exactly once.
        assertEquals(1, report.completedTurn());
        assertEquals(2, report.nextTurn());
        assertEquals(2, state.getTurnNumber());
    }

    @Test
    void calendarOnlyEnginePreservesCityEconomy() {

        GameState state = new GameState();

        City city = new City(
                1L,
                "Test City",
                3,
                3,
                400,
                null
        );

        state.getCityManager().registerCity(city);

        GameEngine engine = new GameEngine(state);

        engine.endTurn();

        // No world attached, so no economy simulation.
        assertEquals(400, city.getPopulation());
        assertEquals(2, state.getTurnNumber());
    }

    private static WorldMap createMap(
            int width,
            int height
    ) {

        WorldMap map = new WorldMap(
                width,
                height
        );

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                map.setTile(
                        new Tile(
                                x,
                                y,
                                TerrainType.PLAINS,
                                1
                        )
                );
            }
        }

        return map;
    }
}
