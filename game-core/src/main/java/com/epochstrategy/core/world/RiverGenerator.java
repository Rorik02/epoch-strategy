
package com.epochstrategy.core.world;

import java.util.Objects;
import java.util.Random;

public final class RiverGenerator {

    private RiverGenerator() {
    }

    public static RiverNetwork generate(
            WorldMap map,
            long seed
    ) {

        Objects.requireNonNull(map, "map");

        DrainageMap drainageMap =
                WatershedCalculator.calculate(map);

        return generate(map, drainageMap, seed);
    }

    public static RiverNetwork generate(
            WorldMap map,
            DrainageMap drainageMap,
            long seed
    ) {

        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(drainageMap, "drainageMap");

        RiverNetwork network = new RiverNetwork(
                map.getWidth(),
                map.getHeight()
        );

        Random random = new Random(seed);

        // Minimum catchment required for a visible stream
        int minVisibleCatchment = 12 + random.nextInt(4);

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {

                Tile tile = map.getTile(x, y);

                TerrainType terrain = tile.getTerrain();

                // Rivers are generated from land tiles
                if (terrain == TerrainType.OCEAN
                        || terrain == TerrainType.LAKE) {
                    continue;
                }

                // The tile must have a drainage path
                if (!drainageMap.hasDrainagePath(x, y)) {
                    continue;
                }

                int catchmentArea =
                        drainageMap.getCatchmentArea(x, y);

                // Very small streams remain invisible
                if (catchmentArea < minVisibleCatchment) {
                    continue;
                }

                int nextX =
                        drainageMap.getDownstreamX(x, y);

                int nextY =
                        drainageMap.getDownstreamY(x, y);

                if (!map.isInsideMap(nextX, nextY)) {
                    continue;
                }

                // Only connect neighboring tiles
                int distance =
                        Math.abs(nextX - x)
                                + Math.abs(nextY - y);

                if (distance != 1) {
                    throw new IllegalStateException(
                            "Invalid drainage connection"
                    );
                }

                int currentElevation =
                        tile.getElevation();

                int nextElevation =
                        map.getTile(
                                nextX,
                                nextY
                        ).getElevation();

                if (nextElevation > currentElevation) {
                    throw new IllegalStateException(
                            "River flows uphill"
                    );
                }

                network.addSegment(
                        new RiverNetwork.RiverSegment(
                                x,
                                y,
                                nextX,
                                nextY
                        )
                );
            }
        }

        // Calculate river flow, width, depth and navigation
        RiverFlowCalculator.calculate(
                map,
                network,
                drainageMap
        );

        System.out.println(
                "Minimum visible catchment: "
                        + minVisibleCatchment
        );

        return network;
    }
}
