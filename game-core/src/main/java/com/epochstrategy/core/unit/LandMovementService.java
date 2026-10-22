
package com.epochstrategy.core.unit;

import com.epochstrategy.core.world.WorldMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public final class LandMovementService {

    private static final int[][] DIRECTIONS = {
            {1, 0},
            {0, 1},
            {-1, 0},
            {0, -1}
    };

    public record ReachableTile(
            int x,
            int y,
            int cost
    ) {}

    public record MovementResult(
            long unitId,
            int x,
            int y,
            int cost,
            int remainingMovementPoints
    ) {}

    public List<ReachableTile> getReachableTiles(
            long unitId,
            WorldMap worldMap,
            LandUnitManager unitManager
    ) {

        Objects.requireNonNull(worldMap, "worldMap");
        Objects.requireNonNull(unitManager, "unitManager");

        LandUnit unit = unitManager.findUnit(unitId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown unit: " + unitId
                        )
                );

        if (!unit.canMove()) {
            return List.of();
        }

        int width = worldMap.getWidth();
        int height = worldMap.getHeight();

        int[][] distances = new int[width][height];

        for (int[] row : distances) {
            Arrays.fill(row, -1);
        }

        Queue<int[]> queue = new ArrayDeque<>();

        distances[unit.getX()][unit.getY()] = 0;

        queue.add(new int[]{
                unit.getX(),
                unit.getY()
        });

        List<ReachableTile> reachable =
                new ArrayList<>();

        while (!queue.isEmpty()) {

            int[] current = queue.remove();

            int x = current[0];
            int y = current[1];

            int currentCost = distances[x][y];

            for (int[] direction : DIRECTIONS) {

                int nextX = x + direction[0];
                int nextY = y + direction[1];

                if (!worldMap.isInsideMap(
                        nextX,
                        nextY
                )) {
                    continue;
                }

                if (distances[nextX][nextY] != -1) {
                    continue;
                }

                int nextCost = currentCost + 1;

                if (nextCost > unit.getMovementPoints()) {
                    continue;
                }

                if (!worldMap.getTile(nextX, nextY)
                        .isPassableForLandUnits()) {
                    continue;
                }

                if (unitManager.findUnitAt(
                        nextX,
                        nextY
                ).isPresent()) {
                    continue;
                }

                distances[nextX][nextY] = nextCost;

                reachable.add(
                        new ReachableTile(
                                nextX,
                                nextY,
                                nextCost
                        )
                );

                queue.add(new int[]{
                        nextX,
                        nextY
                });
            }
        }

        return List.copyOf(reachable);
    }

    public MovementResult moveUnit(
            long unitId,
            int targetX,
            int targetY,
            WorldMap worldMap,
            LandUnitManager unitManager
    ) {

        LandUnit unit = unitManager.findUnit(unitId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown unit: " + unitId
                        )
                );

        ReachableTile destination =
                getReachableTiles(
                        unitId,
                        worldMap,
                        unitManager
                )
                        .stream()
                        .filter(tile ->
                                tile.x() == targetX
                                        && tile.y() == targetY
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Destination is not reachable"
                                )
                        );

        unit.moveTo(
                targetX,
                targetY,
                destination.cost()
        );

        return new MovementResult(
                unit.getId(),
                unit.getX(),
                unit.getY(),
                destination.cost(),
                unit.getMovementPoints()
        );
    }
}
