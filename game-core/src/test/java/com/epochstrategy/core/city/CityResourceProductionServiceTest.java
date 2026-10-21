
package com.epochstrategy.core.city;

import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityResourceProductionServiceTest {

    @Test
    void producesWoodFromAdjacentForests() {

        WorldMap map = createMap(7, 7);
        City city = createCity(3, 3);

        setTerrain(map, 3, 2, TerrainType.FOREST);
        setTerrain(map, 4, 3, TerrainType.FOREST);

        var result =
                CityResourceProductionService.advanceTurn(
                        city, map
                );

        assertEquals(4, result.woodProduced());
        assertEquals(0, result.stoneProduced());
        assertEquals(0, result.ironProduced());

        assertEquals(
                4,
                city.getInventory()
                        .getAmount(ResourceType.WOOD)
        );
    }

    @Test
    void producesStoneFromHills() {

        WorldMap map = createMap(7, 7);
        City city = createCity(3, 3);

        setTerrain(map, 2, 3, TerrainType.HILLS);
        setTerrain(map, 4, 3, TerrainType.HILLS);

        var result =
                CityResourceProductionService.advanceTurn(
                        city, map
                );

        assertEquals(4, result.stoneProduced());

        assertEquals(
                4,
                city.getInventory()
                        .getAmount(ResourceType.STONE)
        );
    }

    @Test
    void producesStoneAndIronFromMountains() {

        WorldMap map = createMap(7, 7);
        City city = createCity(3, 3);

        setTerrain(map, 3, 2, TerrainType.MOUNTAINS);
        setTerrain(map, 4, 3, TerrainType.MOUNTAINS);

        var result =
                CityResourceProductionService.advanceTurn(
                        city, map
                );

        assertEquals(2, result.stoneProduced());
        assertEquals(2, result.ironProduced());

        assertEquals(
                2,
                city.getInventory()
                        .getAmount(ResourceType.IRON)
        );
    }

    @Test
    void ignoresDiagonalTiles() {

        WorldMap map = createMap(7, 7);
        City city = createCity(3, 3);

        setTerrain(map, 2, 2, TerrainType.FOREST);
        setTerrain(map, 4, 4, TerrainType.MOUNTAINS);

        var result =
                CityResourceProductionService.advanceTurn(
                        city, map
                );

        assertEquals(0, result.totalProduced());
        assertEquals(0, city.getInventory().getTotalUnits());
    }

    @Test
    void includesTerrainUnderCity() {

        WorldMap map = createMap(7, 7);
        City city = createCity(3, 3);

        setTerrain(map, 3, 3, TerrainType.FOREST);

        var result =
                CityResourceProductionService.advanceTurn(
                        city, map
                );

        assertEquals(2, result.woodProduced());
        assertEquals(2, result.totalProduced());
    }

    @Test
    void respectsWarehouseCapacity() {

        WorldMap map = createMap(7, 7);
        City city = createCity(3, 3);

        setTerrain(map, 3, 2, TerrainType.FOREST);
        setTerrain(map, 4, 3, TerrainType.HILLS);

        city.getInventory().add(
                ResourceType.FOOD,
                198
        );

        var result =
                CityResourceProductionService.advanceTurn(
                        city, map
                );

        assertEquals(2, result.woodProduced());
        assertEquals(0, result.stoneProduced());

        assertEquals(200, result.storageUsed());
        assertEquals(200, result.storageCapacity());

        // Existing food must not be modified.
        assertEquals(
                198,
                city.getInventory()
                        .getAmount(ResourceType.FOOD)
        );
    }

    @Test
    void handlesCitiesAtMapEdges() {

        WorldMap map = createMap(5, 5);
        City city = createCity(0, 0);

        setTerrain(map, 0, 1, TerrainType.MOUNTAINS);

        var result =
                CityResourceProductionService.advanceTurn(
                        city, map
                );

        assertEquals(1, result.stoneProduced());
        assertEquals(1, result.ironProduced());
        assertEquals(2, result.totalProduced());
    }

    @Test
    void resourceProductionAccumulatesAcrossTurns() {

        WorldMap map = createMap(7, 7);
        City city = createCity(3, 3);

        setTerrain(map, 3, 2, TerrainType.FOREST);

        CityResourceProductionService.advanceTurn(
                city, map
        );

        CityResourceProductionService.advanceTurn(
                city, map
        );

        assertEquals(
                4,
                city.getInventory()
                        .getAmount(ResourceType.WOOD)
        );

        // Resource production does not change population.
        assertEquals(400, city.getPopulation());
    }

    private static City createCity(int x, int y) {

        return new City(
                1L,
                "Test City",
                x,
                y,
                400,
                null
        );
    }

    private static WorldMap createMap(
            int width,
            int height
    ) {

        WorldMap map =
                new WorldMap(width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                setTerrain(
                        map,
                        x,
                        y,
                        TerrainType.PLAINS
                );
            }
        }

        return map;
    }

    private static void setTerrain(
            WorldMap map,
            int x,
            int y,
            TerrainType terrain
    ) {

        map.setTile(
                new Tile(
                        x,
                        y,
                        terrain,
                        1
                )
        );
    }
}
