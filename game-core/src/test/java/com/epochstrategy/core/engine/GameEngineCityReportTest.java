
package com.epochstrategy.core.engine;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.world.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineCityReportTest {

    @Test
    void storesLastTurnCityEconomyResults() {

        WorldMap map = new WorldMap(10, 10);

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                map.setTile(
                        new Tile(
                                x, y,
                                TerrainType.PLAINS,
                                1
                        )
                );
            }
        }

        // Freshwater beside the city.
        map.setTile(
                new Tile(
                        3, 2,
                        TerrainType.LAKE,
                        1
                )
        );

        RiverNetwork rivers =
                new RiverNetwork(10, 10);

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

        GameEngine engine = new GameEngine(
                state,
                map,
                rivers
        );

        assertTrue(
                state.getLastCityFoodResults().isEmpty()
        );

        engine.endTurn();

        var result = state.getLastCityFoodResults()
                .get(city.getId());

        assertNotNull(result);
        assertEquals(8, result.producedFood());
        assertEquals(4, result.consumedFood());
        assertEquals(12, result.populationChange());

        assertEquals(412, city.getPopulation());

        assertEquals(
                4,
                city.getInventory()
                        .getAmount(ResourceType.FOOD)
        );

        engine.endTurn();

        var secondResult =
                state.getLastCityFoodResults()
                        .get(city.getId());

        assertNotNull(secondResult);
        assertEquals(412, secondResult.populationBefore());
        assertEquals(424, secondResult.populationAfter());

        assertEquals(3, state.getTurnNumber());

        assertThrows(
                UnsupportedOperationException.class,
                () -> state.getLastCityFoodResults()
                        .clear()
        );
    }
}
