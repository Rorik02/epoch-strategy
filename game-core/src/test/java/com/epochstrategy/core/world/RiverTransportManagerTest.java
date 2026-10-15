
package com.epochstrategy.core.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiverTransportManagerTest {

    @Test
    void shouldRegisterMultipleUnits() {

        RiverTransportManager manager =
                new RiverTransportManager();

        RiverTransportUnit boat =
                new RiverTransportUnit(
                        1L,
                        RiverVesselType.SMALL_BOAT,
                        10,
                        20
                );

        RiverTransportUnit barge =
                new RiverTransportUnit(
                        2L,
                        RiverVesselType.CARGO_BARGE,
                        30,
                        40
                );

        manager.registerUnit(boat);
        manager.registerUnit(barge);

        assertEquals(2, manager.getUnitCount());

        assertSame(
                boat,
                manager.findUnit(1L).orElseThrow()
        );

        assertSame(
                barge,
                manager.findUnit(2L).orElseThrow()
        );
    }

    @Test
    void shouldRejectDuplicateUnitId() {

        RiverTransportManager manager =
                new RiverTransportManager();

        RiverTransportUnit first =
                new RiverTransportUnit(
                        1L,
                        RiverVesselType.SMALL_BOAT,
                        10,
                        20
                );

        RiverTransportUnit second =
                new RiverTransportUnit(
                        1L,
                        RiverVesselType.RIVER_BOAT,
                        15,
                        25
                );

        manager.registerUnit(first);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.registerUnit(second)
        );

        assertEquals(1, manager.getUnitCount());
    }

    @Test
    void shouldRemoveUnit() {

        RiverTransportManager manager =
                new RiverTransportManager();

        manager.registerUnit(
                new RiverTransportUnit(
                        5L,
                        RiverVesselType.RIVER_BOAT,
                        10,
                        10
                )
        );

        assertTrue(manager.removeUnit(5L));

        assertEquals(0, manager.getUnitCount());

        assertTrue(manager.findUnit(5L).isEmpty());
    }

    @Test
    void shouldAssignTransportRoute() {

        RiverTransportUnit unit =
                new RiverTransportUnit(
                        1L,
                        RiverVesselType.SMALL_BOAT,
                        5,
                        5
                );

        RiverRouteFinder.RiverRoute route =
                new RiverRouteFinder.RiverRoute(
                        List.of(
                                new RiverRouteFinder.Point(5, 5),
                                new RiverRouteFinder.Point(5, 6),
                                new RiverRouteFinder.Point(5, 7)
                        ),
                        2,
                        0
                );

        unit.assignRoute(route);

        assertTrue(unit.hasActiveRoute());
        assertEquals(2, unit.getRemainingSegments());
    }
}
