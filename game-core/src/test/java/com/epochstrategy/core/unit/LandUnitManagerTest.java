
package com.epochstrategy.core.unit;

import com.epochstrategy.core.civilization.Civilization;
import com.epochstrategy.core.civilization.CivilizationManager;
import com.epochstrategy.core.world.TerrainType;
import com.epochstrategy.core.world.Tile;
import com.epochstrategy.core.world.WorldMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LandUnitManagerTest {

    @Test
    void createsSettlerWithMovementPoints() {

        LandUnitManager manager = new LandUnitManager();

        LandUnit settler = manager.createSettler(
                1L,
                3, 3,
                createMap(),
                createCivilizations()
        );

        assertEquals(1L, settler.getId());
        assertEquals(1L, settler.getCivilizationId());
        assertEquals(LandUnitType.SETTLER, settler.getType());

        assertEquals(3, settler.getX());
        assertEquals(3, settler.getY());

        assertEquals(2, settler.getMovementPoints());
        assertEquals(2, settler.getMaxMovementPoints());
        assertTrue(settler.canMove());

        assertEquals(1, manager.getUnitCount());
    }

    @Test
    void preventsTwoUnitsOnSameTile() {

        LandUnitManager manager = new LandUnitManager();

        WorldMap map = createMap();
        CivilizationManager civilizations =
                createCivilizations();

        manager.createSettler(
                1L, 3, 3, map, civilizations
        );

        assertThrows(
                IllegalStateException.class,
                () -> manager.createSettler(
                        2L, 3, 3, map, civilizations
                )
        );

        assertEquals(1, manager.getUnitCount());
    }

    @Test
    void rejectsImpassableTerrain() {

        WorldMap map = createMap();

        map.setTile(
                new Tile(
                        3, 3,
                        TerrainType.OCEAN,
                        1
                )
        );

        LandUnitManager manager = new LandUnitManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.createSettler(
                        1L,
                        3, 3,
                        map,
                        createCivilizations()
                )
        );

        assertEquals(0, manager.getUnitCount());
    }

    @Test
    void rejectsUnknownCivilization() {

        LandUnitManager manager = new LandUnitManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.createSettler(
                        999L,
                        3, 3,
                        createMap(),
                        createCivilizations()
                )
        );
    }

    @Test
    void restoresMovementPoints() {

        LandUnitManager manager = new LandUnitManager();

        LandUnit settler = manager.createSettler(
                1L,
                3, 3,
                createMap(),
                createCivilizations()
        );

        // Simulates validated movement, which will
        // be provided by a service in the next stage.
        settler.moveTo(4, 3, 1);

        assertEquals(1, settler.getMovementPoints());

        settler.moveTo(4, 4, 1);

        assertEquals(0, settler.getMovementPoints());
        assertFalse(settler.canMove());

        manager.restoreAllMovementPoints();

        assertEquals(2, settler.getMovementPoints());
        assertTrue(settler.canMove());
    }

    @Test
    void filtersUnitsByCivilization() {

        LandUnitManager manager = new LandUnitManager();

        WorldMap map = createMap();
        CivilizationManager civilizations =
                createCivilizations();

        manager.createSettler(
                1L, 2, 2, map, civilizations
        );

        manager.createSettler(
                2L, 5, 5, map, civilizations
        );

        assertEquals(
                1,
                manager.getUnitsByCivilization(1L).size()
        );

        assertEquals(
                1,
                manager.getUnitsByCivilization(2L).size()
        );

        assertEquals(
                2,
                manager.getUnitCount()
        );
    }

    @Test
    void returnsUnmodifiableUnitList() {

        LandUnitManager manager = new LandUnitManager();

        manager.createSettler(
                1L,
                3, 3,
                createMap(),
                createCivilizations()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> manager.getUnits().clear()
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

    private static WorldMap createMap() {

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
