
package com.epochstrategy.core.territory;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerritoryExpansionServiceTest {

    private final TerritoryExpansionService service =
            new TerritoryExpansionService();

    @Test
    void initialSquareContainsNineTiles() {

        WorldMap map = createMap();

        CityManager cities = new CityManager();
        City city = createCity();

        cities.registerCity(city);

        TerritoryManager territories =
                createInitialTerritory(city);

        assertEquals(
                9,
                territories.getClaimedTileCount()
        );

        assertEquals(
                12,
                service.getEligibleTiles(
                        city, map, cities, territories
                ).size()
        );
    }

    @Test
    void expansionSpendsInfluenceAndClaimsTile() {

        WorldMap map = createMap();

        CityManager cities = new CityManager();
        City city = createCity();

        cities.registerCity(city);

        TerritoryManager territories =
                createInitialTerritory(city);

        CityInfluenceManager influence =
                new CityInfluenceManager();

        // 500 citizens = 1 Influence per turn.
        for (int i = 0; i < 25; i++) {
            influence.advanceCityTurn(city);
        }

        service.expand(
                city,
                6, 4,
                map,
                cities,
                territories,
                influence
        );

        assertEquals(
                10,
                territories.getClaimedTileCount()
        );

        assertEquals(
                city.getId(),
                territories.findClaim(6, 4)
                        .orElseThrow()
                        .cityId()
        );

        assertEquals(
                5.0,
                influence.getBalance(city.getId()),
                0.0001
        );

        assertEquals(
                30.0,
                influence.getNextExpansionCost(city.getId()),
                0.0001
        );
    }

    @Test
    void rejectsExpansionWithoutInfluence() {

        WorldMap map = createMap();

        CityManager cities = new CityManager();
        City city = createCity();

        cities.registerCity(city);

        TerritoryManager territories =
                createInitialTerritory(city);

        CityInfluenceManager influence =
                new CityInfluenceManager();

        assertThrows(
                IllegalStateException.class,
                () -> service.expand(
                        city,
                        6, 4,
                        map,
                        cities,
                        territories,
                        influence
                )
        );

        assertTrue(
                territories.findClaim(6, 4).isEmpty()
        );
    }

    @Test
    void rejectsTilesOutsideCityLimit() {

        WorldMap map = createMap();

        CityManager cities = new CityManager();
        City city = createCity();

        cities.registerCity(city);

        TerritoryManager territories =
                createInitialTerritory(city);

        CityInfluenceManager influence =
                new CityInfluenceManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.expand(
                        city,
                        8, 4,
                        map,
                        cities,
                        territories,
                        influence
                )
        );
    }

    @Test
    void rejectsOtherCityTerritory() {

        WorldMap map = createMap();

        CityManager cities = new CityManager();
        City city = createCity();

        cities.registerCity(city);

        TerritoryManager territories =
                createInitialTerritory(city);

        territories.claimTile(
                6, 4, 2L, 2L
        );

        var eligible = service.getEligibleTiles(
                city, map, cities, territories
        );

        assertFalse(
                eligible.contains(
                        new TerritoryExpansionService
                                .ExpansionTile(6, 4)
                )
        );
    }

    @Test
    void rejectsDiagonalOnlyTile() {

        WorldMap map = createMap();

        CityManager cities = new CityManager();
        City city = createCity();

        cities.registerCity(city);

        TerritoryManager territories =
                createInitialTerritory(city);

        var eligible = service.getEligibleTiles(
                city, map, cities, territories
        );

        // Diagonally adjacent to the 3x3 square.
        assertFalse(
                eligible.contains(
                        new TerritoryExpansionService
                                .ExpansionTile(6, 6)
                )
        );
    }

    private City createCity() {
        return new City(
                1L, "Rome", 4, 4, 500, 1L
        );
    }

    private TerritoryManager createInitialTerritory(
            City city
    ) {

        TerritoryManager territories =
                new TerritoryManager();

        for (int x = 3; x <= 5; x++) {
            for (int y = 3; y <= 5; y++) {

                territories.claimTile(
                        x, y,
                        city.getCivilizationId(),
                        city.getId()
                );
            }
        }

        return territories;
    }

    private WorldMap createMap() {

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

        return map;
    }
}
