
package com.epochstrategy.core.city;

import com.epochstrategy.core.civilization.Civilization;
import com.epochstrategy.core.engine.GameState;
import com.epochstrategy.core.unit.LandUnit;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;
import com.epochstrategy.core.economy.ResourceType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityFoundingServiceTest {

    private final CityFoundingService service =
            new CityFoundingService();

    // =========================================================
    // CITY DISTANCE SETTINGS
    // =========================================================

    @Test
    void defaultMinimumDistanceIsThree() {

        CityFoundingSettings settings =
                new CityFoundingSettings();

        assertEquals(
                3,
                settings.getMinimumCityDistance()
        );

        // 1 empty tile between city centers
        assertFalse(
                settings.isDistanceAllowed(4, 4, 6, 4)
        );

        // 2 empty tiles between city centers
        assertFalse(
                settings.isDistanceAllowed(4, 4, 7, 4)
        );

        // 2 empty tiles diagonally
        assertFalse(
                settings.isDistanceAllowed(4, 4, 7, 7)
        );

        // 3 empty tiles - allowed
        assertTrue(
                settings.isDistanceAllowed(4, 4, 8, 4)
        );

        // 3 empty tiles diagonally - allowed
        assertTrue(
                settings.isDistanceAllowed(4, 4, 8, 8)
        );
    }



    @Test
    void countsEmptyTilesBetweenCities() {

        CityFoundingSettings settings =
                new CityFoundingSettings();

        assertEquals(
                0,
                settings.countEmptyTilesBetween(
                        4, 4, 5, 4
                )
        );

        assertEquals(
                2,
                settings.countEmptyTilesBetween(
                        4, 4, 7, 4
                )
        );

        assertEquals(
                3,
                settings.countEmptyTilesBetween(
                        4, 4, 8, 4
                )
        );

        assertEquals(
                3,
                settings.countEmptyTilesBetween(
                        4, 4, 8, 8
                )
        );
    }


    @Test
    void foundedCityReceivesStartingResources() {

        TestWorld world = createWorld();

        LandUnit settler =
                createSettler(world, 8, 4);

        City city = service.foundCity(
                settler.getId(),
                world.state(),
                world.map()
        );

        var inventory = city.getInventory();

        assertEquals(
                CityFoundingBalance.STARTING_FOOD,
                inventory.getAmount(ResourceType.FOOD)
        );

        assertEquals(
                CityFoundingBalance.STARTING_WOOD,
                inventory.getAmount(ResourceType.WOOD)
        );

        assertEquals(
                CityFoundingBalance.STARTING_STONE,
                inventory.getAmount(ResourceType.STONE)
        );

        assertEquals(
                70,
                inventory.getTotalUnits()
        );

        assertTrue(
                inventory.getTotalUnits()
                        <= inventory.getCapacity()
        );
    }

    @Test
    void foundedCityStartsGeneratingInfluence() {

        TestWorld world = createWorld();

        LandUnit settler =
                createSettler(world, 8, 4);

        City city = service.foundCity(
                settler.getId(),
                world.state(),
                world.map()
        );

        var influenceManager =
                world.state()
                        .getCityInfluenceManager();

        assertEquals(
                0L,
                influenceManager.getBalanceHundredths(
                        city.getId()
                )
        );

        influenceManager.advanceCityTurn(city);

        assertEquals(
                50L,
                influenceManager.getBalanceHundredths(
                        city.getId()
                )
        );
    }



    @Test
    void minimumDistanceCanBeConfigured() {

        CityFoundingSettings settings =
                new CityFoundingSettings();

        settings.setMinimumCityDistance(4);

        assertEquals(
                4,
                settings.getMinimumCityDistance()
        );

        // 3 empty tiles - too close for this setting
        assertFalse(
                settings.isDistanceAllowed(4, 4, 8, 4)
        );

        // 4 empty tiles - allowed
        assertTrue(
                settings.isDistanceAllowed(4, 4, 9, 4)
        );

        // Negative distance is invalid
        assertThrows(
                IllegalArgumentException.class,
                () -> settings.setMinimumCityDistance(-1)
        );
    }

    // =========================================================
    // CITY FOUNDING VALIDATION
    // =========================================================

    @Test
    void cannotFoundCityTooCloseToAnotherCity() {

        TestWorld world = createWorld();

        LandUnit settler =
                createSettler(world, 6, 4);

        var result = service.checkFounding(
                settler,
                world.state(),
                world.map()
        );

        assertFalse(result.allowed());

        assertThrows(
                IllegalStateException.class,
                () -> service.foundCity(
                        settler.getId(),
                        world.state(),
                        world.map()
                )
        );

        // Existing city is unchanged
        assertEquals(
                1,
                world.state()
                        .getCityManager()
                        .getCities()
                        .size()
        );

        // Settler has not been consumed
        assertEquals(
                1,
                world.state()
                        .getLandUnitManager()
                        .getUnitCount()
        );
    }

    @Test
    void cannotFoundCityWithOnlyTwoEmptyTilesBetween() {

        TestWorld world = createWorld();

        // Existing city: (4,4)
        // Settler:       (7,4)
        // Exactly 2 empty tiles between city centers

        LandUnit settler =
                createSettler(world, 7, 4);

        var result = service.checkFounding(
                settler,
                world.state(),
                world.map()
        );

        assertFalse(result.allowed());

        assertThrows(
                IllegalStateException.class,
                () -> service.foundCity(
                        settler.getId(),
                        world.state(),
                        world.map()
                )
        );
    }

    // =========================================================
    // SUCCESSFUL CITY FOUNDING
    // =========================================================

    @Test
    void canFoundCityExactlyThreeTilesAway() {

        TestWorld world = createWorld();

        // Existing city: (4,4)
        // New city:      (8,4)
        // 3 empty tiles between them

        LandUnit settler =
                createSettler(world, 8, 4);

        var foundingCheck = service.checkFounding(
                settler,
                world.state(),
                world.map()
        );

        assertTrue(foundingCheck.allowed());

        City newCity = service.foundCity(
                settler.getId(),
                world.state(),
                world.map()
        );

        assertEquals(2L, newCity.getId());

        assertEquals(8, newCity.getX());
        assertEquals(4, newCity.getY());

        assertEquals(
                200,
                newCity.getPopulation()
        );

        assertEquals(
                1L,
                newCity.getCivilizationId()
        );

        // The new city is registered
        assertEquals(
                2,
                world.state()
                        .getCityManager()
                        .getCities()
                        .size()
        );

        // 3x3 starting territory
        assertEquals(
                9,
                world.state()
                        .getTerritoryManager()
                        .getClaimedTileCount()
        );

        // Settler is consumed
        assertTrue(
                world.state()
                        .getLandUnitManager()
                        .findUnit(settler.getId())
                        .isEmpty()
        );
    }

    @Test
    void canFoundCityDiagonallyWithThreeEmptyTiles() {

        TestWorld world = createWorld();

        // Existing city: (4,4)
        // New city:      (8,8)

        LandUnit settler =
                createSettler(world, 8, 8);

        var result = service.checkFounding(
                settler,
                world.state(),
                world.map()
        );

        assertTrue(result.allowed());

        City city = service.foundCity(
                settler.getId(),
                world.state(),
                world.map()
        );

        assertEquals(8, city.getX());
        assertEquals(8, city.getY());
    }

    // =========================================================
    // TERRITORY PROTECTION
    // =========================================================

    @Test
    void cannotFoundCityOnControlledTile() {

        TestWorld world = createWorld();

        // This tile belongs to the existing city
        world.state()
                .getTerritoryManager()
                .claimTile(
                        8, 4,
                        1L, 1L
                );

        LandUnit settler =
                createSettler(world, 8, 4);

        var check = service.checkFounding(
                settler,
                world.state(),
                world.map()
        );

        assertFalse(check.allowed());

        assertThrows(
                IllegalStateException.class,
                () -> service.foundCity(
                        settler.getId(),
                        world.state(),
                        world.map()
                )
        );
    }

    @Test
    void doesNotStealOtherCityTerritory() {

        TestWorld world = createWorld();

        // Existing city controls (7,4)
        world.state()
                .getTerritoryManager()
                .claimTile(
                        7, 4,
                        1L, 1L
                );

        // New city is far enough from existing center,
        // but its initial 3x3 area includes (7,4).
        LandUnit settler =
                createSettler(world, 8, 4);

        City newCity = service.foundCity(
                settler.getId(),
                world.state(),
                world.map()
        );

        // Existing territory must remain unchanged
        var existingClaim =
                world.state()
                        .getTerritoryManager()
                        .findClaim(7, 4)
                        .orElseThrow();

        assertEquals(
                1L,
                existingClaim.cityId()
        );

        // New city gets 8 rather than 9 tiles,
        // since one tile is already controlled.
        long newCityTerritorySize =
                world.state()
                        .getTerritoryManager()
                        .getClaims()
                        .stream()
                        .filter(claim ->
                                claim.cityId() == newCity.getId()
                        )
                        .count();

        assertEquals(
                8L,
                newCityTerritorySize
        );
    }

    // =========================================================
    // TEST WORLD HELPERS
    // =========================================================

    private record TestWorld(
            WorldMap map,
            GameState state
    ) {
    }

    private TestWorld createWorld() {

        WorldMap map = new WorldMap(14, 14);

        // Create a flat, fully passable test map.
        for (int x = 0; x < 14; x++) {
            for (int y = 0; y < 14; y++) {

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

        GameState state = new GameState();

        // Register Rome
        state.getCivilizationManager()
                .registerCivilization(
                        new Civilization(
                                1L,
                                "Rome",
                                0xB22222
                        )
                );

        // Register existing city at (4,4)
        state.getCityManager()
                .registerCity(
                        new City(
                                1L,
                                "Existing City",
                                4,
                                4,
                                500,
                                1L
                        )
                );

        return new TestWorld(
                map,
                state
        );
    }

    private LandUnit createSettler(
            TestWorld world,
            int x,
            int y
    ) {

        return world.state()
                .getLandUnitManager()
                .createSettler(
                        1L,
                        x,
                        y,
                        world.map(),
                        world.state()
                                .getCivilizationManager()
                );
    }
}
