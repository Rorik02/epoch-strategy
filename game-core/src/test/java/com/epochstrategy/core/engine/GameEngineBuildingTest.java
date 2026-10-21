
package com.epochstrategy.core.engine;

import com.epochstrategy.core.building.BuildingType;
import com.epochstrategy.core.city.City;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.world.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineBuildingTest {

    @Test
    void buildingsIncreaseProductionDuringTurn() {

        WorldMap map = createMap();

        map.setTile(
                new Tile(3, 2, TerrainType.LAKE, 1)
        );

        map.setTile(
                new Tile(4, 3, TerrainType.FOREST, 1)
        );

        map.setTile(
                new Tile(2, 3, TerrainType.HILLS, 1)
        );

        GameState state = new GameState();

        City city = new City(
                1L, "Rome",
                3, 3, 400, null
        );

        state.getCityManager().registerCity(city);

        // Starting construction materials.
        city.getInventory().add(
                ResourceType.WOOD, 30
        );

        city.getInventory().add(
                ResourceType.STONE, 20
        );

        state.getCityBuildingManager().construct(
                city.getId(),
                BuildingType.FARM,
                state.getCityManager()
        );

        state.getCityBuildingManager().construct(
                city.getId(),
                BuildingType.SAWMILL,
                state.getCityManager()
        );

        GameEngine engine = new GameEngine(
                state,
                map,
                new RiverNetwork(7, 7)
        );

        engine.endTurn();

        var foodResult =
                state.getLastCityFoodResults()
                        .get(city.getId());

        var resourceResult =
                state.getLastCityResourceResults()
                        .get(city.getId());

        assertNotNull(foodResult);
        assertNotNull(resourceResult);

        // Freshwater 8 + farm 4.
        assertEquals(12, foodResult.producedFood());

        // Forest 2 + sawmill 3.
        assertEquals(5, resourceResult.woodProduced());

        // Hills produce 2 stone.
        assertEquals(2, resourceResult.stoneProduced());

        assertEquals(412, city.getPopulation());

        // Food production 12 - consumption 4.
        assertEquals(
                8,
                city.getInventory().getAmount(
                        ResourceType.FOOD
                )
        );

        // Wood after construction: 30 - 8 - 10 = 12.
        // One turn adds 5.
        assertEquals(
                17,
                city.getInventory().getAmount(
                        ResourceType.WOOD
                )
        );

        // Stone after construction: 20 - 4 - 5 = 11.
        // One turn adds 2.
        assertEquals(
                13,
                city.getInventory().getAmount(
                        ResourceType.STONE
                )
        );

        // Second turn should apply bonuses again.
        engine.endTurn();

        assertEquals(
                15,
                city.getInventory().getAmount(
                        ResourceType.FOOD
                )
        );

        assertEquals(
                22,
                city.getInventory().getAmount(
                        ResourceType.WOOD
                )
        );

        assertEquals(424, city.getPopulation());
        assertEquals(3, state.getTurnNumber());
    }

    @Test
    void farmPreventsFoodShortage() {

        WorldMap map = createMap();

        GameState state = new GameState();

        City city = new City(
                1L,
                "Dry City",
                3, 3,
                400,
                null
        );

        state.getCityManager().registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 8
        );

        city.getInventory().add(
                ResourceType.STONE, 4
        );

        state.getCityBuildingManager().construct(
                city.getId(),
                BuildingType.FARM,
                state.getCityManager()
        );

        GameEngine engine = new GameEngine(
                state,
                map,
                new RiverNetwork(7, 7)
        );

        engine.endTurn();

        var result =
                state.getLastCityFoodResults()
                        .get(city.getId());

        // No water: 3 food + farm: 4 food.
        assertEquals(7, result.producedFood());

        assertFalse(result.starving());
        assertEquals(404, city.getPopulation());
    }

    private static WorldMap createMap() {

        WorldMap map = new WorldMap(7, 7);

        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {

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
