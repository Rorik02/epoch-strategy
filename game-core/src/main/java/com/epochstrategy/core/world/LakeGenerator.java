
package com.epochstrategy.core.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class LakeGenerator {

    private static final int MIN_LAKE_SIZE = 2;
    private static final int MAX_LAKE_SIZE = 4;

    private static final int COAST_BUFFER = 2;

    private static final int[][] DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private LakeGenerator() {
    }

    public static int generate(
            WorldMap map,
            long seed
    ) {

        Objects.requireNonNull(map, "map");

        Random random = new Random(seed);

        List<Point> candidates = new ArrayList<>();

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {

                if (canBecomeLake(map, x, y)) {
                    candidates.add(new Point(x, y));
                }
            }
        }

        Collections.shuffle(candidates, random);


        int targetLakes = Math.max(
                1,
                (map.getWidth() * map.getHeight()) / 900
        );


        int generatedLakes = 0;

        for (Point candidate : candidates) {

            if (generatedLakes >= targetLakes) {
                break;
            }

            if (!canBecomeLake(
                    map,
                    candidate.x(),
                    candidate.y()
            )) {
                continue;
            }

            List<Point> lakeTiles = new ArrayList<>();
            lakeTiles.add(candidate);

            int lakeSize = MIN_LAKE_SIZE
                    + random.nextInt(
                    MAX_LAKE_SIZE - MIN_LAKE_SIZE + 1
            );

            while (lakeTiles.size() < lakeSize) {

                List<Point> possibleTiles =
                        new ArrayList<>();

                for (Point current : lakeTiles) {

                    for (int[] direction : DIRECTIONS) {

                        int nx = current.x() + direction[0];
                        int ny = current.y() + direction[1];

                        Point next = new Point(nx, ny);

                        if (!lakeTiles.contains(next)
                                && canBecomeLake(map, nx, ny)
                                && !possibleTiles.contains(next)) {

                            possibleTiles.add(next);
                        }
                    }
                }

                if (possibleTiles.isEmpty()) {
                    break;
                }

                Point next = possibleTiles.get(
                        random.nextInt(possibleTiles.size())
                );

                lakeTiles.add(next);
            }

            if (lakeTiles.size() < MIN_LAKE_SIZE) {
                continue;
            }

            // Convert selected land tiles into freshwater
            for (Point point : lakeTiles) {

                map.setTile(
                        new Tile(
                                point.x(),
                                point.y(),
                                TerrainType.LAKE,
                                0
                        )
                );
            }

            generatedLakes++;
        }

        return generatedLakes;
    }

    private static boolean canBecomeLake(
            WorldMap map,
            int x,
            int y
    ) {

        if (!map.isInsideMap(x, y)) {
            return false;
        }

        Tile tile = map.getTile(x, y);

        // Lakes initially form only on low land
        if (tile.getTerrain() != TerrainType.PLAINS
                && tile.getTerrain() != TerrainType.FOREST) {
            return false;
        }

        // Keep a land buffer between lakes and oceans
        for (int dx = -COAST_BUFFER;
             dx <= COAST_BUFFER;
             dx++) {

            for (int dy = -COAST_BUFFER;
                 dy <= COAST_BUFFER;
                 dy++) {

                if (Math.abs(dx) + Math.abs(dy)
                        > COAST_BUFFER) {
                    continue;
                }

                int nx = x + dx;
                int ny = y + dy;

                if (!map.isInsideMap(nx, ny)) {
                    return false;
                }

                TerrainType terrain =
                        map.getTile(nx, ny).getTerrain();

                if (terrain == TerrainType.OCEAN
                        || terrain == TerrainType.LAKE) {
                    return false;
                }
            }
        }

        return true;
    }

    private record Point(int x, int y) {
    }
}
