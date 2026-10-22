
package com.epochstrategy.core.unit;

import com.epochstrategy.core.civilization.Civilization;
import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LandMovementServiceTest {

    private final LandMovementService movement =
            new LandMovementService();

    @Test
    void movesTwoTilesAndConsumesMovementPoints() {

        WorldMap map = createMap();
        LandUnitManager units = new LandUnitManager();

        LandUnit settler = createSettler(units, map, 3, 3);

        var result = movement.moveUnit(
                settler.getId(), 5, 3, map, units
        );

        assertEquals(5, settler.getX());
        assertEquals(3, settler.getY());
        assertEquals(2, result.cost());
        assertEquals(0, settler.getMovementPoints());
    }

    @Test
    void cannotMoveBeyondAvailableMovementPoints() {

        WorldMap map = createMap();
        LandUnitManager units = new LandUnitManager();

        LandUnit settler = createSettler(units, map, 3, 3);

        assertThrows(
                IllegalArgumentException.class,
                () -> movement.moveUnit(
                        settler.getId(),
                        6, 3,
                        map,
                        units
                )
        );

        assertEquals(3, settler.getX());
        assertEquals(2, settler.getMovementPoints());
    }

    @Test
    void cannotCrossImpassableTerrain() {

        WorldMap map = createMap();

        map.setTile(new Tile(
                4, 3, TerrainType.OCEAN, 1
        ));

        LandUnitManager units = new LandUnitManager();
        LandUnit settler = createSettler(units, map, 3, 3);

        // The direct path is blocked.
        // The shortest detour requires more than 2 moves.
        assertThrows(
                IllegalArgumentException.class,
                () -> movement.moveUnit(
                        settler.getId(),
                        5, 3,
                        map,
                        units
                )
        );
    }

    @Test
    void cannotEnterTileOccupiedByAnotherUnit() {

        WorldMap map = createMap();
        LandUnitManager units = new LandUnitManager();

        LandUnit settler = createSettler(units, map, 3, 3);

        units.createSettler(
                2L, 4, 3, map, createCivilizations()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> movement.moveUnit(
                        settler.getId(),
                        4, 3,
                        map,
                        units
                )
        );
    }

    @Test
    void canMoveAgainAfterMovementPointsRestore() {

        WorldMap map = createMap();
        LandUnitManager units = new LandUnitManager();

        LandUnit settler = createSettler(units, map, 3, 3);

        movement.moveUnit(
                settler.getId(), 5, 3, map, units
        );

        assertFalse(settler.canMove());

        units.restoreAllMovementPoints();

        assertEquals(2, settler.getMovementPoints());

        movement.moveUnit(
                settler.getId(), 6, 3, map, units
        );

        assertEquals(6, settler.getX());
        assertEquals(1, settler.getMovementPoints());
    }

    private LandUnit createSettler(
            LandUnitManager units,
            WorldMap map,
            int x,
            int y
    ) {

        return units.createSettler(
                1L,
                x,
                y,
                map,
                createCivilizations()
        );
    }

    private CivilizationManager createCivilizations() {

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

    private WorldMap createMap() {

        WorldMap map = new WorldMap(8, 8);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {

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
