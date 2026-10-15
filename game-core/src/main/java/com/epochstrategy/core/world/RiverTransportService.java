
package com.epochstrategy.core.world;

import java.util.Objects;

public final class RiverTransportService {

    private RiverTransportService() {
    }

    public static MovementResult advanceTurn(
            WorldMap map,
            RiverNetwork network,
            RiverTransportUnit unit
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(unit, "unit");

        if (!unit.hasActiveRoute()) {
            return new MovementResult(
                    0,
                    unit.getX(),
                    unit.getY(),
                    false,
                    false,
                    0
            );
        }

        WaterwayGraph graph = WaterwayGraph.build(
                map,
                network,
                unit.getVesselType()
        );

        return advanceTurn(unit, graph);
    }

    static MovementResult advanceTurn(
            RiverTransportUnit unit,
            WaterwayGraph graph
    ) {

        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(graph, "graph");

        if (!unit.hasActiveRoute()) {
            return new MovementResult(
                    0,
                    unit.getX(),
                    unit.getY(),
                    false,
                    false,
                    0
            );
        }

        int movementPoints =
                unit.getVesselType()
                        .getMovementPointsPerTurn();

        WaterwayMovementService.MovementResult movement =
                WaterwayMovementService.advanceTurn(
                        graph,
                        unit.getAssignedRoutePoints(),
                        unit.getNextPointIndex(),
                        movementPoints
                );

        for (int i = 0;
             i < movement.traveledSegments();
             i++) {

            unit.moveToNextPoint();
        }

        return new MovementResult(
                movement.traveledSegments(),
                unit.getX(),
                unit.getY(),
                movement.traveledSegments() > 0
                        && !unit.hasActiveRoute(),
                movement.blocked(),
                unit.getRemainingSegments()
        );
    }

    public record MovementResult(
            int traveledSegments,
            int currentX,
            int currentY,
            boolean arrived,
            boolean blocked,
            int remainingSegments
    ) {
    }
}
