
package com.epochstrategy.core.unit;

import com.epochstrategy.core.city.City;
import com.epochstrategy.core.civilization.Civilization;
import com.epochstrategy.core.economy.ResourceType;
import com.epochstrategy.core.engine.GameState;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettlerRecruitmentServiceTest {

    private final SettlerRecruitmentService service =
            new SettlerRecruitmentService();

    @Test
    void recruitsSettlerAndConsumesResources() {

        TestWorld world = createWorld();

        LandUnit settler = service.recruitSettler(
                1L,
                world.state(),
                world.map()
        );

        assertEquals(LandUnitType.SETTLER, settler.getType());

        assertEquals(1L, settler.getCivilizationId());
        assertEquals(5, settler.getX());
        assertEquals(4, settler.getY());

        assertEquals(2, settler.getMovementPoints());

        assertEquals(400, world.city().getPopulation());

        assertEquals(
                30,
                world.city().getInventory()
                        .getAmount(ResourceType.WOOD)
        );

        assertEquals(
                35,
                world.city().getInventory()
                        .getAmount(ResourceType.FOOD)
        );

        assertEquals(
                1,
                world.state().getLandUnitManager()
                        .getUnitCount()
        );
    }

    @Test
    void rejectsRecruitmentWithoutEnoughWood() {

        TestWorld world = createWorld();

        world.city().getInventory()
                .remove(ResourceType.WOOD, 45);

        var check = service.checkRecruitment(
                1L,
                world.state(),
                world.map()
        );

        assertFalse(check.allowed());

        assertThrows(
                IllegalStateException.class,
                () -> service.recruitSettler(
                        1L,
                        world.state(),
                        world.map()
                )
        );

        assertEquals(500, world.city().getPopulation());
        assertEquals(0,
                world.state().getLandUnitManager()
                        .getUnitCount());
    }

    @Test
    void rejectsRecruitmentWithLowPopulation() {

        TestWorld world = createWorld();

        world.city().setPopulation(250);

        var check = service.checkRecruitment(
                1L,
                world.state(),
                world.map()
        );

        assertFalse(check.allowed());

        assertEquals(
                250,
                world.city().getPopulation()
        );
    }

    @Test
    void cannotRecruitWithoutFreeAdjacentTile() {

        TestWorld world = createWorld();

        int[][] adjacentTiles = {
                {5, 4},
                {4, 5},
                {3, 4},
                {4, 3},
                {5, 5},
                {3, 5},
                {5, 3},
                {3, 3}
        };

        // Occupy every neighboring tile.
        for (int[] position : adjacentTiles) {

            world.state().getLandUnitManager()
                    .createSettler(
                            1L,
                            position[0],
                            position[1],
                            world.map(),
                            world.state()
                                    .getCivilizationManager()
                    );
        }

        var check = service.checkRecruitment(
                1L,
                world.state(),
                world.map()
        );

        assertFalse(check.allowed());

        assertThrows(
                IllegalStateException.class,
                () -> service.recruitSettler(
                        1L,
                        world.state(),
                        world.map()
                )
        );

        // Failed recruitment does not spend resources.
        assertEquals(500, world.city().getPopulation());

        assertEquals(
                50,
                world.city().getInventory()
                        .getAmount(ResourceType.WOOD)
        );

        assertEquals(
                50,
                world.city().getInventory()
                        .getAmount(ResourceType.FOOD)
        );

        assertEquals(
                8,
                world.state().getLandUnitManager()
                        .getUnitCount()
        );
    }

    @Test
    void recruitsSecondSettlerOnAnotherTile() {

        TestWorld world = createWorld();

        LandUnit first = service.recruitSettler(
                1L,
                world.state(),
                world.map()
        );

        LandUnit second = service.recruitSettler(
                1L,
                world.state(),
                world.map()
        );

        assertNotEquals(first.getId(), second.getId());

        assertFalse(
                first.getX() == second.getX()
                        && first.getY() == second.getY()
        );

        assertEquals(300, world.city().getPopulation());

        assertEquals(
                2,
                world.state().getLandUnitManager()
                        .getUnitCount()
        );
    }

    @Test
    void requiresOwnCityTerritoryForSpawning() {

        TestWorld world = createWorld();

        // Remove territorial claims.
        world.state().getTerritoryManager().clear();

        var check = service.checkRecruitment(
                1L,
                world.state(),
                world.map()
        );

        assertFalse(check.allowed());

        assertEquals(
                "No free adjacent territory",
                check.reason()
        );
    }

    private record TestWorld(
            GameState state,
            WorldMap map,
            City city
    ) {
    }

    private TestWorld createWorld() {

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

        GameState state = new GameState();

        state.getCivilizationManager()
                .registerCivilization(
                        new Civilization(
                                1L,
                                "Rome",
                                0xB22222
                        )
                );

        City city = new City(
                1L,
                "Rome",
                4,
                4,
                500,
                1L
        );

        state.getCityManager()
                .registerCity(city);

        city.getInventory().add(
                ResourceType.WOOD, 50
        );

        city.getInventory().add(
                ResourceType.FOOD, 50
        );

        // Initial city territory: 3x3.
        for (int x = 3; x <= 5; x++) {
            for (int y = 3; y <= 5; y++) {

                state.getTerritoryManager()
                        .claimTile(
                                x, y,
                                1L,
                                city.getId()
                        );
            }
        }

        return new TestWorld(state, map, city);
    }
}
