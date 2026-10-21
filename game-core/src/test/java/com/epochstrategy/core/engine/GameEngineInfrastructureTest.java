
package com.epochstrategy.core.engine;

import com.epochstrategy.core.building.BuildingType;
import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityPopulationService;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.world.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineInfrastructureTest {

    @Test
    void warehouseIncreasesStorageCapacityOnce() {

        GameState state = new GameState();

        City city = createCity(400);
        state.getCityManager().registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 30
        );

        city.getInventory().add(
                ResourceType.STONE, 20
        );

        assertEquals(
                200,
                city.getInventory().getCapacity()
        );

        state.getCityBuildingManager().construct(
                city.getId(),
                BuildingType.WAREHOUSE,
                state.getCityManager()
        );

        assertEquals(
                300,
                city.getInventory().getCapacity()
        );

        assertEquals(
                10,
                city.getInventory().getAmount(ResourceType.WOOD)
        );

        assertEquals(
                8,
                city.getInventory().getAmount(ResourceType.STONE)
        );

        assertThrows(
                IllegalStateException.class,
                () -> state.getCityBuildingManager().construct(
                        city.getId(),
                        BuildingType.WAREHOUSE,
                        state.getCityManager()
                )
        );

        assertEquals(
                300,
                city.getInventory().getCapacity()
        );
    }

    @Test
    void housingIncreasesPopulationCapacity() {

        WorldMap map = createMap();

        // Freshwater next to city.
        map.setTile(
                new Tile(
                        3, 2,
                        TerrainType.LAKE,
                        1
                )
        );

        GameState state = new GameState();

        City city = createCity(990);
        state.getCityManager().registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 12
        );

        city.getInventory().add(
                ResourceType.STONE, 8
        );

        city.getInventory().add(
                ResourceType.FOOD, 30
        );

        state.getCityBuildingManager().construct(
                city.getId(),
                BuildingType.HOUSING,
                state.getCityManager()
        );

        assertEquals(
                1500,
                CityPopulationService.getPopulationCapacity(
                        city,
                        BuildingType.HOUSING
                                .getPopulationCapacityBonus()
                )
        );

        GameEngine engine = new GameEngine(
                state,
                map,
                new RiverNetwork(7, 7)
        );

        engine.endTurn();

        // 990 + 3% growth (29 people).
        assertEquals(
                1019,
                city.getPopulation()
        );

        assertEquals(2, state.getTurnNumber());
    }

    @Test
    void cityWithoutHousingStopsAtOldLimit() {

        WorldMap map = createMap();

        map.setTile(
                new Tile(
                        3, 2,
                        TerrainType.LAKE,
                        1
                )
        );

        GameState state = new GameState();

        City city = createCity(990);
        state.getCityManager().registerCity(city);

        city.getInventory().add(
                ResourceType.FOOD, 30
        );

        GameEngine engine = new GameEngine(
                state,
                map,
                new RiverNetwork(7, 7)
        );

        engine.endTurn();

        assertEquals(
                1000,
                city.getPopulation()
        );
    }

    @Test
    void capacityExpansionPreservesExistingResources() {

        GameState state = new GameState();

        City city = createCity(400);
        state.getCityManager().registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 30
        );

        city.getInventory().add(
                ResourceType.STONE, 20
        );

        city.getInventory().add(
                ResourceType.FOOD, 100
        );

        state.getCityBuildingManager().construct(
                city.getId(),
                BuildingType.WAREHOUSE,
                state.getCityManager()
        );

        assertEquals(
                100,
                city.getInventory().getAmount(ResourceType.FOOD)
        );

        assertEquals(
                300,
                city.getInventory().getCapacity()
        );

        assertEquals(
                118,
                city.getInventory().getTotalUnits()
        );
    }

    private static City createCity(int population) {

        return new City(
                1L,
                "Test City",
                3,
                3,
                population,
                null
        );
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
