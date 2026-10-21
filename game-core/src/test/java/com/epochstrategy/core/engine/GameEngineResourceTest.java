
package com.epochstrategy.core.engine;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.world.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineResourceTest {

    @Test
    void updatesFoodAndResourcesEveryTurn() {

        WorldMap map = createMap(7, 7);

        // Freshwater for food production.
        map.setTile(
                new Tile(3, 2, TerrainType.LAKE, 1)
        );

        // Wood and stone production.
        map.setTile(
                new Tile(4, 3, TerrainType.FOREST, 1)
        );

        map.setTile(
                new Tile(2, 3, TerrainType.HILLS, 1)
        );

        GameState state = new GameState();

        City city = new City(
                1L, "Test City",
                3, 3, 400, null
        );

        state.getCityManager().registerCity(city);

        GameEngine engine = new GameEngine(
                state,
                map,
                new RiverNetwork(7, 7)
        );

        // First turn.
        engine.endTurn();

        assertEquals(412, city.getPopulation());

        assertEquals(
                4,
                city.getInventory().getAmount(ResourceType.FOOD)
        );

        assertEquals(
                2,
                city.getInventory().getAmount(ResourceType.WOOD)
        );

        assertEquals(
                2,
                city.getInventory().getAmount(ResourceType.STONE)
        );

        var foodReport =
                state.getLastCityFoodResults()
                        .get(city.getId());

        var resourceReport =
                state.getLastCityResourceResults()
                        .get(city.getId());

        assertNotNull(foodReport);
        assertNotNull(resourceReport);

        assertEquals(12, foodReport.populationChange());
        assertEquals(2, resourceReport.woodProduced());
        assertEquals(2, resourceReport.stoneProduced());
        assertEquals(0, resourceReport.ironProduced());
        assertEquals(8, resourceReport.storageUsed());

        // Second turn.
        engine.endTurn();

        assertEquals(424, city.getPopulation());

        assertEquals(
                7,
                city.getInventory().getAmount(ResourceType.FOOD)
        );

        assertEquals(
                4,
                city.getInventory().getAmount(ResourceType.WOOD)
        );

        assertEquals(
                4,
                city.getInventory().getAmount(ResourceType.STONE)
        );

        assertEquals(3, state.getTurnNumber());
    }

    @Test
    void calendarOnlyEngineDoesNotProduceResources() {

        GameState state = new GameState();

        City city = new City(
                1L, "Test City",
                3, 3, 400, null
        );

        state.getCityManager().registerCity(city);

        GameEngine engine = new GameEngine(state);

        engine.endTurn();

        assertEquals(400, city.getPopulation());
        assertEquals(0, city.getInventory().getTotalUnits());

        assertTrue(
                state.getLastCityResourceResults().isEmpty()
        );

        assertEquals(2, state.getTurnNumber());
    }

    private static WorldMap createMap(
            int width,
            int height
    ) {

        WorldMap map = new WorldMap(width, height);

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
