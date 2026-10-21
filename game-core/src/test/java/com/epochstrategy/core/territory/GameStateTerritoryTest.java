
package com.epochstrategy.core.engine;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.civilization.Civilization;
import com.epochstrategy.core.territory.TerritoryGenerator;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTerritoryTest {

    @Test
    void gameStatesHaveIndependentTerritories() {

        GameState first = new GameState();
        GameState second = new GameState();

        assertNotSame(
                first.getTerritoryManager(),
                second.getTerritoryManager()
        );

        first.getTerritoryManager().claimTile(
                3, 3, 1L, 1L
        );

        assertEquals(
                1,
                first.getTerritoryManager()
                        .getClaimedTileCount()
        );

        assertTrue(
                second.getTerritoryManager().isEmpty()
        );
    }

    @Test
    void generatesTerritoriesForRegisteredCivilizations() {

        WorldMap map = createMap(9, 9);

        GameState state = new GameState();

        state.getCivilizationManager()
                .registerCivilization(
                        new Civilization(
                                1L, "Rome", 0xB22222
                        )
                );

        state.getCivilizationManager()
                .registerCivilization(
                        new Civilization(
                                2L, "Egypt", 0xDAA520
                        )
                );

        state.getCityManager().registerCity(
                new City(
                        1L, "Roma",
                        2, 4, 400, 1L
                )
        );

        state.getCityManager().registerCity(
                new City(
                        2L, "Memphis",
                        6, 4, 400, 2L
                )
        );

        TerritoryGenerator generator =
                new TerritoryGenerator(3);

        int generated = generator.generate(
                map,
                state.getCityManager(),
                state.getCivilizationManager(),
                state.getTerritoryManager()
        );

        assertTrue(generated > 0);

        assertTrue(
                state.getTerritoryManager()
                        .isControlledBy(2, 4, 1L)
        );

        assertTrue(
                state.getTerritoryManager()
                        .isControlledBy(6, 4, 2L)
        );

        // Equal distance: city with lower ID wins.
        var middleClaim =
                state.getTerritoryManager()
                        .findClaim(4, 4)
                        .orElseThrow();

        assertEquals(
                1L,
                middleClaim.civilizationId()
        );

        assertEquals(
                1L,
                middleClaim.cityId()
        );

        int romanTiles =
                state.getTerritoryManager()
                        .getClaimedTileCount(1L);

        int egyptianTiles =
                state.getTerritoryManager()
                        .getClaimedTileCount(2L);

        assertEquals(
                generated,
                romanTiles + egyptianTiles
        );
    }

    @Test
    void territoriesCanBeRegeneratedAfterReset() {

        WorldMap map = createMap(9, 9);
        GameState state = new GameState();

        state.getCivilizationManager()
                .registerCivilization(
                        new Civilization(
                                1L, "Rome", 0xB22222
                        )
                );

        City city = new City(
                1L, "Roma",
                4, 4, 400, 1L
        );

        state.getCityManager().registerCity(city);

        TerritoryGenerator generator =
                new TerritoryGenerator(2);

        int firstCount = generator.generate(
                map,
                state.getCityManager(),
                state.getCivilizationManager(),
                state.getTerritoryManager()
        );

        state.getTerritoryManager().clear();

        assertTrue(
                state.getTerritoryManager().isEmpty()
        );

        int secondCount = generator.generate(
                map,
                state.getCityManager(),
                state.getCivilizationManager(),
                state.getTerritoryManager()
        );

        assertEquals(firstCount, secondCount);

        assertTrue(
                state.getTerritoryManager()
                        .isControlledBy(4, 4, 1L)
        );
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
