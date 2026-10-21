
package com.epochstrategy.core.territory;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.city.CityManager;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CityAutomaticExpansionServiceTest {

    private final CityAutomaticExpansionService service =
            new CityAutomaticExpansionService();

    @Test
    void manualCityDoesNotExpandAutomatically() {

        TestContext context = createContext();

        generateInfluence(context, 25);

        var results = advance(context);

        assertTrue(results.isEmpty());

        assertEquals(
                9,
                context.territories.getClaimedTileCount()
        );

        assertEquals(
                25.0,
                context.influence.getBalance(1L),
                0.0001
        );
    }

    @Test
    void automaticCityBuysBestAvailableTile() {

        TestContext context = createContext();

        context.map.setTile(
                new Tile(
                        6, 4,
                        TerrainType.FOREST,
                        1
                )
        );

        context.modes.setMode(
                1L,
                CityExpansionMode.AUTOMATIC
        );

        generateInfluence(context, 25);

        var results = advance(context);

        assertEquals(
                List.of(
                        new CityAutomaticExpansionService
                                .AutomaticExpansion(
                                1L, 6, 4
                        )
                ),
                results
        );

        assertEquals(
                10,
                context.territories.getClaimedTileCount()
        );

        assertEquals(
                1L,
                context.territories.findClaim(6, 4)
                        .orElseThrow()
                        .cityId()
        );

        assertEquals(
                5.0,
                context.influence.getBalance(1L),
                0.0001
        );

        assertEquals(
                30.0,
                context.influence.getNextExpansionCost(1L),
                0.0001
        );
    }

    @Test
    void automaticCityWaitsForEnoughInfluence() {

        TestContext context = createContext();

        context.modes.setMode(
                1L,
                CityExpansionMode.AUTOMATIC
        );

        generateInfluence(context, 10);

        assertTrue(
                advance(context).isEmpty()
        );

        assertEquals(
                9,
                context.territories.getClaimedTileCount()
        );

        assertEquals(
                10.0,
                context.influence.getBalance(1L),
                0.0001
        );
    }

    @Test
    void fullCityDoesNotExpandOrSpendInfluence() {

        TestContext context = createContext();

        context.territories.clear();

        // Complete 7x7 city territory.
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y <= 7; y++) {

                context.territories.claimTile(
                        x, y, 1L, 1L
                );
            }
        }

        context.modes.setMode(
                1L,
                CityExpansionMode.AUTOMATIC
        );

        generateInfluence(context, 25);

        assertTrue(
                advance(context).isEmpty()
        );

        assertEquals(
                49,
                context.territories.getClaimedTileCount()
        );

        assertEquals(
                25.0,
                context.influence.getBalance(1L),
                0.0001
        );
    }

    @Test
    void expansionModeIsIndependentForEachCity() {

        CityExpansionManager manager =
                new CityExpansionManager();

        assertEquals(
                CityExpansionMode.MANUAL,
                manager.getMode(1L)
        );

        manager.toggleMode(1L);

        assertEquals(
                CityExpansionMode.AUTOMATIC,
                manager.getMode(1L)
        );

        assertEquals(
                CityExpansionMode.MANUAL,
                manager.getMode(2L)
        );

        manager.toggleMode(1L);

        assertEquals(
                CityExpansionMode.MANUAL,
                manager.getMode(1L)
        );
    }

    private void generateInfluence(
            TestContext context,
            int turns
    ) {

        for (int i = 0; i < turns; i++) {
            context.influence.advanceCityTurn(
                    context.city
            );
        }
    }

    private List<CityAutomaticExpansionService.AutomaticExpansion>
    advance(TestContext context) {

        return service.advanceTurn(
                context.map,
                context.cities,
                context.territories,
                context.influence,
                context.modes
        );
    }

    private TestContext createContext() {

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

        City city = new City(
                1L, "Rome", 4, 4, 500, 1L
        );

        CityManager cities = new CityManager();
        cities.registerCity(city);

        TerritoryManager territories =
                new TerritoryManager();

        for (int x = 3; x <= 5; x++) {
            for (int y = 3; y <= 5; y++) {

                territories.claimTile(
                        x, y, 1L, 1L
                );
            }
        }

        return new TestContext(
                map,
                city,
                cities,
                territories,
                new CityInfluenceManager(),
                new CityExpansionManager()
        );
    }

    private record TestContext(
            WorldMap map,
            City city,
            CityManager cities,
            TerritoryManager territories,
            CityInfluenceManager influence,
            CityExpansionManager modes
    ) {
    }
}
