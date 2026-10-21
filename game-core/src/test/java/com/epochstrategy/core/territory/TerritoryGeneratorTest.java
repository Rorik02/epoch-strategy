
package com.epochstrategy.core.territory;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.civilization.Civilization;
import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerritoryGeneratorTest {

    @Test
    void generatesTerritoryAroundCity() {

        WorldMap map = createMap(7, 7);

        CityManager cities = new CityManager();
        CivilizationManager civilizations =
                createCivilizations();

        TerritoryManager territories =
                new TerritoryManager();

        cities.registerCity(
                new City(
                        1L, "Rome",
                        3, 3, 400, 1L
                )
        );

        int generated = new TerritoryGenerator(1).generate(
                map,
                cities,
                civilizations,
                territories
        );

        // Center and four neighboring tiles.
        assertEquals(5, generated);

        assertTrue(
                territories.isControlledBy(3, 3, 1L)
        );

        assertTrue(
                territories.isControlledBy(4, 3, 1L)
        );

        assertFalse(
                territories.findClaim(5, 5).isPresent()
        );
    }

    @Test
    void excludesOceanButIncludesLake() {

        WorldMap map = createMap(7, 7);

        map.setTile(
                new Tile(3, 2, TerrainType.OCEAN, 1)
        );

        map.setTile(
                new Tile(2, 3, TerrainType.LAKE, 1)
        );

        CityManager cities = new CityManager();

        cities.registerCity(
                new City(
                        1L, "Rome",
                        3, 3, 400, 1L
                )
        );

        TerritoryManager territories =
                new TerritoryManager();

        new TerritoryGenerator(1).generate(
                map,
                cities,
                createCivilizations(),
                territories
        );

        assertEquals(4, territories.getClaimedTileCount());

        assertTrue(
                territories.findClaim(3, 2).isEmpty()
        );

        assertTrue(
                territories.isControlledBy(2, 3, 1L)
        );
    }

    @Test
    void closestCityControlsSharedTile() {

        WorldMap map = createMap(9, 7);

        CityManager cities = new CityManager();

        cities.registerCity(
                new City(
                        1L, "Rome",
                        2, 3, 400, 1L
                )
        );

        cities.registerCity(
                new City(
                        2L, "Egypt",
                        5, 3, 400, 2L
                )
        );

        TerritoryManager territories =
                new TerritoryManager();

        new TerritoryGenerator(3).generate(
                map,
                cities,
                createCivilizations(),
                territories
        );

        // (3,3) is closer to Rome.
        assertTrue(
                territories.isControlledBy(3, 3, 1L)
        );

        // (4,3) is closer to Egypt.
        assertTrue(
                territories.isControlledBy(4, 3, 2L)
        );

        assertEquals(
                1L,
                territories.findClaim(3, 3)
                        .orElseThrow()
                        .cityId()
        );
    }

    @Test
    void tiesAreResolvedByLowestCityId() {

        WorldMap map = createMap(7, 5);

        CityManager cities = new CityManager();

        // Register in reverse ID order deliberately.
        cities.registerCity(
                new City(
                        2L, "Egypt",
                        5, 2, 400, 2L
                )
        );

        cities.registerCity(
                new City(
                        1L, "Rome",
                        1, 2, 400, 1L
                )
        );

        TerritoryManager territories =
                new TerritoryManager();

        new TerritoryGenerator(2).generate(
                map,
                cities,
                createCivilizations(),
                territories
        );

        var claim = territories.findClaim(
                3, 2
        ).orElseThrow();

        assertEquals(1L, claim.cityId());
        assertEquals(1L, claim.civilizationId());
    }

    @Test
    void neutralCitiesDoNotGenerateTerritories() {

        WorldMap map = createMap(7, 7);

        CityManager cities = new CityManager();

        cities.registerCity(
                new City(
                        1L, "Neutral City",
                        3, 3, 400, null
                )
        );

        TerritoryManager territories =
                new TerritoryManager();

        int result = new TerritoryGenerator(3).generate(
                map,
                cities,
                createCivilizations(),
                territories
        );

        assertEquals(0, result);
        assertTrue(territories.isEmpty());
    }

    @Test
    void respectsMapBoundaries() {

        WorldMap map = createMap(5, 5);

        CityManager cities = new CityManager();

        cities.registerCity(
                new City(
                        1L, "Corner City",
                        0, 0, 400, 1L
                )
        );

        TerritoryManager territories =
                new TerritoryManager();

        new TerritoryGenerator(2).generate(
                map,
                cities,
                createCivilizations(),
                territories
        );

        assertEquals(6, territories.getClaimedTileCount());

        assertTrue(
                territories.isControlledBy(0, 0, 1L)
        );
    }

    @Test
    void rejectsUnknownCivilization() {

        WorldMap map = createMap(7, 7);

        CityManager cities = new CityManager();

        cities.registerCity(
                new City(
                        1L, "Unknown City",
                        3, 3, 400, 999L
                )
        );

        TerritoryManager territories =
                new TerritoryManager();

        assertThrows(
                IllegalStateException.class,
                () -> new TerritoryGenerator(2).generate(
                        map,
                        cities,
                        createCivilizations(),
                        territories
                )
        );

        assertTrue(territories.isEmpty());
    }

    @Test
    void preventsDuplicateGeneration() {

        WorldMap map = createMap(7, 7);

        CityManager cities = new CityManager();

        cities.registerCity(
                new City(
                        1L, "Rome",
                        3, 3, 400, 1L
                )
        );

        CivilizationManager civilizations =
                createCivilizations();

        TerritoryManager territories =
                new TerritoryManager();

        TerritoryGenerator generator =
                new TerritoryGenerator(2);

        generator.generate(
                map, cities, civilizations, territories
        );

        int originalCount =
                territories.getClaimedTileCount();

        assertThrows(
                IllegalStateException.class,
                () -> generator.generate(
                        map, cities, civilizations, territories
                )
        );

        assertEquals(
                originalCount,
                territories.getClaimedTileCount()
        );
    }

    @Test
    void claimsCannotBeModifiedExternally() {

        TerritoryManager territories =
                new TerritoryManager();

        territories.claimTile(
                2, 3, 1L, 1L
        );

        var claims = territories.getClaims();

        assertThrows(
                UnsupportedOperationException.class,
                claims::clear
        );

        assertThrows(
                IllegalStateException.class,
                () -> territories.claimTile(
                        2, 3, 2L, 2L
                )
        );

        assertEquals(
                1,
                territories.getClaimedTileCount()
        );
    }

    private static CivilizationManager createCivilizations() {

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

    private static WorldMap createMap(
            int width,
            int height
    ) {

        WorldMap map =
                new WorldMap(width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                map.setTile(
                        new Tile(
                                x, y,
                                TerrainType.PLAINS,
                                1
                        )
                );
            }
        }

        return map;
    }
}
