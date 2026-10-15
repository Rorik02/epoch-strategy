
package com.epochstrategy.core.world;

import java.util.List;
import java.util.Objects;

public final class WaterwayMovementService {

    private WaterwayMovementService() {
    }

    public record MovementResult(
            int nextPointIndex,
            int currentX,
            int currentY,
            int traveledSegments,
            int usedMovementPoints,
            int remainingSegments,
            boolean arrived,
            boolean blocked
    ) {
    }

    public static MovementResult advanceTurn(
            WaterwayGraph graph,
            WaterwayFastestRouteFinder.WaterwayRoute route,
            int nextPointIndex,
            int movementPoints
    ) {

        Objects.requireNonNull(route, "route");

        return advanceTurn(
                graph,
                route.points(),
                nextPointIndex,
                movementPoints
        );
    }

    public static MovementResult advanceTurn(
            WaterwayGraph graph,
            List<RiverRouteFinder.Point> points,
            int nextPointIndex,
            int movementPoints
    ) {

        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(points, "points");

        if (points.isEmpty()) {
            throw new IllegalArgumentException(
                    "Route cannot be empty"
            );
        }

        if (nextPointIndex < 1
                || nextPointIndex > points.size()) {

            throw new IllegalArgumentException(
                    "Invalid route progress index"
            );
        }

        if (movementPoints < 0) {
            throw new IllegalArgumentException(
                    "Movement points cannot be negative"
            );
        }

        int remainingMovementPoints = movementPoints;
        int traveledSegments = 0;
        boolean blocked = false;

        while (nextPointIndex < points.size()) {

            RiverRouteFinder.Point current =
                    points.get(nextPointIndex - 1);

            RiverRouteFinder.Point next =
                    points.get(nextPointIndex);

            var connection =
                    graph.findConnection(current, next);

            if (connection.isEmpty()) {
                blocked = true;
                break;
            }

            int movementCost =
                    connection.get().movementCost();

            if (movementCost <= 0) {
                throw new IllegalStateException(
                        "Invalid waterway movement cost"
                );
            }

            if (movementCost > remainingMovementPoints) {
                break;
            }

            remainingMovementPoints -= movementCost;

            nextPointIndex++;
            traveledSegments++;
        }

        RiverRouteFinder.Point position =
                points.get(nextPointIndex - 1);

        int remainingSegments =
                points.size() - nextPointIndex;

        boolean arrived = remainingSegments == 0;

        return new MovementResult(
                nextPointIndex,
                position.x(),
                position.y(),
                traveledSegments,
                movementPoints - remainingMovementPoints,
                remainingSegments,
                arrived,
                blocked
        );
    }
}
