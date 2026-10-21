
package com.epochstrategy.core.city;

import com.epochstrategy.core.world.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityPlacementServiceTest {

    private final CityPlacementService service =
            new CityPlacementService(5);

    @Test
    void allowsCityOnPlains() {

        WorldMap map = createMap(10, 10);
        RiverNetwork rivers = new RiverNetwork(10, 10);
        CityManager cities = new CityManager();

        var result = service.evaluate(
                map, rivers, cities, 5, 5
        );

        assertTrue(result.canPlace());

        assertEquals(
                CityPlacementService.WaterAccess.NONE,
                result.waterAccess()
        );

        assertEquals(10, result.locationScore());
    }

    @Test
    void prefersFreshwaterOverOcean() {

        WorldMap map = createMap(10, 10);
        RiverNetwork rivers = new RiverNetwork(10, 10);

        CityManager cities = new CityManager();

        map.setTile(
                new Tile(5, 4, TerrainType.LAKE, 1)
        );

        map.setTile(
                new Tile(6, 5, TerrainType.OCEAN, 0)
        );

        var result = service.evaluate(
                map, rivers, cities, 5, 5
        );

        assertTrue(result.canPlace());

        assertEquals(
                CityPlacementService.WaterAccess.FRESHWATER,
                result.waterAccess()
        );

        assertEquals(40, result.locationScore());
    }

    @Test
    void recognizesCoastalLocation() {

        WorldMap map = createMap(10, 10);
        RiverNetwork rivers = new RiverNetwork(10, 10);

        map.setTile(
                new Tile(5, 4, TerrainType.OCEAN, 0)
        );

        var result = service.evaluate(
                map,
                rivers,
                new CityManager(),
                5,
                5
        );

        assertEquals(
                CityPlacementService.WaterAccess.COASTAL,
                result.waterAccess()
        );

        assertEquals(25, result.locationScore());
    }

    @Test
    void rejectsCityOnWater() {

        WorldMap map = createMap(10, 10);
        RiverNetwork rivers = new RiverNetwork(10, 10);

        map.setTile(
                new Tile(5, 5, TerrainType.LAKE, 1)
        );

        var result = service.evaluate(
                map,
                rivers,
                new CityManager(),
                5,
                5
        );

        assertFalse(result.canPlace());

        assertEquals(
                CityPlacementService.PlacementStatus.INVALID_TERRAIN,
                result.status()
        );
    }

    @Test
    void rejectsCitiesThatAreTooClose() {

        WorldMap map = createMap(15, 15);
        RiverNetwork rivers = new RiverNetwork(15, 15);

        CityManager cities = new CityManager();

        cities.registerCity(
                new City(
                        1L, "Warsaw",
                        5, 5,
                        1000, 1L
                )
        );

        var tooClose = service.evaluate(
                map, rivers, cities, 7, 5
        );

        assertEquals(
                CityPlacementService.PlacementStatus.TOO_CLOSE_TO_CITY,
                tooClose.status()
        );

        var farEnough = service.evaluate(
                map, rivers, cities, 10, 5
        );

        assertTrue(farEnough.canPlace());
    }

    @Test
    void registersCityOnlyAtValidLocation() {

        WorldMap map = createMap(10, 10);
        RiverNetwork rivers = new RiverNetwork(10, 10);
        CityManager cities = new CityManager();

        City city = new City(
                1L, "Opole",
                5, 5,
                500, null
        );

        service.placeCity(
                map, rivers, cities, city
        );

        assertEquals(1, cities.getCityCount());

        assertSame(
                city,
                cities.findCityAt(5, 5).orElseThrow()
        );

        City secondCity = new City(
                2L, "Second City",
                6, 5,
                300, null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.placeCity(
                        map, rivers, cities, secondCity
                )
        );

        assertEquals(1, cities.getCityCount());
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
