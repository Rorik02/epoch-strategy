
package com.epochstrategy.core.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class WatershedCalculator {

    private static final int[][] DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private WatershedCalculator() {
    }

    public static DrainageMap calculate(WorldMap map) {

        Objects.requireNonNull(map, "map");

        int width = map.getWidth();
        int height = map.getHeight();

        int[][] distance = new int[width][height];
        int[][] downstreamX = new int[width][height];
        int[][] downstreamY = new int[width][height];
        int[][] catchment = new int[width][height];

        ArrayDeque<Point> queue = new ArrayDeque<>();
        List<Point> discoveredLand = new ArrayList<>();

        // Initialize drainage data
        for (int x = 0; x < width; x++) {

            Arrays.fill(distance[x], -1);
            Arrays.fill(downstreamX[x], -1);
            Arrays.fill(downstreamY[x], -1);

            for (int y = 0; y < height; y++) {

                TerrainType terrain =
                        map.getTile(x, y).getTerrain();

                if (terrain == TerrainType.OCEAN
                        || terrain == TerrainType.LAKE) {

                    distance[x][y] = 0;
                    queue.addLast(new Point(x, y));
                }
            }
        }

        // Calculate downhill drainage paths
        while (!queue.isEmpty()) {

            Point current = queue.removeFirst();

            int x = current.x();
            int y = current.y();

            int currentElevation =
                    map.getTile(x, y).getElevation();

            for (int[] direction : DIRECTIONS) {

                int nx = x + direction[0];
                int ny = y + direction[1];

                if (!map.isInsideMap(nx, ny)) {
                    continue;
                }

                if (distance[nx][ny] != -1) {
                    continue;
                }

                Tile neighbor = map.getTile(nx, ny);

                // No uphill flow from neighbor to current
                if (neighbor.getElevation()
                        < currentElevation) {
                    continue;
                }

                distance[nx][ny] = distance[x][y] + 1;

                // Neighbor drains towards current
                downstreamX[nx][ny] = x;
                downstreamY[nx][ny] = y;

                discoveredLand.add(new Point(nx, ny));
                queue.addLast(new Point(nx, ny));
            }
        }

        // Every connected land tile contributes one unit
        for (Point point : discoveredLand) {

            catchment[point.x()][point.y()] = 1;
        }

        // Accumulate water from upstream to downstream.
        // Reverse BFS order guarantees upstream tiles first.
        for (int i = discoveredLand.size() - 1; i >= 0; i--) {

            Point point = discoveredLand.get(i);

            int x = point.x();
            int y = point.y();

            int nextX = downstreamX[x][y];
            int nextY = downstreamY[x][y];

            if (nextX == -1 || nextY == -1) {
                continue;
            }

            catchment[nextX][nextY] += catchment[x][y];
        }

        return new DrainageMap(
                distance,
                downstreamX,
                downstreamY,
                catchment
        );
    }

    private record Point(int x, int y) {
    }
}
