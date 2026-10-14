
package com.epochstrategy.core.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class RiverGenerator {

    private static final int[][] DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private RiverGenerator() {
    }

    public static RiverNetwork generate(
            WorldMap map,
            long seed
    ) {

        Objects.requireNonNull(map, "map");

        Random random = new Random(seed);

        RiverNetwork network = new RiverNetwork(
                map.getWidth(),
                map.getHeight()
        );

        // Find valid downhill routes to water
        int[][] drainageDistance =
                calculateDrainageDistance(map);

        List<int[]> sources = new ArrayList<>();

        // Find potential river sources
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {

                Tile tile = map.getTile(x, y);

                boolean elevated =
                        tile.getTerrain() == TerrainType.HILLS
                                || tile.getTerrain() == TerrainType.MOUNTAINS;

                if (elevated && drainageDistance[x][y] >= 3) {
                    sources.add(new int[]{x, y});
                }
            }
        }

        Collections.shuffle(sources, random);

        int targetRivers = Math.max(
                2,
                map.getWidth() * map.getHeight() / 180
        );

        int generatedRivers = 0;

        for (int[] source : sources) {

            if (generatedRivers >= targetRivers) {
                break;
            }

            int x = source[0];
            int y = source[1];

            if (network.hasRiverAt(x, y)) {
                continue;
            }

            boolean generated = false;

            while (drainageDistance[x][y] > 0) {

                int nextX = -1;
                int nextY = -1;

                double bestScore = Double.MAX_VALUE;

                int currentElevation =
                        map.getTile(x, y).getElevation();

                for (int[] direction : DIRECTIONS) {

                    int nx = x + direction[0];
                    int ny = y + direction[1];

                    if (!map.isInsideMap(nx, ny)) {
                        continue;
                    }

                    // Move closer to a valid water outlet
                    if (drainageDistance[nx][ny]
                            != drainageDistance[x][y] - 1) {
                        continue;
                    }

                    Tile nextTile = map.getTile(nx, ny);

                    // A river cannot flow uphill
                    if (nextTile.getElevation()
                            > currentElevation) {
                        continue;
                    }

                    // Prefer lower terrain
                    double score =
                            nextTile.getElevation() * 10.0
                                    + random.nextDouble();

                    if (score < bestScore) {
                        bestScore = score;
                        nextX = nx;
                        nextY = ny;
                    }
                }

                if (nextX == -1) {
                    break;
                }

                boolean joinsExistingRiver =
                        network.hasRiverAt(nextX, nextY);

                network.addSegment(
                        new RiverNetwork.RiverSegment(
                                x,
                                y,
                                nextX,
                                nextY
                        )
                );

                generated = true;

                x = nextX;
                y = nextY;

                if (drainageDistance[x][y] == 0
                        || joinsExistingRiver) {
                    break;
                }
            }

            if (generated) {
                generatedRivers++;
            }
        }

        return network;
    }

    private static int[][] calculateDrainageDistance(
            WorldMap map
    ) {

        int width = map.getWidth();
        int height = map.getHeight();

        int[][] distance = new int[width][height];

        ArrayDeque<int[]> queue = new ArrayDeque<>();

        // Start from all oceans and lakes
        for (int x = 0; x < width; x++) {

            Arrays.fill(distance[x], -1);

            for (int y = 0; y < height; y++) {

                TerrainType terrain =
                        map.getTile(x, y).getTerrain();

                if (terrain == TerrainType.OCEAN
                        || terrain == TerrainType.LAKE) {

                    distance[x][y] = 0;
                    queue.addLast(new int[]{x, y});
                }
            }
        }

        // Explore backwards from water.
        // A neighboring tile can drain into the current
        // tile only when it is equally high or higher.
        while (!queue.isEmpty()) {

            int[] current = queue.removeFirst();

            int x = current[0];
            int y = current[1];

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

                // Reject routes requiring uphill flow
                if (neighbor.getElevation()
                        < currentElevation) {
                    continue;
                }

                distance[nx][ny] = distance[x][y] + 1;

                queue.addLast(new int[]{nx, ny});
            }
        }

        return distance;
    }
}
